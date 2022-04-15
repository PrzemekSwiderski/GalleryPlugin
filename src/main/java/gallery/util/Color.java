package gallery.util;

import org.bukkit.ChatColor;

public class Color {

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
