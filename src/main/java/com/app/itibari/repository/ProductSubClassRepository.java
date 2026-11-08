package com.app.itibari.repository;

import com.app.itibari.models.entities.ProductClass;
import com.app.itibari.models.entities.ProductSubClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductSubClassRepository extends JpaRepository<ProductSubClass, UUID> {

    Optional<ProductSubClass> findByNameIgnoreCaseAndDeletedIsFalse(String name);

    Optional<ProductSubClass> findByPublicIdAndProductClassAndDeletedIsFalse(UUID subClassPublicId, ProductClass productClass);

    Optional<ProductSubClass> findByPublicIdAndDeletedIsFalse(UUID subClassPublicId);

    Page<ProductSubClass> findByNameContainingIgnoreCaseAndDeletedIsFalse(String searchParam, Pageable pageable);

    Page<ProductSubClass> findByDeletedIsFalse(Pageable pageable);

    Page<ProductSubClass> findByProductClassAndDeletedIsFalse(ProductClass productClass, Pageable pageable);

    Optional<ProductSubClass> findByProductClassAndNameIgnoreCaseAndDeletedIsFalse(ProductClass productClass, String subClass);
}
