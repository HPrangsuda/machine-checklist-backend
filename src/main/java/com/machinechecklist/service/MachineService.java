package com.machinechecklist.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.machinechecklist.model.Machine;
import com.machinechecklist.repo.MachineRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MachineService {
    private final MachineRepo machineRepo;

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

    public Machine createMachine(Machine machine) throws WriterException, IOException {
        // Save the machine first to ensure all fields are populated
        Machine savedMachine = machineRepo.save(machine);

        // Generate QR code URL with machineCode instead of id
        String qrCodeUrl = "http://localhost:4200/machine-checklist/checklist/" + savedMachine.getMachineCode();
        // Generate QR code and convert to Base64 string
        String qrCodeBase64 = generateQRCodeBase64(qrCodeUrl);
        savedMachine.setQrCode(qrCodeBase64);

        // Update the machine with the QR code
        return machineRepo.save(savedMachine);
    }

    private String generateQRCodeBase64(String url) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, 200, 200);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);

        byte[] pngData = pngOutputStream.toByteArray();
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngData);
    }

    public static class MachineResponse {
        private Machine machine;
        private String qrCodeImage;

        public MachineResponse(Machine machine, String qrCodeImage) {
            this.machine = machine;
            this.qrCodeImage = qrCodeImage;
        }

        public Machine getMachine() { return machine; }
        public void setMachine(Machine machine) { this.machine = machine; }
        public String getQrCodeImage() { return qrCodeImage; }
        public void setQrCodeImage(String qrCodeImage) { this.qrCodeImage = qrCodeImage; }
    }
}
