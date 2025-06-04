package de.coerdevelopment.essentials.job;

import com.google.gson.Gson;
import de.coerdevelopment.essentials.repository.*;
import org.postgresql.util.PGobject;

import java.sql.Timestamp;
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
        super("JobExecutionLog");
    }

    @Override
    public void createTable() {
        SQLTable table = new SQLTable(tableName);
        table.addAutoKey("id");
        table.addString("executionId", 120, false);
        table.addString("name", 120, false);
        SQLEntity options = new SQLEntity("options", "JSONB", true);
        table.addEntity(options);
        SQLEntity data = new SQLEntity("data", "TEXT", true);
        table.addEntity(data);
        SQLEntity stacktrace = new SQLEntity("stacktrace", "TEXT", true);
        table.addEntity(stacktrace);
        table.addDateTime("startTime", false);
        table.addDateTime("endTime", true);
        table.addLong("duration", false);
        table.addDateTime("logTime", false);
        sql.executeQuery(table.getCreateTableStatement());
        // add index
        sql.executeQuery("CREATE INDEX IF NOT EXISTS idx_job_log_execution_id ON " + tableName + " (executionId);");
        sql.executeQuery("CREATE INDEX IF NOT EXISTS idx_job_log_name ON " + tableName + " (name);");

    }

    public void insertLog(JobExecution jobExecution) {
        String query = "INSERT INTO " + tableName + " (executionId, name, options, data, stacktrace, startTime, endTime, duration, logTime) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String optionsJson = new Gson().toJson(jobExecution.options);
        sql.executeQuery(query, jobExecution.uuid.toString(),
                jobExecution.job.getName(),
                SQLUtil.getPGobject(optionsJson),
                jobExecution.data,
                jobExecution.stackTrace,
                jobExecution.startTime,
                jobExecution.endTime,
                jobExecution.duration,
                new Timestamp(System.currentTimeMillis()));
    }

    private final ColumnMapper<JobExecution> columnMapper = new ColumnMapper<JobExecution>() {
        @Override
        public Map<String, Object> mapColumns(JobExecution obj) {
            PGobject optionsObject = new PGobject();
            optionsObject.setType("jsonb");
            try {
                Gson gson = new Gson();
                optionsObject.setValue(gson.toJson(obj.options));
            } catch (Exception e) {
                throw new RuntimeException("Failed to convert JobOptions to JSON: " + e.getMessage(), e);
            }
            return Map.of("executionId", obj.uuid.toString(),
                          "name", obj.job.getName(),
                          "options", optionsObject,
                          "data", obj.data,
                          "stacktrace", obj.stackTrace,
                          "startTime", obj.startTime,
                          "endTime", obj.endTime,
                          "duration", obj.duration,
                        "logTime", new Timestamp(System.currentTimeMillis()));
        }
    };

}
