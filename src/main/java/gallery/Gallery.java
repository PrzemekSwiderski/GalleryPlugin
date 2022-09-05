package gallery;

import gallery.listeners.ChatListener;
import gallery.listeners.ItemFrameListener;
import gallery.listeners.Protector;
import gallery.managers.ConfigManager;
import gallery.managers.MySQLManager;
import gallery.util.Utility;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class Gallery extends JavaPlugin {
    private static Gallery instance;
    private ConfigManager configManager;
    private MySQLManager playerDao;
    private ChatListener chatListener;
    private Protector protector;
    private Utility utility;


    @Override
    public void onEnable() {
        instance = this;
        configManager = new ConfigManager();
        playerDao = new MySQLManager();
        chatListener = new ChatListener();
        protector = new Protector();
        utility = new Utility();

        getCommand("galeria").setExecutor(chatListener);
        getCommand("wyspa").setExecutor(chatListener);

        getServer().getPluginManager().registerEvents(new Protector(), this );
        getServer().getPluginManager().registerEvents(new ItemFrameListener(), this);
        getLogger().severe("Galeria uruchomiona.");
    }

    @Override
    public void onDisable() {
    }

    public static Gallery getInstance() {
        return instance;
    }
}

