package com.app.itibari.services;

import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.DebitNoteRequest;
import com.app.itibari.models.request.CashCreditReconAdjustmentRequest;
import com.app.itibari.models.request.ReconciliationRequest;
import com.app.itibari.models.response.CashCreditReconReportDTO;
import com.app.itibari.models.response.CashCreditReconAdjustmentDTO;
import com.app.itibari.models.response.CashCreditReconPendingAdjustmentsDTO;
import com.app.itibari.dataTypes.CashCreditReconAdjustmentStatus;
import com.app.itibari.models.response.ReconOtpInfo;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReconciliationService {

    Page<Reconciliation> getAllReconciliations(String searchParam, String deliveryAgentPublicId,
                                               String deliveryId, String deliveryStatus, String vehicleNumber,
                                               Boolean reconciled, LocalDate from, LocalDate to, Boolean walkIn, Pageable pageable);

    Page<ProductItem> getReconciliationProducts(UUID reconciliationId, String searchParam, Pageable pageable);

    void reconcile(@Valid ReconciliationRequest reconciliationRequest, String name);

    DebitNotes createDebitNote(@Valid DebitNoteRequest debitNoteRequest, String name);

    Page<DebitNotes> getDebitNotes(UUID deliveryAgentPublicId,
                                   String searchParam,
                                   String reconciliationPublicId,
                                   Boolean reconciled,
                                   LocalDate from,
                                   LocalDate to,
                                   String routeNumber,
                                   Pageable pageable);

    DebitNotes getDebitNoteById(UUID publicId);

    void updateDebitNote(UUID publicId, BigDecimal amount, String name);

    void deleteDebitNote(UUID publicId, String name);

    List<ReconOtpInfo> getOrderNumbersAndOtps(UUID reconciliationId);

    Page<CreditRecon> getAllCreditRecons(String searchParam, String deliveryAgentPublicId, String salesAgentPublicId, String customerPublicId, String orderNumber, LocalDate from, LocalDate to, Pageable pageable);

    Page<CreditReconItem> getCreditReconItems(UUID creditReconId, String searchParam, Pageable pageable);

    void reconcileCreditRecon(UUID creditReconId, String comment, String name);

    CashCreditReconReportDTO generateCashCreditReconReport(LocalDate reportDate, String name);

    CashCreditReconReportDTO getCashCreditReconReport(UUID publicId);

    CashCreditReconReportDTO refreshCashCreditReconReport(UUID publicId, String name);

    CashCreditReconReportDTO updateCashCreditReconAdjustment(UUID reportPublicId,
                                                             UUID linePublicId,
                                                             CashCreditReconAdjustmentRequest request,
                                                             String name);

    CashCreditReconReportDTO reconcileCashCreditReconReport(UUID publicId, String comment, String name);

    Page<Events> getCashCreditReconReportEvents(UUID publicId, Pageable pageable);

    String addCashCreditReconAdjustment(@Valid CashCreditReconAdjustmentRequest request, String name);

    Page<CashCreditReconPendingAdjustmentsDTO> getCashCreditReconAdjustments(String deliveryAgentAgent, String searchParam, LocalDate from, LocalDate to, Pageable pageable);

    CashCreditReconAdjustmentDTO reviewCashCreditReconAdjustment(UUID adjustmentPublicId,
                                                                  CashCreditReconAdjustmentStatus status,
                                                                  String comment,
                                                                  String name);

    CashCreditReconAdjustmentDTO updateCashCreditReconAdjustmentAmount(UUID adjustmentPublicId,
                                                                        BigDecimal amount,
                                                                        String comment,
                                                                        String name);
}
