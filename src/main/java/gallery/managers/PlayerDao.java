package gallery.managers;

import gallery.Gallery;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PlayerDao {
    private final String PLAYERS = "players";

    private Gallery plugin;
    private MySQLManager mysql;

    public PlayerDao() {
        plugin = Gallery.getInstance();
        initiateDB();
    }

    private void initiateDB() {
        mysql = new MySQLManager(plugin.getConfigManager().getHost(),
                plugin.getConfigManager().getPort(),
                plugin.getConfigManager().getDatabase(),
                plugin.getConfigManager().getUser(),
                plugin.getConfigManager().getPassword());
        if (mysql.checkConnection()) {
            if (!mysql.existsTable(PLAYERS)) {
                mysql.execute("CREATE TABLE IF NOT EXISTS `players` (" +
                        "`name` varchar(255), " +
                        "PRIMARY KEY (`name`), " +
                        "frames INT);");
            }
        } else {
            plugin.getLogger().warning("Cannot initiate database - connection error");
        }
    }

    public int getLimit(String player) {
        try {
            String query = String.format("SELECT frames FROM players WHERE name = '%s'", player);
            ResultSet resultSet = mysql.select(query);
            resultSet.next();
            return resultSet.getInt("frames");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 1000;
    }

    public boolean updatePlayerLimit(Player player, String modify) {
        String query = String.format("UPDATE players SET frames = frames %s Where name = '%s'", modify, player.getName());
        return mysql.update(query);
    }

    public boolean setPlayerLimit(String player, int modify) {
        String query = null;
        if (!checkIfPlayerExist(player)) {
            query = String.format("INSERT INTO players (`name`, `frames`) VALUES('%s', %s)", player, modify);
        } else {
            query = String.format("UPDATE players SET frames = %s Where name = '%s'", modify, player);
        }
        return mysql.update(query);
    }

    public void insertPlayer(String player) {
        String query = String.format("INSERT INTO players (`name`, `frames`) VALUES('%s', 0)", player);
        mysql.insert(query);
    }

    public void insertPlayer(String player, int frames) {
        String query = String.format("INSERT INTO players (`name`, `frames`) VALUES('%s', %s)", player, frames);
        mysql.insert(query);
    }

    public boolean checkIfPlayerExist(String player) {
        String query = String.format("SELECT * from players WHERE name = '%s'", player);
        ResultSet resultSet = mysql.select(query);
        try {
            if (resultSet.next()) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}
