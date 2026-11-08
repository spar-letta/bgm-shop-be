package com.app.itibari.repository;

import com.app.itibari.models.entities.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findByNameIgnoreCaseAndDeletedIsFalse(String categoryName);

    Optional<Category> findByPublicIdAndDeletedIsFalse(UUID publicId);

    Page<Category> findByDeletedIsFalse(Pageable pageable);

    Page<Category> findByNameContainingIgnoreCaseAndDeletedIsFalse(String searchParam, Pageable pageable);

    List<Category> findAllByDeletedIsFalse();
}
