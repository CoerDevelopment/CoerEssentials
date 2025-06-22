package de.coerdevelopment.essentials.repository;

import de.coerdevelopment.essentials.api.FileMetadata;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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
        super("LocalFileStorageMetadata");
    }

    @Override
    public void createTable() {
        SQLTable table = new SQLTable(tableName);
        table.addAutoKey("fileId");
        table.addString("fileName", 256, false);
        table.addString("storagePath", 256, false);
        table.addString("storedFileName", 256, false);
        table.addString("mimeType", 128, false);
        table.addLong("fileSizeBytes", false);
        table.addForeignKey("accountId", "Account", "accountId", false, false);
        table.addDateTime("createdAt", false);
        sql.executeQuery(table.getCreateTableStatement());

        sql.executeQuery(table.getMultiUniqueStatement("uq_idx_fileName_accountId", "fileName", "accountId"));

        sql.executeQuery(table.getCreateIndexStatement("idx_fileName_accountId", "fileName", "accountId"));
    }

    public void insertFileMetadata(List<FileMetadata> files) throws SQLException {
        sql.batchInsert(tableName, files, columnMapper, 200);
    }

    public List<FileMetadata> getFilesByAccountId(int accountId) {
        List<FileMetadata> files = new ArrayList<>();
        String query = "SELECT * FROM " + tableName + " WHERE accountId = ?";
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

    public FileMetadata getFileMetadataByFileName(int accountId, String fileName) {
        List<FileMetadata> files = new ArrayList<>();
        String query = "SELECT * FROM " + tableName + " WHERE accountId = ? AND fileName = ?";
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

    public void deleteMetadata(int accountId, String fileName) {
        String query = "DELETE FROM " + tableName + " WHERE accountId = ? AND fileName = ?";
        sql.executeQuery(query, accountId, fileName);
    }

    private static ColumnMapper<FileMetadata> columnMapper = new ColumnMapper<>() {
        @Override
        public Map<String, Object> mapColumns(FileMetadata obj) {
            return Map.of(
                    "fileName", obj.fileName,
                    "storagePath", obj.storagePath,
                    "storedFileName", obj.storedFileName,
                    "mimeType", obj.mimeType,
                    "fileSizeBytes", obj.fileSizeBytes,
                    "accountId", obj.accountId,
                    "createdAt", obj.createdAt
            );
        }

        @Override
        public FileMetadata getObjectFromResultSetEntry(ResultSet resultSet) throws SQLException {
            int fileId = resultSet.getInt("fileId");
            String fileName = resultSet.getString("fileName");
            String storagePath = resultSet.getString("storagePath");
            String storedFileName = resultSet.getString("storedFileName");
            String mimeType = resultSet.getString("mimeType");
            long fileSizeBytes = resultSet.getLong("fileSizeBytes");
            int accountId = resultSet.getInt("accountId");
            Timestamp createdAt = resultSet.getTimestamp("createdAt");
            return new FileMetadata(fileId, fileName, storagePath, storedFileName, mimeType, fileSizeBytes, accountId, createdAt);
        }
    };

}
