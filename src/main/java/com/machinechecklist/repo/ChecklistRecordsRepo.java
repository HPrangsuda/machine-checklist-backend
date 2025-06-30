package com.machinechecklist.repo;

import com.machinechecklist.model.ChecklistRecords;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Repository
public interface ChecklistRecordsRepo extends JpaRepository<ChecklistRecords, Long> {

    @Query("SELECT cr FROM ChecklistRecords cr JOIN Machine m ON m.machineCode = cr.machineCode WHERE cr.userId = :personId ORDER BY cr.checklistId DESC")
    List<ChecklistRecords> findByUserId(@Param("personId") String personId);

    @Query("SELECT cr FROM ChecklistRecords cr JOIN Machine m ON cr.machineCode = m.machineCode WHERE m.department = :department")
    List<ChecklistRecords> findByMachineDepartment(@Param("department") String department);

    @Query("SELECT c FROM ChecklistRecords c WHERE " +
            "c.recheck = true AND " +
            "((c.checklistStatus = 'รอหัวหน้างานตรวจสอบ' AND c.supervisor = :personId) OR " +
            "(c.checklistStatus = 'รอผู้จัดการฝ่ายตรวจสอบ' AND c.manager = :personId)) ORDER BY c.checklistId ASC")
    List<ChecklistRecords> findByManagerOrSupervisor(String personId);

    List<ChecklistRecords> findByChecklistStatusIn(List<String> statuses);

    List<ChecklistRecords> findByMachineCodeAndUserIdAndDateCreatedBetween(
            String machineCode, String userId, Date start, Date end);

    @Query("SELECT COUNT(c) FROM ChecklistRecords c WHERE c.userId = :userId AND c.dateCreated BETWEEN :startDate AND :endDate AND c.recheck = true AND (c.reasonNotChecked NOT IN ('ไม่ได้ดำเนินการ', 'ผู้รับผิดชอบไม่ดำเนินการ') OR c.reasonNotChecked IS NULL)")
    long countByUserIdAndDateRangeAndReasonNotChecked(@Param("userId") String userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}