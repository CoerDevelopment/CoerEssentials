package de.coerdevelopment.essentials.job.instances;

import de.coerdevelopment.essentials.CoerEssentials;
import de.coerdevelopment.essentials.job.Job;
import de.coerdevelopment.essentials.job.JobExecution;
import de.coerdevelopment.essentials.job.JobOptions;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class AccountCacheJob extends Job {

    public AccountCacheJob() {
        super("AccountCacheJob", "Caches all accounts to improve performance");
    }

    @Override
    protected void before(JobExecution execution) {

    }

    @Override
    protected void execute(JobExecution execution) {
        int updateAmount = CoerEssentials.getInstance().getAccountModule().updateAccounts();
        execution.data = "Cached " + updateAmount + " accounts.";
    }

    @Override
    protected void finish(JobExecution execution) {

    }

    @Override
    public JobOptions getDefaultOptions() {
        return JobOptions.repeating("AccountCacheJob", Duration.of(30, ChronoUnit.MINUTES));
    }
}
