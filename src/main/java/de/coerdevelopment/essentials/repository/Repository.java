package de.coerdevelopment.essentials.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class Repository {

    public static List<Repository> repositories = new ArrayList<>();

    public String tableName;
    protected SQL sql;

    public Repository(String tableName) {
        this.tableName = tableName;
        this.sql = SQL.getSQL();
        repositories.add(this);
    }

    public abstract void createTable();

    public void dropTable() {
        try {
            sql.getConnection().prepareStatement("DROP TABLE " + tableName + " CASCADE").execute();
        } catch (Exception e) {
            if (e.getMessage().contains("Unknown table")) {
                // table does not exist -> ignore
                return;
            }
            e.printStackTrace();
        }
    }

    protected <T> Map<Integer, T> batchInsert(List<T> objects, ColumnMapper<T> columnMapper, int batchSize) throws SQLException {
        if (objects.isEmpty()) {
            throw new IllegalArgumentException("The Objects are empty.");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size has to be greater than zero.");
        }

        Map<String, Object> firstMapping = columnMapper.mapColumns(objects.get(0));
        List<String> columns = firstMapping.keySet().stream().collect(Collectors.toList());
        String columnNames = String.join(", ", columns);
        String placeholders = columns.stream().map(col -> "?").collect(Collectors.joining(", "));

        String query = "INSERT INTO " + tableName + " (" + columnNames + ") VALUES (" + placeholders + ")";

        Map<Integer, T> idObjectMap = new HashMap<>();

        try (PreparedStatement pstmt = this.sql.getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            int count = 0;

            for (T obj : objects) {
                Map<String, Object> columnValues = columnMapper.mapColumns(obj);

                int index = 1;
                for (String column : columns) {
                    pstmt.setObject(index++, columnValues.get(column));
                }

                pstmt.addBatch();
                count++;

                if (count % batchSize == 0) {
                    executeBatchAndStoreKeys(pstmt, objects, idObjectMap, count - batchSize);
                }
            }

            if (count % batchSize != 0) {
                executeBatchAndStoreKeys(pstmt, objects, idObjectMap, count - (count % batchSize));
            }
        }

        return idObjectMap;
    }

    private <T> void executeBatchAndStoreKeys(PreparedStatement pstmt, List<T> objects, Map<Integer, T> idObjectMap, int startIndex) throws SQLException {
        pstmt.executeBatch();

        try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
            int currentIndex = startIndex;
            while (generatedKeys.next()) {
                int generatedId = generatedKeys.getInt(1); // Generierte ID abrufen
                idObjectMap.put(generatedId, objects.get(currentIndex++));
            }
        }
    }

}
