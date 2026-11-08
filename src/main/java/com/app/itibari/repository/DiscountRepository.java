package com.app.itibari.repository;

import com.app.itibari.dataTypes.DiscountStatus;
import com.app.itibari.dataTypes.DiscountType;
import com.app.itibari.models.entities.Discounts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiscountRepository extends JpaRepository<Discounts, Long>, JpaSpecificationExecutor<Discounts> {

    Optional<Discounts> findByPublicIdAndDeletedIsFalse(UUID publicId);

    Optional<Discounts> findDiscountByProductPublicIdAndDiscountTypeAndStatusAndDeletedIsFalse(UUID publicId, DiscountType discountType, DiscountStatus discountStatus);

    Discounts findByPublicId(UUID uuid);

    List<Discounts> findByCustomerPublicIdAndDiscountTypeAndStatusAndDeletedIsFalse(UUID uuid, DiscountType discountType, DiscountStatus discountStatus);

    Optional<Discounts> findDiscountsByCustomerPublicIdAndDiscountTypeAndStatusAndDeletedIsFalseAndOrderPublicIdIsNull(UUID publicId, DiscountType discountType, DiscountStatus discountStatus);
}
