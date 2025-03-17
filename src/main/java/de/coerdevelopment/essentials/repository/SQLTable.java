package de.coerdevelopment.essentials.repository;

import java.util.ArrayList;
import java.util.List;

public class SQLTable {

    private String name;
    private List<SQLEntity> entities;

    public SQLTable(String name) {
        this.name = name;
        this.entities = new ArrayList<>();
    }

    public String getCreateTableStatement(SQLDialect dialect) {
        String query = "CREATE TABLE IF NOT EXISTS " + this.name + " (";
        for (SQLEntity entity : this.entities) {
            query += entity.getSQL(dialect) + ",";
        }
        query = query.substring(0, query.length() - 1);
        query += ");";
        return query;
    }

    public void addEntity(SQLEntity entity) {
        this.entities.add(entity);
    }

    public void addAutoKey(String name) {
        SQLEntity entity = new SQLEntity(name, "INT", false);
        entity.setAutoIncrement();
        entity.setPrimary();
        this.entities.add(entity);
    }

    public void addString(String name, int length, boolean nullable) {
        addEntity(name, "VARCHAR(" + length + ")", nullable);
    }

    public void addUniqueString(String name, int length, boolean nullable) {
        SQLEntity entity = new SQLEntity(name, "VARCHAR(" + length + ")", nullable);
        entity.setUnique();
        this.entities.add(entity);
    }

    public void addForeignKey(String name, String foreignKeyTable, String foreignKeyColumn, boolean onDeleteCascade, boolean nullable) {
        SQLEntity entity = new SQLEntity(name, "INT");
        if (!nullable) {
            entity.setNotNull();
        }
        entity.setForeignKey(foreignKeyTable, foreignKeyColumn, onDeleteCascade);
        this.entities.add(entity);
    }

    public void addInteger(String name, boolean nullable) {
        addEntity(name, "INT", nullable);
    }

    public void addLong(String name, boolean nullable) {
        addEntity(name, "BIGINT", nullable);
    }

    public void addBoolean(String name, boolean nullable) {
        addEntity(name, "BOOLEAN", nullable);
    }

    public void addBooleanWithDefault(String name, boolean defaultValue) {
        SQLEntity entity = new SQLEntity(name, "BOOLEAN", true);
        entity.setDefaultValue(defaultValue ? "TRUE" : "FALSE");
        this.entities.add(entity);
    }

    public void addEntity(String name, String type, boolean nullable) {
        this.entities.add(new SQLEntity(name, type, nullable));
    }

    public void addDate(String name, boolean nullable) {
        addEntity(name, "DATE", nullable);
    }

    public void addDateTime(String name, boolean nullable) {
        addEntity(name, "TIMESTAMP", nullable);
    }

    public void addDouble(String name, int length, int digits, boolean nullable) {
        addEntity(name, "DECIMAL(" + length + "," + digits + ")", nullable);
    }

}
