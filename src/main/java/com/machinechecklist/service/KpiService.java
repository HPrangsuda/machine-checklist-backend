package com.machinechecklist.service;

import com.machinechecklist.model.Kpi;
import com.machinechecklist.repo.KpiRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KpiService {

    private final KpiRepo kpiRepo;

    public Kpi getKpiByYearAndMonth(String year, String month) {
        Optional<Kpi> kpiOptional = kpiRepo.findByYearAndMonth(year, month);
        return kpiOptional.orElseThrow(() -> new RuntimeException("ไม่พบข้อมูล KPI ปี: " + year + ", เดือน: " + month));
    }

    public Kpi getKpiByEmployeeIdAndYearAndMonth(String employeeId, String year, String month) {
        Optional<Kpi> kpiOptional = kpiRepo.findByEmployeeIdAndYearAndMonth(employeeId, year, month);
        return kpiOptional.orElseThrow(() -> new RuntimeException("ไม่พบข้อมูล KPI สำหรับ employeeId: " + employeeId + ", ปี: " + year + ", เดือน: " + month));
    }
}
