package com.machinechecklist.repo;

import com.machinechecklist.model.Machine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MachineRepo extends JpaRepository<Machine, Long> {
    @Query("SELECT m FROM Machine m WHERE SUBSTRING(m.department, 1, 2) = :department")
    List<Machine> findByDepartment(@Param("department") String department);

    List<Machine> findByResponsiblePersonId(String responsiblePersonId);

    @Query("SELECT c FROM Machine c WHERE c.supervisorId = :personId OR c.managerId = :personId")
    List<Machine> findBySupervisorIdOrManagerId(String personId);

    Optional<Machine> findByMachineCode(String machineCode);

    boolean existsByMachineCode(String machineCode);
}