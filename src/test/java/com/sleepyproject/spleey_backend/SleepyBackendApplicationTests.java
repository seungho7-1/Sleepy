package com.sleepyproject.spleey_backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import com.sleepyproject.sleepy_backend.SleepyBackendApplication;

@SpringBootTest(classes = SleepyBackendApplication.class, properties = {
    "spring.data.redis.repositories.enabled=false",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
})
class SleepyBackendApplicationTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void contextLoads() {
		try {
			jdbcTemplate.execute("ALTER TABLE post MODIFY COLUMN board_type VARCHAR(255) NOT NULL");
			System.out.println("==================================================");
			System.out.println("DATABASE SCHEMA UPDATED SUCCESSFULLY: board_type modified!");
			System.out.println("==================================================");
		} catch (Exception e) {
			System.err.println("Failed to alter table post: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
