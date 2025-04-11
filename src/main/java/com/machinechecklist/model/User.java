package com.machinechecklist.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.machinechecklist.model.enums.RoleType;
import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

@Entity
@Data
@Table(name = "app_users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String employeeId;
    private String position;
    private String status;
    private String department;

    @Column(nullable = false)
    private String firstName;

    private String lastName;
    private String nickName;

    @Enumerated(EnumType.STRING)
    private RoleType role;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    @JsonIgnore
    private String password;

    @Column(nullable = false)
    private Timestamp createDate;
}
