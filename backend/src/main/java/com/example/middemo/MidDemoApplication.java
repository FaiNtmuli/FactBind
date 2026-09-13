package com.example.middemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the MidDemo Management System backend.
 *
 * <p>This application is the "Native Spring Baseline": a plain Spring Boot + Spring MVC +
 * Spring Data JPA application that deliberately keeps standard annotations
 * ({@code @GetMapping}, {@code @PathVariable}, {@code @RequestParam}, {@code @RequestBody}, ...)
 * so that it can later be compared against alternative API binding approaches.
 */
@SpringBootApplication
public class MidDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MidDemoApplication.class, args);
    }
}
