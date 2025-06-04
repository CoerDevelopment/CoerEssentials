package de.coerdevelopment.essentials.job.instances;

import de.coerdevelopment.essentials.CoerEssentials;
import de.coerdevelopment.essentials.api.AccountLogin;
import de.coerdevelopment.essentials.job.Job;
import de.coerdevelopment.essentials.job.JobExecution;
import de.coerdevelopment.essentials.job.JobOptions;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class AccountLoginHistoryJob extends Job {

    public static final List<AccountLogin> loginsToBeProcessed = new ArrayList<>();

    public AccountLoginHistoryJob() {
        super("AccountLoginHistory", "Logs the login for accounts to database");
    }

    @Override
    protected void before(JobExecution execution) {

    }

    @Override
    protected void execute(JobExecution execution) {
        int recordsInserted = 0;
        if (!loginsToBeProcessed.isEmpty()) {
            List<AccountLogin> currentLoginsToBeProcessed = new ArrayList<>(loginsToBeProcessed);
            CoerEssentials.getInstance().getAccountModule().accountLoginRepository.insertLogins(currentLoginsToBeProcessed);
            recordsInserted = loginsToBeProcessed.size();
            loginsToBeProcessed.removeAll(currentLoginsToBeProcessed);
        }
        execution.data = "Records processed: " + recordsInserted;
    }

    @Override
    protected void finish(JobExecution execution) {

    }

    @Override
    public JobOptions getDefaultOptions() {
        return new JobOptions(
                "AccountLoginHistory",
                true,
                true,
                Duration.of(5, ChronoUnit.MINUTES),
                null,
                false,
                0,
                null);
    }
}
