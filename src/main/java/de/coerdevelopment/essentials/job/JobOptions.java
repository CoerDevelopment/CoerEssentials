package de.coerdevelopment.essentials.job;

import java.time.Duration;

public class JobOptions {

    public final String jobName;
    public final boolean enabled;

    public final boolean repeating;
    public final long intervalMilliseconds;
    public final String cronExpression;

    public final boolean retryOnFailure;
    public final int maxRetries;
    public final long retryDelayMilliseconds;

    public JobOptions(String jobName, boolean enabled, boolean repeating, Duration interval, String cronExpression, boolean retryOnFailure, int maxRetries, Duration retryDelay) {
        this.jobName = jobName;
        this.enabled = enabled;
        this.repeating = repeating;
        this.intervalMilliseconds = interval != null ? interval.toMillis() : -1;
        this.cronExpression = cronExpression;
        this.retryOnFailure = retryOnFailure;
        this.maxRetries = maxRetries;
        this.retryDelayMilliseconds = retryDelay != null ? retryDelay.toMillis() : -1;
    }
}
