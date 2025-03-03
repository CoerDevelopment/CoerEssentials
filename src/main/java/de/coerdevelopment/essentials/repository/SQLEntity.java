package de.coerdevelopment.essentials.repository;

public class SQLEntity {

    private String name;
    private String type;
    private boolean isPrimary;
    private boolean isNotNull;
    private boolean isUnique;
    private boolean isAutoIncrement;
    private boolean isForeignKey;
    private String foreignKeyTable;
    private String foreignKeyColumn;
    private boolean onDeleteCascade;
    private String defaultValue;
    private String customSQL;

    public SQLEntity(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public SQLEntity(String name, String type, boolean nullable) {
        this.name = name;
        this.type = type;
        if (!nullable) {
            isNotNull = true;
        }
    }

    public String getSQL(SQLDialect dialect) {
        String sql = name;
        if (isAutoIncrement && dialect.equals(SQLDialect.POSTGRESQL)) {
            sql += " SERIAL";
        } else {
            sql += " " + type;
        }
        if (isPrimary) {
            sql += " PRIMARY KEY";
        }
        if (isNotNull) {
            sql += " NOT NULL";
        }
        if (isUnique) {
            sql += " UNIQUE";
        }
        if (isAutoIncrement && !dialect.equals(SQLDialect.POSTGRESQL)) {
            sql += " AUTO_INCREMENT";
        }
        if (isForeignKey) {
            sql += ",CONSTRAINT fk_" + name + "_" + foreignKeyTable + " FOREIGN KEY (" + name + ") REFERENCES " + foreignKeyTable + "(" + foreignKeyColumn + ")" + (onDeleteCascade ? " ON DELETE CASCADE" : "");
        }
        if (defaultValue != null) {
            sql += " DEFAULT " + defaultValue;
        }
        if (customSQL != null) {
            sql += " " + customSQL;
        }
        return sql;
    }

    // Getter and Setter

    public void setPrimary() {
        isPrimary = true;
    }

    public void setNotNull() {
        isNotNull = true;
    }

    public void setUnique() {
        isUnique = true;
    }

    public void setAutoIncrement() {
        isAutoIncrement = true;
    }

    public void setForeignKey(String table, String column, boolean onDeleteCascade) {
        isForeignKey = true;
        foreignKeyTable = table;
        foreignKeyColumn = column;
        this.onDeleteCascade = onDeleteCascade;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public boolean isNotNull() {
        return isNotNull;
    }

    public boolean isUnique() {
        return isUnique;
    }

    public boolean isAutoIncrement() {
        return isAutoIncrement;
    }

    public boolean isForeignKey() {
        return isForeignKey;
    }

    public String getForeignKeyTable() {
        return foreignKeyTable;
    }

    public String getForeignKeyColumn() {
        return foreignKeyColumn;
    }

    public void setDefaultValue(String value) {
        defaultValue = value;
    }

    public void setCustomSQL(String sql) {
        customSQL = sql;
    }

}
