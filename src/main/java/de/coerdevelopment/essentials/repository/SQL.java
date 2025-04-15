package de.coerdevelopment.essentials.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        config.setLeakDetectionThreshold(120000);

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

    private <T> Map<Integer, T> genericBatchInsert(String tableName, List<T> objects, ColumnMapper<T> columnMapper, int batchSize, boolean storeKeys) throws SQLException {
        if (objects.isEmpty()) {
            throw new IllegalArgumentException("The Objects are empty.");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size has to be greater than zero.");
        }

        Map<String, Object> firstMapping = columnMapper.mapColumns(objects.get(0));
        List<String> columns = new ArrayList<>(firstMapping.keySet());
        String columnNames = String.join(", ", columns);

        Map<Integer, T> idObjectMap = new HashMap<>();

        try (Connection connection = getConnection()) {
            boolean initialAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            int total = objects.size();
            for (int i = 0; i < total; i += batchSize) {
                int end = Math.min(i + batchSize, total);
                List<T> batch = objects.subList(i, end);

                String placeholders = batch.stream()
                        .map(o -> "(" + columns.stream().map(c -> "?").collect(Collectors.joining(", ")) + ")")
                        .collect(Collectors.joining(", "));

                String query = "INSERT INTO " + tableName + " (" + columnNames + ") VALUES " + placeholders;

                try (PreparedStatement pstmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                    int paramIndex = 1;
                    for (T obj : batch) {
                        Map<String, Object> values = columnMapper.mapColumns(obj);
                        for (String column : columns) {
                            pstmt.setObject(paramIndex++, values.get(column));
                        }
                    }

                    pstmt.executeUpdate();

                    if (storeKeys) {
                        storeKeys(pstmt, batch, idObjectMap);
                    }
                }
            }

            connection.commit();
            connection.setAutoCommit(initialAutoCommit);
        }

        return idObjectMap;
    }

    public <T> void batchInsert(String tableName, List<T> objects, ColumnMapper<T> columnMapper, int batchSize) throws SQLException {
        genericBatchInsert(tableName, objects, columnMapper, batchSize, false);
    }

    public <T> Map<Integer, T> batchInsertReturningKeys(String tableName, List<T> objects, ColumnMapper<T> columnMapper, int batchSize) throws SQLException {
        return genericBatchInsert(tableName, objects, columnMapper, batchSize, true);
    }

    private <T> void storeKeys(PreparedStatement pstmt, List<T> batch, Map<Integer, T> idObjectMap) throws SQLException {
        try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
            int i = 0;
            while (generatedKeys.next()) {
                int generatedId = generatedKeys.getInt(1);
                if (i >= batch.size()) {
                    throw new SQLException("Generated more keys than objects in batch.");
                }
                idObjectMap.put(generatedId, batch.get(i++));
            }
        }
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
