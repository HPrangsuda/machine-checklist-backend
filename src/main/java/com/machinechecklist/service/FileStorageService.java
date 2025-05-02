package com.machinechecklist.service;

import com.machinechecklist.config.FileStorageConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    public String storeFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Failed to store empty file");
        }

        // Generate unique filename with UUID
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFilename = UUID.randomUUID().toString() + fileExtension;

        // Encode filename to Base64
        String base64Filename = Base64.getEncoder().encodeToString(newFilename.getBytes());

        // Save file to upload directory using Base64 filename
        Path destinationPath = Paths.get(FileStorageConfig.getUploadDir()).resolve(base64Filename);
        Files.copy(file.getInputStream(), destinationPath);

        return base64Filename;
    }
}
