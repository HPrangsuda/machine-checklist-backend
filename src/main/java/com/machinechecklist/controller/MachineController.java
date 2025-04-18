package com.machinechecklist.controller;

import com.machinechecklist.model.Machine;
import com.machinechecklist.service.MachineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/machines")
@RequiredArgsConstructor
public class MachineController {
    private final MachineService machineService;

    @GetMapping
    public List<Machine> getAllMachines() {
        return machineService.getAllMachines();
    }

    @GetMapping("/responsible/{personId}")
    public ResponseEntity<List<Machine>> getMachinesByResponsiblePerson(@PathVariable String personId) {
        List<Machine> machines = machineService.getMachinesByResponsiblePerson(personId);
        return ResponseEntity.ok(machines);
    }

    @GetMapping("/responsible-all/{personId}")
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
    public ResponseEntity<Machine> createMachine(@RequestBody Machine machine) {
        try {
            Machine createdMachine = machineService.createMachine(machine);
            return ResponseEntity.ok(createdMachine);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Machine> updateMachine(@PathVariable Long id, @RequestBody Machine machine) {
        Machine updatedMachine = machineService.updateMachine(id, machine);
        return ResponseEntity.ok(updatedMachine);
    }
}
