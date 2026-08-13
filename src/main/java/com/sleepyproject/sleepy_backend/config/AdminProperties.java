package com.sleepyproject.sleepy_backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.admin")
public class AdminProperties {

    private List<Account> accounts;

    @Getter
    @Setter
    public static class Account {
        private String email;
        private String username;
        private String password;
        private String nickname;
    }
}
