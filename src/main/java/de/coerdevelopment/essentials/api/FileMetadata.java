package de.coerdevelopment.essentials.api;

import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;

public class FileMetadata {

    public long fileId;
    public String fileName;
    public String storagePath;
    public String storedFileName;
    public String mimeType;
    public long fileSizeBytes;
    public long accountId;
    public OffsetDateTime createdAt;

    public FileMetadata(long fileId, String fileName, String storagePath, String storedFileName, String mimeType, long fileSizeBytes, long accountId, OffsetDateTime createdAt) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.storagePath = storagePath;
        this.storedFileName = storedFileName;
        this.mimeType = mimeType;
        this.fileSizeBytes = fileSizeBytes;
        this.accountId = accountId;
        this.createdAt = createdAt;
    }

    public FileMetadata(long accountId, String fileName, MultipartFile file, String storagePath, String storedFileName, OffsetDateTime createdAt) {
        this.fileName = fileName;
        this.storagePath = storagePath;
        this.storedFileName = storedFileName;
        this.mimeType = file.getContentType();
        this.fileSizeBytes = file.getSize();
        this.accountId = accountId;
        this.createdAt = createdAt;
    }
}
