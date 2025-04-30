package com.machinechecklist.service;

import com.machinechecklist.model.*;
import com.machinechecklist.repo.ChecklistRecordsRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.machinechecklist.repo.MachineRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChecklistRecordsService {

    private final FileStorageService fileStorageService;
    private final ChecklistRecordsRepo checklistRecordsRepo;
    private final MachineRepo machineRepo;
    private final ObjectMapper objectMapper;

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
        try {
            ChecklistRecords record = new ChecklistRecords();
            record.setMachineCode(request.getMachineCode());
            record.setMachineName(request.getMachineName());
            record.setMachineStatus(request.getMachineStatus());

            String checklistJson = objectMapper.writeValueAsString(request.getChecklistItems());
            record.setMachineChecklist(checklistJson);

            record.setMachineNote(request.getNote());
            record.setMachineImage(request.getMachineImage());
            record.setUserId(request.getUserId());
            record.setUserName(request.getUserName());
            record.setSupervisor(request.getSupervisor());
            record.setManager(request.getManager());
            record.setDateCreated(new Date());

            LocalDate today = LocalDate.now(ZoneId.of("Asia/Bangkok"));
            if (today.getDayOfWeek() == DayOfWeek.FRIDAY) {
                record.setChecklistStatus("รอหัวหน้างานตรวจสอบ");
                record.setRecheck(true);
            } else {
                record.setChecklistStatus("ดำเนินการเสร็จสิ้น");
                record.setRecheck(false);
            }

            ChecklistRecords savedRecord = checklistRecordsRepo.save(record);

            Machine machine = machineRepo.findByMachineCode(request.getMachineCode())
                    .orElseThrow(() -> new RuntimeException("Machine not found with code: " + request.getMachineCode()));

            machine.setMachineStatus(savedRecord.getMachineStatus());
            machine.setCheckStatus(savedRecord.getChecklistStatus());

            if (file != null) {
                String fileName = fileStorageService.storeFile(file);
                machine.setImage(fileName);
            }
            machineRepo.save(machine);
            return savedRecord;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save checklist record: " + e.getMessage());
        }
    }

    public ChecklistRecords approveChecklist(Long checklistId) {
        ChecklistRecords checklist = checklistRecordsRepo.findById(checklistId)
                .orElseThrow(() -> new RuntimeException("Checklist not found with id: " + checklistId));

        Machine machine = machineRepo.findByMachineCode(checklist.getMachineCode())
                .orElseThrow(() -> new RuntimeException("Machine not found with code: " + checklist.getMachineCode()));

        if ("รอหัวหน้างานตรวจสอบ".equals(checklist.getChecklistStatus())) {
            checklist.setChecklistStatus("รอผู้จัดการฝ่ายตรวจสอบ");
            checklist.setDateSupervisorChecked(new Date());
            machine.setCheckStatus("รอผู้จัดการฝ่ายตรวจสอบ");
        } else if ("รอผู้จัดการฝ่ายตรวจสอบ".equals(checklist.getChecklistStatus())) {
            checklist.setChecklistStatus("ดำเนินการเสร็จสิ้น");
            checklist.setDateManagerChecked(new Date());
            machine.setCheckStatus("ดำเนินการเสร็จสิ้น");
        } else {
            throw new RuntimeException("Invalid checklist status for approval: " + checklist.getChecklistStatus());
        }

        machineRepo.save(machine);
        return checklistRecordsRepo.save(checklist);
    }
}