package com.machinechecklist.service;

import com.machinechecklist.model.Kpi;
import com.machinechecklist.model.Machine;
import com.machinechecklist.model.User;
import com.machinechecklist.repo.ChecklistRecordsRepo;
import com.machinechecklist.repo.KpiRepo;
import com.machinechecklist.repo.MachineRepo;
import com.machinechecklist.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KpiService {

    private final KpiRepo kpiRepo;
    private final UserRepo userRepo;
    private final MachineRepo machineRepo;
    private final ChecklistRecordsRepo checklistRecordsRepo;

    public List<Kpi> getKpiByYearAndMonth(String year, String month) {
        return kpiRepo.findByYearAndMonth(year, month);
    }

    public List<Kpi> getKpiByManagerIdAndYearAndMonth(String managerId, String year, String month) {
        return kpiRepo.findByManagerIdAndYearAndMonth(managerId, year, month);
    }

    public List<Kpi> getKpiBySupervisorIdAndYearAndMonth(String supervisorId, String year, String month) {
        return kpiRepo.findBySupervisorIdAndYearAndMonth(supervisorId, year, month);
    }

    public Kpi getKpiByEmployeeIdAndYearAndMonth(String employeeId, String year, String month) {
        Optional<Kpi> kpiOptional = kpiRepo.findByEmployeeIdAndYearAndMonth(employeeId, year, month);
        return kpiOptional.orElseThrow(() -> new RuntimeException("ไม่พบข้อมูล KPI สำหรับ employeeId: " + employeeId + ", ปี: " + year + ", เดือน: " + month));
    }

    public void updateOrCreateKpi(String responsiblePersonId, String year, String month) {
        long machineCount = machineRepo.findByResponsiblePersonId(responsiblePersonId).stream()
                .filter(machine -> !"ยกเลิกใช้งาน".equals(machine.getMachineStatus()))
                .count();

        YearMonth yearMonth = YearMonth.of(Integer.parseInt(year), Integer.parseInt(month));
        int fridays = countFridaysInMonth(yearMonth);

        User user = userRepo.findByUsername(responsiblePersonId)
                .orElseThrow(() -> new RuntimeException("User not found for ID: " + responsiblePersonId));
        String employeeName = user.getFirstName() + " " + user.getLastName();

        Optional<Machine> machine = machineRepo.findByResponsiblePersonId(responsiblePersonId).stream()
                .filter(machine1 -> !"ยกเลิกใช้งาน".equals(machine1.getMachineStatus()))
                .findFirst();
        if (machine.isEmpty()) {
            throw new RuntimeException("No machine found for responsiblePersonId: " + responsiblePersonId);
        }
        String managerId = machine.get().getManagerId();
        String supervisorId = machine.get().getSupervisorId();

        Optional<Kpi> existingKpi = kpiRepo.findByEmployeeIdAndYearAndMonth(responsiblePersonId, year, month);
        Kpi kpi;
        if (existingKpi.isPresent()) {
            kpi = existingKpi.get();
            // Calculate date range: first Monday of the week containing day 1 to last Friday of the month
            LocalDate firstMonday = getFirstMondayOfWeekContainingFirstDay(yearMonth);
            LocalDate lastFriday = getLastFridayOfMonth(yearMonth);
            // Count checklist records for the update where userId = employeeId and reason_not_checked condition
            long checkedCount = checklistRecordsRepo.countByUserIdAndDateRangeAndReasonNotChecked(
                    responsiblePersonId,
                    firstMonday.atStartOfDay(),
                    lastFriday.atTime(23, 59, 59)
            );
            kpi.setCheckAll(fridays * machineCount);
            kpi.setChecked(checkedCount); // Update with sum from checklist_record
            kpi.setEmployeeName(employeeName);
            kpi.setManagerId(managerId);
            kpi.setSupervisorId(supervisorId);
        } else {
            kpi = new Kpi();
            kpi.setEmployeeId(responsiblePersonId);
            kpi.setYear(year);
            kpi.setMonth(month);
            kpi.setCheckAll(fridays * machineCount);
            kpi.setChecked(0L);
            kpi.setEmployeeName(employeeName);
            kpi.setManagerId(managerId);
            kpi.setSupervisorId(supervisorId);
        }

        kpiRepo.save(kpi);
    }

    private int countFridaysInMonth(YearMonth yearMonth) {
        LocalDate firstDay = yearMonth.atDay(1);
        LocalDate lastDay = yearMonth.atEndOfMonth();
        int fridays = 0;

        LocalDate date = firstDay;
        while (!date.isAfter(lastDay)) {
            if (date.getDayOfWeek().getValue() == 5) { // Friday
                fridays++;
            }
            date = date.plusDays(1);
        }
        return fridays;
    }

    private LocalDate getFirstMondayOfWeekContainingFirstDay(YearMonth yearMonth) {
        LocalDate date = yearMonth.atDay(1);
        while (date.getDayOfWeek().getValue() != 1) { // Find Monday of the week
            date = date.minusDays(1);
        }
        return date;
    }

    private LocalDate getLastFridayOfMonth(YearMonth yearMonth) {
        LocalDate date = yearMonth.atEndOfMonth();
        while (date.getDayOfWeek().getValue() != 5) { // Find last Friday
            date = date.minusDays(1);
        }
        return date;
    }
}
