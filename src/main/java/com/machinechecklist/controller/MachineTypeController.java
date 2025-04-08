package com.machinechecklist.controller;

import com.machinechecklist.model.MachineType;
import com.machinechecklist.service.MachineTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/type")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class MachineTypeController {
    private final MachineTypeService machineTypeService;

    @GetMapping
    public ResponseEntity<List<MachineType>> getAllMachineTypes() {
        List<MachineType> types = machineTypeService.getAllMachineTypes();
        return ResponseEntity.ok(types);
    }
}
