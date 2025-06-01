package com.machinechecklist.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.machinechecklist.model.Machine;
import com.machinechecklist.repo.MachineRepo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MachineService {

    private final FileStorageService fileStorageService;
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
            String qrCodeBase64 = generateQRCode(machine.getQrCode(), machine.getMachineCode());
            return new MachineResponse(machine, qrCodeBase64);
        }
        throw new RuntimeException("Machine not found with id: " + id);
    }

    private String generateQRCode(String qrContent, String machineCode) {
        try {
            int qrSize = 200;
            int textAreaHeight = 20;
            int totalHeight = qrSize + textAreaHeight;

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, qrSize, qrSize);

            BufferedImage combinedImage = new BufferedImage(qrSize, totalHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = combinedImage.createGraphics();

            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, qrSize, totalHeight);

            for (int x = 0; x < qrSize; x++) {
                for (int y = 0; y < qrSize; y++) {
                    if (bitMatrix.get(x, y)) {
                        graphics.setColor(Color.BLACK);
                        graphics.fillRect(x, y, 1, 1);
                    }
                }
            }

            graphics.setColor(Color.BLACK);

            FontMetrics fontMetrics = graphics.getFontMetrics();
            int machineCodeWidth = fontMetrics.stringWidth(machineCode);

            graphics.drawString(machineCode, (qrSize - machineCodeWidth) / 2, qrSize + 45);

            graphics.dispose();

            // Convert to Base64
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            ImageIO.write(combinedImage, "PNG", pngOutputStream);
            return Base64.getEncoder().encodeToString(pngOutputStream.toByteArray());

        } catch (WriterException | IOException e) {
            throw new RuntimeException("Error generating QR code: " + e.getMessage());
        }
    }

    public Machine getMachineByMachineCode(String machineCode) {
        return machineRepo.findByMachineCode(machineCode).orElse(null);
    }

    public Machine createMachine(Machine machine, MultipartFile file) throws IOException {
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

        if (file != null) {
            log.info("file not null");
            String filename = fileStorageService.storeFile(file);
            machine.setImage(filename);
        }

        return machineRepo.save(machine);
    }

    public byte[] exportMachinesToExcel() throws IOException, WriterException {
        List<Machine> machines = machineRepo.findAll();
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Machine-QRCode");

        // Set column widths (in 1/256th of a character width)
        sheet.setColumnWidth(0, 8000); // machineName
        sheet.setColumnWidth(1, 8000); // machineCode
        sheet.setColumnWidth(2, 6000); // QR code image

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Machine Name", "Machine Code", "QR Code"};
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        int rowNum = 1;
        for (Machine machine : machines) {
            Row row = sheet.createRow(rowNum);

            // machineName
            Cell nameCell = row.createCell(0);
            nameCell.setCellValue(machine.getMachineName() != null ? machine.getMachineName() : "");

            // machineCode
            Cell codeCell = row.createCell(1);
            codeCell.setCellValue(machine.getMachineCode() != null ? machine.getMachineCode() : "");

            // QR code image
            if (machine.getQrCode() != null && !machine.getQrCode().isEmpty()) {
                // Generate QR code image
                QRCodeWriter qrCodeWriter = new QRCodeWriter();
                BitMatrix bitMatrix = qrCodeWriter.encode(machine.getQrCode(), BarcodeFormat.QR_CODE, 300, 300);
                BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

                // Overlay machineCode text on QR code
                String machineCode = machine.getMachineCode() != null ? machine.getMachineCode() : "";
                Graphics2D g2d = qrImage.createGraphics();
                g2d.setColor(Color.BLACK);
                int textWidth = g2d.getFontMetrics().stringWidth(machineCode);
                int textHeight = g2d.getFontMetrics().getHeight();

                // Define bottom region (bottom 20% of image, e.g., 40px for 200px height)
                int bottomRegionHeight = 40;
                int bottomRegionY = qrImage.getHeight() - bottomRegionHeight;

                // Center text horizontally and vertically in bottom region
                int x = (qrImage.getWidth() - textWidth) / 2;
                int y = bottomRegionY + (bottomRegionHeight + textHeight) / 2 - g2d.getFontMetrics().getDescent();

                // Draw white background for text readability
                g2d.setColor(Color.WHITE);
                g2d.fillRect(x - 5, bottomRegionY, textWidth + 10, bottomRegionHeight);
                g2d.setColor(Color.BLACK);
                g2d.drawString(machineCode, x, y);
                g2d.dispose();

                // Convert modified image to byte array
                ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
                ImageIO.write(qrImage, "PNG", pngOutputStream);
                byte[] qrCodeBytes = pngOutputStream.toByteArray();

                // Add picture to workbook
                int pictureIdx = workbook.addPicture(qrCodeBytes, Workbook.PICTURE_TYPE_PNG);
                CreationHelper helper = workbook.getCreationHelper();
                Drawing<?> drawing = sheet.createDrawingPatriarch();
                ClientAnchor anchor = helper.createClientAnchor();
                anchor.setCol1(2);
                anchor.setRow1(rowNum);
                anchor.setDx1(Units.pixelToEMU(50)); // Offset to center in cell
                anchor.setDy1(0);
                // Set anchor to match QR code size (200px)
                anchor.setCol2(2);
                anchor.setRow2(rowNum + 1);
                anchor.setDx2(Units.pixelToEMU(250)); // 200px + offset
                anchor.setDy2(Units.pixelToEMU(200)); // 200px

                //don't delete
                Picture picture = drawing.createPicture(anchor, pictureIdx);
                // No resize to maintain 1:1 aspect ratio

                // Adjust row height for QR code (200px converted to points)
                row.setHeight((short) Units.pixelToPoints(200));

                rowNum++; // Move to next row
            } else {
                Cell qrCell = row.createCell(2);
                qrCell.setCellValue("No QR Code");
                row.setHeight((short) (20 * 20)); // Default row height for text
                rowNum++;
            }
        }

        // Auto-size columns (optional, as we set widths manually)
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Write to byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
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
            machine.setMachineStatus(updatedMachine.getMachineStatus());

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
}
