package com.machinechecklist.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "checklist_records")
@Data
@RequiredArgsConstructor
public class ChecklistRecords {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long checklistId;

    private Boolean recheck;
    private String machineCode;
    private String machineName;
    private String machineStatus;

    @Column(columnDefinition="TEXT")
    private String machineChecklist;

    private String machineNote;
    private String machineImage;
    private String userId;
    private String userName;

    @Temporal(TemporalType.TIMESTAMP)
    private Date dateCreated;

    private String supervisor;

    @Temporal(TemporalType.TIMESTAMP)
    private Date dateSupervisorChecked;

    private String manager;

    @Temporal(TemporalType.TIMESTAMP)
    private Date dateManagerChecked;

    private String checklistStatus;
}
