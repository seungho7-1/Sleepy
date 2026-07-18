package com.sleepyproject.sleepy_backend.service.admin;

import com.sleepyproject.sleepy_backend.domain.member.Member;
import com.sleepyproject.sleepy_backend.domain.product.Product;
import com.sleepyproject.sleepy_backend.domain.board.Post;
import com.sleepyproject.sleepy_backend.domain.board.Comment;
import com.sleepyproject.sleepy_backend.domain.report.Report;
import com.sleepyproject.sleepy_backend.domain.seller.ApplicationStatus;
import com.sleepyproject.sleepy_backend.repository.member.MemberRepository;
import com.sleepyproject.sleepy_backend.repository.product.ProductRepository;
import com.sleepyproject.sleepy_backend.repository.board.PostRepository;
import com.sleepyproject.sleepy_backend.repository.board.CommentRepository;
import com.sleepyproject.sleepy_backend.repository.report.ReportRepository;
import com.sleepyproject.sleepy_backend.repository.seller.SellerApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReportRepository reportRepository;
    private final SellerApplicationRepository sellerApplicationRepository;

    public Map<String, Object> getDashboardStats() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        long todayNewUsers = memberRepository.countByCreatedAtAfter(startOfToday);
        long todayNewProducts = productRepository.countByCreatedAtAfter(startOfToday);
        long todayPosts = postRepository.countByCreatedAtAfter(startOfToday);
        long todayComments = commentRepository.countByCreatedAtAfter(startOfToday);
        long pendingSellers = sellerApplicationRepository.countByStatus(ApplicationStatus.PENDING);

        Map<String, Object> stats = new HashMap<>();
        stats.put("todayNewUsers", todayNewUsers);
        stats.put("todayNewProducts", todayNewProducts);
        stats.put("todayPosts", todayPosts);
        stats.put("todayComments", todayComments);
        stats.put("pendingSellers", pendingSellers);

        return stats;
    }

    public List<Member> getAllUsers() {
        return memberRepository.findAll();
    }

    @Transactional
    public void suspendUser(Long id) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
        member.updateStatus("SUSPENDED");
    }

    @Transactional
    public void deleteUser(Long id) {
        memberRepository.deleteById(id);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional
    public void hideProduct(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Product not found"));
        product.hide();
    }

    @Transactional
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    @Transactional
    public void hidePost(Long id) {
        Post post = postRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Post not found"));
        post.hide();
    }

    @Transactional
    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }

    @Transactional
    public void hideComment(Long id) {
        Comment comment = commentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Comment not found"));
        comment.hide();
    }

    @Transactional
    public void deleteComment(Long id) {
        commentRepository.deleteById(id);
    }
}
