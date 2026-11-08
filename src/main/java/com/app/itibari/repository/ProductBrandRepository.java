package com.app.itibari.repository;

import com.app.itibari.models.entities.ProductBrand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductBrandRepository extends JpaRepository<ProductBrand, Long> {
    Optional<ProductBrand> findByNameIgnoreCaseAndDeletedIsFalse(String name);

    @Query("select pb from ProductBrand pb " +
            "where (:searchParam is null or lower(pb.name) like lower(concat('%', :searchParam, '%')))")
    Page<ProductBrand> fetchBySearchParam(@Param("searchParam") String searchParam, Pageable pageable);

    Page<ProductBrand> findAllByDeletedIsFalse(Pageable pageable);

    Optional<ProductBrand> findByPublicIdAndDeletedIsFalse(UUID publicId);
}
