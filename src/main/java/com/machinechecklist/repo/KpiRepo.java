package com.machinechecklist.repo;

import com.machinechecklist.model.Kpi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KpiRepo extends JpaRepository<Kpi, Long> {
    List<Kpi> findByYearAndMonth(String year, String month);

    List<Kpi> findByManagerIdAndYearAndMonth(String managerId, String year, String month);

    List<Kpi> findBySupervisorIdAndYearAndMonth(String supervisorId, String year, String month);

    Optional<Kpi> findByEmployeeIdAndYearAndMonth(String employeeId, String year, String month);
}
