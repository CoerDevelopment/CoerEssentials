package de.coerdevelopment.essentials.job.instances;

import de.coerdevelopment.essentials.CoerEssentials;
import de.coerdevelopment.essentials.api.AccountLogin;
import de.coerdevelopment.essentials.job.Job;
import de.coerdevelopment.essentials.job.JobExecution;

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
}
