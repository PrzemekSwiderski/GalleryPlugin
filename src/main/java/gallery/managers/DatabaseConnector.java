package gallery.managers;

import gallery.Gallery;

import java.sql.*;
import java.util.logging.Logger;

public class DatabaseConnector {

    private final Logger log;
    private Connection connection;

    private final String host;
    private final String user;
    private final String password;
    private final String database;
    private final int port;

    public DatabaseConnector() {
        Gallery plugin = Gallery.getInstance();
        this.database = plugin.getConfigManager().getDatabase();
        this.host = plugin.getConfigManager().getHost();
        this.port = plugin.getConfigManager().getPort();
        this.user = plugin.getConfigManager().getUser();
        this.password = plugin.getConfigManager().getPassword();
        this.log = plugin.getLogger();
    }

    private void initialize() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://"
                            + host + ":"
                            + port + "/"
                            + database
                            + "?autoReconnect=true&useUnicode=true&characterEncoding=utf-8",
                    user, password);
        } catch (ClassNotFoundException e) {
            log.severe("ClassNotFoundException! " + e.getMessage());
        } catch (SQLException e) {
            log.severe("SQLException! " + e.getMessage());
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                initialize();
            }
        } catch (SQLException e) {
            log.severe("Failed to get connection! " + e.getMessage());
        }
        return connection;
    }

    public Boolean checkConnection() {
        return getConnection() != null;
    }
}
