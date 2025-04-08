package com.machinechecklist.service;

import com.machinechecklist.model.MachineType;
import com.machinechecklist.repo.MachineTypeRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MachineTypeService {
    private final MachineTypeRepo machineTypeRepo;

    public List<MachineType> getAllMachineTypes() {
        return machineTypeRepo.findAll();
    }
}
