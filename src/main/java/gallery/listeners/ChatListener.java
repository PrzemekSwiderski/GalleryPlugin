package gallery.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.World;
import gallery.Gallery;
import gallery.util.Color;
import lombok.SneakyThrows;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeEqualityPredicate;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pl.islandworld.Config;
import pl.islandworld.IslandWorld;
import pl.islandworld.entity.SimpleIsland;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.*;

import static pl.islandworld.api.IslandWorldApi.*;

public class ChatListener implements CommandExecutor {

    private Gallery plugin;

    public ChatListener() {
        this.plugin = Gallery.getInstance();
    }

    @SneakyThrows
    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return false;
        }

        switch (cmd.getName().toLowerCase()) {
            case "galeria":
                if (args.length == 0) {
                    if (haveIsland(player.getName()) && hasGalleryPermission(player, ".1")) {
                        return teleportToGallery(player);
                    } else if (hasGalleryPermission(player, ".1")) {
                        return createGallery(player);
                    } else {
                        player.sendMessage(Color.color(plugin.getConfigManager().getPrefix() + "A co to? " +
                                "Wygląda na to, że nie posiadasz galerii! " +
                                "Pamiętaj, że galeria to jedyny sposób, żeby zachować przedmioty pomiędzy edycjami! " +
                                "Po więcej szczegółów porozmawiaj z NPC Galeria."));
                        return true;
                    }
                }
                switch (args[0].toLowerCase()) {
                    case "limit":
                        if (!hasGalleryPermission(player, ".admin")) {
                            getCommandGalleryHelpForPlayer(player);
                            return false;
                        }
                        if (args.length != 2) {
                            player.sendMessage(Color.color(plugin.getConfigManager().getPrefix() +
                                    ": Poprawne użycie to &f/galeria limit <player>"));
                            return false;
                        }
                        if (!plugin.getPlayerDao().checkIfPlayerExist(args[1])) {
                            player.sendMessage(Color.color(plugin.getConfigManager().getPrefix() +
                                    ": W bazie danych nie ma podanego gracza."));
                            return false;
                        }
                        player.sendMessage(Color.color(plugin.getConfigManager().getPrefix()
                                + ": Ilość ramek postawionych przez gracza &f"
                                + args[1] + "&7: &2" + plugin.getPlayerDao().getLimit(args[1])));
                        return true;
                    case "setlimit":
                        if (args.length == 3 && hasGalleryPermission(player, ".admin")) {
                            try {
                                Integer.parseInt(args[2]);
                            } catch (NumberFormatException exception) {
                                player.sendMessage(Color.color(plugin.getConfigManager().getPrefix() +
                                        ": Poprawne użycie to &f/galeria setlimit <player> <ilosc>"));
                                return false;
                            }
                            if (plugin.getPlayerDao().setPlayerLimit(args[1], Integer.parseInt(args[2]))) {
                                player.sendMessage(Color.color(plugin.getConfigManager().getPrefix() +
                                        ": Ustawiono ilość postawionych ramek graczowi: &f"
                                        + args[1] + " &7na: &2" + args[2]));
                                return true;
                            } else {
                                player.sendMessage(Color.color(plugin.getConfigManager().getPrefix() +
                                        ": &cWystąpił błąd."));
                            }
                        } else if (hasGalleryPermission(player, ".admin")) {
                            player.sendMessage(Color.color(plugin.getConfigManager().getPrefix() +
                                    ": Poprawne użycie to &f/galeria setlimit <player> <ilosc>"));
                        } else {
                            getCommandGalleryHelpForPlayer(player);
                        }
                        return false;
                    case "create":
                    case "stworz":
                        return createGallery(player);
                    case "calculate":
                        if (args.length == 2 && hasGalleryPermission(player, ".admin")) {
                            loadChunks(args[1]);

                            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                                calculatePlayerFrames(args[1], player);
                                player.sendMessage(Color.color(plugin.getConfigManager().getPrefix()
                                        + ": Gracz &f" + args[1]
                                        + "&7 posiada &f" + plugin.getPlayerDao().getLimit(args[1])
                                        + " ramek w swojej galerii."));
                            }, 20L);

                            return true;
                        } else if (hasGalleryPermission(player, ".admin")) {
                            player.sendMessage(Color.color(plugin.getConfigManager().getPrefix() +
                                    ": Poprawne użycie to &f/galeria calculate <player>"));
                        } else {
                            getCommandGalleryHelpForPlayer(player);
                        }
                        return false;
                    case "calculateall":
                        if (hasGalleryPermission(player, ".admin")) {
                            calculateAllFrames(player);
                            return true;
                        }
                        getCommandGalleryHelpForPlayer(player);
                        return false;
                    case "help":
                        if (hasGalleryPermission(player, ".admin")) {
                            getCommandGalleryHelpForAdmin(player);
                        } else {
                            getCommandGalleryHelpForPlayer(player);
                        }
                        return true;
                    case "tp":
                        if (args.length == 2) {
                            return teleportToGallery(args[1], player);
                        }
                        player.sendMessage(Color.color(plugin.getConfigManager().getPrefix() +
                                ": Poprawne użycie to &f/galeria tp <player>"));
                        return false;
                    case "check":
                        if (hasGalleryPermission(player, ".admin")) {
                            return checkWhichIslandDontHaveOwner(Paths.get("gallerieswithoutOnwers.yml"));
                        }
                        getCommandGalleryHelpForPlayer(player);
                        return true;
                    default:
                        return teleportToGallery(args[0], player);
                }
            case "wyspa":
                if (!player.hasPermission(plugin.getConfigManager().getPermission() + ".admin")) {
                    getCommandGalleryHelpForPlayer(player);
                    return false;
                }
                if (args.length != 2) {
                    player.sendMessage(Color.color(plugin.getConfigManager().getPrefix() +
                            ": Poprawne użycie komendy: &f/wyspa <staryNick> <nowyNick>"));
                    return false;
                }
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
                        if (changeGallery(player, oldPlayer, args[1], level, lpApi)) {
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
            default:
                return false;
        }
    }

    private boolean changeGallery(Player player, Player oldPlayer, String newPlayer, int level, LuckPerms lpApi) {
        if (!changeOwner(oldPlayer, newPlayer, lpApi)) {
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
        player.sendMessage(Color.color(plugin.getConfigManager().getPrefix() + ": Poprawnie przeniesiono galerię z &f" + oldPlayer.getName() + " &7do&f " + newPlayer));
        return true;
    }

    private boolean changeFrames(Player oldPlayer, String newPlayer) {
        int oldPlayerFrames = plugin.getPlayerDao().getLimit(oldPlayer.getName());
        plugin.getPlayerDao().setPlayerLimit(newPlayer, oldPlayerFrames);
        plugin.getPlayerDao().setPlayerLimit(oldPlayer.getName(), 0);
        return plugin.getPlayerDao().getLimit(newPlayer) == oldPlayerFrames
                && plugin.getPlayerDao().getLimit(oldPlayer.getName()) == 0;
    }

    @SneakyThrows
    private boolean changeOwner(Player oldPlayer, String newPlayer, LuckPerms lpApi) {
        SimpleIsland island = getIsland(oldPlayer.getUniqueId());
        IslandWorld is = IslandWorld.getInstance();
        if (is.getIsleList().remove(Objects.requireNonNull(oldPlayer.getName()).toLowerCase()) == null) {
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
        is.removePoints(oldPlayer.getName());
        return haveIsland(newPlayer) && !haveIsland(oldPlayer.getName());
    }


    @SneakyThrows
    private boolean changePermission(Player oldPlayer, String newPlayer, int level, LuckPerms lpApi) {
        User oldUser = lpApi.getUserManager().getUser(oldPlayer.getUniqueId());
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

        return !Objects.requireNonNull(lpApi.getUserManager().getUser(oldPlayer.getUniqueId())).data().
                contains(Node.builder("group." + plugin.getConfigManager().getPermission()).build(), NodeEqualityPredicate.EXACT)
                .asBoolean()
                && lpApi.getUserManager().loadUser(lpApi.getUserManager().lookupUniqueId(newPlayer).get()).get().data()
                .contains(Node.builder("group." + plugin.getConfigManager().getPermission()).build(), NodeEqualityPredicate.EXACT)
                .asBoolean();
    }

    private void getCommandGalleryHelpForAdmin(Player player) {
        player.sendMessage(Color.color("&7==================== \n" +
                plugin.getConfigManager().getPrefix() + ": GALERIA\n" +
                "&7==================== \n" +
                "/galeria limit <player> - zobacz ile gracz ma postawionych ramek.\n" +
                "/galeria setlimit <player> <ilosc> - ustaw graczowi ilość  postawionych ramek.\n" +
                "/galeria calculate <player> - podlicz ile ramek jest w galerii gracza.\n" +
                "/galeria calculateall - podlicz ramki we wszystkich galeriach.\n" +
                " \n" +
                "&cKomendy dla gracza:\n" +
                "&7/galeria - teleportuje na swoją galerię. Jeśli nie posiadasz galerii tworzą ją.\n" +
                "/galeria <player> - teleportuje na wyspę innego gracza.\n" +
                "/galeria tp <player> - teleportuje na wyspę innego gracza.\n" +
                "/galeria stworz - tworzy galerię.\n" +
                "/galeria help - wyświetla okno pomocy.\n" +
                "&7==================== \n"
        ));

    }

    private void getCommandGalleryHelpForPlayer(Player player) {
        player.sendMessage(Color.color("&7==================== \n" +
                plugin.getConfigManager().getPrefix() + ": GALERIA\n" +
                "&7==================== \n" +
                "&7/galeria - teleportuje na swoją galerię. Jeśli nie posiadasz galerii tworzą ją.\n" +
                "/galeria <player> - teleportuje na wyspę innego gracza.\n" +
                "/galeria tp <player> - teleportuje na wyspę innego gracza.\n" +
                "/galeria stworz - tworzy galerię.\n" +
                "/galeria help - wyświetla okno pomocy.\n" +
                "&7==================== \n"
        ));
    }

    private boolean hasGalleryPermission(Player player, String x) {
        return player.hasPermission(plugin.getConfigManager().getPermission() + x);
    }

    private boolean createGallery(Player player) {
        if (!hasGalleryPermission(player, ".1")) {
            player.sendMessage(Color.color(plugin.getConfigManager().getPrefix()
                    + ": Brak permisji."));
            return false;
        }
        if (haveIsland(player.getName())) {
            player.sendMessage(Color.color(plugin.getConfigManager().getPrefix()
                    + ": Posiadasz już własną galerię"));
            return false;
        }
        if (getCreateLimit(player.getName()) < Config.CREATE_LIMIT) {
            player.sendMessage(Color.color(plugin.getConfigManager().getPrefix()
                    + ": Nie możesz ponownie stworzyć galerii."));
            return false;
        }
        createIsland(player.getName(), null, null);
        plugin.getPlayerDao().insertPlayer(player.getName());
        return true;
    }

    private boolean teleportToGallery(String otherPlayer, Player player) {
        if (haveIsland(otherPlayer)) {
            Location location = getIsland(otherPlayer, false).getLocation().toLocation();
            location.setWorld(getIslandWorld());
            player.teleport(location);
            return true;
        } else {
            player.sendMessage(Color.color(plugin.getConfigManager().getPrefix()
                    + ": Dany gracz nie posiada galerii."));
            return false;
        }
    }

    private boolean teleportToGallery(Player player) {
        Location location = getIsland(player.getUniqueId()).getLocation().toLocation();
        location.setWorld(getIslandWorld());
        player.teleport(location);
        TextComponent mainTitle = Component.text("Przedmiot włożony do ramki")
                .color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true);
        TextComponent subTitle = Component.text("przy próbie wyciągnięcia zniknie!")
                .color(NamedTextColor.DARK_RED).decoration(TextDecoration.BOLD, true);
        Title title = Title.title(mainTitle,
                subTitle,
                Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(7), Duration.ofSeconds(1)));
        Audience.audience(player).showTitle(title);

        return true;
    }

    private void calculateAllFrames(Player player) {
        List<String> owners = getListOfAllGalleriesOwners();

        int count = 1;
        for (String owner : owners) {
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                loadChunks(owner);
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () ->
                        calculatePlayerFrames(owner, player), 20L);
            }, 20L * count++);
        }

        plugin.getServer().getScheduler()
                .scheduleSyncDelayedTask(plugin, () -> player.sendMessage(Color.color("&6&lProces zakończony")), 20L * (count + 1));

    }


    private void calculatePlayerFrames(String player, Player commander) {
        int[] array = getIslandRealCoords(player, false);
        Location loc = new Location(getIslandWorld(), array[0] + 22, 64, array[1] + 22);
        if (loc.getChunk().isEntitiesLoaded()) {
            commander.sendMessage(Color.color("&2&lZaładowano"));
        } else {
            commander.sendMessage(Color.color("&4&lNie załadowano"));
        }

        World world = BukkitAdapter.adapt(getIslandWorld());
        CuboidRegion gallery = new CuboidRegion(BlockVector3.at(array[0], 60, array[1]), BlockVector3.at(array[0] + 49, 120, array[1] + 49));
        gallery.setWorld(world);

        List<Entity> entities = new ArrayList<>(world.getEntities(gallery));
        String itemFrame = "minecraft:item_frame";
        String glowItemFrame = "minecraft:glow_item_frame";

        int count = 0;
        for (Entity ent : entities) {
            if (itemFrame.equalsIgnoreCase(Objects.requireNonNull(ent.getState()).getType().getName())
                    || glowItemFrame.equalsIgnoreCase(Objects.requireNonNull(ent.getState()).getType().getName())) {
                count++;
            } else {
                ent.remove();
            }
        }
        plugin.getPlayerDao().setPlayerLimit(player, count);

        if (count < entities.size()) {
            printPlayersWithSomeEntities(player, count, entities.size());
        }
    }


    private void loadChunks(String player) {
        int[] array = getIslandRealCoords(player, false);

        List<Location> chunks = Arrays.asList(new Location(getIslandWorld(), array[0], 64, array[1]),
                new Location(getIslandWorld(), array[0] + 16, 64, array[1]),
                new Location(getIslandWorld(), array[0] + 32, 64, array[1]),
                new Location(getIslandWorld(), array[0] + 48, 64, array[1]),
                new Location(getIslandWorld(), array[0], 64, array[1] + 16),
                new Location(getIslandWorld(), array[0] + 16, 64, array[1] + 16),
                new Location(getIslandWorld(), array[0] + 32, 64, array[1] + 16),
                new Location(getIslandWorld(), array[0] + 48, 64, array[1] + 16),
                new Location(getIslandWorld(), array[0], 64, array[1] + 32),
                new Location(getIslandWorld(), array[0] + 16, 64, array[1] + 32),
                new Location(getIslandWorld(), array[0] + 32, 64, array[1] + 32),
                new Location(getIslandWorld(), array[0] + 48, 64, array[1] + 32),
                new Location(getIslandWorld(), array[0], 64, array[1] + 48),
                new Location(getIslandWorld(), array[0] + 16, 64, array[1] + 48),
                new Location(getIslandWorld(), array[0] + 32, 64, array[1] + 48),
                new Location(getIslandWorld(), array[0] + 48, 64, array[1] + 48)
        );

        chunks.forEach(chunk -> chunk.getChunk().load());
    }


    private void printPlayersWithSomeEntities(String player, int frames, int entities) {
        Path path = Paths.get("./plugins/Galeria/logi/playersWithmoreEntitiesThanFrames.yml");
        if (!Files.isDirectory(path)) {
            try {
                Files.createDirectories(Paths.get("./plugins/Galeria/logi/"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (!Files.exists(path)) {
            try {
                Files.createFile(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        try {
            Files.write(path, (player + " ilość ramek - " + frames + " ilość entity - " + entities + "\n").getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private List<String> getListOfAllGalleriesOwners() {
        double x = 24.5;
        double z;
        Location location;
        Block block;
        SimpleIsland island;
        List<String> owners = new ArrayList<>();

        while (x < 500.0) {
            z = 24.5;
            while (z < 5000.0) {
                location = new Location(getIslandWorld(), x, 66.0, z);
                block = location.getBlock();
                if (block.getType().equals(Material.CYAN_TERRACOTTA)) {
                    island = getIsland(location);
                    owners.add(island.getOwner().toLowerCase());
                }
                z += 49;
            }
            x += 49;
        }

        return owners;
    }

    private boolean checkWhichIslandDontHaveOwner(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Files.createFile(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Files.write(path, "Lista galerii bez właściciela: \n".getBytes(), StandardOpenOption.WRITE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        double x = 24.5;
        double z;
        Location location;
        Block block;
        SimpleIsland island;
        List<Location> galleries = new ArrayList<>();

        while (x < 500.0) {
            z = 24.5;
            while (z < 5000.0) {
                location = new Location(getIslandWorld(), x, 66.0, z);
                block = location.getBlock();
                if (block.getType().equals(Material.CYAN_TERRACOTTA)) {
                    island = getIsland(location);
                    if (island == null) {
                        galleries.add(location);
                    }
                }
                z += 49;
            }
            x += 49;
        }

        galleries.forEach(user -> {
            String record = (galleries.indexOf(user) + 1) + ". " + user + "\n";
            try {
                Files.write(path, record.getBytes(), StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return true;
    }
}