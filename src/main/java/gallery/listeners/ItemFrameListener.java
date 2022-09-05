package gallery.listeners;

import gallery.Gallery;
import gallery.util.Color;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;

import java.util.List;
import java.util.Objects;

import static pl.islandworld.api.IslandWorldApi.canBuildOnLocation;


@Getter
public class ItemFrameListener implements Listener {
    public final int ZERO = 0;

    private final Gallery plugin;

    public ItemFrameListener() {
        plugin = Gallery.getInstance();
    }

    @EventHandler
    public void onItemFramePlace(HangingPlaceEvent event) {
        Player player = Objects.requireNonNull(event.getPlayer());

        if (!isGalleryWorld(player) || !isOwner(player)) {
            return;
        }
        if (!(event.getEntity() instanceof ItemFrame)){
            return;
        }

        List<Integer> limits = plugin.getConfigManager().getLimits();

        for (Integer limit : limits) {
            if (plugin.getUtility().whichLimitPlayerHas(player, limits.size() - limits.indexOf(limit))) {
                if (isLimitReach(player, limit)) {
                    if (plugin.getPlayerDao().updatePlayerLimit(player.getName(), 1)) {
                        player.sendMessage(Color.color(plugin.getConfigManager().getPrefix()
                                + ": Poprawnie postawiono ramkę."));
                    } else {
                        event.setCancelled(true);
                        player.sendMessage(Color.color(plugin.getConfigManager().getPrefix()
                                + ": &cBłąd podczas stawiania ramki. Zgłoś to administratorowi!"));
                    }
                } else {
                    event.setCancelled(true);
                    player.sendMessage(Color.color(plugin.getConfigManager().getPrefix()
                            + ": Limit ramek został osiągnięty."));
                }
                return;
            }
        }
        event.setCancelled(true);
        player.sendMessage(Color.color(plugin.getConfigManager().getPrefix()
                + ": Brak permisji by postawić ramkę."));

    }


    @EventHandler
    public void onFrameBreak(HangingBreakByEntityEvent event) {
        if (isGalleryWorld(event)) {
            Player player = (Player) event.getRemover();
            if (isOwner(player) && player != null) {
                if (player.isSneaking()) {
                    if (plugin.getPlayerDao().updatePlayerLimit(player.getName(), -1)) {
                        player.sendMessage(Color.color(plugin.getConfigManager().getPrefix()
                                + ": Poprawnie usunięto ramkę."));
                        if (lowerThanZero(player)) {
                            plugin.getPlayerDao().setPlayerLimit(player.getName(), 0);
                        }
                    } else {
                        event.setCancelled(true);
                        player.sendMessage(Color.color(plugin.getConfigManager().getPrefix()
                                + ": &cBłąd podczas usuwania ramki. Zgłoś to administratorowi!"));
                    }
                } else {
                    event.setCancelled(true);
                    TextComponent textComponent = Component.text(Color.color(plugin.getConfigManager().getPrefix()))
                            .append(Component.text(": By usunąć ramkę wciśnij klawisz ").color(NamedTextColor.GRAY))
                            .append(Component.keybind("key.sneak").color(NamedTextColor.WHITE))
                            .append(Component.text(" i kliknij ").color(NamedTextColor.GRAY))
                            .append(Component.keybind("key.attack").color(NamedTextColor.WHITE));

                    player.sendMessage(textComponent);

                }

            } else {
                event.setCancelled(true);
            }
        }
    }

    private boolean lowerThanZero(Player player) {
        return plugin.getPlayerDao().getLimit(player.getName()) < ZERO;
    }

    private boolean isGalleryWorld(HangingBreakByEntityEvent event) {
        return event.getRemover() instanceof Player && event.getRemover().getWorld().getName().equals(plugin.getConfigManager().getWorld());
    }

    private boolean isOwner(Player player) {
        return canBuildOnLocation(player, player.getLocation(), false);
    }

    private boolean isGalleryWorld(Player player) {
        return player.getWorld().getName().equalsIgnoreCase(plugin.getConfigManager().getWorld());
    }

    private boolean isLimitReach(Player player, int limit) {
        return plugin.getPlayerDao().getLimit(player.getName()) < limit;
    }


}
