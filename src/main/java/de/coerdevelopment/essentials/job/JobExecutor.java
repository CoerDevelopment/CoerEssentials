package de.coerdevelopment.essentials.job;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import de.coerdevelopment.essentials.CoerEssentials;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class JobExecutor {

    public static Map<String, Job> registeredJobs = new HashMap<>();

    public static void registerJob(Job job) {
        registeredJobs.put(job.getName(), job);
        JobOptionsConfig.getInstance().saveOptions(job.name, job.getDefaultOptions(), false);
    }

    private static JobExecutor instance;

    public static JobExecutor getInstance() {
        if (instance == null) {
            instance = new JobExecutor();
        }
        return instance;
    }

    private final ScheduledExecutorService scheduler;
    private final ExecutorService workerPool;
    private final CronParser cronParser;

    private final JobConfig config;

    private JobExecutor() {
        this.config = new JobConfig();
        this.config.initConfig();
        this.config.loadConfig();
        this.scheduler = Executors.newScheduledThreadPool(config.schedulerPoolSize);
        this.workerPool = Executors.newFixedThreadPool(config.schedulerPoolSize);
        this.cronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
        init();
    }

    public void submit(Job job, JobOptions options) {
        if (options.cronExpression != null && !options.cronExpression.isEmpty()) {
            scheduleCronJob(job, options);
        } else if (options.repeating && options.intervalMilliseconds != -1) {
            scheduleRepeatingJob(job, options);
        } else {
            runOnce(job, options);
        }
    }

    private void init() {
        for (JobOptions options : JobOptionsConfig.getInstance().loadOptions()) {
            Job job = registeredJobs.get(options.jobName);
            if (job == null) {
                throw new RuntimeException("Job '" + options.jobName + "' not found");
            }
            if (!options.enabled) {
                continue;
            }
            submit(job, options);
        }
    }

    private void runOnce(Job job, JobOptions options) {
        workerPool.submit(() -> createJobExecution(job, options).execute());
    }

    private void scheduleRepeatingJob(Job job, JobOptions options) {
        scheduler.scheduleAtFixedRate(() -> createJobExecution(job, options).execute(),
                0,
                options.intervalMilliseconds,
                TimeUnit.MILLISECONDS);
    }

    private void scheduleCronJob(Job job, JobOptions options) {
        scheduler.submit(() -> scheduleNextCronExecution(job, options));
    }

    private void scheduleNextCronExecution(Job job, JobOptions options) {
        try {
            Cron cron = cronParser.parse(options.cronExpression);
            cron.validate();
            ExecutionTime executionTime = ExecutionTime.forCron(cron);

            while (true) {
                ZonedDateTime now = ZonedDateTime.now();
                Optional<ZonedDateTime> nextExecution = executionTime.nextExecution(now);
                if (nextExecution.isEmpty()) {
                    CoerEssentials.getInstance().logWarning("No further executions found for cron expression: " + options.cronExpression);
                    break;
                }
                long delay = Duration.between(now, nextExecution.get()).toMillis();

                Thread.sleep(delay);
                workerPool.submit(() -> createJobExecution(job, options).execute());
            }
        } catch (Exception e) {
            CoerEssentials.getInstance().logError("Cron scheduling failed: " + e.getMessage());
        }
    }

    private JobExecution createJobExecution(Job job, JobOptions options) {
        return new JobExecution(job, options);
    }

    public void shutdown() {
        scheduler.shutdown();
        workerPool.shutdown();
    }
}