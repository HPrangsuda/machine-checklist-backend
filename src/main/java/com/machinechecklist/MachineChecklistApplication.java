package com.machinechecklist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MachineChecklistApplication {

	public static void main(String[] args) {
		SpringApplication.run(MachineChecklistApplication.class, args);
	}

}
