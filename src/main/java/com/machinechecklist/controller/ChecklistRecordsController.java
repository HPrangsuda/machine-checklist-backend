package com.machinechecklist.controller;

import com.machinechecklist.model.*;
import com.machinechecklist.service.ChecklistRecordsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

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
    public ResponseEntity<ChecklistRecords> approveChecklist(@PathVariable Long checklistId) {
        ChecklistRecords updatedChecklist = checklistRecordsService.approveChecklist(checklistId);
        return ResponseEntity.ok(updatedChecklist);
    }
}