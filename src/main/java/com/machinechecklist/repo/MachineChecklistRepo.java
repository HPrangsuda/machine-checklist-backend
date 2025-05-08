package com.machinechecklist.repo;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.machinechecklist.model.MachineChecklist;

import java.util.List;

@Repository
public interface MachineChecklistRepo extends JpaRepository<MachineChecklist, Long> {
    List<MachineChecklist> findByMachineCodeAndCheckStatus(String machineCode, Boolean checkStatus, Sort sort);

    default List<MachineChecklist> findByMachineCodeAndCheckStatus(String machineCode, Boolean checkStatus) {
        return findByMachineCodeAndCheckStatus(machineCode, checkStatus, Sort.by(Sort.Direction.ASC, "id"));
    }

    @Query("SELECT m FROM MachineChecklist m WHERE m.checkStatus = :checkStatus")
    List<MachineChecklist> findByCheckStatus(Boolean checkStatus);

    @Modifying
    @Query("UPDATE MachineChecklist m SET m.checkStatus = false WHERE m.id = :id")
    void resetCheckStatusById(Long id);
}