package com.se.sebtl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Creates the TaskScheduler for your 5-minute timers
@EnableAsync      // Activates the background threads for the Gate and Signs
public class SebtlApplication {

	public static void main(String[] args) {
		SpringApplication.run(SebtlApplication.class, args);
	}

}