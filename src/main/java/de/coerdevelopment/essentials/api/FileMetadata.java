package de.coerdevelopment.essentials.api;

import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;

public class FileMetadata {

    public int fileId;
    public String fileName;
    public String storagePath;
    public String storedFileName;
    public String mimeType;
    public long fileSizeBytes;
    public int accountId;
    public OffsetDateTime createdAt;

    public FileMetadata(int fileId, String fileName, String storagePath, String storedFileName, String mimeType, long fileSizeBytes, int accountId, OffsetDateTime createdAt) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.storagePath = storagePath;
        this.storedFileName = storedFileName;
        this.mimeType = mimeType;
        this.fileSizeBytes = fileSizeBytes;
        this.accountId = accountId;
        this.createdAt = createdAt;
    }

    public FileMetadata(int accountId, String fileName, MultipartFile file, String storagePath, String storedFileName, OffsetDateTime createdAt) {
        this.fileName = fileName;
        this.storagePath = storagePath;
        this.storedFileName = storedFileName;
        this.mimeType = file.getContentType();
        this.fileSizeBytes = file.getSize();
        this.accountId = accountId;
        this.createdAt = createdAt;
    }
}
