package com.machinechecklist.repo;

import com.machinechecklist.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);
    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u WHERE u.role != 'SUPERADMIN' ORDER BY u.firstName ASC")
    List<User> findAllNonSuperAdminUsers();
}