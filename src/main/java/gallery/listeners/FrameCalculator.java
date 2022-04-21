package gallery.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.World;
import gallery.Gallery;
import gallery.util.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import pl.islandworld.entity.SimpleIsland;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static pl.islandworld.api.IslandWorldApi.*;

public class FrameCalculator {
    private final Gallery plugin;

    public FrameCalculator() {
        plugin = Gallery.getInstance();
    }

    public void calculateAllFrames(Player player) {
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

    public void calculatePlayerFrames(String player, Player commander) {
        int[] array = getIslandRealCoords(player, false);
        Location loc = new Location(getIslandWorld(), array[0] + 22, 64, array[1] + 22);
        if (!loc.getChunk().isEntitiesLoaded()) {
            commander.sendMessage(Color.color("&4&lNie załadowano galerii " + player));
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

    }

    public void loadChunks(String player) {
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

}
