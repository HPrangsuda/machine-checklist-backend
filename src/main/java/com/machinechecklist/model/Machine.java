package com.machinechecklist.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "machine")
@Data
@RequiredArgsConstructor
public class Machine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(unique = true, nullable = false)
    private String machineCode;

    private String machineName;
    private String machineModel;
    private String machineNumber;

    @Column(columnDefinition="TEXT")
    private String image;

    private String frequency;

    private String responsiblePersonId;
    private String responsiblePersonName;
    private String supervisorId;
    private String supervisorName;
    private String managerId;
    private String managerName;
    private String machineStatus;
    private String machineTypeName;
    private String checkStatus;
    private String qrCode;
}
