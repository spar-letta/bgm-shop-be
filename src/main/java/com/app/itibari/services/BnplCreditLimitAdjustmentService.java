package com.app.itibari.services;

import com.app.itibari.models.response.BnplCreditLimitAdjustmentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface BnplCreditLimitAdjustmentService {

    int adjustCreditLimits();

    Page<BnplCreditLimitAdjustmentResponse> fetchAdjustments(LocalDate startDate, LocalDate endDate,
                                                           UUID customerPublicId, String searchParam,
                                                           Pageable pageable);
}
