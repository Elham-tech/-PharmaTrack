package com.example.PharmaTrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
 * ARCHITECTURE: @SpringBootApplication is the entry point that enables auto-configuration.
 * It scans all packages under this class for @Component, @Service, @Repository, @Controller
 * annotations and registers them as Spring beans. This eliminates manual XML/Java config
 * for component scanning, dependency wiring, and database setup.
 */
// Combines @Configuration + @EnableAutoConfiguration + @ComponentScan; bootstraps the Spring app
@SpringBootApplication
public class PharmaTrackApplication {

	public static void main(String[] args) {
		SpringApplication.run(PharmaTrackApplication.class, args);
	}

}
