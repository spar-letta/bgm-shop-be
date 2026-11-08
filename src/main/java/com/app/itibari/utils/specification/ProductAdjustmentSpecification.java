package com.app.itibari.utils.specification;

import com.app.itibari.dataTypes.AdjustmentStatus;
import com.app.itibari.dataTypes.AdjustmentType;
import com.app.itibari.models.entities.ProductAdjustment;
import org.springframework.data.jpa.domain.Specification;

public class ProductAdjustmentSpecification {

    public static Specification<ProductAdjustment> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<ProductAdjustment> hasStatus(AdjustmentStatus status) {

        return ((root, query, criteriaBuilder) -> {
            if (status == null) {
                return null;
            }
            return criteriaBuilder.equal(root.get("status"), status);
        });
    }

    public static Specification<ProductAdjustment> hasAdjustmentType(AdjustmentType adjustmentType) {

        return ((root, query, criteriaBuilder) -> {
            if (adjustmentType == null) {
                return null;
            }
            return criteriaBuilder.equal(root.get("adjustmentType"), adjustmentType);
        });
    }

    public static Specification<ProductAdjustment> hasWarehousePublicId(String warehousePublicId) {

        return ((root, query, criteriaBuilder) -> {
            if (warehousePublicId == null || warehousePublicId.isEmpty()) {
                return null;
            }
            return criteriaBuilder.equal(root.get("warehousePublicId"), warehousePublicId);
        });
    }

    public static Specification<ProductAdjustment> searchParamMatches(String searchParam) {

        return ((root, query, criteriaBuilder) -> {
            if (searchParam == null || searchParam.isEmpty()) {
                return null;
            }
            String pattern = "%" + searchParam.toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("warehouseName")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("productSku")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("productName")), pattern)
            );
        });
    }
}
