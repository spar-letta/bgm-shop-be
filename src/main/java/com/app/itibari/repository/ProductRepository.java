package com.app.itibari.repository;

import com.app.itibari.models.entities.Product;
import com.app.itibari.models.entities.Suppliers;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByProductNameIgnoreCaseAndDeletedIsFalse(@NotNull(message = "Product name is required") String productName);

    Optional<Product> findByPublicIdAndDeletedIsFalse(UUID publicId);

    Page<Product> findBySuppliersAndDeletedIsFalse(List<Suppliers> suppliers, Pageable pageable);

    Optional<Product> findProductBySkuIgnoreCaseAndDeletedIsFalse(String sku);

    Product findByPublicId(UUID productPublicId);


    @Query("SELECT p FROM Product p " +
            "WHERE LOWER(p.productName) LIKE LOWER(CONCAT('%', :searchParam, '%')) " +
            "AND :supplier member of p.suppliers " +
            "AND p.deleted = false")
    Page<Product> findByProductNameAndSuppliersAndDeletedIsFalse(@Param("searchParam") String searchParam,
                                                                 @Param("supplier") Suppliers supplier,
                                                                 Pageable pageable);


    List<Product> findByDeletedIsFalse();

    List<Product> findByDeletedFalseAndTaxCodeIsNotNull();

    @Query("SELECT COUNT(p) > 0 FROM Product p JOIN p.suppliers s "
            + "WHERE p.publicId = :productPublicId AND s.publicId = :supplierPublicId AND p.deleted = false")
    boolean existsByPublicIdAndSupplierPublicId(@Param("productPublicId") UUID productPublicId,
                                                  @Param("supplierPublicId") UUID supplierPublicId);

    List<Product> findByBrandIsContainingIgnoreCaseAndDeletedIsFalse(String searchParam);

    @Query("SELECT DISTINCT TRIM(p.brand) FROM Product p "
            + "WHERE p.deleted = false AND p.brand IS NOT NULL AND TRIM(p.brand) <> '' "
            + "ORDER BY TRIM(p.brand)")
    List<String> findDistinctProductBrands();

    @Query("SELECT DISTINCT TRIM(p.brand) FROM Product p "
            + "WHERE p.deleted = false AND p.brand IS NOT NULL AND TRIM(p.brand) <> '' "
            + "AND LOWER(p.brand) LIKE LOWER(CONCAT('%', :searchParam, '%')) "
            + "ORDER BY TRIM(p.brand)")
    List<String> findDistinctProductBrandsBySearch(@Param("searchParam") String searchParam);

    @Query("select p from Product p where p.deleted = false and p.publicId in ?1")
    List<Product> getAllInList(List<UUID> productPublicId);
}
