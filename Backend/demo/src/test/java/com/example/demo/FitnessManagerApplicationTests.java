package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class FitnessManagerApplicationTests {

	@Test
	void applicationEntrypointIsConfigured() {
		assertNotNull(FitnessManagerApplication.class.getAnnotation(SpringBootApplication.class));
	}

}
