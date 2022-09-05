package gallery.managers;

import gallery.Gallery;
import lombok.Getter;
import lombok.SneakyThrows;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

@Getter
public class MySQLManager {
    private final String PLAYERS = "players";

    private final Gallery plugin;
    private final Logger log;
    private DatabaseConnector connector;

    public MySQLManager() {
        plugin = Gallery.getInstance();
        log = plugin.getLogger();
        initiateDB();
    }

    @SneakyThrows
    private void initiateDB() {
        connector = new DatabaseConnector();
        if (connector.checkConnection()) {
            if (!existsTable(PLAYERS)) {
                execute("CREATE TABLE IF NOT EXISTS `players` (" +
                        "`name` varchar(32), " +
                        "PRIMARY KEY (`name`), " +
                        "frames INT);");
            }
        } else {
            log.warning("Cannot initiate database - connection error");
        }
        connector.getConnection().close();
    }

    public Boolean existsTable(String table) {
        String database = plugin.getConfigManager().getDatabase();
        try (Connection connection = connector.getConnection();
             ResultSet tables = connection
                     .getMetaData()
                     .getTables(database, null, table, new String[]{"TABLE"})) {
            return tables.next();
        } catch (SQLException e) {
            log.severe("Failed to check if table " + table + " exists: " + e.getMessage());
            return false;
        }
    }

    public void execute(String query) {
        try (Connection connection = connector.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.severe("Error at execute(): " + e.getMessage());
        }
    }

    public int getLimit(String player) {
        String query = "SELECT `frames` FROM " + PLAYERS + " WHERE `name` = ?";
        try (Connection connection = connector.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, player);
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            return resultSet.getInt("frames");
        } catch (SQLException e) {
            log.severe("Error at getLimit(): " + e.getMessage());
        }
        return -1;
    }

    public boolean updatePlayerLimit(String player, int modify) {
        String query = "UPDATE " + PLAYERS + " SET `frames` = `frames` + ? WHERE `name` = ?";
        try (Connection connection = connector.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, modify);
            preparedStatement.setString(2, player);
            preparedStatement.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.severe("Error at updatePlayerLimit(): " + e.getMessage());
        }

        return false;
    }

    public boolean setPlayerLimit(String player, int modify) {
        if (isPlayerInDB(player)) {
            return setPlayerLimitIfPlayerIsInDB(player, modify);
        } else {
            return setPlayerLimitIfPlayerIsNotInDB(player, modify);
        }
    }

    public boolean isPlayerInDB(String player) {
        String query = "SELECT * from " + PLAYERS + " WHERE name = ?";
        try (Connection connection = connector.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, player);
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return true;
        }
    }

    private boolean setPlayerLimitIfPlayerIsInDB(String player, int modify) {
        String query = "UPDATE " + PLAYERS + " SET `frames` = ? Where `name` = ?";
        try (Connection connection = connector.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, modify);
            preparedStatement.setString(2, player);
            preparedStatement.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.severe("Error at setPlayerLimitIfPlayerIsInDB(): " + e.getMessage());
        }
        return false;
    }

    private boolean setPlayerLimitIfPlayerIsNotInDB(String player, int modify) {
        String query = "INSERT INTO " + PLAYERS + " (`name`, `frames`) VALUES(?, ?)";
        try (Connection connection = connector.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, player);
            preparedStatement.setInt(2, modify);
            preparedStatement.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.severe("Error at setPlayerLimitIfPlayerIsNotInDB(): " + e.getMessage());
        }
        return false;
    }

    public void insertPlayer(String player) {
        setPlayerLimitIfPlayerIsNotInDB(player, 0);
    }
}


