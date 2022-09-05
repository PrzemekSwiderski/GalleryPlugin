package gallery.listeners;

import gallery.Gallery;
import gallery.util.Color;
import lombok.SneakyThrows;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pl.islandworld.Config;
import pl.islandworld.entity.SimpleIsland;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static pl.islandworld.api.IslandWorldApi.*;

public class ChatListener implements CommandExecutor {

    private final Gallery plugin;
    private final GalleryChanger galleryChanger;
    private final FrameCalculator frameCalculator;

    public ChatListener() {
        plugin = Gallery.getInstance();
        galleryChanger = new GalleryChanger();
        frameCalculator = new FrameCalculator();
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
                        if (!plugin.getPlayerDao().isPlayerInDB(args[1])) {
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
                        if (!hasGalleryPermission(player, ".admin")) {
                            getCommandGalleryHelpForPlayer(player);
                            return false;
                        }
                        if (args.length != 2) {
                            player.sendMessage(Color.color(plugin.getConfigManager().getPrefix() +
                                    ": Poprawne użycie to &f/galeria calculate <player>"));
                            return false;
                        }
                        if (!haveIsland(args[1])) {
                            player.sendMessage(Color.color(plugin.getConfigManager().getPrefix() +
                                    ": Gracz &f" + args[1] + " &7nie posiada galerii."));
                            return false;
                        }
                        frameCalculator.loadChunks(args[1]);

                        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                            frameCalculator.calculatePlayerFrames(args[1], player);
                            player.sendMessage(Color.color(plugin.getConfigManager().getPrefix()
                                    + ": Gracz &f" + args[1]
                                    + "&7 posiada &f" + plugin.getPlayerDao().getLimit(args[1])
                                    + " ramek w swojej galerii."));
                        }, 20L);

                        return true;
                    case "calculateall":
                        if (hasGalleryPermission(player, ".admin")) {
                            frameCalculator.calculateAllFrames(player);
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
                if (args.length != 2 && args.length != 3) {
                    player.sendMessage(Color.color(plugin.getConfigManager().getPrefix() +
                            ": Poprawne użycie komendy: &f/wyspa <staryNick> <nowyNick> [optional -f]"));
                    return false;
                }
                if (args.length == 3) {
                    if ("-f".equals(args[2])) {
                        return galleryChanger.changeForced(args, player);
                    }
                }
                return galleryChanger.change(args, player);

            default:
                return false;
        }
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
        if (getCreateLimit(player.getName().toLowerCase()) >= Config.CREATE_LIMIT) {
            player.sendMessage(Color.color(plugin.getConfigManager().getPrefix()
                    + ": Nie możesz ponownie stworzyć galerii."));
            return false;
        }
        createIsland(player.getName(), null, null);
        if (!plugin.getPlayerDao().isPlayerInDB(player.getName())) {
            plugin.getPlayerDao().insertPlayer(player.getName());
        }
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