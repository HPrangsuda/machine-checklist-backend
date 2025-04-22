package com.machinechecklist.controller;

import com.machinechecklist.model.Machine;
import com.machinechecklist.service.FileStorageService;
import com.machinechecklist.service.MachineService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.UrlResource;
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
    private final FileStorageService fileStorageService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Machine> getAllMachines() {
        return machineService.getAllMachines();
    }

    @GetMapping("responsible/{personId}")
    public ResponseEntity<List<Machine>> getMachinesByResponsiblePerson(@PathVariable String personId) {
        List<Machine> machines = machineService.getMachinesByResponsiblePerson(personId);
        return ResponseEntity.ok(machines);
    }

    @GetMapping("/responsible-all/{personId}")
    @PreAuthorize("hasRole('SUPERVISOR')")
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

    @PostMapping
    public ResponseEntity<Machine> createMachine(
            @RequestPart("machine") Machine machine,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {

        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String imagePath = fileStorageService.storeFile(imageFile);
                machine.setImage(imagePath);
            }

            Machine savedMachine = machineService.createMachine(machine);
            return ResponseEntity.ok(savedMachine);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/image/{fileName}")
    public ResponseEntity<Resource> getImage(@PathVariable String fileName) {
        try {
            Path filePath = fileStorageService.getFilePath(fileName);
            UrlResource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG) // Adjust based on file type
                        .body((Resource) resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Machine> updateMachine(@PathVariable Long id, @RequestBody Machine machine) {
        Machine updatedMachine = machineService.updateMachine(id, machine);
        return ResponseEntity.ok(updatedMachine);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMachine(@PathVariable Long id) {
        machineService.deleteMachine(id);
        return ResponseEntity.noContent().build();
    }
}
