package com.machinechecklist.service;

import com.machinechecklist.model.User;
import com.machinechecklist.model.enums.RoleType;
import com.machinechecklist.repo.UserRepo;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void defaultUser(){
        if(!userRepo.existsByUsername("admin")){
            User user = new User();
            user.setUsername("admin");
            user.setFirstName("admin");
            user.setRole(RoleType.ADMIN);
            user.setPassword(passwordEncoder.encode("admin"));
            user.setCreateDate(new Timestamp(System.currentTimeMillis()));
            userRepo.save(user);
        }
    }
}
