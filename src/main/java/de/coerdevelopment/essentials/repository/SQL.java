package de.coerdevelopment.essentials.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQL {

    private static SQL instance;

    public static SQL getSQL() {
        return instance;
    }

    public static SQL newSQL(String host, String username, String password, String database, int port, String type, int minPoolSize, int maxPoolSize) {
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
            instance.dialect = SQLDialect.valueOf(type.toUpperCase());
        } else {
            instance = new SQL(host, username, password, database, port, type, minPoolSize, maxPoolSize);
        }
        return instance;
    }

    private SQLDialect dialect;
    private String host;
    private String username;
    private String password;
    private String database;
    private int port;
    private int minPoolSize;
    private int maxPoolSize;

    private HikariDataSource dataSource;

    private SQL(String host, String username, String password, String database, int port, String type, int minPoolSize, int maxPoolSize) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.database = database;
        this.port = port;
        this.dialect = SQLDialect.valueOf(type.toUpperCase());
        this.minPoolSize = minPoolSize;
        this.maxPoolSize = maxPoolSize;
    }

    public void connect() throws SQLException, ClassNotFoundException {
        if (isPoolConnected()) {
            return;
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(getURL());
        config.setUsername(username);
        config.setPassword(password);
        config.setMinimumIdle(minPoolSize);
        config.setMaximumPoolSize(maxPoolSize);
        config.setLeakDetectionThreshold(30000);

        this.dataSource = new HikariDataSource(config);
    }

    public void disconnect() throws SQLException {
        if (isPoolConnected()) {
            dataSource.close();
        }
    }

    /**
     * Returns true if the initialization was successful, otherwise false
     */
    public boolean initSQL() {
        try {
            connect();
            return isPoolConnected();
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isPoolConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    public int countDatabaseTables() {
        try (Connection connection = getConnection()) {
            PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) AS tables " +
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

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }


    public PreparedStatement executeQuery(String query, Object... params) {
        return executeQueryWithParameters(query, false, null, params);
    }

    public PreparedStatement executeQuery(String query, StatementCustomAction customAction, Object... params) {
        return executeQueryWithParameters(query, false, customAction, params);
    }

    public PreparedStatement executeQueryReturningKeys(String query, Object... params) {
        return executeQueryWithParameters(query, true, null, params);
    }

    public PreparedStatement executeQueryReturningKeys(String query,StatementCustomAction customAction, Object... params) {
        return executeQueryWithParameters(query, true, customAction, params);
    }

    private PreparedStatement executeQueryWithParameters(String query, boolean returnGeneratedKeys, StatementCustomAction customAction, Object... params) {
        PreparedStatement statement = null;
        try (Connection connection = getConnection()) {
            statement = connection.prepareStatement(query, returnGeneratedKeys ? PreparedStatement.RETURN_GENERATED_KEYS : PreparedStatement.NO_GENERATED_KEYS);
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    statement.setObject(i + 1, params[i]);
                }
            }
            if (customAction != null) {
                customAction.onBeforeExecute(statement);
            }
            statement.execute();
            if (customAction != null) {
                customAction.onAfterExecute(statement);
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return statement;
    }



    public String getDriver() {
        return dialect.driverUrl;
    }

    public String getURL() {
        return getDriver() + host + ":" + port + "/" + database;
    }

    public boolean isMySQLDialect() {
        return dialect.equals(SQLDialect.MYSQL) || dialect.equals(SQLDialect.MARIADB);
    }

    public boolean isPostgreSQLDialect() {
        return dialect.equals(SQLDialect.POSTGRESQL);
    }

    public SQLDialect getDialect() {
        return dialect;
    }
}
