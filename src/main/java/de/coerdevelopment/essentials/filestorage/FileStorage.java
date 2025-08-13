package de.coerdevelopment.essentials.filestorage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public abstract class FileStorage {

    /**
     * List of supported MIME types for files to be stored
     * If the list contains "*", all MIME types are supported
     */
    protected List<String> supportedMimeTypes;
    /**
     * Maximum size a single file can have in bytes
     * If set to -1, there is no limit
     */
    protected long maxFileSizeBytes;
    /**
     * Maximum size of the storage in bytes
     * If set to -1, there is no limit
     */
    protected long maxStorageSizeBytes = -1;

    public FileStorage(List<String> supportedMimeTypes, long maxFileSizeBytes, long maxStorageSizeBytes) {
        this.supportedMimeTypes = supportedMimeTypes;
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.maxStorageSizeBytes = maxStorageSizeBytes;
    }

    /**
     * Stores a file in the storage
     * @return the path or identifier of the stored file
     * @throws IOException if an error occurs during storage
     */
    public abstract String store(long accountId, MultipartFile file, String fileName) throws IOException;

    public String store(long accountId, MultipartFile file) throws IOException {
        return store(accountId, file, file.getOriginalFilename());
    }

    /**
     * Loads a file from the storage
     * @param fileName the name of the file to load
     */
    public abstract Resource load(long accountId, String fileName);

    /**
     * Deletes a file from the storage
     */
    public abstract void delete(long accountId, String fileName);

    /**
     * Returns the current used storage size in bytes
     */
    public abstract long getUsedStorageSizeBytes();

    /**
     * Validates if the file can be stored in the storage
     * @param file
     * @exception throws IOException if the file is invalid or cannot be processed
     */
    protected void validateFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("Unable to store empty files");
        }
        if (maxFileSizeBytes > 0 && file.getSize() > maxFileSizeBytes) {
            throw new IOException("File size exceeds the maximum allowed size of " + maxFileSizeBytes + " bytes");
        }

        if (maxStorageSizeBytes > 0 && file.getSize() + getUsedStorageSizeBytes() > maxStorageSizeBytes) {
            throw new IOException("Storage is full");
        }

        if (!isMimeTypeSupported(getMimeType(file))) {
            throw new IOException("Unsupported mime type: " + file.getContentType());
        }
    }

    public String getMimeType(MultipartFile file) {
        return file != null ? file.getContentType() : null;
    }

    public List<String> getSupportedMimeTypes() {
        return supportedMimeTypes;
    }

    public boolean isMimeTypeSupported(String mimeType) {
        return mimeType != null && (supportedMimeTypes.contains("*") || supportedMimeTypes.contains(mimeType));
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public long getMaxStorageSizeBytes() {
        return maxStorageSizeBytes;
    }
}
