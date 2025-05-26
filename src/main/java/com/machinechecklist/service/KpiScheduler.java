package com.machinechecklist.service;

import com.machinechecklist.model.Kpi;
import com.machinechecklist.model.Machine;
import com.machinechecklist.repo.KpiRepo;
import com.machinechecklist.repo.MachineRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KpiScheduler {
    private final MachineRepo machineRepo;
    private final KpiRepo kpiRepo;

    @Scheduled(cron = "0 1 0 26 * ?")
    public void createKpiRecords() {
        LocalDate currentDate = LocalDate.now();
        String year = String.valueOf(currentDate.getYear());
        String month = String.format("%02d", currentDate.getMonthValue());

        // Calculate number of Fridays in the current month
        YearMonth yearMonth = YearMonth.of(currentDate.getYear(), currentDate.getMonth());
        int fridays = countFridaysInMonth(yearMonth);

        // Group machines by responsiblePersonId
        List<Machine> machines = machineRepo.findAll();
        Map<String, Long> machineCountByResponsiblePerson = machines.stream()
                .filter(machine -> machine.getResponsiblePersonId() != null)
                .collect(Collectors.groupingBy(
                        Machine::getResponsiblePersonId,
                        Collectors.counting()
                ));

        // Create Kpi record for each responsiblePersonId
        machineCountByResponsiblePerson.forEach((responsiblePersonId, machineCount) -> {
            Kpi kpi = new Kpi();
            kpi.setEmployeeId(responsiblePersonId);
            kpi.setYear(year);
            kpi.setMonth(month);
            kpi.setCheckAll(String.valueOf(fridays * machineCount));
            kpi.setChecked("0");

            kpiRepo.save(kpi);
        });
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
}
