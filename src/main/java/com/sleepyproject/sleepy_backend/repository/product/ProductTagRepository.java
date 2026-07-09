package com.sleepyproject.sleepy_backend.repository.product;

import com.sleepyproject.sleepy_backend.domain.product.Product;
import com.sleepyproject.sleepy_backend.domain.product.ProductTag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductTagRepository extends JpaRepository<ProductTag, Long> {
    List<ProductTag> findByProduct(Product product);
    void deleteByProduct(Product product);
}
