package gallery.managers;

import gallery.Gallery;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class ConfigManager {
    private Gallery plugin;
    private FileConfiguration config;

    private String host;
    private String user;
    private String password;
    private String database;
    private int port;

    private String world;
    private String prefix;
    private String permission;
    private List<Integer> limits;

    public ConfigManager() {
        this.plugin = Gallery.getInstance();
        this.config = plugin.getConfig();
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveDefaultConfig();
        loadConfig();
    }

    public void loadConfig() {
        this.host = config.getString("mysql.host");
        this.user = config.getString("mysql.user");
        this.password = config.getString("mysql.password");
        this.database = config.getString("mysql.database");
        this.port = config.getInt("mysql.port");
        this.world = config.getString("galleryWorld");
        this.permission = config.getString("permission");
        this.limits = config.getIntegerList("limits").stream()
                .distinct()
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList());
        this.prefix = config.getString("prefix");
    }

}
