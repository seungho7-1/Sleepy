package com.sleepyproject.sleepy_backend.service.report;

import com.sleepyproject.sleepy_backend.domain.member.Member;
import com.sleepyproject.sleepy_backend.domain.report.Report;
import com.sleepyproject.sleepy_backend.domain.report.ReportStatus;
import com.sleepyproject.sleepy_backend.domain.report.ReportTargetType;
import com.sleepyproject.sleepy_backend.repository.member.MemberRepository;
import com.sleepyproject.sleepy_backend.repository.report.ReportRepository;
import com.sleepyproject.sleepy_backend.service.admin.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;
    private final AdminService adminService;

    @Transactional
    public Long createReport(String username, String targetTypeStr, Long targetId, String reason) {
        Member reporter = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("신고자를 찾을 수 없습니다."));

        ReportTargetType targetType = ReportTargetType.valueOf(targetTypeStr.toUpperCase());

        Report report = Report.builder()
                .reporter(reporter)
                .targetType(targetType)
                .targetId(targetId)
                .reason(reason)
                .status(ReportStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Report savedReport = reportRepository.save(report);

        // 관리자에게 알림 전송
        adminService.notifyAdminsOfNewReport(targetTypeStr.toUpperCase(), targetId);

        return savedReport.getId();
    }
}
