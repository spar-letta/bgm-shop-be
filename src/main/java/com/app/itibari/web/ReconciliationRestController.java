package com.app.itibari.web;

import com.app.itibari.dataTypes.CashCreditReconAdjustmentStatus;
import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.CashCreditReconAdjustmentRequest;
import com.app.itibari.models.request.DebitNoteRequest;
import com.app.itibari.models.request.ReconciliationRequest;
import com.app.itibari.models.response.CashCreditReconAdjustmentDTO;
import com.app.itibari.models.response.CashCreditReconPendingAdjustmentsDTO;
import com.app.itibari.models.response.CashCreditReconReportDTO;
import com.app.itibari.models.response.ReconOtpInfo;
import com.app.itibari.services.ReconciliationService;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "reconciliation", description = "Reconciliation API")
@RestController
@RequestMapping("/reconciliation")
public class ReconciliationRestController {

    private final ReconciliationService reconciliationService;

    public ReconciliationRestController(ReconciliationService reconciliationService) {

        this.reconciliationService = reconciliationService;
    }

    //fetch all reconciliations

    @PreAuthorize("hasAuthority('FINANCE_GET_RECONCILIATION')")
    @JsonView(BaseView.ReconciliationSummaryView.class)
    @GetMapping
    @Operation(summary = "Fetch all Finance reconciliations", description = "Fetch all reconciliations",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET RECONCILATIONS", value = Examples.GET_RECONS_OK)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<Reconciliation> getAllReconciliations(
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @RequestParam(name = "deliveryAgentPublicId", required = false) String deliveryAgentPublicId,
            @RequestParam(name = "deliveryId", required = false) String deliveryId,
            @RequestParam(name = "deliveryStatus", required = false) String deliveryStatus,
            @RequestParam(name = "vehicleNumber", required = false) String vehicleNumber,
            @RequestParam(name = "reconciled", required = false) Boolean reconciled,
            @RequestParam(name = "isWalkIn", required = false) Boolean isWalkIn,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return reconciliationService.getAllReconciliations(searchParam,
                deliveryAgentPublicId,
                deliveryId,
                deliveryStatus,
                vehicleNumber,
                reconciled,
                from,
                to,
                isWalkIn,
                pageable);
    }

    //fetch reconciliation by id

    @PreAuthorize("hasAuthority('FINANCE_GET_RECONCILIATION')")
    @JsonView(BaseView.ReconciliationSummaryView.class)
    @GetMapping("/{publicId}/items")
    @Operation(summary = "Fetch reconciliation products", description = "Fetch reconciliation products",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET RECONCILIATIONS items", value = Examples.GET_RECONS_P_ITEMS_OK)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<ProductItem> getReconciliationProducts(
            @PathVariable("publicId") UUID reconciliationId,
            @RequestParam(name = "searchParam", required = false) String searchParam,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return reconciliationService.getReconciliationProducts(reconciliationId, searchParam, pageable);
    }

    // actions

