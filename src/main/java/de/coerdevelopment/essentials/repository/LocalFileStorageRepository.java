package de.coerdevelopment.essentials.repository;

import de.coerdevelopment.essentials.CoerEssentials;
import de.coerdevelopment.essentials.api.FileMetadata;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LocalFileStorageRepository extends Repository{

    private static LocalFileStorageRepository instance;

    public static LocalFileStorageRepository getInstance() {
        if (instance == null) {
            instance = new LocalFileStorageRepository();
        }
        return instance;
    }

    private LocalFileStorageRepository() {
        super("local_file_storage_metadata");
    }

    @Override
    public void createTable() {
        SQLTable table = new SQLTable(tableName);
        table.addAutoKey("file_id");
        table.addString("file_name", 256, false);
        table.addString("storage_path", 256, false);
        table.addString("stored_file_name", 256, false);
        table.addString("mime_type", 128, false);
        table.addLong("file_size_bytes", false);
        table.addForeignKey("account_id", CoerEssentials.getInstance().getAccountModule().tableName, "account_id", false, false);
        table.addDateTimeWithTimezone("created_at", false);
        sql.executeQuery(table.getCreateTableStatement());

        sql.executeQuery(table.getMultiUniqueStatement("uq_idx_fileName_accountId", "file_name", "account_id"));

        sql.executeQuery(table.getCreateIndexStatement("idx_fileName_accountId", "file_name", "account_id"));
    }

    public void insertFileMetadata(List<FileMetadata> files) throws SQLException {
        sql.batchInsert(tableName, files, columnMapper, 200);
    }

    public List<FileMetadata> getFilesByAccountId(long accountId) {
        List<FileMetadata> files = new ArrayList<>();
        String query = "SELECT * FROM " + tableName + " WHERE account_id = ?";
        sql.executeQuery(query, new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    files.add(columnMapper.getObjectFromResultSetEntry(rs));
                }
            }
        }, accountId);
        return files;
    }

    public FileMetadata getFileMetadataByFileName(long accountId, String fileName) {
        List<FileMetadata> files = new ArrayList<>();
        String query = "SELECT * FROM " + tableName + " WHERE account_id = ? AND file_name = ?";
        sql.executeQuery(query, new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    files.add(columnMapper.getObjectFromResultSetEntry(rs));
                }
            }
        }, accountId, fileName);
        return files.isEmpty() ? null : files.getFirst();
    }

    public List<FileMetadata> getFileMetadataByAccounts(List<Long> accountIds, String fileName) {
        List<FileMetadata> files = new ArrayList<>();
        if (accountIds.isEmpty()) {
            return files;
        }
        String query = "SELECT * FROM " + tableName + " WHERE account_id IN (" + SQLUtil.longListToSearchTerm(accountIds) + ") AND file_name = ?";
        sql.executeQuery(query, new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    files.add(columnMapper.getObjectFromResultSetEntry(rs));
                }
            }
        }, fileName);
        return files;
    }

    public FileMetadata getFileMetadataByUUID(String uuid) {
        List<FileMetadata> files = new ArrayList<>();
        String query = "SELECT * FROM " + tableName + " WHERE stored_file_name LIKE ?";
        sql.executeQuery(query, new StatementCustomAction() {
            @Override
            public void onAfterExecute(PreparedStatement statement) throws SQLException {
                ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    files.add(columnMapper.getObjectFromResultSetEntry(rs));
                }
            }
        }, uuid + "%");
        return files.isEmpty() ? null : files.getFirst();
    }

    public void deleteMetadata(long accountId, String fileName) {
        String query = "DELETE FROM " + tableName + " WHERE account_id = ? AND file_name = ?";
        sql.executeQuery(query, accountId, fileName);
    }

    private static ColumnMapper<FileMetadata> columnMapper = new ColumnMapper<>() {
        @Override
        public Map<String, Object> mapColumns(FileMetadata obj) {
            return Map.of(
                    "file_name", obj.fileName,
                    "storage_path", obj.storagePath,
                    "stored_file_name", obj.storedFileName,
                    "mime_type", obj.mimeType,
                    "file_size_bytes", obj.fileSizeBytes,
                    "account_id", obj.accountId,
                    "created_at", obj.createdAt
            );
        }

        @Override
        public FileMetadata getObjectFromResultSetEntry(ResultSet resultSet) throws SQLException {
            long fileId = resultSet.getLong("file_id");
            String fileName = resultSet.getString("file_name");
            String storagePath = resultSet.getString("storage_path");
            String storedFileName = resultSet.getString("stored_file_name");
            String mimeType = resultSet.getString("mime_type");
            long fileSizeBytes = resultSet.getLong("file_size_bytes");
            long accountId = resultSet.getLong("account_id");
            OffsetDateTime createdAt = resultSet.getObject("created_at", OffsetDateTime.class);
            return new FileMetadata(fileId, fileName, storagePath, storedFileName, mimeType, fileSizeBytes, accountId, createdAt);
        }
    };

}
