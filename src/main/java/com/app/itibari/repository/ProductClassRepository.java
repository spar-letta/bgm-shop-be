package com.app.itibari.repository;

import com.app.itibari.models.entities.Category;
import com.app.itibari.models.entities.ProductClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductClassRepository extends JpaRepository<ProductClass, UUID> {

    Optional<ProductClass> findByNameIgnoreCaseAndDeletedIsFalse(String name);

    Optional<ProductClass> findByPublicIdAndDeletedIsFalse(UUID publicId);

    Page<ProductClass> findByNameContainingIgnoreCaseAndDeletedIsFalse(String searchParam, Pageable pageable);

    Page<ProductClass> findByDeletedIsFalse(Pageable pageable);

    Page<ProductClass> findByCategoryAndDeletedIsFalse(Category category, Pageable pageable);

    Optional<ProductClass> findByCategoryAndNameIgnoreCaseAndDeletedIsFalse(Category category, String productClass);
}
