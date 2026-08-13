package com.sleepyproject.sleepy_backend.init;

import com.sleepyproject.sleepy_backend.config.AdminProperties;
import com.sleepyproject.sleepy_backend.domain.member.Member;
import com.sleepyproject.sleepy_backend.domain.member.Role;
import com.sleepyproject.sleepy_backend.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final AdminProperties adminProperties;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminProperties.getAccounts() == null || adminProperties.getAccounts().isEmpty()) {
            log.info("No admin accounts configured in application properties.");
            return;
        }

        for (AdminProperties.Account account : adminProperties.getAccounts()) {
            memberRepository.findByEmail(account.getEmail()).ifPresentOrElse(
                    member -> {
                        log.info("Admin account already exists: {}", account.getEmail());
                        // Optional: update roles or passwords if needed, but usually we just skip.
                    },
                    () -> {
                        log.info("Creating new admin account: {}", account.getEmail());
                        Member adminMember = Member.builder()
                                .email(account.getEmail())
                                .username(account.getUsername())
                                .password(passwordEncoder.encode(account.getPassword()))
                                .nickname(account.getNickname())
                                .role(Role.ADMIN)
                                .createdAt(LocalDateTime.now())
                                .status("ACTIVE")
                                .onboarded(true)
                                .build();
                        
                        memberRepository.save(adminMember);
                        log.info("Successfully created admin account: {}", account.getEmail());
                    }
            );
        }
    }
}
