package de.coerdevelopment.essentials.repository;

import java.sql.*;

public class SQL {

    private static SQL instance;

    public static SQL getSQL() {
        return instance;
    }

    public static SQL newSQL(String host, String username, String password, String database, String port) {
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
        } else {
            instance = new SQL(host, username, password, database, port);
        }
        return instance;
    }

    private String host;
    private String username;
    private String password;
    private String database;
    private String port;

    private Connection connection;

    private SQL(String host, String username, String password, String database, String port) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.database = database;
        this.port = port;
    }

    public void connect() throws SQLException, ClassNotFoundException {
        if (isConnected()) {
            return;
        }
        connection = DriverManager.getConnection("jdbc:mariadb://" + host + ":" + port + "/" + database + "?useUnicode=true&autoReconnect=true", username, password);
        if (isConnected()) {
            System.out.println("Successfully connected to database!");
        } else {
            System.err.println("Couldn't connect to database!");
        }
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

}
