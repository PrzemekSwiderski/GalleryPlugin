package gallery.listeners;

import gallery.Gallery;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

public class Protector implements Listener {
    private Gallery plugin;

    public Protector() {
        this.plugin = Gallery.getInstance();
    }

    @EventHandler
    public void onRemoveItem(EntityDropItemEvent event) {
        if (!plugin.getConfigManager().getWorld().equals(event.getEntity().getWorld().getName())) {
            return;
        }
        if (!(event.getEntity() instanceof ItemFrame)) {
            return;
        }
        if (event.getEntity() instanceof Player player) {
            player.sendMessage("Wydropiles item");
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        if (!plugin.getConfigManager().getWorld().equals(event.getPlayer().getWorld().getName())) {
            return;
        }

        event.getPlayer().sendMessage("Rzuciles item");

        event.setCancelled(true);
    }

    @EventHandler
    public void spawnEntity(EntitySpawnEvent event) {
        if (!plugin.getConfigManager().getWorld().equals(event.getEntity().getWorld().getName())) {
            return;
        }
        if (event.getEntity() instanceof ItemFrame) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void blockPlace(BlockPlaceEvent event) {
        if (!plugin.getConfigManager().getWorld().equals(event.getBlock().getWorld().getName())) {
            return;
        }
        if (event.getPlayer().hasPermission(plugin.getConfigManager().getPermission() + ".admin")) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void blockBreak(BlockBreakEvent event) {
        if (!plugin.getConfigManager().getWorld().equals(event.getBlock().getWorld().getName())) {
            return;
        }
        if (event.getPlayer().hasPermission(plugin.getConfigManager().getPermission() + ".admin")) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void fluids(PlayerBucketEmptyEvent event) {
        if (!plugin.getConfigManager().getWorld().equals(event.getBlock().getWorld().getName())) {
            return;
        }

        event.setCancelled(true);
    }


}
