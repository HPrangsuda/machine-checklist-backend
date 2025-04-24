package com.machinechecklist.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {
    private final String uploadDir = "uploads/"; // ปรับ path ตามต้องการ

    public String storeFile(MultipartFile file) throws IOException {
        // สร้างโฟลเดอร์ถ้ายังไม่มี
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // สร้างชื่อไฟล์ที่ไม่ซ้ำ
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);

        // บันทึกไฟล์
        Files.copy(file.getInputStream(), filePath);

        return fileName; // หรือคืน full path ถ้าต้องการ
    }
}
