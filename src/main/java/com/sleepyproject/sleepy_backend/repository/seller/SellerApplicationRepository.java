package com.sleepyproject.sleepy_backend.repository.seller;

import com.sleepyproject.sleepy_backend.domain.seller.SellerApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SellerApplicationRepository extends JpaRepository<SellerApplication, Long> {
    long countByStatus(com.sleepyproject.sleepy_backend.domain.seller.ApplicationStatus status);
    List<SellerApplication> findByStatus(com.sleepyproject.sleepy_backend.domain.seller.ApplicationStatus status);
}
