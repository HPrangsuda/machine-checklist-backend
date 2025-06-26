package com.machinechecklist.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@Table(name = "kpi")
@Data
@RequiredArgsConstructor
public class Kpi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    private String employeeId;
    private String employeeName;
    private String year;
    private String month;
    private Long checkAll;
    private Long checked;
    private String managerId;
    private String supervisorId;
}
