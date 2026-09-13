package com.example.middemo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables Spring Data JPA auditing so that {@code createdAt} / {@code updatedAt} are filled
 * automatically. It lives in its own configuration class (instead of on the application class)
 * so that web-only slice tests do not need a JPA metamodel.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
