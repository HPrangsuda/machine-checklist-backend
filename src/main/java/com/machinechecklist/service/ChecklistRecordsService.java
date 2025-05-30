package com.machinechecklist.service;

import com.machinechecklist.dto.ChecklistItemDTO;
import com.machinechecklist.model.*;
import com.machinechecklist.repo.ChecklistRecordsRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.machinechecklist.repo.KpiRepo;
import com.machinechecklist.repo.MachineChecklistRepo;
import com.machinechecklist.repo.MachineRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
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
    private final KpiRepo kpiRepo;
    private final ObjectMapper objectMapper;


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
            machine.setCheckStatus(currentStatus + "-เกินกำหนด");
            machineRepo.save(machine);
        }
    }

    public List<ChecklistRecords> getAllRecords() {
        return checklistRecordsRepo.findAll();
    }

    public Optional<ChecklistRecords> getRecordById(Long id) {
        return checklistRecordsRepo.findById(id);
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

            record.setMachineNote(request.getNote());

            if (file != null) {
                String fileName = fileStorageService.storeFile(file);
                record.setMachineImage(fileName);
            }

            record.setUserId(request.getUserId());
            record.setUserName(request.getUserName());
            record.setSupervisor(request.getSupervisor());
            record.setManager(request.getManager());
            record.setDateCreated(new Date());

            LocalDate today = LocalDate.now(ZoneId.systemDefault());
            if (today.getDayOfWeek() == DayOfWeek.FRIDAY && Objects.equals(responsibleId, record.getUserId())) {
                if(machine.getSupervisorId() != null) {
                    record.setChecklistStatus("รอหัวหน้างานตรวจสอบ");
                }else {
                    record.setChecklistStatus("รอผู้จัดการฝ่ายตรวจสอบ");
                }
                record.setRecheck(true);
            } else {
                record.setChecklistStatus("ดำเนินการเสร็จสิ้น");
                record.setRecheck(false);
            }

            ChecklistRecords savedRecord = checklistRecordsRepo.save(record);

            for (ChecklistItemDTO item : request.getChecklistItems()) {
                System.out.println("sss");
                //if (item.getId() != null) {

//                    MachineChecklist checklist = machineChecklistRepo.findById(item.getId())
//                            .orElseThrow(() -> new RuntimeException("Checklist item not found with id: " + item.getId()));
//                    checklist.setCheckStatus(true);
//                    machineChecklistRepo.save(checklist);
               // } else {
                //    throw new RuntimeException("Checklist item id is missing");
               // }
            }

            machine.setMachineStatus(savedRecord.getMachineStatus());
            machine.setCheckStatus(savedRecord.getChecklistStatus());
            machineRepo.save(machine);

            if (Objects.equals(responsibleId, record.getUserId())) {
                LocalDate currentDate = LocalDate.now();
                String year = String.valueOf(currentDate.getYear());
                String month = String.format("%02d", currentDate.getMonthValue());
                Optional<Kpi> kpiOptional = kpiRepo.findByEmployeeIdAndYearAndMonth(responsibleId, year, month);
                if (kpiOptional.isPresent()) {
                    Kpi kpi = kpiOptional.get();
                    kpi.setChecked(kpi.getChecked() + 1);
                    kpiRepo.save(kpi);
                } else {
                    throw new RuntimeException("Kpi record not found for employeeId: " + responsibleId + ", year: " + year + ", month: " + month);
                }
            }

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
            checklist.setChecklistStatus("รอผู้จัดการฝ่ายตรวจสอบ");
            checklist.setReasonNotChecked(request.getReasonNotChecked());
            checklist.setDateSupervisorChecked(new Date());
            machine.setCheckStatus("รอผู้จัดการฝ่ายตรวจสอบ");
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
            String month = new SimpleDateFormat("MM").format(checklist.getDateCreated());
            Optional<Kpi> kpiOptional = kpiRepo.findByEmployeeIdAndYearAndMonth(responsibleId, year, month);
            if (kpiOptional.isPresent()) {
                Kpi kpi = kpiOptional.get();
                kpi.setChecked(kpi.getChecked() + 1);
                kpiRepo.save(kpi);
            } else {
                throw new RuntimeException("Kpi record not found for employeeId: " + responsibleId + ", year: " + year + ", month: " + month);
            }
        }

        machineRepo.save(machine);
        return checklistRecordsRepo.save(checklist);
    }
}