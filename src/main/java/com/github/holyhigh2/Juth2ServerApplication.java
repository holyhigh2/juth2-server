package com.github.holyhigh2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * for test
 */
@EnableAutoConfiguration
@SpringBootApplication
public class Juth2ServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(Juth2ServerApplication.class, args);
	}

}
