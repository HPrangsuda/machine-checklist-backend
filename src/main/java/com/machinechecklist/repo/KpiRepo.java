package com.machinechecklist.repo;

import com.machinechecklist.model.Kpi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KpiRepo extends JpaRepository<Kpi, Long> {
    @Query("SELECT k FROM Kpi k JOIN Machine m ON k.employeeId = m.responsiblePersonId WHERE k.year = :year AND k.month = :month AND m.machineStatus != 'ยกเลิกใช้งาน'")
    List<Kpi> findByYearAndMonth(String year, String month);

    @Query("SELECT k FROM Kpi k JOIN Machine m ON k.employeeId = m.responsiblePersonId WHERE k.managerId = :managerId AND k.year = :year AND k.month = :month AND m.machineStatus != 'ยกเลิกใช้งาน'")
    List<Kpi> findByManagerIdAndYearAndMonth(String managerId, String year, String month);

    @Query("SELECT k FROM Kpi k JOIN Machine m ON k.employeeId = m.responsiblePersonId WHERE k.supervisorId = :supervisorId AND k.year = :year AND k.month = :month AND m.machineStatus != 'ยกเลิกใช้งาน'")
    List<Kpi> findBySupervisorIdAndYearAndMonth(String supervisorId, String year, String month);

    @Query("SELECT k FROM Kpi k JOIN Machine m ON k.employeeId = m.responsiblePersonId WHERE k.employeeId = :employeeId AND k.year = :year AND k.month = :month AND m.machineStatus != 'ยกเลิกใช้งาน'")
    Optional<Kpi> findByEmployeeIdAndYearAndMonth(String employeeId, String year, String month);
}
