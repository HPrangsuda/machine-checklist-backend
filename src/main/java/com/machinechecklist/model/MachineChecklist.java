package com.machinechecklist.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class MachineChecklist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String machineCode;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "question_id")
    @JsonManagedReference
    private Question question;

    private String answerChoice;

    @Column(name = "check_status", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean checkStatus = false;

    private String resetTime;
}
