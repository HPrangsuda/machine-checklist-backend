package com.machinechecklist.repo;

import com.machinechecklist.model.MachineType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MachineTypeRepo extends JpaRepository<MachineType, Long> {
}
