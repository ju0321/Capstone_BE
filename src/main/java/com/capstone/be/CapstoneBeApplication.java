package com.capstone.be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class CapstoneBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(CapstoneBeApplication.class, args);
	}

}
