package de.coerdevelopment.essentials.repository;

import java.util.Map;

public interface ColumnMapper<T> {
    Map<String, Object> mapColumns(T obj);
}
