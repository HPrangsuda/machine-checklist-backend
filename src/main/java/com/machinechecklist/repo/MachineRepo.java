package com.machinechecklist.repo;

import com.machinechecklist.model.Machine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MachineRepo extends JpaRepository<Machine, Long> {
    List<Machine> findByResponsiblePersonId(String responsiblePersonId);

    @Query("SELECT c FROM Machine c WHERE c.supervisorId = :personId OR c.managerId = :personId")
    List<Machine> findBySupervisorIdOrManagerId(String personId);

    Optional<Machine> findByMachineCode(String machineCode);

    boolean existsByMachineCode(String machineCode);
}