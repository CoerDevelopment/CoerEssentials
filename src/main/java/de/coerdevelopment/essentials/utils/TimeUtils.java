package de.coerdevelopment.essentials.utils;

public class TimeUtils {

    private static TimeUtils instance;

    public static TimeUtils getInstance() {
        if (instance == null) {
            instance = new TimeUtils();
        }
        return instance;
    }

    private TimeUtils() {
        // private constructor used to secure singleton
    }

    public Long getMillisecondsFromMinutes(int minutes) {
        return (long) minutes * 60 * 1000;
    }

    public Long getMillisecondsFromHours(int hours) {
        return (long) hours * 60 * 60 * 1000;
    }

    public Long getMillisecondsFromDays(int days) {
        return (long) days * 24 * 60 * 60 * 1000;
    }

}
