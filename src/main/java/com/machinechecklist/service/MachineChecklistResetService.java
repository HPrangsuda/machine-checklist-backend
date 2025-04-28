package com.machinechecklist.service;

import com.machinechecklist.model.MachineChecklist;
import com.machinechecklist.repo.MachineChecklistRepo;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
public class MachineChecklistResetService {
    private static final Logger logger = LoggerFactory.getLogger(MachineChecklistResetService.class);

    private final MachineChecklistRepo checklistRepo;
    private final TaskScheduler taskScheduler;

    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new HashMap<>();

    @PostConstruct
    public void init() {
        if (taskScheduler == null) {
            throw new IllegalStateException("TaskScheduler is not initialized");
        }
        scheduleAllResets();
    }

    @Scheduled(fixedRate = 3600000) // Refresh schedules every hour
    public void refreshSchedules() {
        logger.info("Refreshing all checklist schedules");
        cancelAllScheduledTasks();
        scheduleAllResets();
    }

    private void scheduleAllResets() {
        List<MachineChecklist> allChecklists = checklistRepo.findAll();
        logger.info("Scheduling resets for {} checklists", allChecklists.size());
        for (MachineChecklist checklist : allChecklists) {
            scheduleReset(checklist);
        }
    }

    public void scheduleReset(MachineChecklist checklist) {
        if (checklist.getResetTime() == null || checklist.getResetTime().isEmpty()) {
            logger.warn("No reset time specified for checklist ID {}", checklist.getId());
            return;
        }

        try {
            // Handle 5-field cron expressions by adding seconds
            String cronExpression = checklist.getResetTime().trim();
            String[] fields = cronExpression.split("\\s+");
            if (fields.length == 5) {
                cronExpression = "0 " + cronExpression; // Add 0 for seconds
            } else if (fields.length != 6) {
                throw new IllegalArgumentException("Cron expression must have 5 or 6 fields: " + cronExpression);
            }

            ScheduledFuture<?> scheduledTask = taskScheduler.schedule(
                    () -> resetCheckStatus(checklist.getId()),
                    new CronTrigger(cronExpression, TimeZone.getDefault())
            );

            // Store the scheduled task for potential cancellation
            if (scheduledTasks.containsKey(checklist.getId())) {
                scheduledTasks.get(checklist.getId()).cancel(false);
            }
            scheduledTasks.put(checklist.getId(), scheduledTask);
            logger.debug("Scheduled reset for checklist ID {} with cron: {}", checklist.getId(), cronExpression);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid cron expression for checklist ID {}: {}", checklist.getId(), checklist.getResetTime(), e);
        }
    }

    private void resetCheckStatus(Long checklistId) {
        MachineChecklist checklist = checklistRepo.findById(checklistId).orElse(null);
        if (checklist != null) {
            checklist.setCheckStatus("false");
            checklistRepo.save(checklist);
            logger.debug("Reset check status for checklist ID {}", checklistId);
        } else {
            logger.warn("Checklist ID {} not found during reset", checklistId);
        }
    }

    private void cancelAllScheduledTasks() {
        logger.info("Cancelling all scheduled tasks");
        for (ScheduledFuture<?> task : scheduledTasks.values()) {
            task.cancel(false);
        }
        scheduledTasks.clear();
    }

    public void updateChecklistSchedule(MachineChecklist checklist) {
        if (scheduledTasks.containsKey(checklist.getId())) {
            scheduledTasks.get(checklist.getId()).cancel(false);
            scheduledTasks.remove(checklist.getId());
            logger.debug("Cancelled existing schedule for checklist ID {}", checklist.getId());
        }
        scheduleReset(checklist);
    }
}