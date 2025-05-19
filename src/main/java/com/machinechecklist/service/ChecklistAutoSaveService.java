package com.machinechecklist.service;

import com.machinechecklist.model.ChecklistRecords;
import com.machinechecklist.model.Machine;
import com.machinechecklist.repo.ChecklistRecordsRepo;
import com.machinechecklist.repo.MachineRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChecklistAutoSaveService {
    private final MachineRepo machineRepo;
    private final ChecklistRecordsRepo checklistRecordsRepo;

    @Scheduled(cron = "0 59 14 * * MON", zone = "Asia/Bangkok")
    @Transactional
    public void autoSaveChecklistRecords() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Bangkok"));
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate friday = today.with(DayOfWeek.FRIDAY);

        Date startOfWeek = Date.from(monday.atStartOfDay(ZoneId.of("Asia/Bangkok")).toInstant());
        Date endOfWeek = Date.from(friday.atTime(23, 59, 59).atZone(ZoneId.of("Asia/Bangkok")).toInstant());

        List<Machine> machines = machineRepo.findAll();

        for (Machine machine : machines) {
            String responsibleId = machine.getResponsiblePersonId();

            List<ChecklistRecords> records = checklistRecordsRepo.findByMachineCodeAndUserIdAndDateCreatedBetween(
                    machine.getMachineCode(), responsibleId, startOfWeek, endOfWeek);

            if (records.isEmpty()) {
                createDefaultChecklistRecord(machine);
            }
        }
    }

    private void createDefaultChecklistRecord(Machine machine) {
        try {
            ChecklistRecords record = new ChecklistRecords();
            record.setMachineCode(machine.getMachineCode());
            record.setMachineName(machine.getMachineName());
            record.setMachineStatus("ปกติ");
            record.setChecklistStatus("รอหัวหน้างานตรวจสอบ");
            record.setRecheck(true);record.setMachineChecklist("");
            record.setMachineNote("บันทึกอัตโนมัติเมื่อไม่มีรายการตรวจสอบในสัปดาห์");
            record.setUserId(machine.getResponsiblePersonId());
            record.setUserName("ระบบอัตโนมัติ");
            record.setSupervisor("");
            record.setManager("");
            record.setDateCreated(new Date());

            ChecklistRecords savedRecord = checklistRecordsRepo.save(record);

            machine.setMachineStatus(savedRecord.getMachineStatus());
            machine.setCheckStatus(savedRecord.getChecklistStatus());
            machineRepo.save(machine);

        } catch (Exception e) {
            throw new RuntimeException("Failed to auto-save checklist record for machine " + machine.getMachineCode() + ": " + e.getMessage());
        }
    }
}
