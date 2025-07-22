package com.machinechecklist.controller;

import com.machinechecklist.model.*;
import com.machinechecklist.service.ChecklistRecordsService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/checklist-records")
@RequiredArgsConstructor
public class ChecklistRecordsController {
    private final ChecklistRecordsService checklistRecordsService;

    @GetMapping
    public List<ChecklistRecords> getAllRecords() {
        return checklistRecordsService.getAllRecords();
    }

    @GetMapping("/record")
    public ResponseEntity<ChecklistRecords> getRecordById(@RequestParam Long id) {
        return checklistRecordsService.getRecordById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("period/{employeeId}")
    public ResponseEntity<List<ChecklistRecords>> getRecordByPeriod(
            @PathVariable String employeeId,
            @RequestParam String year,
            @RequestParam String month) {
        List<ChecklistRecords> records = checklistRecordsService.getRecordByPeriod(employeeId, year, month);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/department/{personId}")
    public ResponseEntity<List<ChecklistRecords>> getRecordByDepartment(@PathVariable String personId) {
        List<ChecklistRecords> records = checklistRecordsService.getChecklistRecordsByUserId(personId);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/responsible/{personId}")
    public ResponseEntity<List<ChecklistRecords>> getRecordByResponsiblePerson(@PathVariable String personId) {
        List<ChecklistRecords> checklistRecords = checklistRecordsService.getRecordByResponsiblePerson(personId);
        return ResponseEntity.ok(checklistRecords);
    }

    @GetMapping("/recheck/{personId}")
    public ResponseEntity<List<ChecklistRecords>> getRecheck(@PathVariable String personId) {
        List<ChecklistRecords> checklistRecords = checklistRecordsService.getRecheck(personId);
        return ResponseEntity.ok(checklistRecords);
    }

    @PostMapping
    public ResponseEntity<ChecklistRecords> createChecklistRecord(
            @RequestPart("request") ChecklistRequestDTO request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        ChecklistRecords savedRecord = checklistRecordsService.saveChecklistRecord(request, file);
        return ResponseEntity.ok(savedRecord);
    }

    @PutMapping("/approve/{checklistId}")
    public ResponseEntity<ChecklistRecords> approveChecklist(
            @PathVariable Long checklistId,
            @RequestBody ChecklistRecords request) {
        ChecklistRecords updatedChecklist = checklistRecordsService.approveChecklist(checklistId, request);
        return ResponseEntity.ok(updatedChecklist);
    }

    @GetMapping("/export-excel")
    public ResponseEntity<ByteArrayResource> exportMachinesToExcel() {
        try {
            byte[] excelBytes = checklistRecordsService.exportChecklistToExcel();
            ByteArrayResource resource = new ByteArrayResource(excelBytes);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=machines.xlsx");
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            headers.add(HttpHeaders.PRAGMA, "no-cache");
            headers.add(HttpHeaders.EXPIRES, "0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(excelBytes.length)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}