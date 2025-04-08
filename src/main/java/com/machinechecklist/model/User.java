package com.machinechecklist.model;

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
    private String firstName;
    private String lastName;
    private String nickName;

    private RoleType role;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    private Timestamp createDate;
}
