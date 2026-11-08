package com.app.itibari.services;

import com.app.itibari.dataTypes.DiscountApprovalStatus;
import com.app.itibari.dataTypes.DiscountStatus;
import com.app.itibari.dataTypes.DiscountType;
import com.app.itibari.models.entities.DiscountApproval;
import com.app.itibari.models.entities.Discounts;
import com.app.itibari.models.entities.VolumeRange;
import com.app.itibari.models.request.ApprovalRequest;
import com.app.itibari.models.request.DiscountCreateRequest;
import com.app.itibari.models.request.VolumeRangeCreateRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DiscountService {

    void createUpdateDiscount(@Valid DiscountCreateRequest discountCreateRequest, String name);

    void update(UUID publicId, @Valid DiscountCreateRequest discountCreateRequest, String name);

    Page<Discounts> getAllDiscounts(DiscountType discountType, String searchParam, LocalDate startDate, LocalDate endDate, DiscountStatus status, Pageable pageable);

    void delete(UUID publicId, String name);

    void suspend(UUID publicId, String name);

    void firstApproval(UUID publicId, String comment, boolean approve, String name);

    void finalApproval(UUID publicId, ApprovalRequest request, String name);

    Page<DiscountApproval> getAllDiscountApprovals(String searchParam,
                                                   UUID salesAgentPublicId,
                                                   UUID customerPublicId,
                                                   DiscountApprovalStatus status,
                                                   LocalDate startDate,
                                                   LocalDate endDate,
                                                   Pageable pageable);

    void approve(UUID publicId, String name);

    void addVolumeRange(UUID discountPublicId, @Valid VolumeRangeCreateRequest volumeRangeCreateRequest, String name);

    List<VolumeRange> getDiscountVolumeRanges(UUID discountPublicId);

    void deleteVolumeRange(UUID discountPublicId, UUID publicId, String name);
}
