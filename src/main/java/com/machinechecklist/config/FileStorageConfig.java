package com.machinechecklist.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileStorageConfig implements WebMvcConfigurer {

    private static final String UPLOAD_DIR = "/opt/acme/machine/uploads/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/opt/acme/machine/uploads/**")
                .addResourceLocations("file:" + UPLOAD_DIR);
    }

    // Create upload directory if it doesn't exist
    public FileStorageConfig() {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not create upload directory!", e);
        }
    }

    // Getter for upload directory path
    public static String getUploadDir() {
        return UPLOAD_DIR;
    }
}