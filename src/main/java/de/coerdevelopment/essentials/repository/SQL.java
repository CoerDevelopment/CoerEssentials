package de.coerdevelopment.essentials.repository;

import java.sql.*;

public class SQL {

    private static SQL instance;

    public static SQL getSQL() {
        return instance;
    }

    public static SQL newSQL(String host, String username, String password, String database, int port, String type) {
        if (instance != null) { // close old connection and use new connection
            try {
                instance.disconnect();
            } catch (SQLException e) {
                throw new RuntimeException("Unable to close current sql connection");
            }
            instance.host = host;
            instance.username = username;
            instance.password = password;
            instance.database = database;
            instance.port = port;
            instance.type = type;
        } else {
            instance = new SQL(host, username, password, database, port, type);
        }
        return instance;
    }

    private String type;
    private String host;
    private String username;
    private String password;
    private String database;
    private int port;

    private Connection connection;

    private SQL(String host, String username, String password, String database, int port, String type) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.database = database;
        this.port = port;
        this.type = type;
    }

    public void connect() throws SQLException, ClassNotFoundException {
        if (isConnected()) {
            return;
        }
        connection = DriverManager.getConnection(getDriver() + host + ":" + port + "/" + database + "?useUnicode=true&autoReconnect=true", username, password);
    }

    public void disconnect() throws SQLException {
        if (isConnected()) {
            connection.close();
        }
    }

    /**
     * Returns true if the initialization was successful, otherwise false
     */
    public boolean initSQL() {
        try {
            connect();
            return isConnected();
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isConnected() {
        if (connection != null) {
            try {
                return !connection.isClosed();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public int countDatabaseTables() {
        try {
            PreparedStatement ps = getConnection().prepareStatement("SELECT COUNT(*) AS tables " +
                    "FROM information_schema.tables " +
                    "WHERE table_schema = ?");
            ps.setString(1, database);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("tables");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return -1;
    }

    public synchronized Connection getConnection() {
        return connection;
    }

    public String getDriver() {
        switch (type.toLowerCase()) {
            case "mysql":
                return "jdbc:mysql://";
            case "mariadb":
                return "jdbc:mariadb://";
            case "postgresql":
                return "jdbc:postgresql://";
            default:
                throw new RuntimeException("Unsupported database type: " + type);
        }
    }

    public boolean isMySQLDialect() {
        return type.equalsIgnoreCase("mysql") || type.equalsIgnoreCase("mariadb");
    }

    public boolean isPostgreSQLDialect() {
        return type.equalsIgnoreCase("postgresql");
    }

}
