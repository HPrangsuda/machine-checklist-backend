package com.machinechecklist.service;

import com.machinechecklist.dto.ChecklistItemDTO;
import com.machinechecklist.model.*;
import com.machinechecklist.model.enums.Frequency;
import com.machinechecklist.repo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChecklistRecordsService {

    private final FileStorageService fileStorageService;
    private final ChecklistRecordsRepo checklistRecordsRepo;
    private final MachineRepo machineRepo;
    private final MachineChecklistRepo machineChecklistRepo;
    private final ObjectMapper objectMapper;
    private final UserRepo userRepo;
    private final KpiService kpiService;

    @Scheduled(cron = "0 1 0 * * MON")
    @Transactional
    public void updateOverdueChecklistsWeek() {
        List<ChecklistRecords> pendingRecords = checklistRecordsRepo.findByChecklistStatusIn(
                List.of("รอหัวหน้างานตรวจสอบ", "รอผู้จัดการฝ่ายตรวจสอบ")
        );

        for (ChecklistRecords record : pendingRecords) {
            Machine machine = machineRepo.findByMachineCode(record.getMachineCode())
                    .orElseThrow(() -> new RuntimeException("ไม่พบเครื่องจักรที่มีรหัส: " + record.getMachineCode()));

            if (machine.getResetPeriod() == Frequency.WEEKLY) {
                String currentStatus = record.getChecklistStatus();
                record.setChecklistStatus(currentStatus + "-เกินกำหนด");
                checklistRecordsRepo.save(record);

                machine.setCheckStatus("รอดำเนินการ");
                machineRepo.save(machine);
            }
        }
    }

    @Scheduled(cron = "0 0 0 1 * *")
    @Transactional
    public void updateOverdueChecklistsMonth() {
        List<ChecklistRecords> pendingRecords = checklistRecordsRepo.findByChecklistStatusIn(
                List.of("รอหัวหน้างานตรวจสอบ", "รอผู้จัดการฝ่ายตรวจสอบ")
        );

        for (ChecklistRecords record : pendingRecords) {
            Machine machine = machineRepo.findByMachineCode(record.getMachineCode())
                    .orElseThrow(() -> new RuntimeException("ไม่พบเครื่องจักรที่มีรหัส: " + record.getMachineCode()));

            if (machine.getResetPeriod() == Frequency.MONTHLY) {
                String currentStatus = record.getChecklistStatus();
                record.setChecklistStatus(currentStatus + "-เกินกำหนด");
                checklistRecordsRepo.save(record);

                machine.setCheckStatus("รอดำเนินการ");
                machineRepo.save(machine);
            }
        }
    }

    public List<ChecklistRecords> getAllRecords() {
        return checklistRecordsRepo.findAll();
    }

    public Optional<ChecklistRecords> getRecordById(Long id) {
        return checklistRecordsRepo.findById(id);
    }

    public List<ChecklistRecords> getChecklistRecordsByUserId(String personId) {
        User user = userRepo.findByUsername(personId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String department = user.getDepartment();
        return checklistRecordsRepo.findByMachineDepartment(department);
    }

    public List<ChecklistRecords> getRecordByResponsiblePerson(String personId) {
        return checklistRecordsRepo.findByUserId(personId);
    }

    public List<ChecklistRecords> getRecheck(String personId) {
        return checklistRecordsRepo.findByManagerOrSupervisor(personId);
    }

    public ChecklistRecords saveChecklistRecord(ChecklistRequestDTO request, MultipartFile file) {
        Machine machine = machineRepo.findByMachineCode(request.getMachineCode())
                .orElseThrow(() -> new RuntimeException("Machine not found with code: " + request.getMachineCode()));
        String responsibleId = machine.getResponsiblePersonId();

        try {
            ChecklistRecords record = new ChecklistRecords();
            record.setMachineCode(request.getMachineCode());
            record.setMachineName(request.getMachineName());
            record.setMachineStatus(request.getMachineStatus());

            String checklistJson = objectMapper.writeValueAsString(request.getChecklistItems());
            record.setMachineChecklist(checklistJson);

            record.setMachineNote(request.getMachineNote());

            if (file != null) {
                String fileName = fileStorageService.storeFile(file);
                record.setMachineImage(fileName);
            }

            record.setUserId(request.getUserId());
            record.setUserName(request.getUserName());
            record.setSupervisor(request.getSupervisor());
            record.setManager(request.getManager());
            record.setDateCreated(new Date());
            record.setJobDetails(request.getJobDetails());

            if (Objects.equals(responsibleId, record.getUserId()) && "รอดำเนินการ".equals(machine.getCheckStatus()) && !LocalDate.now().getDayOfWeek().equals(DayOfWeek.SATURDAY) && !LocalDate.now().getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
                if (machine.getSupervisorId() != null) {
                    record.setChecklistStatus("รอหัวหน้างานตรวจสอบ");
                } else {
                    record.setChecklistStatus("รอผู้จัดการฝ่ายตรวจสอบ");
                }
                record.setRecheck(true);

                // Update checkStatus in machineChecklist
                for (ChecklistItemDTO item : request.getChecklistItems()) {
                    if (item.getId() != null) {
                        MachineChecklist checklist = machineChecklistRepo.findById(item.getId())
                                .orElseThrow(() -> new RuntimeException("Checklist item not found with id: " + item.getId()));
                        checklist.setCheckStatus(true);
                        machineChecklistRepo.save(checklist);
                    } else {
                        throw new RuntimeException("Checklist item id is missing");
                    }
                }

                // KPI
                LocalDate currentDate = LocalDate.now();
                String year = String.valueOf(currentDate.getYear());
                String monthStr = String.format("%02d", currentDate.getMonthValue());
                kpiService.updateOrCreateKpi(responsibleId, year, monthStr);

                machine.setCheckStatus(record.getChecklistStatus());
            } else {
                record.setChecklistStatus("ดำเนินการเสร็จสิ้น");
                record.setRecheck(false);
            }

            ChecklistRecords savedRecord = checklistRecordsRepo.save(record);

            machine.setMachineStatus(savedRecord.getMachineStatus());
            machineRepo.save(machine);

            return savedRecord;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save checklist record: " + e.getMessage());
        }
    }

    public ChecklistRecords approveChecklist(Long checklistId, ChecklistRecords request) {
        ChecklistRecords checklist = checklistRecordsRepo.findById(checklistId)
                .orElseThrow(() -> new RuntimeException("Checklist not found with id: " + checklistId));

        Machine machine = machineRepo.findByMachineCode(checklist.getMachineCode())
                .orElseThrow(() -> new RuntimeException("Machine not found with code: " + checklist.getMachineCode()));

        String responsibleId = machine.getResponsiblePersonId();
        String reasonNotChecked = request.getReasonNotChecked();

        if ("รอหัวหน้างานตรวจสอบ".equals(checklist.getChecklistStatus())) {
            if (machine.getManagerId() == null) {
                checklist.setChecklistStatus("ดำเนินการเสร็จสิ้น");
                checklist.setReasonNotChecked(request.getReasonNotChecked());
                checklist.setDateSupervisorChecked(new Date());
                machine.setCheckStatus("ดำเนินการเสร็จสิ้น");
            } else {
                checklist.setChecklistStatus("รอผู้จัดการฝ่ายตรวจสอบ");
                checklist.setReasonNotChecked(request.getReasonNotChecked());
                checklist.setDateSupervisorChecked(new Date());
                machine.setCheckStatus("รอผู้จัดการฝ่ายตรวจสอบ");
            }
        } else if ("รอผู้จัดการฝ่ายตรวจสอบ".equals(checklist.getChecklistStatus())) {
            checklist.setChecklistStatus("ดำเนินการเสร็จสิ้น");
            checklist.setReasonNotChecked(request.getReasonNotChecked());
            checklist.setDateManagerChecked(new Date());
            machine.setCheckStatus("ดำเนินการเสร็จสิ้น");
        } else {
            throw new RuntimeException("Invalid checklist status for approval: " + checklist.getChecklistStatus());
        }

        if (reasonNotChecked != null && !reasonNotChecked.isEmpty() && !reasonNotChecked.equals("ผู้รับผิดชอบไม่ดำเนินการ")) {
            String year = new SimpleDateFormat("yyyy").format(checklist.getDateCreated());
            String monthStr = new SimpleDateFormat("MM").format(checklist.getDateCreated());
            kpiService.updateOrCreateKpi(responsibleId, year, monthStr);
        }

        machineRepo.save(machine);
        return checklistRecordsRepo.save(checklist);
    }

    public byte[] exportChecklistToExcel() throws IOException {
        List<ChecklistRecords> records = checklistRecordsRepo.findAll();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Checklist Records");

        Row headerRow = sheet.createRow(0);
        String[] headers = {"Checklist ID", "Recheck", "Machine Code", "Machine Name", "Machine Status",
                "Machine Checklist", "Machine Note", "Machine Image", "User ID", "User Name",
                "Date Created", "Supervisor", "Date Supervisor Checked", "Manager",
                "Date Manager Checked", "Checklist Status", "Reason Not Checked", "Job Details"};
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

        CellStyle dateCellStyle = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));

        int rowNum = 1;
        for (ChecklistRecords record : records) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(Optional.ofNullable(record.getChecklistId()).map(String::valueOf).orElse(""));
            row.createCell(1).setCellValue(record.getRecheck() != null ? record.getRecheck() : false);
            row.createCell(2).setCellValue(Optional.ofNullable(record.getMachineCode()).orElse(""));
            row.createCell(3).setCellValue(Optional.ofNullable(record.getMachineName()).orElse(""));
            row.createCell(4).setCellValue(Optional.ofNullable(record.getMachineStatus()).orElse(""));
            row.createCell(5).setCellValue(Optional.ofNullable(record.getMachineChecklist()).orElse(""));
            row.createCell(6).setCellValue(Optional.ofNullable(record.getMachineNote()).orElse(""));
            row.createCell(7).setCellValue(Optional.ofNullable(record.getMachineImage()).orElse(""));
            row.createCell(8).setCellValue(Optional.ofNullable(record.getUserId()).orElse(""));
            row.createCell(9).setCellValue(Optional.ofNullable(record.getUserName()).orElse(""));

            Cell dateCreatedCell = row.createCell(10);
            if (record.getDateCreated() != null) {
                dateCreatedCell.setCellValue(record.getDateCreated());
                dateCreatedCell.setCellStyle(dateCellStyle);
            }

            row.createCell(11).setCellValue(Optional.ofNullable(record.getSupervisor()).orElse(""));

            Cell dateSupervisorCell = row.createCell(12);
            if (record.getDateSupervisorChecked() != null) {
                dateSupervisorCell.setCellValue(record.getDateSupervisorChecked());
                dateSupervisorCell.setCellStyle(dateCellStyle);
            }

            row.createCell(13).setCellValue(Optional.ofNullable(record.getManager()).orElse(""));

            Cell dateManagerCell = row.createCell(14);
            if (record.getDateManagerChecked() != null) {
                dateManagerCell.setCellValue(record.getDateManagerChecked());
                dateManagerCell.setCellStyle(dateCellStyle);
            }

            row.createCell(15).setCellValue(Optional.ofNullable(record.getChecklistStatus()).orElse(""));
            row.createCell(16).setCellValue(Optional.ofNullable(record.getReasonNotChecked()).orElse(""));
            row.createCell(17).setCellValue(Optional.ofNullable(record.getJobDetails()).orElse(""));
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }
}