package gallery;

import gallery.listeners.ChatListener;
import gallery.listeners.ItemFrameListener;
import gallery.managers.ConfigManager;
import gallery.managers.PlayerDao;
import gallery.util.Utility;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class Gallery extends JavaPlugin {
    private static Gallery instance;
    private ConfigManager configManager;
    private PlayerDao playerDao;
    private ChatListener chatListener;
    private Utility utility;

    @Override
    public void onEnable() {
        instance = this;
        configManager = new ConfigManager();
        playerDao = new PlayerDao();
        chatListener = new ChatListener();
        utility = new Utility();
        getCommand("galeria").setExecutor(chatListener);
        getCommand("wyspa").setExecutor(chatListener);


        getServer().getPluginManager().registerEvents(new ItemFrameListener(this), this);
        getLogger().severe("Galeria uruchomiona.");
    }

    @Override
    public void onDisable() {
    }

    public static Gallery getInstance() {
        return instance;
    }
    //todo komendy i permisje
    //todo blokada wyrzucania przedmiotow, zbierania itemow budowania i niszczenia
}

