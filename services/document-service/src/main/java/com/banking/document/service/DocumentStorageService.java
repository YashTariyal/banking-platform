package com.banking.document.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class DocumentStorageService {

    private static final Logger log = LoggerFactory.getLogger(DocumentStorageService.class);

    private final Path basePath;

    public DocumentStorageService(@Value("${document.storage.base-path:/tmp/banking-documents}") String basePath) {
        this.basePath = Paths.get(basePath);
        try {
            Files.createDirectories(this.basePath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create document storage directory", e);
        }
    }

    public StorageResult store(MultipartFile file, UUID customerId) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String storedFilename = UUID.randomUUID() + extension;

        // Organize by date and customer
        LocalDate today = LocalDate.now();
        Path targetDir = basePath
                .resolve(String.valueOf(today.getYear()))
                .resolve(String.format("%02d", today.getMonthValue()))
                .resolve(customerId.toString());

        Files.createDirectories(targetDir);
        Path targetPath = targetDir.resolve(storedFilename);

        // Calculate checksum while copying
        String checksum;
        try (InputStream is = file.getInputStream()) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

            while ((read = is.read(buffer)) != -1) {
                md.update(buffer, 0, read);
                baos.write(buffer, 0, read);
            }

            Files.write(targetPath, baos.toByteArray());
            checksum = HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }

        String relativePath = basePath.relativize(targetPath).toString();
        log.info("Stored document: {} -> {}", originalFilename, relativePath);

        return new StorageResult(storedFilename, relativePath, checksum, file.getSize());
    }

    public byte[] retrieve(String storagePath) throws IOException {
        Path fullPath = basePath.resolve(storagePath);
        if (!Files.exists(fullPath)) {
            throw new IOException("Document not found: " + storagePath);
        }
        return Files.readAllBytes(fullPath);
    }

    public void delete(String storagePath) throws IOException {
        Path fullPath = basePath.resolve(storagePath);
        Files.deleteIfExists(fullPath);
        log.info("Deleted document: {}", storagePath);
    }

    public Path storeGeneratedPdf(byte[] pdfContent, UUID customerId, String filename) throws IOException {
        LocalDate today = LocalDate.now();
        Path targetDir = basePath
                .resolve(String.valueOf(today.getYear()))
                .resolve(String.format("%02d", today.getMonthValue()))
                .resolve(customerId.toString())
                .resolve("statements");

        Files.createDirectories(targetDir);
        Path targetPath = targetDir.resolve(filename);
        Files.write(targetPath, pdfContent);

        return targetPath;
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(dotIndex) : "";
    }

    public record StorageResult(String fileName, String storagePath, String checksum, long fileSize) {}
}
