package com.sleepyproject.sleepy_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableScheduling //스케줄러 활성화
@EnableAsync //비동기(백그라운드) 작업 활성화
@SpringBootApplication
public class SleepyBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SleepyBackendApplication.class, args);
    }
}