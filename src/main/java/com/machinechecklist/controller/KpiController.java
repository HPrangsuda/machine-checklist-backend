package com.machinechecklist.controller;

import com.machinechecklist.model.Kpi;
import com.machinechecklist.service.KpiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kpi")
@RequiredArgsConstructor
public class KpiController {
    private final KpiService kpiService;

    @GetMapping
    public ResponseEntity<List<Kpi>> getKpiAll(
            @RequestParam String year,
            @RequestParam String month) {
        try {
            List<Kpi> kpis = kpiService.getKpiByYearAndMonth(year, month);
            return ResponseEntity.ok(kpis);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/responsible/{employeeId}")
    public ResponseEntity<Kpi> getKpiResponsible(
            @PathVariable String employeeId,
            @RequestParam String year,
            @RequestParam String month) {
        try {
            Kpi kpi = kpiService.getKpiByEmployeeIdAndYearAndMonth(employeeId, year, month);
            return ResponseEntity.ok(kpi);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/{employeeId}")
    public ResponseEntity<Kpi> getKpi(
            @PathVariable String employeeId,
            @RequestParam String year,
            @RequestParam String month) {
        try {
            Kpi kpi = kpiService.getKpiByEmployeeIdAndYearAndMonth(employeeId, year, month);
            return ResponseEntity.ok(kpi);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
