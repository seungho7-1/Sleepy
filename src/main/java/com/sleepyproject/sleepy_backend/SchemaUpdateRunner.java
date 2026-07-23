package com.sleepyproject.sleepy_backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchemaUpdateRunner implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    public SchemaUpdateRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("ALTER TABLE post MODIFY COLUMN board_type VARCHAR(255) NOT NULL");
            System.out.println("SCHEMA UPDATED SUCCESSFULLY");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
