package com.machinechecklist.service;

import com.machinechecklist.model.MachineChecklist;
import com.machinechecklist.repo.MachineChecklistRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduledResetService {
    private final MachineChecklistRepo checklistRepo;

    @Scheduled(cron = "0 */5 * * * *")
    public void checkAndResetStatus() {
        List<MachineChecklist> checklists = checklistRepo.findByCheckStatus("true");
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS); // ปัดเป็นวันที่ ไม่รวมเวลา

        for (MachineChecklist checklist : checklists) {
            String resetTime = checklist.getResetTime();
            if (resetTime != null && !resetTime.isEmpty()) {
                try {
                    CronExpression cronExpression = CronExpression.parse(resetTime);
                    LocalDateTime nextExecution = cronExpression.next(now.minusDays(1)); // ตรวจสอบตั้งแต่วันก่อนหน้า
                    if (nextExecution != null) {
                        // ปัด nextExecution ให้เป็นวันที่เพื่อเปรียบเทียบ
                        LocalDateTime nextExecutionDate = nextExecution.truncatedTo(ChronoUnit.DAYS);
                        if (nextExecutionDate.equals(now)) {
                            checklistRepo.resetCheckStatusById(checklist.getId());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Invalid cron expression for checklist ID " + checklist.getId() + ": " + resetTime);
                }
            }
        }
    }
}
