package de.coerdevelopment.essentials.repository;

import org.postgresql.util.PGobject;

import java.util.List;

public class SQLUtil {

    /**
     * Converts a list of string to a search term which can be used in a SQL query
     * Input: {1,2,3}
     * Output: 1,2,3
     */
    public static String integerListToSearchTerm(List<Integer> input) {
        List<String> terms = input.stream().map(String::valueOf).toList();
        return String.join(",", terms);
    }

    /**
     * Converts a list of string to a search term which can be used in a SQL query
     * Input: {"Max","Hans","Klaus"}
     * Output: Max,Hans,Klaus
     */
    public static String stringListToSearchTerm(List<String> input) {
        return String.join(",", input);
    }

    /**
     * Converts a list of string to a search term which can be used in a SQL query
     * Input: {1,2,3}
     * Output: 1,2,3
     */
    public static String longListToSearchTerm(List<Long> input) {
        List<String> terms = input.stream().map(String::valueOf).toList();
        return String.join(",", terms);
    }

    public static PGobject getJsonPgObject(String json) {
        PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        try {
            pgObject.setValue(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set JSON value in PGobject: " + e.getMessage(), e);
        }
        return pgObject;
    }

    public static PGobject getEnumPgObject(DatabaseEnum databaseEnum) {
        PGobject pgObject = new PGobject();
        pgObject.setType(databaseEnum.getDatabaseEnumName());
        try {
            pgObject.setValue(databaseEnum.toString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to set enum value in PGobject: " + e.getMessage(), e);
        }
        return pgObject;
    }

}
