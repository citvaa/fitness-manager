package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
@EnableAsync
public class FitnessManagerApplication {
    public static void main(String[] args) {
		SpringApplication.run(FitnessManagerApplication.class, args);
	}
}
