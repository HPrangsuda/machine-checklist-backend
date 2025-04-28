package com.machinechecklist.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.machinechecklist.model.MachineChecklist;
import com.machinechecklist.repo.MachineChecklistRepo;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MachineChecklistService {
    private final MachineChecklistRepo checklistRepo;

    private MachineChecklistResetService resetService;

    public MachineChecklist save(MachineChecklist checklist) {
        MachineChecklist savedChecklist = checklistRepo.save(checklist);
        // Schedule or update the reset task after saving
        resetService.updateChecklistSchedule(savedChecklist);
        return savedChecklist;
    }

    public List<MachineChecklist> getChecklistByMachineCodeAndStatus(String machineCode, String checkStatus) {
        return checklistRepo.findByMachineCodeAndCheckStatus(machineCode, checkStatus);
    }

    public void resetChecklistStatus(Long id) {
        checklistRepo.resetCheckStatusById(id);
    }
}