    @PreAuthorize("hasAuthority('FINANCE_UPDATE_RECONCILIATION')")
    @PostMapping("/reconcile/{publicId}")
    @Operation(summary = "Reconcile", description = "Reconcile",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void reconcile(@PathVariable(name = "publicId") UUID publicId,
                          @RequestBody @Valid ReconciliationRequest reconciliationRequest,
                          @Parameter(hidden = true) Principal principal) {

        reconciliationRequest.setPublicId(publicId);
        reconciliationService.reconcile(reconciliationRequest, principal.getName());
    }

    ///  debit note

    //@PreAuthorize("hasAuthority('FINANCE_CREATE_DEBIT_NOTE')")
    @JsonView(BaseView.DebitNotesView.class)
    @PostMapping("/{publicId}/debitNote")
    @Operation(summary = "Create Debit Note", description = "Create a debit note for a reconciliation",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public DebitNotes createDebitNote(@PathVariable(name = "publicId") UUID publicId,
                                      @RequestBody @Valid DebitNoteRequest debitNoteRequest,
                                      @Parameter(hidden = true) Principal principal) {

        debitNoteRequest.setReconciliationPublicId(publicId);
        return reconciliationService.createDebitNote(debitNoteRequest, principal.getName());
    }

    @JsonView(BaseView.DebitNotesView.class)
    @GetMapping("/debitNotes")
    @Operation(summary = "Fetch Debit Notes", description = "Fetch all debit notes, optionally filtered by delivery agent, reconciliation, status, creation date range and route number",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET DEBIT NOTES", value = "")})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<DebitNotes> getDebitNotes(@RequestParam(name = "deliveryAgentPublicId", required = false) UUID deliveryAgentPublicId,
                                          @RequestParam(name = "searchParam", required = false) String searchParam,
                                          @RequestParam(name = "reconciliationPublicId", required = false) String reconciliationPublicId,
                                          @RequestParam(name = "reconciled", required = false) Boolean reconciled,
                                          @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                          @RequestParam(name = "routeNumber", required = false) String routeNumber,
                                          @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return reconciliationService.getDebitNotes(deliveryAgentPublicId, searchParam,
                reconciliationPublicId, reconciled, from, to, routeNumber, pageable);
    }

    @JsonView(BaseView.DebitNotesView.class)
    @GetMapping("/debitNote/{publicId}")
    @Operation(summary = "Fetch Debit Note by ID", description = "Fetch a debit note by its public ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public DebitNotes getDebitNoteById(@PathVariable("publicId") UUID publicId) {

        return reconciliationService.getDebitNoteById(publicId);
    }

    @PatchMapping("/debitNote/{publicId}")
    @Operation(summary = "Update Debit Note", description = "Update an existing debit note",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void updateDebitNote(@PathVariable("publicId") UUID publicId,
                                @RequestParam(name = "amount") BigDecimal amount,
                                @Parameter(hidden = true) Principal principal) {

        //reconciliationService.updateDebitNote(publicId, amount, principal.getName());
    }

    @DeleteMapping("/debitNotes/{publicId}")
    @Operation(summary = "Delete Debit Note", description = "Delete a debit note by its public ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void deleteDebitNote(@PathVariable("publicId") UUID publicId,
                                @Parameter(hidden = true) Principal principal) {

        reconciliationService.deleteDebitNote(publicId, principal.getName());
    }


    // fetch order number and otp's for reconciliation

    @PreAuthorize("hasAuthority('FINANCE_GET_RECONCILIATION')")
    @GetMapping("/{publicId}/orderOtps")
    @Operation(summary = "Fetch order numbers and OTPs for a reconciliation", description = "Fetch order numbers and OTPs for a reconciliation",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET RECONCILIATION ORDER OTPS", value = "")})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public List<ReconOtpInfo> getOrderNumbersAndOtps(@PathVariable("publicId") UUID reconciliationId) {

        return reconciliationService.getOrderNumbersAndOtps(reconciliationId);
    }


    @PreAuthorize("hasAuthority('FINANCE_GET_RECONCILIATION')")
    @JsonView(BaseView.ReconciliationSummaryView.class)
    @GetMapping("/creditRecons")
    @Operation(summary = "Fetch all Finance credit  reconciliations", description = "Fetch all credit reconciliations",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET RECONCILATIONS", value = "")})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    Page<CreditRecon> getAllCreditRecons(@RequestParam(name = "searchParam", required = false) String searchParam,
                                         @RequestParam(name = "deliveryAgentPublicId", required = false) String deliveryAgentPublicId,
                                         @RequestParam(name = "salesAgentPublicId", required = false) String salesAgentPublicId,
                                         @RequestParam(name = "customerPublicId", required = false) String customerPublicId,
                                         @RequestParam(name = "orderNumber", required = false) String orderNumber,
                                         @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return reconciliationService.getAllCreditRecons(searchParam, deliveryAgentPublicId, salesAgentPublicId, customerPublicId, orderNumber, from, to, pageable);
    }


    // credits recon items
    @PreAuthorize("hasAuthority('FINANCE_GET_RECONCILIATION')")
    @JsonView(BaseView.ReconciliationSummaryView.class)
    @GetMapping("/creditRecon/{publicId}/items")
    @Operation(summary = "Fetch credit recon products", description = "Fetch credit recon products",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET CREDIT RECONCILATION items", value = "")})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    @PageableAsQueryParam
    public Page<CreditReconItem> getCreditReconProducts(@PathVariable("publicId") UUID creditReconId,
                                                        @RequestParam(name = "searchParam", required = false) String searchParam,
                                                        @PageableDefault(size = 20, sort = "productName", direction = Sort.Direction.ASC) Pageable pageable) {
        return reconciliationService.getCreditReconItems(creditReconId, searchParam, pageable);
    }

    // reconcile credit recon

    @PreAuthorize("hasAuthority('FINANCE_GET_RECONCILIATION')")
    @PatchMapping("/creditRecon/{publicId}/reconcile")
    @Operation(summary = "Reconcile credit recon", description = "Reconcile a credit reconciliation",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void reconcileCreditRecon(@PathVariable("publicId") UUID creditReconId,
                                     @RequestParam(name = "comment", required = false) String comment,
                                     @Parameter(hidden = true) Principal principal) {
        reconciliationService.reconcileCreditRecon(creditReconId, comment, principal.getName());
    }

    // @PreAuthorize("hasAuthority('FINANCE_GET_RECONCILIATION')")
    @PostMapping("/cash-credit-reports")
    @Operation(summary = "Fetch cash and credit reconciliation report", description = "Fetch cash and credit reconciliation report",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET CASH CREDIT RECONCILIATION REPORT", value = Examples.GET_CASH_CREDIT_RECON_REPORT_OK)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public CashCreditReconReportDTO generateCashCreditReconReport(
            @RequestParam(name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate,
            @Parameter(hidden = true) Principal principal) {

        return reconciliationService.generateCashCreditReconReport(reportDate, principal.getName());
    }

    // @PreAuthorize("hasAuthority('FINANCE_GET_RECONCILIATION')")
    @GetMapping("/cash-credit-reports/{publicId}")
    @Operation(summary = "Fetch cash and credit reconciliation report")
    public CashCreditReconReportDTO getCashCreditReconReport(@PathVariable("publicId") UUID publicId) {

        return reconciliationService.getCashCreditReconReport(publicId);
    }

    // @PreAuthorize("hasAuthority('FINANCE_UPDATE_RECONCILIATION')")
    @PatchMapping("/cash-credit-reports/{publicId}/refresh")
    @Operation(summary = "Refresh cash and credit reconciliation report")
    public CashCreditReconReportDTO refreshCashCreditReconReport(@PathVariable("publicId") UUID publicId,
                                                                 @Parameter(hidden = true) Principal principal) {

        return reconciliationService.refreshCashCreditReconReport(publicId, principal.getName());
    }

    //  @PreAuthorize("hasAuthority('FINANCE_UPDATE_RECONCILIATION')")
    @PatchMapping("/cash-credit-reports/{reportPublicId}/lines/{linePublicId}/adjustments")
    @Operation(summary = "Update a cash and credit reconciliation report adjustment")
    public CashCreditReconReportDTO updateCashCreditReconAdjustment(
            @PathVariable("reportPublicId") UUID reportPublicId,
            @PathVariable("linePublicId") UUID linePublicId,
            @RequestBody @Valid CashCreditReconAdjustmentRequest request,
            @Parameter(hidden = true) Principal principal) {

        return reconciliationService.updateCashCreditReconAdjustment(reportPublicId, linePublicId, request, principal.getName());
    }

    //  @PreAuthorize("hasAuthority('FINANCE_UPDATE_RECONCILIATION')")
    @PatchMapping("/cash-credit-reports/{publicId}/reconcile")
    @Operation(summary = "Reconcile cash and credit reconciliation report")
    public CashCreditReconReportDTO reconcileCashCreditReconReport(@PathVariable("publicId") UUID publicId,
                                                                   @RequestParam(name = "comment") String comment,
                                                                   @Parameter(hidden = true) Principal principal) {

        return reconciliationService.reconcileCashCreditReconReport(publicId, comment, principal.getName());
    }

    // @PreAuthorize("hasAuthority('FINANCE_GET_RECONCILIATION')")
    @GetMapping("/cash-credit-reports/{publicId}/events")
    @Operation(summary = "Fetch cash and credit reconciliation report events")
    @PageableAsQueryParam
    public Page<Events> getCashCreditReconReportEvents(
            @PathVariable("publicId") UUID publicId,
            @PageableDefault(size = 20, sort = "eventDateTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return reconciliationService.getCashCreditReconReportEvents(publicId, pageable);
    }


    // add adjustment


    @PostMapping("/addAjustment")
    @Operation(summary = "Add adjustment to cash and credit reconciliation report")
    public String addCashCreditReconAdjustment(@RequestBody @Valid CashCreditReconAdjustmentRequest request,
                                               @Parameter(hidden = true) Principal principal) {

        return reconciliationService.addCashCreditReconAdjustment(request, principal.getName());
    }

    @GetMapping("/cash-credit-reports/adjustments")
    @Operation(summary = "List logistics expenses and their review status, grouped by report line")
    @PageableAsQueryParam
    public Page<CashCreditReconPendingAdjustmentsDTO> getCashCreditReconAdjustments(@RequestParam(name = "deliveryAgentPublicId", required = false) String deliveryAgentPublicId,
                                                                                    @RequestParam(name = "searchParam", required = false) String searchParam,
                                                                                    @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                                    @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                                                    @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return reconciliationService.getCashCreditReconAdjustments(deliveryAgentPublicId, searchParam, from, to, pageable);
    }

    @PatchMapping("/cash-credit-reports/adjustments/{publicId}/review")
    @Operation(summary = "Approve or reject a submitted logistics expense")
    public CashCreditReconAdjustmentDTO reviewCashCreditReconAdjustment(
            @PathVariable UUID publicId,
            @RequestParam CashCreditReconAdjustmentStatus status,
            @RequestParam(required = false) String comment,
            @Parameter(hidden = true) Principal principal) {
        return reconciliationService.reviewCashCreditReconAdjustment(publicId, status, comment, principal.getName());
    }

    @PatchMapping("/cash-credit-reports/adjustments/{publicId}/amount")
    @Operation(summary = "Edit the amount of a pending logistics expense")
    public CashCreditReconAdjustmentDTO updateCashCreditReconAdjustmentAmount(
            @PathVariable UUID publicId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String comment,
            @Parameter(hidden = true) Principal principal) {
        return reconciliationService.updateCashCreditReconAdjustmentAmount(publicId, amount, comment, principal.getName());
    }
}
