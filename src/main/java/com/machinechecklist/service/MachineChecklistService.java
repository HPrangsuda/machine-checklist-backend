package com.machinechecklist.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.machinechecklist.model.MachineChecklist;
import com.machinechecklist.repo.MachineChecklistRepo;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MachineChecklistService {
    private final MachineChecklistRepo checklistRepo;

    public List<MachineChecklist> getChecklistByMachineCodeAndStatus(String machineCode, Boolean checkStatus) {
        return checklistRepo.findByMachineCodeAndCheckStatus(machineCode, checkStatus);
    }

    public List<MachineChecklist> getChecklistByMachineCodeAndResetTime(String machineCode) {
        return checklistRepo.findByMachineCodeAndResetTime(machineCode);
    }

    public void resetChecklistStatus(Long id) {
        checklistRepo.resetCheckStatusById(id);
    }
}