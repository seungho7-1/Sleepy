package com.sleepyproject.sleepy_backend.repository.product;

import com.sleepyproject.sleepy_backend.domain.product.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
}
