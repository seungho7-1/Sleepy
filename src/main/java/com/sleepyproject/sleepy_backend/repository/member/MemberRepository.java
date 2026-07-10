package com.sleepyproject.sleepy_backend.repository.member;

import com.sleepyproject.sleepy_backend.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Member(회원) 엔티티에 대한 데이터베이스 접근을 담당하는 리포지토리 인터페이스입니다.
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    /**
     * 이메일을 기준으로 회원을 찾습니다.
     *
     * @param email 회원 이메일
     * @return 조회된 회원 객체 Optional
     */
    Optional<Member> findByEmail(String email);

    /**
     * 해당 이메일을 가진 회원이 이미 존재하는지 여부를 파악합니다.
     *
     * @param email 조회할 이메일
     * @return 가입 여부 (true/false)
     */
    boolean existsByEmail(String email);
}