package com.machinechecklist.repo;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.machinechecklist.model.MachineChecklist;

import java.util.List;

@Repository
public interface MachineChecklistRepo extends JpaRepository<MachineChecklist, Long> {
    List<MachineChecklist> findByMachineCodeAndCheckStatus(String machineCode, String checkStatus, Sort sort);

    default List<MachineChecklist> findByMachineCodeAndCheckStatus(String machineCode, String checkStatus) {
        return findByMachineCodeAndCheckStatus(machineCode, checkStatus, Sort.by(Sort.Direction.ASC, "id"));
    }
}