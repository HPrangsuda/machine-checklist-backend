package com.machinechecklist.service;

import com.machinechecklist.dto.ChecklistItemDTO;
import com.machinechecklist.model.*;
import com.machinechecklist.repo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
    public void updateOverdueChecklists() {
        List<ChecklistRecords> pendingRecords = checklistRecordsRepo.findByChecklistStatusIn(
                List.of("รอหัวหน้างานตรวจสอบ", "รอผู้จัดการฝ่ายตรวจสอบ")
        );

        for (ChecklistRecords record : pendingRecords) {
            String currentStatus = record.getChecklistStatus();
            record.setChecklistStatus(currentStatus + "-เกินกำหนด");
            checklistRecordsRepo.save(record);

            Machine machine = machineRepo.findByMachineCode(record.getMachineCode())
                    .orElseThrow(() -> new RuntimeException("Machine not found with code: " + record.getMachineCode()));
            machine.setCheckStatus("รอดำเนินการ");
            machineRepo.save(machine);
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
}