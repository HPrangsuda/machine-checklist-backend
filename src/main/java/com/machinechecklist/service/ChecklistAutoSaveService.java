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

import static com.machinechecklist.model.enums.Frequency.MONTHLY;
import static com.machinechecklist.model.enums.Frequency.WEEKLY;

@Service
@RequiredArgsConstructor
public class ChecklistAutoSaveService {
    private final MachineRepo machineRepo;
    private final ChecklistRecordsRepo checklistRecordsRepo;

    @Scheduled(cron = "0 59 23 * * FRI")
    @Transactional
    public void autoSaveChecklistRecords() {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate friday = today.with(DayOfWeek.FRIDAY);

        Date startOfWeek = Date.from(monday.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endOfWeek = Date.from(friday.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());

        List<Machine> machines = machineRepo.findAll().stream()
                .filter(machine -> !"ยกเลิกใช้งาน".equals(machine.getMachineStatus()) && machine.getResetPeriod() == WEEKLY)
                .toList();

        for (Machine machine : machines) {
            String responsibleId = machine.getResponsiblePersonId();

            List<ChecklistRecords> records = checklistRecordsRepo.findByMachineCodeAndUserIdAndDateCreatedBetween(
                    machine.getMachineCode(), responsibleId, startOfWeek, endOfWeek);

            if (records.isEmpty()) {
                createDefaultChecklistRecord(machine);
            }
        }
    }

    @Scheduled(cron = "0 59 23 1 * *")
    @Transactional
    public void autoSaveChecklistRecordsMonth() {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDate firstDayOfPreviousMonth = today.minusMonths(1).withDayOfMonth(1);
        LocalDate lastDayOfPreviousMonth = today.minusMonths(1).withDayOfMonth(today.minusMonths(1).lengthOfMonth());

        Date startOfPreviousMonth = Date.from(firstDayOfPreviousMonth.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endOfPreviousMonth = Date.from(lastDayOfPreviousMonth.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());

        List<Machine> machines = machineRepo.findAll().stream()
                .filter(machine -> !"ยกเลิกใช้งาน".equals(machine.getMachineStatus()) && machine.getResetPeriod() == MONTHLY)
                .toList();

        for (Machine machine : machines) {
            String responsibleId = machine.getResponsiblePersonId();

            List<ChecklistRecords> records = checklistRecordsRepo.findByMachineCodeAndUserIdAndDateCreatedBetween(
                    machine.getMachineCode(), responsibleId, startOfPreviousMonth, endOfPreviousMonth);

            if (records.isEmpty()) {
                createDefaultChecklistRecord(machine);
            }
        }
    }

    private void createDefaultChecklistRecord(Machine machine) {
        try {
            ChecklistRecords record = new ChecklistRecords();

            if(machine.getSupervisorId() != null) {
                record.setChecklistStatus("รอหัวหน้างานตรวจสอบ");
            }else {
                record.setChecklistStatus("รอผู้จัดการฝ่ายตรวจสอบ");
            }
            record.setDateCreated(new Date());
            record.setMachineChecklist("");
            record.setMachineCode(machine.getMachineCode());
            record.setMachineName(machine.getMachineName());
            record.setMachineNote("บันทึกอัตโนมัติ");
            record.setMachineStatus(machine.getMachineStatus());
            record.setManager(machine.getManagerId());
            record.setRecheck(true);
            record.setReasonNotChecked("ไม่ได้ดำเนินการ");
            record.setSupervisor(machine.getSupervisorId());
            record.setUserId(machine.getResponsiblePersonId());
            record.setUserName(machine.getResponsiblePersonName());
            ChecklistRecords savedRecord = checklistRecordsRepo.save(record);

            machine.setMachineStatus(savedRecord.getMachineStatus());
            machine.setCheckStatus(savedRecord.getChecklistStatus());
            machineRepo.save(machine);

        } catch (Exception e) {
            throw new RuntimeException("Failed to auto-save checklist record for machine " + machine.getMachineCode() + ": " + e.getMessage());
        }
    }
}