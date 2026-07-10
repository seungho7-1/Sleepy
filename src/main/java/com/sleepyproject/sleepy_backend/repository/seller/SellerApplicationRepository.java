package com.sleepyproject.sleepy_backend.repository.seller;

import com.sleepyproject.sleepy_backend.domain.seller.SellerApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerApplicationRepository extends JpaRepository<SellerApplication, Long> {
}
