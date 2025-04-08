package com.machinechecklist.repo;

import com.machinechecklist.model.ChecklistRecords;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChecklistRecordsRepo extends JpaRepository<ChecklistRecords, Long> {
    Optional<ChecklistRecords> findById(Long id);

    List<ChecklistRecords> findByUserId(String personId);

    @Query("SELECT c FROM ChecklistRecords c WHERE " +
            "c.recheck = true AND " +
            "((c.checklistStatus = 'รอหัวหน้างานตรวจสอบ' AND c.supervisor = :personId) OR " +
            "(c.checklistStatus = 'รอผู้จัดการฝ่ายตรวจสอบ' AND c.manager = :personId))")
    List<ChecklistRecords> findByManagerOrSupervisor(String personId);
}