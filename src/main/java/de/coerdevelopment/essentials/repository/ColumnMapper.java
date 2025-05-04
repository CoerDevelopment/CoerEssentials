package de.coerdevelopment.essentials.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class ColumnMapper<T> {
    public Map<String, Object> mapColumns(T obj) {
        return null;
    }

    public T getObjectFromResultSetEntry(ResultSet resultSet) throws SQLException {
        return null;
    }
}
