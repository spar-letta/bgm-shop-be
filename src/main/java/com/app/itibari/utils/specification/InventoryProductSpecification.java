package com.app.itibari.utils.specification;

import com.app.itibari.models.entities.InventoryProducts;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class InventoryProductSpecification {

    public static Specification<InventoryProducts> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<InventoryProducts> hasProduct(UUID productId) {

        return (root, query, cb) -> {
            if (productId == null) {
                return null;
            }

            return cb.equal(root.get("product").get("publicId"), productId);
        };
    }

    // has inventory

    public static Specification<InventoryProducts> hasInventory(UUID inventoryId) {

        return (root, query, cb) -> {
            if (inventoryId == null) {
                return null;
            }

            return cb.equal(root.get("inventory").get("publicId"), inventoryId);
        };
    }

    // search param

    public static Specification<InventoryProducts> hasProductNameOrSkuLike(String searchParam) {
        return (root, query, cb) -> {
            if (searchParam == null || searchParam.trim().isEmpty()) {
                return null;
            }

            String likePattern = "%" + searchParam.toLowerCase() + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("product").get("productName")), likePattern),
                    cb.like(cb.lower(root.get("product").get("sku")), likePattern)
            );
        };
    }
}
