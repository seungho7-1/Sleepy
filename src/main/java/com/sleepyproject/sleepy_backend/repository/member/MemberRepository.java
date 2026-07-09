package com.sleepyproject.sleepy_backend.repository.member;

import com.sleepyproject.sleepy_backend.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    //email로 db에서 찾자.
    boolean existsByEmail(String email);
}