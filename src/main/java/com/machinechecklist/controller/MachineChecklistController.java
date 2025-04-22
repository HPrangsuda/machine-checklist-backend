package com.machinechecklist.controller;

import com.machinechecklist.model.Machine;
import com.machinechecklist.model.MachineChecklist;
import com.machinechecklist.service.MachineChecklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/checklist")
@RequiredArgsConstructor
public class MachineChecklistController {
    private final MachineChecklistService checklistService;

    @GetMapping("/machine")
    public ResponseEntity<List<MachineChecklist>> getChecklist(@RequestParam String machineCode) {
        List<MachineChecklist> checklist = checklistService.getChecklistByMachineCodeAndStatus(machineCode, "false");
        return ResponseEntity.ok(checklist);
    }

    @PostMapping("/reset/{id}")
    public ResponseEntity<String> resetChecklist(@PathVariable Long id) {
        checklistService.resetChecklistStatus(id);
        return ResponseEntity.ok("รีเซ็ต checklist ID " + id + " สำเร็จ");
    }
}