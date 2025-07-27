package de.coerdevelopment.essentials.utils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

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

    public OffsetDateTime getOffsetDateTimeFromMilliseconds(long milliseconds) {
        return OffsetDateTime.ofInstant(
                Instant.ofEpochMilli(milliseconds),
                ZoneOffset.UTC
        );
    }

    public OffsetDateTime getOffsetDateTimeEpochStart() {
        return OffsetDateTime.ofInstant(
                Instant.EPOCH,
                ZoneId.systemDefault()
        );
    }

}
