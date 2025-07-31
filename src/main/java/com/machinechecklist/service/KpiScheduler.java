package com.machinechecklist.service;

import com.machinechecklist.model.Kpi;
import com.machinechecklist.model.Machine;
import com.machinechecklist.model.User;
import com.machinechecklist.repo.ChecklistRecordsRepo;
import com.machinechecklist.repo.KpiRepo;
import com.machinechecklist.repo.MachineRepo;
import com.machinechecklist.repo.UserRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KpiScheduler {
    private final MachineRepo machineRepo;
    private final UserRepo userRepo;
    private final KpiRepo kpiRepo;
    private final ChecklistRecordsRepo checklistRecordsRepo;

    @Scheduled(cron = "0 1 0 1 * ?")
    @Transactional
    public void createKpiRecords() {
        LocalDate currentDate = LocalDate.now();
        String year = String.valueOf(currentDate.getYear());
        String month = String.format("%02d", currentDate.getMonthValue());

        YearMonth yearMonth = YearMonth.of(currentDate.getYear(), currentDate.getMonth());
        int fridays = countFridaysInMonth(yearMonth);

        List<Machine> machines = machineRepo.findAll().stream()
                .filter(machine -> !"ยกเลิกใช้งาน".equals(machine.getMachineStatus()))
                .toList();

        Map<String, Long> machineCountByResponsiblePerson = machines.stream()
                .filter(machine -> machine.getResponsiblePersonId() != null)
                .collect(Collectors.groupingBy(
                        Machine::getResponsiblePersonId,
                        Collectors.counting()
                ));

        machineCountByResponsiblePerson.forEach((responsiblePersonId, machineCount) -> {
            Kpi kpi = new Kpi();
            kpi.setEmployeeId(responsiblePersonId);
            kpi.setYear(year);
            kpi.setMonth(month);
            kpi.setCheckAll(fridays * machineCount);
            kpi.setChecked(0L);

            User user = userRepo.findByUsername(responsiblePersonId)
                    .orElseThrow(() -> new RuntimeException("User not found for ID: " + responsiblePersonId));

            String employeeName = user.getFirstName() + " " + user.getLastName();
            kpi.setEmployeeName(employeeName);

            Machine machine = machines.stream()
                    .filter(m -> responsiblePersonId.equals(m.getResponsiblePersonId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No machine found for responsible person: " + responsiblePersonId));

            String managerId = machine.getManagerId();
            kpi.setManagerId(managerId);

            String supervisorId = machine.getSupervisorId();
            kpi.setSupervisorId(supervisorId);

            kpiRepo.save(kpi);
        });
    }

    @Scheduled(cron = "0 30 0 * * *")
    @Transactional
    public void recalculateCurrentMonthKpi() {
        LocalDate currentDate = LocalDate.now();
        String year = String.valueOf(currentDate.getYear());
        String month = String.format("%02d", currentDate.getMonthValue());

        System.out.println("Recalculating KPI for current month: " + year + "-" + month);

        YearMonth yearMonth = YearMonth.of(currentDate.getYear(), currentDate.getMonth());

        List<Kpi> currentMonthKpis = kpiRepo.findByYearAndMonth(year, month);

        int updated = 0;

        for (Kpi kpi : currentMonthKpis) {
            try {
                String responsiblePersonId = kpi.getEmployeeId();

                long machineCount = machineRepo.findByResponsiblePersonId(responsiblePersonId).stream()
                        .filter(machine -> !"ยกเลิกใช้งาน".equals(machine.getMachineStatus()))
                        .count();

                if (machineCount == 0) {
                    System.out.println("No active machines for user " + responsiblePersonId + ", skipping");
                    continue;
                }

                int fridays = countFridaysInMonth(yearMonth);
                long newCheckAll = fridays * machineCount;

                LocalDate firstMonday = getFirstMondayOfWeekContainingFirstDay(yearMonth);
                LocalDate lastFriday = getLastFridayOfMonth(yearMonth);

                LocalDate endDate = currentDate.isBefore(lastFriday) ? currentDate : lastFriday;

                long checkedCount = checklistRecordsRepo.countByUserIdAndDateRangeAndReasonNotChecked(
                        responsiblePersonId,
                        firstMonday.atStartOfDay(),
                        endDate.atTime(23, 59, 59)
                );

                if (kpi.getCheckAll() != newCheckAll || kpi.getChecked() != checkedCount) {
                    kpi.setCheckAll(newCheckAll);
                    kpi.setChecked(checkedCount);

                    Optional<Machine> machine = machineRepo.findByResponsiblePersonId(responsiblePersonId).stream()
                            .filter(m -> !"ยกเลิกใช้งาน".equals(m.getMachineStatus()))
                            .findFirst();

                    if (machine.isPresent()) {
                        kpi.setManagerId(machine.get().getManagerId());
                        kpi.setSupervisorId(machine.get().getSupervisorId());
                    }

                    kpiRepo.save(kpi);
                    updated++;

                    System.out.println("Updated KPI for " + responsiblePersonId +
                            " - CheckAll: " + newCheckAll + ", Checked: " + checkedCount);
                }
            } catch (Exception e) {
                System.err.println("Failed to recalculate KPI for " + kpi.getEmployeeId() + ": " + e.getMessage());
            }
        }

        System.out.println("KPI recalculation completed - Updated: " + updated + " records");
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
        while (date.getDayOfWeek().getValue() != 1) {
            date = date.minusDays(1);
        }
        return date;
    }

    private LocalDate getLastFridayOfMonth(YearMonth yearMonth) {
        LocalDate date = yearMonth.atEndOfMonth();
        while (date.getDayOfWeek().getValue() != 5) {
            date = date.minusDays(1);
        }
        return date;
    }
}
