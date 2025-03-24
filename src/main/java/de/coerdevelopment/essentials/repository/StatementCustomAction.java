package de.coerdevelopment.essentials.repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class StatementCustomAction {

    /**
     * Will be executed before the PreparedStatement is executed
     */
    public void onBeforeExecute(PreparedStatement statement) throws SQLException {

    }

    /**
     * Will be exceuted after the PreparedStatement has been executed
     */
    public void onAfterExecute(PreparedStatement statement) throws SQLException {

    }
}