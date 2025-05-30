package com.machinechecklist.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.machinechecklist.model.Machine;
import com.machinechecklist.service.FileStorageService;
import com.machinechecklist.service.MachineService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    @GetMapping("/export-excel")
    public ResponseEntity<ByteArrayResource> exportMachinesToExcel() {
        try {
            byte[] excelBytes = machineService.exportMachinesToExcel();
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

    @PostMapping("/create")
    public ResponseEntity<Machine> createMachine(
            @RequestPart("machine") Machine machine,
            @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {

        Machine savedMachine = machineService.createMachine(machine, file);
        return ResponseEntity.ok(savedMachine);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Machine> updateMachine(
            @PathVariable Long id,
            @RequestPart("formData") Machine machine,
            @RequestPart(value = "image", required = false) MultipartFile file) {
        try {
            Machine updatedMachine = machineService.updateMachine(id, machine, file);
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
