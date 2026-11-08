package com.app.itibari.services;

import com.app.itibari.dataTypes.BatchStatus;
import com.app.itibari.models.request.CreditBatchAction;
import com.app.itibari.models.request.CreditBatchRequest;
import com.app.itibari.models.response.BNPLPaymentResponse;
import com.app.itibari.models.response.BatchResponsePageable;
import com.app.itibari.models.response.CreditBatchResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CreditLimitBatchService {
    CreditBatchResponse createCreditBatch(CreditBatchRequest creditBatchRequest, String name);

    void creditBatchActions(UUID publicId, CreditBatchAction creditBatchAction, String name);

    CreditBatchResponse getCreditBatchResponse(UUID publicId);

    List<BNPLPaymentResponse> getBatchItems(UUID publicId);

    BatchResponsePageable fetchCreditBatches(String search, List<BatchStatus> statuses, UUID parentCustomerPublicId, LocalDate startDate, LocalDate endDate, Pageable pageable);
}
