package com.machinechecklist.service;

import com.machinechecklist.model.User;
import com.machinechecklist.model.enums.RoleType;
import com.machinechecklist.repo.UserRepo;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    public List<User> getAllUser() {
        return userRepo.findAll();
    }

    @PostConstruct
    public void defaultUser() {
        try {
            if (!userRepo.existsByUsername("admin")) {
                User user = new User();
                user.setUsername("admin");
                user.setFirstName("administrator");
                user.setRole(RoleType.ADMIN);
                user.setPassword(passwordEncoder.encode("12345678"));
                user.setCreateDate(new Timestamp(System.currentTimeMillis()));
                userRepo.save(user);
                System.out.println("Admin user created successfully");
            } else {
                System.out.println("Admin user already exists");
            }
        } catch (Exception e) {
            System.err.println("Error creating admin user: " + e.getMessage());
        }
    }

    public Optional<User> getUserById(Long id) {
        return userRepo.findById(id);
    }

    public User createUser(User user) {

        if (userRepo.existsByUsername(user.getUsername())) {
            throw new RuntimeException("User with username already exists.");
        }

        user.setCreateDate(new Timestamp(System.currentTimeMillis()));

        try {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } catch (Exception e) {
            throw new RuntimeException("Error encoding password: " + e.getMessage());
        }

        return userRepo.save(user);
    }

    public boolean checkIfUsernameExists(String username) {
        return userRepo.existsByUsername(username);
    }

    public User updateUser(Long id, User updatedUser) {
        return userRepo.findById(id).map(user -> {
            user.setFirstName(updatedUser.getFirstName());
            user.setLastName(updatedUser.getLastName());
            user.setNickName(updatedUser.getNickName());
            user.setPosition(updatedUser.getPosition());
            user.setStatus(updatedUser.getStatus());
            user.setDepartment(updatedUser.getDepartment());
            user.setRole(updatedUser.getRole());
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            return userRepo.save(user);
        }).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void deleteUser(Long id) {
        if (!userRepo.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        userRepo.deleteById(id);
    }
}
