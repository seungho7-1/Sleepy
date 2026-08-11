package com.sleepyproject.sleepy_backend.service.member;

import com.sleepyproject.sleepy_backend.domain.member.Member;
import com.sleepyproject.sleepy_backend.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 중복되는 유저(Member) 조회 로직을 통합 처리하는 컴포넌트입니다.
 */
@Component
@RequiredArgsConstructor
public class MemberReader {
    
    private final MemberRepository memberRepository;

    /**
     * 주어진 username(email)으로 회원을 단건 조회합니다.
     * 존재하지 않을 경우 즉시 예외를 발생시킵니다.
     * 
     * @param username 찾고자 하는 유저 이메일
     * @return 조회된 Member 엔티티
     * @throws IllegalArgumentException 유저를 찾을 수 없는 경우
     */
    public Member getMember(String username) {
        return memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
    }
}
