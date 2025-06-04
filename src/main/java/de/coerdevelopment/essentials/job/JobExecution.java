package de.coerdevelopment.essentials.job;

import de.coerdevelopment.essentials.CoerEssentials;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class JobExecution {

    public UUID uuid;
    public Job job;
    public JobOptions options;
    public String data;
    public String stackTrace;
    public Timestamp startTime;
    public Timestamp endTime;
    public long duration;

    public JobExecution(Job job, JobOptions options) {
        this.uuid = UUID.randomUUID();
        this.job = job;
        this.options = options;
    }

    public void execute() {
        if (!options.retryOnFailure) {
            executeJob();
        } else {
            for (int attempt = 1; attempt <= options.maxRetries; attempt++) {
                try {
                    executeJob();
                    return;
                } catch (Exception e) {
                    if (attempt != options.maxRetries) {
                        try {
                            Thread.sleep(options.retryDelayMilliseconds);
                        } catch (InterruptedException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
            }
        }
    }

    private void executeJob() {
        startTime = new Timestamp(System.currentTimeMillis());
        try {
            job.before(this);
            job.execute(this);
            job.finish(this);
        } catch (Exception e) {
            logError("JobExecution '" + job.name + "' execution failed: " + e.getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            stackTrace = sw.toString();
            throw e;
        } finally {
            endTime = new Timestamp(System.currentTimeMillis());
            duration = ChronoUnit.MILLIS.between(startTime.toInstant(), endTime.toInstant());
            if (CoerEssentials.getInstance().getSQLModule() != null) {
                JobExecutionRepository.getInstance().insertLog(this);
            }
        }
    }

    public void logInfo(String message) {
        CoerEssentials.getInstance().logInfo(message);
    }

    public void logWarning(String message) {
        CoerEssentials.getInstance().logWarning(message);
    }

    public void logError(String message) {
        CoerEssentials.getInstance().logError(message);
    }

}
