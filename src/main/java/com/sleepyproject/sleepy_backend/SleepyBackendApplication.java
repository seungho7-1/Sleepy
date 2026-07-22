package com.sleepyproject.sleepy_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@EnableScheduling //스케줄러 활성화
@EnableAsync //비동기(백그라운드) 작업 활성화
@SpringBootApplication
public class SleepyBackendApplication {

    @PostConstruct
    public void started() {
        // 서버 인스턴스(EC2 등)의 기본 시간이 UTC로 설정되어 있는 경우를 대비해
        // 애플리케이션의 기본 타임존을 한국 시간(Asia/Seoul)으로 고정합니다.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public static void main(String[] args) {
        SpringApplication.run(SleepyBackendApplication.class, args);
    }
}