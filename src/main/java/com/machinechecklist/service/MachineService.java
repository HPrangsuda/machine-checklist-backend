package com.machinechecklist.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.machinechecklist.model.Machine;
import com.machinechecklist.repo.MachineRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MachineService {
    private final MachineRepo machineRepo;
    @Value("${file.upload.directory:/uploads/machines}") // ตั้งค่าใน application.properties หรือใช้ค่าเริ่มต้น
    private String uploadDirectory;

    public List<Machine> getAllMachines() {
        return machineRepo.findAll();
    }

    public List<Machine> getMachinesByResponsiblePerson(String personId) {
        return machineRepo.findByResponsiblePersonId(personId);
    }

    public List<Machine> getMachinesByResponsibleAll(String personId) {
        return machineRepo.findBySupervisorIdOrManagerId(personId);
    }

    public MachineResponse getMachineWithQRCode(Long id) {
        Optional<Machine> machineOpt = machineRepo.findById(id);
        if (machineOpt.isPresent()) {
            Machine machine = machineOpt.get();
            if (machine.getQrCode() == null || machine.getQrCode().isEmpty()) {
                throw new RuntimeException("QR code data is missing for machine id: " + id);
            }
            String qrCodeBase64 = generateQRCode(machine.getQrCode());
            return new MachineResponse(machine, qrCodeBase64);
        }
        throw new RuntimeException("Machine not found with id: " + id);
    }

    private String generateQRCode(String qrContent) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 200, 200);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return Base64.getEncoder().encodeToString(pngOutputStream.toByteArray());
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Error generating QR code: " + e.getMessage());
        }
    }

    public Machine getMachineByMachineCode(String machineCode) {
        return machineRepo.findByMachineCode(machineCode).orElse(null);
    }

    public Machine createMachine(Machine machine, MultipartFile file) {
        if (machine.getMachineCode() == null || machine.getMachineCode().isEmpty()) {
            throw new IllegalArgumentException("Machine code is required");
        }

        if (machineRepo.existsByMachineCode(machine.getMachineCode())) {
            throw new IllegalStateException("Machine code already exists");
        }

        machine.setCheckStatus("false");

        String qrCodeJson = String.format("{\"status\": true, \"code\": \"%s\"}",
                machine.getMachineCode());
        machine.setQrCode(qrCodeJson);

        if (file != null && !file.isEmpty()) {
            try {
                String fileName = StringUtils.cleanPath(file.getOriginalFilename());
                if (fileName.contains("..")) {
                    throw new IllegalArgumentException("ชื่อไฟล์ไม่ถูกต้อง: " + fileName);
                }

                byte[] imageBytes = file.getBytes();
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                machine.setImage("data:" + file.getContentType() + ";base64," + base64Image);

            } catch (IOException e) {
                throw new RuntimeException("ไม่สามารถประมวลผลไฟล์รูปภาพได้", e);
            }
        }

        return machineRepo.save(machine);
    }

    @Setter
    @Getter
    public static class MachineResponse {
        private Machine machine;
        private String qrCodeImage;

        public MachineResponse(Machine machine, String qrCodeImage) {
            this.machine = machine;
            this.qrCodeImage = qrCodeImage;
        }

    }

    public Machine updateMachine(Long id, Machine updatedMachine) {
        Optional<Machine> existingMachine = machineRepo.findById(id);
        if (existingMachine.isPresent()) {
            Machine machine = existingMachine.get();
            machine.setResponsiblePersonId(updatedMachine.getResponsiblePersonId());
            machine.setResponsiblePersonName(updatedMachine.getResponsiblePersonName());
            machine.setSupervisorId(updatedMachine.getSupervisorId());
            machine.setSupervisorName(updatedMachine.getSupervisorName());
            machine.setManagerId(updatedMachine.getManagerId());
            machine.setManagerName(updatedMachine.getManagerName());
            machine.setFrequency(updatedMachine.getFrequency());
            machine.setMachineStatus(updatedMachine.getMachineStatus());
            machine.setMachineTypeName(updatedMachine.getMachineTypeName());
            return machineRepo.save(machine);
        } else {
            throw new RuntimeException("Machine not found with id: " + id);
        }
    }

    public void deleteMachine(Long id) {
        if (machineRepo.existsById(id)) {
            machineRepo.deleteById(id);
        } else {
            throw new RuntimeException("Machine not found with id: " + id);
        }
    }

    public class FileStorageException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public FileStorageException(String message) {
            super(message);
        }

        public FileStorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
