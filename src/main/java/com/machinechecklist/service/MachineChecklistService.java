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

    public List<MachineChecklist> getChecklistByMachineCodeAndStatus(String machineCode, String checkStatus) {
        return checklistRepo.findByMachineCodeAndCheckStatus(machineCode, checkStatus);
    }
}