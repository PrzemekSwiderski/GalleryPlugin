package gallery.util;

import gallery.Gallery;
import org.bukkit.entity.Player;


public class Utility {
    private Gallery plugin;

    public Utility(){
        plugin = Gallery.getInstance();
    }
    public boolean whichLimitPlayerHas(Player player, int level) {
        return player.hasPermission(plugin.getConfigManager().getPermission() + "." + level);
    }
}
