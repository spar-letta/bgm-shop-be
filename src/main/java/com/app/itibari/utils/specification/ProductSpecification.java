package com.app.itibari.utils.specification;

import com.app.itibari.dataTypes.ProductStatus;
import com.app.itibari.models.entities.Product;
import com.app.itibari.models.entities.Suppliers;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class ProductSpecification {

    public static Specification<Product> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<Product> hasSupplier(UUID supplierId) {

        return (root, query, cb) -> {
            if (supplierId == null) {
                return null;
            }

            Join<Product, Suppliers> supplierJoin = root.join("suppliers");
            return cb.equal(supplierJoin.get("publicId"), supplierId);
        };
    }

    public static Specification<Product> hasCategory(UUID categoryId) {

        return (root, query, cb) -> {
            if (categoryId == null) {
                return null;
            }
            return cb.equal(root.get("category").get("publicId"), categoryId);
        };
    }

    public static Specification<Product> hasStatus(ProductStatus status) {

        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            return cb.equal(root.get("productStatus"), status);
        };
    }

    public static Specification<Product> hasProductClass(UUID classPublicId) {

        return (root, query, cb) -> {
            if (classPublicId == null) {
                return null;
            }
            return cb.equal(root.get("productClass").get("publicId"), classPublicId);
        };
    }

    public static Specification<Product> hasProductSubClass(UUID subClassPublicId) {

        return (root, query, cb) -> {
            if (subClassPublicId == null) {
                return null;
            }
            return cb.equal(root.get("productSubClass").get("publicId"), subClassPublicId);
        };
    }

    public static Specification<Product> hasProductNameLike(String productName) {

        return (root, query, cb) -> {
            if (productName == null) {
                return null;
            }
            return cb.like(cb.upper(root.get("productName")), "%" + productName.toUpperCase() + "%");
        };
    }

    public static Specification<Product> searchParamMatches(String searchParam) {

        return (root, query, cb) -> {
            if (searchParam == null || searchParam.trim().isEmpty()) {
                return cb.conjunction(); // Return true predicate for empty input
            }

            String formattedSearchParam = "%" + searchParam.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("productName")), formattedSearchParam),
                    cb.like(cb.lower(root.get("sku")), formattedSearchParam)
            );
        };
    }

    public static Specification<Product> isActive(Boolean isActive) {

        return (root, query, cb) -> {
            if (isActive == null) {
                return null;
            }
            boolean isInactive = !isActive;
            return cb.equal(root.get("inActive"), isInactive);
        };
    }

    public static Specification<Product> hasStock(Boolean hasStock) {

        return (root, query, cb) -> {
            if (hasStock == null) {
                return null;
            }
            if (hasStock) {
                return cb.greaterThan(root.get("availableStock"), 0);
            } else {
                return cb.equal(root.get("availableStock"), 0);
            }
        };
    }

    public static Specification<Product> hasProductBrand(UUID brandPublicId) {

        return (root, query, cb) -> {
            if (brandPublicId == null) {
                return null;
            }
            return cb.equal(root.get("productBrand").get("publicId"), brandPublicId);
        };
    }
}
