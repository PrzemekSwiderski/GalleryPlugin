package gallery.managers;

import gallery.Gallery;

import java.sql.*;
import java.util.logging.Logger;

public class MySQLManager {

    private final Logger log;
    private Connection connection;

    private final String host;
    private final String user;
    private final String password;
    private final String database;
    private final int port;

    public MySQLManager(String host, int port, String database, String user, String password) {
        this.database = database;
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.log = Gallery.getInstance().getLogger();
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



    public ResultSet select(String query) {
        try {
            Statement statement = getConnection().createStatement();
            return statement.executeQuery(query);
        } catch (SQLException e) {
            log.severe("Error at SQL Query: " + e.getMessage());
            log.severe("Query: " + query);
            return null;
        }
    }

    public void insert(String query) {
        try (Statement statement = getConnection().createStatement()) {
            statement.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);
        } catch (SQLException e) {
            if (!e.toString().contains("not return ResultSet")) {
                log.severe("Error at SQL INSERT Query: " + e);
            }
        }
    }

    public boolean update(String query) {
        try (Statement statement = getConnection().createStatement()) {
            statement.executeUpdate(query);
            return true;
        } catch (SQLException e) {
            if (!e.toString().contains("not return ResultSet")) {
                log.severe("Error at SQL UPDATE Query: " + e);
            }
            return false;
        }
    }

    public void execute(String query) {
        try (Statement statement = getConnection().createStatement()) {
            statement.execute(query);

        } catch (SQLException e) {
            log.severe("Error at SQL Query: " + e.getMessage());
        }
    }

    public Boolean existsTable(String table) {
        try {
            ResultSet tables = getConnection().getMetaData().getTables(database, null, table, new String[] {"TABLE"});
            return tables.next();
        } catch (SQLException e) {
            log.severe("Failed to check if table " + table + " exists: " + e.getMessage());
            return false;
        }
    }


}
