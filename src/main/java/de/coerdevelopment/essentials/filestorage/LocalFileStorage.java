package de.coerdevelopment.essentials.filestorage;

import de.coerdevelopment.essentials.api.FileMetadata;
import de.coerdevelopment.essentials.repository.LocalFileStorageRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class LocalFileStorage extends FileStorage {

    private final Path storageDirectory;

    public LocalFileStorage(Path storageDirectory, List<String> supportedMimeTypes, long maxFileSizeBytes, long maxStorageSizeBytes) throws IOException {
        super(supportedMimeTypes, maxFileSizeBytes, maxStorageSizeBytes);
        this.storageDirectory = storageDirectory.toAbsolutePath().normalize();

        if (!Files.exists(this.storageDirectory)) {
            Files.createDirectories(this.storageDirectory);
        }
    }

    @Override
    public String store(long accountId, MultipartFile file, String fileName) throws IOException {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1);
        }
        String uniqueFileName = UUID.randomUUID() + "." + extension;
        Path destinationPath = storageDirectory.resolve(uniqueFileName);

        FileMetadata fileMetadata = new FileMetadata(accountId, fileName, file, storageDirectory.toFile().getAbsolutePath().toString(), uniqueFileName, OffsetDateTime.now());

        try {
            LocalFileStorageRepository.getInstance().insertFileMetadata(Arrays.asList(fileMetadata));
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IOException("File already exists");
        }

        Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);

        return uniqueFileName;
    }

    @Override
    public Resource load(long accountId, String fileName) {
        FileMetadata metadata = LocalFileStorageRepository.getInstance().getFileMetadataByFileName(accountId, fileName);
        if (metadata == null) {
            throw new RuntimeException("File not found: " + fileName);
        }
        try {
            Path filePath = storageDirectory.resolve(metadata.storedFileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File not found or not readable: " + fileName);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid file path: " + fileName, e);
        }
    }

    @Override
    public void delete(long accountId, String fileName) {
        FileMetadata metadata = LocalFileStorageRepository.getInstance().getFileMetadataByFileName(accountId, fileName);
        if (metadata == null) {
            return;
        }
        storageDirectory.resolve(metadata.storedFileName).toFile().delete();
        LocalFileStorageRepository.getInstance().deleteMetadata(accountId, fileName);
    }

    @Override
    public long getUsedStorageSizeBytes() {
        try {
            return Files.walk(storageDirectory)
                    .filter(Files::isRegularFile)
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException e) {
                            return 0L;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            return 0L;
        }
    }

    public Path getStorageDirectory() {
        return storageDirectory;
    }
}
