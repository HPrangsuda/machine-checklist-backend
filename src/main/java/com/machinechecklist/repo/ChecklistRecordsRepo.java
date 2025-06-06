package com.machinechecklist.repo;

import com.machinechecklist.model.ChecklistRecords;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface ChecklistRecordsRepo extends JpaRepository<ChecklistRecords, Long> {

    @Query("SELECT cr FROM ChecklistRecords cr JOIN Machine m ON m.machineCode = cr.machineCode WHERE cr.userId = :personId ORDER BY cr.checklistId DESC")
    List<ChecklistRecords> findByUserId(@Param("personId") String personId);

    @Query("SELECT c FROM ChecklistRecords c WHERE " +
            "c.recheck = true AND " +
            "((c.checklistStatus = 'รอหัวหน้างานตรวจสอบ' AND c.supervisor = :personId) OR " +
            "(c.checklistStatus = 'รอผู้จัดการฝ่ายตรวจสอบ' AND c.manager = :personId)) ORDER BY c.checklistId ASC")
    List<ChecklistRecords> findByManagerOrSupervisor(String personId);

    List<ChecklistRecords> findByChecklistStatusIn(List<String> statuses);

    List<ChecklistRecords> findByMachineCodeAndUserIdAndDateCreatedBetween(
            String machineCode, String userId, Date start, Date end);
}