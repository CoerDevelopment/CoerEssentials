package de.coerdevelopment.essentials.repository;

import java.util.ArrayList;
import java.util.List;

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

}
