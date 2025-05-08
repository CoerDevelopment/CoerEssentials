package de.coerdevelopment.essentials.repository;

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

}
