package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.profiles.active=dev"})
class FitnessManagerApplicationTests {

	@Test
	void contextLoads() {
	}

}
