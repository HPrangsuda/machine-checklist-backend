package com.machinechecklist.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.machinechecklist.model.ChecklistRecords;
import com.machinechecklist.model.Machine;
import com.machinechecklist.service.FileStorageService;
import com.machinechecklist.service.MachineService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/machines")
@RequiredArgsConstructor
public class MachineController {
    private final MachineService machineService;
    private ObjectMapper objectMapper;
    private final FileStorageService fileStorageService;

    @GetMapping
    //@PreAuthorize("hasRole('ADMIN')")
    public List<Machine> getAllMachines() {
        return machineService.getAllMachines();
    }

    @GetMapping("responsible/{personId}")
    public ResponseEntity<List<Machine>> getMachinesByResponsiblePerson(@PathVariable String personId) {
        List<Machine> machines = machineService.getMachinesByResponsiblePerson(personId);
        return ResponseEntity.ok(machines);
    }

    @GetMapping("/responsible-all/{personId}")
    //@PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<List<Machine>> getMachinesByResponsibleAll(@PathVariable String personId) {
        List<Machine> machines = machineService.getMachinesByResponsibleAll(personId);
        return ResponseEntity.ok(machines);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MachineService.MachineResponse> getMachineWithQRCode(@PathVariable Long id) {
        try {
            MachineService.MachineResponse response = machineService.getMachineWithQRCode(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/machine-code/{machineCode}")
    public ResponseEntity<Machine> getMachineByMachineCode(@PathVariable String machineCode) {
        try {
            Machine machine = machineService.getMachineByMachineCode(machineCode);
            if (machine != null) {
                return ResponseEntity.ok(machine);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/create")
    public ResponseEntity<Machine> createMachine(
            @RequestPart("machine") Machine machine,
            @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {

        Machine savedMachine = machineService.createMachine(machine, file);
        return ResponseEntity.ok(savedMachine);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Machine> updateMachine(@PathVariable Long id, @RequestBody Machine machine) {
        try {
            Machine updatedMachine = machineService.updateMachine(id, machine);
            return ResponseEntity.ok(updatedMachine);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @DeleteMapping("/{id}")
    //@PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMachine(@PathVariable Long id) {
        machineService.deleteMachine(id);
        return ResponseEntity.noContent().build();
    }
}
