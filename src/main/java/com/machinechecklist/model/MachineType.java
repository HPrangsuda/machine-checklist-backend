package com.machinechecklist.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class MachineType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String machineTypeName;
}
