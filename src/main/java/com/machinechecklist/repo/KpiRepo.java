package com.machinechecklist.repo;

import com.machinechecklist.model.Kpi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KpiRepo extends JpaRepository<Kpi, Long> {
    Optional<Kpi> findByEmployeeIdAndYearAndMonth(String employeeId, String year, String month);
}
