package com.example.TestNodo.repository;

import com.example.TestNodo.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByCategoryCode(String categoryCode);

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.images WHERE c.id = :id")
    Optional<Category> findByIdWithImages(@Param("id") Long id);

    @Query("SELECT DISTINCT c FROM Category c WHERE c.status = '1' " +
            "AND (:name IS NULL OR c.name LIKE %:name%) " +
            "AND (:categoryCode IS NULL OR c.categoryCode = :categoryCode) " +
            "AND (:createdFrom IS NULL OR c.createdDate >= :createdFrom) " +
            "AND (:createdTo IS NULL OR c.createdDate <= :createdTo)")
    Page<Category> search(String name, String categoryCode, LocalDateTime createdFrom, LocalDateTime createdTo, Pageable pageable);
}
