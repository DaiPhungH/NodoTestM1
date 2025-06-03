package com.example.TestNodo.repository;

import com.example.TestNodo.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    List<ProductCategory> findByProductIdAndStatus(Long productId, String status);
}