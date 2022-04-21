package gallery.listeners;

import gallery.Gallery;
import gallery.util.Color;
import lombok.SneakyThrows;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeEqualityPredicate;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.entity.Player;
import pl.islandworld.Config;
import pl.islandworld.IslandWorld;
import pl.islandworld.entity.SimpleIsland;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static pl.islandworld.api.IslandWorldApi.getIsland;
import static pl.islandworld.api.IslandWorldApi.haveIsland;

public class GalleryChanger {

    private final Gallery  plugin;

    public GalleryChanger() {
        plugin = Gallery.getInstance();
    }

    public boolean change(String[] args, Player player) throws InterruptedException, ExecutionException {
        LuckPerms lpApi = LuckPermsProvider.get();
        UUID uuidNewPlayer = lpApi.getUserManager().lookupUniqueId(args[1]).get();
        Player oldPlayer = plugin.getServer().getPlayer(args[0]);

        if (uuidNewPlayer == null) {
            player.sendMessage(Color.color(plugin.getConfigManager().getPrefix() +
                    ": Gracz: &f" + args[1] + " &7 nie istnieje."));
            return false;
        }
        if (oldPlayer == null) {
            player.sendMessage(Color.color(plugin.getConfigManager().getPrefix() +
                    ": Gracz: &f" + args[0] + " &7 nie jest online."));
            return false;
        }
        if (!haveIsland(oldPlayer.getName())) {
            player.sendMessage(Color.color(plugin.getConfigManager().getPrefix() +
                    ": Gracz: &f" + args[0] + " &7 nie posiada galerii."));
            return false;
        }
        if (haveIsland(args[1])) {
            player.sendMessage(Color.color(plugin.getConfigManager().getPrefix() +
                    ": Gracz: &f" + args[1] + " &7 posiada już galerię."));
            return false;
        }

        List<Integer> limits = plugin.getConfigManager().getLimits();
        for (Integer limit : limits) {
            int level = limits.size() - limits.indexOf(limit);

            if (plugin.getUtility().whichLimitPlayerHas(oldPlayer, level)) {
                if (changeGallery(player, oldPlayer.getName(), args[1], level, lpApi)) {
                    return true;
                } else {
                    player.sendMessage(Color.color(plugin.getConfigManager().getPrefix() + ": Błąd podczas przenoszenia galerii."));
                    return false;
                }
            }
        }

        player.sendMessage(Color.color(plugin.getConfigManager().getPrefix() +
                ": Gracz: &f" + args[0] + " &7 nie posiada permisji!"));
        return false;
    }

    public boolean changeForced(String[] args, Player player) throws InterruptedException, ExecutionException {
        LuckPerms lpApi = LuckPermsProvider.get();
        UUID uuidNewPlayer = lpApi.getUserManager().lookupUniqueId(args[1]).get();
        UUID uuidOldPlayer = lpApi.getUserManager().lookupUniqueId(args[0]).get();

        if (uuidNewPlayer == null) {
            player.sendMessage(Color.color(plugin.getConfigManager().getPrefix() +
                    ": Gracz: &f" + args[1] + " &7 nie istnieje."));
            return false;
        }
        if (uuidOldPlayer == null) {
            player.sendMessage(Color.color(plugin.getConfigManager().getPrefix() +
                    ": Gracz: &f" + args[0] + " &7  nie istnieje."));
            return false;
        }

        User oldPlayer = lpApi.getUserManager().loadUser(uuidOldPlayer).get();

        if (!haveIsland(args[0])) {
            player.sendMessage(Color.color(plugin.getConfigManager().getPrefix() +
                    ": Gracz: &f" + args[0] + " &7 nie posiada galerii."));
            return false;
        }
        if (haveIsland(args[1])) {
            player.sendMessage(Color.color(plugin.getConfigManager().getPrefix() +
                    ": Gracz: &f" + args[1] + " &7 posiada już galerię."));
            return false;
        }

        QueryOptions queryOptions = oldPlayer.getQueryOptions();
        CachedPermissionData permissionData = oldPlayer.getCachedData().getPermissionData(queryOptions);
        List<Integer> limits = plugin.getConfigManager().getLimits();

        for (Integer limit : limits) {
            int level = limits.size() - limits.indexOf(limit);

            boolean checkResult = permissionData.checkPermission(plugin.getConfigManager().getPermission() + "." + level).asBoolean();

            if (checkResult) {
                if (changeGallery(player, Objects.requireNonNull(oldPlayer.getUsername()), args[1], level, lpApi)) {
                    return true;
                } else {
                    player.sendMessage(Color.color(plugin.getConfigManager().getPrefix() + ": Błąd podczas przenoszenia galerii."));
                    return false;
                }
            }
        }


        player.sendMessage(Color.color(plugin.getConfigManager().getPrefix() +
                ": Gracz: &f" + args[0] + " &7 nie posiada permisji!"));

        return false;
    }

