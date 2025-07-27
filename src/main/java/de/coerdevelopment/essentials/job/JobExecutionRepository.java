package de.coerdevelopment.essentials.job;

import com.google.gson.Gson;
import de.coerdevelopment.essentials.repository.*;

import java.time.OffsetDateTime;
import java.util.Map;

public class JobExecutionRepository extends Repository {

    private static JobExecutionRepository instance;

    public static JobExecutionRepository getInstance() {
        if (instance == null) {
            instance = new JobExecutionRepository();
        }
        return instance;
    }

    private JobExecutionRepository() {
        super("job_executions");
    }

    @Override
    public void createTable() {
        SQLTable table = new SQLTable(tableName);
        table.addAutoKey("job_id");
        table.addString("execution_uuid", 120, false);
        table.addString("name", 120, false);
        SQLEntity options = new SQLEntity("options", "JSONB", true);
        table.addEntity(options);
        SQLEntity data = new SQLEntity("data", "TEXT", true);
        table.addEntity(data);
        SQLEntity stacktrace = new SQLEntity("stacktrace", "TEXT", true);
        table.addEntity(stacktrace);
        table.addDateTimeWithTimezone("started_at", false);
        table.addDateTimeWithTimezone("ended_at", true);
        table.addLong("duration", false);
        table.addDateTimeWithTimezone("logged_at", false);
        sql.executeQuery(table.getCreateTableStatement());
        // add index
        sql.executeQuery("CREATE INDEX IF NOT EXISTS idx_job_log_execution_uuid ON " + tableName + " (execution_uuid);");
        sql.executeQuery("CREATE INDEX IF NOT EXISTS idx_job_log_name ON " + tableName + " (name);");

    }

    public void insertLog(JobExecution jobExecution) {
        String query = "INSERT INTO " + tableName + " (execution_uuid, name, options, data, stacktrace, started_at, ended_at, duration, logged_at) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String optionsJson = new Gson().toJson(jobExecution.options);
        sql.executeQuery(query, jobExecution.uuid.toString(),
                jobExecution.job.getName(),
                SQLUtil.getJsonPgObject(optionsJson),
                jobExecution.data,
                jobExecution.stackTrace,
                jobExecution.startetAt,
                jobExecution.endedAt,
                jobExecution.duration,
                OffsetDateTime.now());
    }

    private final ColumnMapper<JobExecution> columnMapper = new ColumnMapper<JobExecution>() {
        @Override
        public Map<String, Object> mapColumns(JobExecution obj) {
            return Map.of("execution_uuid", obj.uuid.toString(),
                          "name", obj.job.getName(),
                          "options", SQLUtil.getJsonPgObject(new Gson().toJson(obj.options)),
                          "data", obj.data,
                          "stacktrace", obj.stackTrace,
                          "started_at", obj.startetAt,
                          "ended_at", obj.endedAt,
                          "duration", obj.duration,
                        "logged_at", OffsetDateTime.now());
        }
    };

}