    private boolean changeGallery(Player player, String oldPlayer, String newPlayer, int level, LuckPerms lpApi) {
        if (!changeIsland(oldPlayer.toLowerCase(), newPlayer, lpApi)) {
            player.sendMessage(Color.color(plugin.getConfigManager().getPrefix() + ": Błąd związany z zmianą właściciela."));
            return false;
        }
        if (!changePermission(oldPlayer, newPlayer, level, lpApi)) {
            player.sendMessage(Color.color(plugin.getConfigManager().getPrefix() + ": Błąd związany z permisjami."));

            return false;
        }
        if (!changeFrames(oldPlayer, newPlayer)) {
            player.sendMessage(Color.color(plugin.getConfigManager().getPrefix() + ": Błąd związany z ramkami."));
            return false;
        }
        player.sendMessage(Color.color(plugin.getConfigManager().getPrefix() + ": Poprawnie przeniesiono galerię z &f" + oldPlayer + " &7do&f " + newPlayer));
        return true;
    }

    private boolean changeFrames(String oldPlayer, String newPlayer) {
        int oldPlayerFrames = plugin.getPlayerDao().getLimit(oldPlayer);
        plugin.getPlayerDao().setPlayerLimit(newPlayer, oldPlayerFrames);
        plugin.getPlayerDao().setPlayerLimit(oldPlayer, 0);
        return plugin.getPlayerDao().getLimit(newPlayer) == oldPlayerFrames
                && plugin.getPlayerDao().getLimit(oldPlayer) == 0;
    }

    @SneakyThrows
    private boolean changeIsland(String oldPlayer, String newPlayer, LuckPerms lpApi) {
        SimpleIsland island = getIsland(oldPlayer, false);
        IslandWorld is = IslandWorld.getInstance();
        if (is.getIsleList().remove(Objects.requireNonNull(oldPlayer)) == null) {
            return false;
        }
        UUID oldUuid = island.getOwnerUUID();
        UUID newUuid = lpApi.getUserManager().lookupUniqueId(newPlayer).get();
        if (Config.TRACK_UUID && oldUuid != null && is.getUUIDList().containsKey(oldUuid)) {
            is.getUUIDList().remove(oldUuid);
            is.getUUIDList().put(newUuid, island);
        }
        island.setOwnerUUID(newUuid);
        island.setOwner(newPlayer);
        is.getIsleList().put(newPlayer.toLowerCase(), island);
        is.saveDatFiles();
        is.removePoints(oldPlayer);
        return haveIsland(newPlayer) && !haveIsland(oldPlayer);
    }

    @SneakyThrows
    private boolean changePermission(String oldPlayer, String newPlayer, int level, LuckPerms lpApi) {
        UUID uuidOldUser = lpApi.getUserManager().lookupUniqueId(oldPlayer).get();
        User oldUser = lpApi.getUserManager().loadUser(uuidOldUser).get();
        UUID uuidNewUser = lpApi.getUserManager().lookupUniqueId(newPlayer).get();
        User newUser = lpApi.getUserManager().loadUser(uuidNewUser).get();

        Objects.requireNonNull(oldUser).data()
                .remove(Node.builder("group." + plugin.getConfigManager().getPermission()).build());
        newUser.data().add(Node.builder("group." + plugin.getConfigManager().getPermission()).build());
        if (level != 1) {
            Objects.requireNonNull(oldUser).data()
                    .remove(Node.builder(plugin.getConfigManager().getPermission() + "." + level).build());
            newUser.data().add(Node.builder(plugin.getConfigManager().getPermission() + "." + level).build());
        }

        lpApi.getUserManager().saveUser(oldUser);
        lpApi.getUserManager().saveUser(newUser);

        return !oldUser.data().
                contains(Node.builder("group." + plugin.getConfigManager().getPermission()).build(), NodeEqualityPredicate.EXACT)
                .asBoolean()
                && newUser.data()
                .contains(Node.builder("group." + plugin.getConfigManager().getPermission()).build(), NodeEqualityPredicate.EXACT)
                .asBoolean();
    }

}
