package com.app.itibari.web;

import com.app.itibari.dataTypes.*;
import com.app.itibari.docs.Examples;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.*;
import com.app.itibari.models.response.*;
import com.app.itibari.services.*;
import com.app.itibari.utils.views.BaseView;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "BNPL", description = "BNPL API")
@RestController
@RequestMapping("/bnpl")
public class BNPLRestController {

    private final BNPLService bnplService;
    private final FileService fileService;
    private final BnplCrmService bnplCrmService;
    private final CreditLimitBatchService creditLimitBatchService;
    private final BnplCreditLimitAdjustmentService creditLimitAdjustmentService;

    public BNPLRestController(BNPLService bnplService, FileService fileService, BnplCrmService bnplCrmService, CreditLimitBatchService creditLimitBatchService,
                              BnplCreditLimitAdjustmentService creditLimitAdjustmentService) {
        this.bnplService = bnplService;
        this.fileService = fileService;
        this.bnplCrmService = bnplCrmService;
        this.creditLimitBatchService = creditLimitBatchService;
        this.creditLimitAdjustmentService = creditLimitAdjustmentService;
    }

    @GetMapping("/creditLimitAdjustments")
    @Operation(summary = "Get BNPL credit limit adjustments",
            description = "Filter by inclusive creation dates (East Africa Time), customer public ID, "
                    + "and case-insensitive customer name, store name or phone search. "
                    + "Example: /bnpl/creditLimitAdjustments?startDate=2026-09-01&endDate=2026-09-30"
                    + "&searchParam=Example&page=0&size=20&sort=creationTime,desc",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Paginated credit limit adjustments",
                            content = @Content(mediaType = "application/json", examples = {
                                    @ExampleObject(name = "Credit limit adjustments", value = Examples.BNPL_CREDIT_LIMIT_ADJUSTMENTS_RESPONSE)
                            })),
                    @ApiResponse(responseCode = "400", description = "Invalid filter or date range", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content)
            })
    public Page<BnplCreditLimitAdjustmentResponse> fetchCreditLimitAdjustments(@Parameter(description = "Inclusive creation start date in East Africa Time", example = "2026-09-01")
                                                                               @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                               @Parameter(description = "Inclusive creation end date in East Africa Time", example = "2026-09-30")
                                                                               @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                                               @Parameter(description = "Customer public ID", example = "ef2f4703-b478-4ca6-98d1-bc3aa88e1c3f")
                                                                               @RequestParam(name = "customerPublicId", required = false) UUID customerPublicId,
                                                                               @Parameter(description = "Partial customer name, store name or phone number", example = "Example")
                                                                               @RequestParam(name = "searchParam", required = false) String searchParam,
                                                                               @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return creditLimitAdjustmentService.fetchAdjustments(startDate, endDate, customerPublicId, searchParam, pageable);
    }


    @GetMapping("/customers")
    @Operation(summary = "Get BNPL customers", description = "Get BNPL customers",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public BNPLCustomerResponsePageable getBNPLCustomers(@RequestParam(name = "search", required = false) String search,
                                                         @RequestParam(name = "creditStatus", required = false) CreditStatus creditStatus,
                                                         @RequestParam(name = "customerPublicId", required = false) UUID customerPublicId,
                                                         @RequestParam(name = "customerType", required = false) CustomerType customerType,
                                                         @RequestParam(name = "regionPublicId", required = false) UUID regionPublicId,
                                                         @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return bnplService.getBNPLCustomers(search, creditStatus, customerPublicId, customerType, regionPublicId, pageable);
    }

    @GetMapping("/customers/withChildCustomers")
    @Operation(summary = "Get BNPL customers with child customers", description = "Get BNPL customers with child customers",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public BNPLCustomerResponsePageable getBNPLCustomersWithChildCustomers(@RequestParam(name = "search", required = false) String search,
                                                                           @RequestParam(name = "creditStatus", required = false) CreditStatus creditStatus,
                                                                           @RequestParam(name = "customerPublicId", required = false) UUID customerPublicId,
                                                                           @RequestParam(name = "customerType", required = false) CustomerType customerType,
                                                                           @RequestParam(name = "regionPublicId", required = false) UUID regionPublicId,
                                                                           @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return bnplService.getBNPLCustomersWithChildCustomers(search, creditStatus, customerPublicId, customerType, regionPublicId, pageable);
    }

    // top 5 credit customers

    @GetMapping("/customers/topCredit")
    @Operation(summary = "Get Top 5 BNPL customers by credit limit balance", description = "Get Top 5 BNPL customers by credit limit balance",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<BNPLCustomerResponseDTO> getTop5BNPLCustomersByCreditLimitBalance() {
        return bnplService.getBNPLCustomerTop();
    }


    @GetMapping("/creditSales")
    @Operation(summary = "Get Credit Sales Data", description = "Get Credit Sales Data",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public List<CreditSalesDTO> getCreditSalesData() {
        return bnplService.getCreditSalesData();
    }

    // fetch eligible customers for BNPL account creation


    // LEAD LISTS
    @GetMapping("/eligibleCustomers")
    @Operation(summary = "Get Eligible Customers for BNPL Account Creation", description = "Get Eligible Customers for BNPL Account Creation",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public BNPLEligibleCustomerPageable getEligibleCustomersForBNPLAccountCreation(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "customerPublicId", required = false) UUID customerPublicId,
            @RequestParam(name = "customerType", required = false) CustomerType customerType,
            @RequestParam(name = "regionPublicId", required = false) UUID regionPublicId,
            @PageableDefault(size = 20) Pageable pageable) {
        return bnplService.getEligibleCustomersForBNPLAccountCreation(search, customerPublicId, customerType, regionPublicId, pageable);

    }

    // churned customers

    // TODO created a similar end-point
    @Deprecated
    @GetMapping("/churnedCustomerss")
    @Operation(summary = "Get Churned BNPL Customers", description = "Get Churned BNPL Customers",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public BNPLEligibleCustomerPageable getChurnedBNPLCustomers(@RequestParam(name = "search", required = false) String search,
                                                                @RequestParam(name = "customerPublicId", required = false) UUID customerPublicId,
                                                                @RequestParam(name = "customerType", required = false) CustomerType customerType,
                                                                @RequestParam(name = "regionPublicId", required = false) UUID regionPublicId,
                                                                @RequestParam(name = "locations", required = false) List<UUID> locations,
                                                                @RequestParam(name = "fromDate", required = false) String fromDate,
                                                                @RequestParam(name = "toDate", required = false) String toDate,
                                                                @Parameter(hidden = true) Principal principal,
                                                                @PageableDefault(size = 20) Pageable pageable) {
        return bnplService.getChurnedBNPLCustomers(search, customerPublicId, customerType, regionPublicId, locations, principal.getName(), fromDate, toDate, pageable);
    }

    @GetMapping("/churnedCustomers")
    @Operation(summary = "Get Churned Credit Customers", description = "Get customers with previous credit orders but no credit order in the last 30 days",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public BNPLEligibleCustomerPageable getChurnedCreditCustomers(@RequestParam(name = "search", required = false) String search,
                                                                  @RequestParam(name = "customerPublicId", required = false) UUID customerPublicId,
                                                                  @RequestParam(name = "customerType", required = false) CustomerType customerType,
                                                                  @RequestParam(name = "regionPublicId", required = false) UUID regionPublicId,
                                                                  @RequestParam(name = "locations", required = false) List<UUID> locations,
                                                                  @RequestParam(name = "fromDate", required = false) String fromDate,
                                                                  @RequestParam(name = "toDate", required = false) String toDate,
                                                                  @Parameter(hidden = true) Principal principal,
                                                                  @PageableDefault(size = 20) Pageable pageable) {
        return bnplService.getChurnedCreditCustomers(search, customerPublicId, customerType, regionPublicId, locations, principal.getName(), fromDate, toDate, pageable);
    }


    @PostMapping("/onboardCustomer")
    @Operation(summary = "Onboard Customers to BNPL", description = "Onboard Customer to BNPL",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void onboardCustomerToBNPL(@RequestBody BNPLOnboarding bnplOnboardingRequest,
                                      @Parameter(hidden = true) Principal principal) {

        bnplService.onboardCustomerToBNPL(bnplOnboardingRequest, principal.getName());
    }

    // wallets

    @GetMapping("/wallets")
    @Operation(summary = "Get BNPL Wallets", description = "Get BNPL Wallets",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<WalletResponseDTO> getBNPLWallets(@RequestParam(name = "search", required = false) String search,
                                                  @RequestParam(name = "customerType", required = false) CustomerType customerType,
                                                  @RequestParam(name = "customerPublicId", required = false) UUID customerPublicId,
                                                  @RequestParam(name = "startDate", required = false) String startDate,
                                                  @RequestParam(name = "dueDate", required = false) String dueDate,
                                                  @RequestParam(name = "creditStatus", required = false) CreditStatus creditStatus,
                                                  @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return bnplService.getBNPLWallets(search, customerType, customerPublicId, startDate, dueDate, creditStatus, pageable);
    }


    // wallet payments

    @GetMapping("/wallets/{walletId}/payments")
    @JsonView(BaseView.DetailedView.class)
    @Operation(summary = "Get BNPL Wallet Payments", description = "Get BNPL Wallet Payments",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<PaymentItem> getBNPLWalletPayments(@PathVariable UUID walletId,
                                                   @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return bnplService.getBNPLWalletPayments(walletId, pageable);
    }

    // credit limit items

    @GetMapping("/wallets/{walletId}/creditLimitItems")
    @JsonView(BaseView.DetailedView.class)
    @Operation(summary = "Get BNPL Wallet Credit Limit Items", description = "Get BNPL Wallet Credit Limit Items",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<CreditLimitItem> getBNPLWalletCreditLimitItems(@PathVariable UUID walletId,
                                                               @RequestParam(name = "parentCustomerPublicId", required = false) UUID parentCustomerPublicId,
                                                               @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return bnplService.getBNPLWalletCredits(walletId, parentCustomerPublicId, pageable);
    }

    // payment dashboard summary

    @GetMapping("/paymentSummary")
    @Operation(summary = "Get BNPL Payment Dashboard Summary", description = "Get BNPL Payment Dashboard Summary",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public BNPLCreditControlDTO getBNPLPaymentDashboardSummary() {
        return bnplService.getBNPLPaymentDashboardSummary();
    }


    // credit payments

    @GetMapping("/creditPayments")
    @Operation(summary = "Get BNPL Credit Payments Data", description = "Get BNPL Credit Payments Data",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public BNPLPaymentResponsePageable getBNPLCreditPaymentsData(@RequestParam(name = "search", required = false) String search,
                                                                 @RequestParam(name = "customerPublicId", required = false) UUID customerPublicId,
                                                                 @RequestParam(name = "paymentStatus", required = false) PaymentStatusEnum paymentStatus,
                                                                 @RequestParam(name = "customerType", required = false) CustomerType customerType,
                                                                 @RequestParam(name = "regionPublicId", required = false) UUID regionPublicId,
                                                                 @RequestParam(name = "startDate", required = false) String startDate,
                                                                 @RequestParam(name = "endDate", required = false) String dueDate,
                                                                 @RequestParam(name = "parentCustomerPublicId", required = false) UUID parentCustomerPublicId,
                                                                 @RequestParam(name = "batched", required = false) Batched batched,
                                                                 @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return bnplService.getBNPLCreditPaymentsData(search, customerPublicId, paymentStatus, customerType, regionPublicId, startDate, dueDate, parentCustomerPublicId, batched, pageable);
    }

    @GetMapping("/customerPurchaseTrend/{customerPublicId}")
    @Operation(summary = "Get Customer Purchase Trend", description = "Get Customer Purchase Trend",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public List<CustomerPurchaseTrend> getCustomerPurchaseTrend(@PathVariable UUID customerPublicId,
                                                                @RequestParam(name = "year", required = false) Integer year) {
        return bnplService.getCustomerPurchaseTrend(customerPublicId, year);
    }


    //transaction items for wallet

    @GetMapping("/wallets/{walletId}/transactionItems")
    @JsonView(BaseView.DetailedView.class)
    @Operation(summary = "Get BNPL Wallet Transaction Items", description = "Get BNPL Wallet Transaction Items",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<TransactionItem> getBNPLWalletTransactionItems(@PathVariable UUID walletId,
                                                               @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return bnplService.getBNPLWalletTransactionItems(walletId, pageable);
    }


    // actions on wallet

    @PatchMapping("/{customerPublicId}/actions")
    @Operation(summary = "Perform Actions on BNPL Wallet", description = "Perform Actions on BNPL Wallet",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void performActionsOnBNPLWallet(@PathVariable UUID customerPublicId,
                                           @RequestParam(name = "action") WalletAction action,
                                           @RequestParam(name = "reasonPublicId", required = false) UUID reasonPublicId,
                                           @RequestParam(name = "amount", required = false) BigDecimal amount,
                                           @Parameter(hidden = true) Principal principal) {
        bnplService.performActionsOnBNPLWallet(customerPublicId, action, reasonPublicId, amount, principal.getName());
    }

    // make deposit to wallet/payment

    @PatchMapping("/{customerPublicId}/pay")
    @Operation(summary = "Make Deposit to BNPL Wallet", description = "Make Deposit to BNPL Wallet",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void makeDepositToBNPLWallet(@PathVariable UUID customerPublicId,
                                        @RequestParam(name = "amount") BigDecimal amount,
                                        @RequestParam(name = "payViaWallet", required = false) Boolean payViaWallet,
                                        @RequestParam(name = "receipts", required = false) List<String> receipts,
                                        @Parameter(hidden = true) Principal principal) {
        bnplService.makeDepositToBNPLWallet(customerPublicId, amount, receipts, payViaWallet, principal.getName());
    }


    // raise a customer feedback ticket

    @PostMapping("/{customerPublicId}/raiseFeedbackTicket")
    @Operation(summary = "Raise Customer Feedback Ticket", description = "Raise Customer Feedback Ticket",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void raiseCustomerFeedbackTicket(@PathVariable UUID customerPublicId,
                                            @RequestParam(name = "issueDescription") String issueDescription,
                                            @RequestParam("feedbackReasonPublicId") UUID feedbackReasonPublicId,
                                            @Parameter(hidden = true) Principal principal) {
        bnplService.raiseCustomerFeedbackTicket(customerPublicId, issueDescription, feedbackReasonPublicId, principal.getName());
    }

    // get customer tickets

    @JsonView(BaseView.DetailedView.class)
    @GetMapping("/customerFeedbackTicket")
    @Operation(summary = "Get Customer Feedback Tickets", description = "Get Customer Feedback Tickets",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<CustomerFeedBack> getCustomerFeedbackTickets(@RequestParam(name = "customerPublicId", required = false) UUID customerPublicId,
                                                             @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return bnplService.getCustomerFeedbackTickets(customerPublicId, pageable);
    }

    // customer self onboarding
    @PostMapping("/customerSelfOnboard")
    @Operation(summary = "customer Self Onboard to BNPL", description = "Customer Self Onboard to BNPL",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void customerSelfOnBoard(@RequestBody BNPLOnboarding bnplOnboardingRequest,
                                    @Parameter(hidden = true) Principal principal) {

        bnplService.customerSelfOnBoard(bnplOnboardingRequest, principal.getName());
    }

    // get credit limit for customer

    @GetMapping("/customer/{customerId}/creditLimit")
    @Operation(summary = "Get Customer Credit Limit", description = "Get Customer Credit Limit",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public ResponseEntity<CreditLimitResponse> getCustomerCreditLimit(@PathVariable UUID customerId,
                                                                      @Parameter(hidden = true) Principal principal) {
        CreditLimitResponse response = bnplService.getCustomerCreditLimitForCustomer(customerId, principal.getName());

        return ResponseEntity.ok(response);
    }

    // get credit status dashboard

    @GetMapping("/creditStatusDashboard")
    @Operation(summary = "Get Credit Status Dashboard", description = "Get Credit Status Dashboard",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public CreditStatusDashboardDTO getCreditStatusDashboard() {
        return bnplService.getCreditStatusDashboard();
    }

    @GetMapping("/unPaidCustomerCreditLimitItems")
    @JsonView(BaseView.DetailedView.class)
    @Operation(summary = "Get BNPL Wallet Credit Limit Items", description = "Get BNPL Wallet Credit Limit Items",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<CreditLimitItem> getUnPaidCustomerCreditItems(@Parameter(hidden = true) Principal principal,
                                                              @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return bnplService.getCustomerUnPaidCreditItems(principal.getName(), pageable);
    }


    // get customer payment transactions history

    @GetMapping("/customerPaymentTransactionsHistory")
    @JsonView(BaseView.DetailedView.class)
    @Operation(summary = "Get Customer Payment Transactions History", description = "Get Customer Payment Transactions History",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<TransactionItem> getCustomerPaymentTransactionsHistory(@Parameter(hidden = true) Principal principal,
                                                                       @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return bnplService.getCustomerPaymentTransactionsHistory(principal.getName(), pageable);
    }


    @GetMapping("/customerPurchaseTrends")
    @Operation(summary = "Get all Customer Purchase Trends from bnpl", description = "Get Customer all Purchase Trends from bnpl",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public List<AllCustomerPurchaseTrend> getCustomerPurchaseTrends(@RequestParam(name = "year", required = false) Integer year) {
        return bnplService.getCustomerPurchaseTrends(year);
    }

    // loan management pageable

    @GetMapping("/loanManagement")
    @Operation(summary = "Get BNPL Loan Management Data", description = "Get BNPL Loan Management Data",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public BnplLoanManagementPageable getBNPLLoanManagementData(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) PaymentStatusEnum paymentStatus,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "dueDate", required = false) String dueDate,
            @RequestParam(name = "customerPublicId", required = false) UUID customerPublicId,
            @RequestParam(name = "parentCustomerPublicId", required = false) UUID parentCustomerPublicId,
            @PageableDefault(size = 20) Pageable pageable) {
        return bnplService.getBNPLLoanManagementData(search, customerPublicId, startDate, dueDate, paymentStatus, parentCustomerPublicId, pageable);
    }

    // send reminder emails to customers with due payments

    @PostMapping("/sendDuePaymentReminders")
    @Operation(summary = "Send Due Payment Reminders", description = "Send Due Payment Reminders",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void sendDuePaymentReminders(@RequestParam("creditPublicId") UUID creditPublicId,
                                        @Parameter(hidden = true) Principal principal) {
        bnplService.sendDuePaymentReminders(creditPublicId, principal.getName());
    }

    // customer credit pay

    @PostMapping("/customerCreditPay")
    @Operation(summary = "Customer Credit Pay", description = "Customer Credit Pay",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void customerCreditPay(@RequestParam(value = "file", required = false) MultipartFile file,
                                  @RequestParam(value = "referenceId") UUID referenceId,
                                  @RequestParam(value = "orderNumber") String description,
                                  @RequestParam(value = "receipt", required = false) String receipt,
                                  @RequestParam(value = "amount", required = false) String amount,
                                  @RequestParam(value = "payViaWallet", defaultValue = "false") Boolean payViaWallet,
                                  @RequestParam(value = "hasFile", defaultValue = "false") Boolean hasFile,
                                  @Parameter(hidden = true) Principal principal) {

        if (hasFile) {
            fileService.uploadFile(file, referenceId, ReferenceType.CREDIT_PAYMENT, description, receipt, amount, null, null, false, null, null);
        }
        // double entry for credit payment, one for file upload and another for wallet payment if payViaWallet is true
        if (payViaWallet != null && payViaWallet) {
            bnplService.customerCreditPay(referenceId, description, true, hasFile, principal.getName());

        }
    }


    // get bnpl corporate approval customers

    @GetMapping("/corporateApprovalRequests")
    @JsonView(BaseView.DetailedView.class)
    @Operation(summary = "Get Customer approvals", description = "Get Customer approval requests for corporate",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<CorporateCustomerBnplApproval> getCorporateCustomerBnplApproval(@RequestParam(name = "searchParam", required = false) String searchParam,
                                                                                @RequestParam(name = "status", required = false) CorporateApprovalStatus corporateApprovalStatus,
                                                                                @Parameter(hidden = true) Principal principal,
                                                                                @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return bnplService.getCorporateCustomerBnplApproval(searchParam, corporateApprovalStatus, principal.getName(), pageable);
    }


    // approve or reject

    @PatchMapping("/corporateApprovalRequests/{publicId}")
    @Operation(summary = "Get Customer approvals", description = "Get Customer approval requests for corporate",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void actionOnCorporateCustomerBnplApproval(@PathVariable UUID publicId,
                                                      @RequestParam(name = "action") ApprovalAction approvalAction,
                                                      @Parameter(hidden = true) Principal principal) {
        bnplService.actionOnCorporateCustomerBnplApproval(publicId, approvalAction, principal.getName());
    }


    // recalculate credit limit for a customer/AOV

    @PostMapping("/{customerPublicId}/recalculateCreditLimit")
    @Operation(summary = "Recalculate Credit Limit for Customer", description = "Recalculate Credit Limit for Customer",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void recalculateCreditLimitForCustomer(@PathVariable UUID customerPublicId,
                                                  @Parameter(hidden = true) Principal principal) {
        bnplService.recalculateCreditLimitForCustomer(customerPublicId, principal.getName());
    }


    @PatchMapping("/{creditLimitPublicId}/reverseCreditPayment")
    @Operation(summary = "Reverse Credit Payment", description = "Reverse Credit Payment",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void reverseCreditPayment(@PathVariable UUID creditLimitPublicId,
                                     @RequestParam(name = "reasonPublicId") UUID reasonPublicId,
                                     @Parameter(hidden = true) Principal principal) {
        bnplService.reverseCreditPayment(creditLimitPublicId, reasonPublicId, principal.getName());
    }


    @PatchMapping("/{creditLimitPublicId}/payFromWallet")
    @Operation(summary = "Pay Credit from Wallet", description = "Pay Credit from Wallet",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void payCreditFromWallet(@PathVariable UUID creditLimitPublicId,
                                    @RequestParam(name = "amount") BigDecimal amount,
                                    @Parameter(hidden = true) Principal principal) {
        bnplService.payCreditFromWallet(creditLimitPublicId, amount, principal.getName());
    }

    // ─── BNPL CRM ────────────────────────────────────────────────────────────

    //TODO created a new endpoint
    @Deprecated
    @GetMapping("removed/crm/leadss")
    @Operation(summary = "List CRM leads", description = "Paginated list of BNPL-eligible leads",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "Get_LEADS", value = Examples.FETCH_LEADS_OKAY)})),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public BnplLeadPageable getLeads(@RequestParam(name = "search", required = false) String search,
                                     @RequestParam(name = "funnelStage", required = false) BnplFunnelStage funnelStage,
                                     @RequestParam(name = "leadStatus", required = false) BnplLeadStatus leadStatus,
                                     @RequestParam(name = "callStatus", required = false) UUID callStatus,
                                     @RequestParam(name = "districtId", required = false) UUID districtId,
                                     @RequestParam(name = "wardId", required = false) UUID wardId,
                                     @RequestParam(name = "vipOverOneMillion", required = false) Boolean vipOverOneMillion,
                                     @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return bnplCrmService.getLeads(search, funnelStage, leadStatus, callStatus, districtId, wardId, vipOverOneMillion, pageable);
    }

    @GetMapping("/crm/leads")
    @Operation(summary = "List qualified CRM leads", description = "Paginated list of non-BNPL customers with at least three orders in each of the last three 30-day periods",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public BnplLeadPageable getQualifiedLeads(@RequestParam(name = "search", required = false) String search,
                                              @RequestParam(name = "funnelStage", required = false) BnplFunnelStage funnelStage,
                                              @RequestParam(name = "leadStatus", required = false) BnplLeadStatus leadStatus,
                                              @RequestParam(name = "callStatus", required = false) UUID callStatus,
                                              @RequestParam(name = "districtId", required = false) UUID districtId,
                                              @RequestParam(name = "wardId", required = false) UUID wardId,
                                              @RequestParam(name = "vipOverOneMillion", required = false) Boolean vipOverOneMillion,
                                              @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return bnplCrmService.getQualifiedLeads(search, funnelStage, leadStatus, callStatus, districtId, wardId, vipOverOneMillion, pageable);
    }

    @GetMapping("/crm/leads/{leadPublicId}")
    @Operation(summary = "Get lead detail", description = "Full lead record including customer activity panel",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "GET_LEADS", value = Examples.FETCH_LEADS)})),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public BnplLeadResponse getLead(@PathVariable UUID leadPublicId) {
        return bnplCrmService.getLead(leadPublicId);
    }

    @PatchMapping("/crm/leads/{leadPublicId}/status")
    @Operation(summary = "Update lead colour status", description = "Set GREEN(okay) / YELLOW(undecided) / RED(No) classification on a lead",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "LEAD_STATUS", value = Examples.BNPL_CRM_LEAD_STATUS)
    })))
    public void updateLeadStatus(@PathVariable UUID leadPublicId,
                                 @RequestBody BnplLeadStatusRequest request) {
        bnplCrmService.updateLeadStatus(leadPublicId, request);
    }

    @PostMapping("/crm/leads/{leadPublicId}/sendAppLink")
    @Operation(summary = "Send app link to lead customer", description = "Send the configured app link to the lead customer's phone number",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public void sendAppLink(@PathVariable UUID leadPublicId) {
        bnplCrmService.sendAppLink(leadPublicId);
    }

    @PatchMapping("/crm/leads/{leadPublicId}/callback")
    @Operation(summary = "Flag lead for callback", description = "Flag a YELLOW lead with a callback date (enables setting another visit)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CALLBACK_FLAG", value = Examples.BNPL_CRM_CALLBACK_FLAG)
    })))
    public void flagCallback(@PathVariable UUID leadPublicId,
                             @RequestBody BnplCallbackFlagRequest request) {
        bnplCrmService.flagCallback(leadPublicId, request);
    }

    @PostMapping("/crm/leads/{leadPublicId}/calls")
    @Operation(summary = "Initiate a call", description = "Log call start — records agent, timestamp and attempt number automatically",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public BnplCallAttemptResponse initiateCall(@PathVariable UUID leadPublicId,
                                                @Parameter(hidden = true) Principal principal) {
        return bnplCrmService.initiateCall(leadPublicId, principal.getName());
    }

    @PatchMapping("/crm/calls/{callPublicId}/outcome")
    @Operation(summary = "Tag call outcome", description = "Record call status (Answered / No Answer / Rejected), lead classification, reason codes, and notes",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "ANSWERED", value = Examples.BNPL_CRM_CALL_OUTCOME_ANSWERED),
            @ExampleObject(name = "NO_ANSWER", value = Examples.BNPL_CRM_CALL_OUTCOME_NO_ANSWER),
            @ExampleObject(name = "REJECTED", value = Examples.BNPL_CRM_CALL_OUTCOME_REJECTED)
    })))
    public void tagCallOutcome(@PathVariable UUID callPublicId,
                               @RequestBody BnplCallOutcomeRequest request) {
        bnplCrmService.tagCallOutcome(callPublicId, request);
    }

    @GetMapping("/crm/leads/{leadPublicId}/calls")
    @Operation(summary = "Call history timeline", description = "All call attempts for a lead, ordered newest first",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<BnplCallAttemptResponse> getCallHistory(@PathVariable UUID leadPublicId,
                                                        @PageableDefault(size = 20, sort = "startTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return bnplCrmService.getCallHistory(leadPublicId, pageable);
    }

    @PostMapping("/crm/leads/{leadPublicId}/visits")
    @Operation(summary = "Log a field visit", description = "Record a credit officer field visit against a lead",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = {@ExampleObject(name = "Get_fields", value = Examples.FETCH_FIELD_VISIT)})),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "FIELD_VISIT", value = Examples.BNPL_CRM_FIELD_VISIT)
    })))
    public BnplFieldVisitResponse logFieldVisit(@PathVariable UUID leadPublicId,
                                                @RequestBody BnplFieldVisitRequest request,
                                                @Parameter(hidden = true) Principal principal) {
        return bnplCrmService.logFieldVisit(leadPublicId, request, principal.getName());
    }

    @GetMapping("/crm/leads/{leadPublicId}/visits")
    @Operation(summary = "Field visit history", description = "All field visits for a lead, ordered newest first",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public Page<BnplFieldVisitResponse> getVisitHistory(@PathVariable UUID leadPublicId,
                                                        @PageableDefault(size = 20, sort = "visitDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return bnplCrmService.getVisitHistory(leadPublicId, pageable);
    }

    @GetMapping("/crm/reason-codes")
    @Operation(summary = "List active reason codes", description = "All active call outcome reason codes",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public List<BnplReasonCodeResponse> getReasonCodes() {
        return bnplCrmService.getReasonCodes();
    }

    @PostMapping("/crm/reason-codes")
    @Operation(summary = "Create a reason code", description = "Supervisor creates a new call outcome reason code",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_REASON_CODE", value = Examples.BNPL_CRM_REASON_CODE)
    })))
    public BnplReasonCodeResponse createReasonCode(@RequestBody BnplReasonCodeRequest request) {
        return bnplCrmService.createReasonCode(request);
    }

    @PatchMapping("/crm/reason-codes/{codePublicId}")
    @Operation(summary = "Update a reason code", description = "Edit or deactivate an existing reason code",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "UPDATE_REASON_CODE", value = Examples.BNPL_CRM_UPDATE_REASON_CODE)
    })))
    public void updateReasonCode(@PathVariable UUID codePublicId,
                                 @RequestBody BnplReasonCodeRequest request) {
        bnplCrmService.updateReasonCode(codePublicId, request);
    }

    // batch creditItems for a parent customer.

    @PostMapping("/creditBatch")
    @Operation(summary = "Create a credit item batch", description = "Create a credit items batch",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "CREATE_CREDIT_BATCH", value = Examples.CREDIT_ITEMS_BATCH)
    })))
    public CreditBatchResponse createCreditBatch(@RequestBody CreditBatchRequest creditBatchRequest,
                                                 @Parameter(hidden = true) Principal principal) {
        return creditLimitBatchService.createCreditBatch(creditBatchRequest, principal.getName());
    }

    // update, using CreditBatchRequest as requestBody


    @PatchMapping("/creditBatch/{publicId}/actions")
    @Operation(summary = "Batch Actions", description = "Credit Batch Actions",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "APPROVE_BATCH", value = Examples.CREDIT_BATCH_APPROVE),
            @ExampleObject(name = "ADD_CREDIT_ITEMS", value = Examples.CREDIT_BATCH_ADD_ITEMS),
            @ExampleObject(name = "REMOVE_CREDIT_ITEMS", value = Examples.CREDIT_BATCH_REMOVE_ITEMS),
            @ExampleObject(name = "GENERATE_INVOICE", value = Examples.CREDIT_BATCH_GENERATE_INVOICE),
            @ExampleObject(name = "ALLOCATE_RECONCILE", value = Examples.CREDIT_BATCH_ALLOCATE_RECONCILE),
            @ExampleObject(name = "DELETE_BATCH", value = Examples.CREDIT_BATCH_DELETE)
    })))
    public void creditBatchActions(@PathVariable UUID publicId,
                                   @RequestBody CreditBatchAction creditBatchAction,
                                   @Parameter(hidden = true) Principal principal) {
        creditLimitBatchService.creditBatchActions(publicId, creditBatchAction, principal.getName());
    }


    //get

    @GetMapping("/creditBatch/{publicId}")
    @Operation(summary = "Get batch", description = "View a batch",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public CreditBatchResponse getCreditBatchResponse(@PathVariable UUID publicId) {
        return creditLimitBatchService.getCreditBatchResponse(publicId);
    }


    @GetMapping("/creditBatch")
    @Operation(summary = "Get batch", description = "View a batch",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public BatchResponsePageable fetchCreditBatches(@RequestParam(name = "search", required = false) String search,
                                                    @RequestParam(name = "statuses", required = false) List<BatchStatus> statuses,
                                                    @RequestParam(name = "parentCustomerPublicId", required = false) UUID parentCustomerPublicId,
                                                    @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                    @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                    @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return creditLimitBatchService.fetchCreditBatches(search, statuses, parentCustomerPublicId, startDate, endDate, pageable);
    }

    // get items

    @GetMapping("/creditBatch/{publicId}/creditItems")
    @Operation(summary = "Get batch items", description = "View a batch items",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })

    public List<BNPLPaymentResponse> getBatchItems(@PathVariable UUID publicId) {
        return creditLimitBatchService.getBatchItems(publicId);
    }

    @PostMapping("/crm/leads/{leadPublicId}/notes")
//    @PreAuthorize("hasAuthority('CREATE_BNPL_NOTES')")
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.CREATE_CUSTOMER_EXPERIENCE_response)))
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "CREATE_CUSTOMER_NOTES", required = true, content = @Content(examples = {
            @ExampleObject(value = Examples.ADD_UPDATE_CUSTOMER_NOTE_REQUEST),})))
    public BnplLeadNotesResponse addLeadCustomerNotes(@PathVariable UUID leadPublicId,
                                                      @Valid @RequestBody CustomerNotesRequest customerNotesRequest,
                                                      @Parameter(hidden = true) Principal principal) {

        return bnplCrmService.addLeadCustomerNotes(leadPublicId, customerNotesRequest, principal);
    }

    @GetMapping("/customerNotes/{leadPublicId}/listings")
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "Get lead customer notes", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.GET_ALL_CUSTOMER_EXPERIENCE_response)))})
    public Page<BnplLeadNotesResponse> listLeadCustomerNotes(@PathVariable UUID leadPublicId,
                                                             @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return bnplCrmService.listLeadNotes(leadPublicId, pageable);
    }

    @PutMapping("/crm/leads/{notePublicId}/notes")
//    @PreAuthorize("hasAuthority('UPDATE_LEAD_NOTES')")
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", schema = @Schema(example = Examples.CREATE_CUSTOMER_EXPERIENCE_response)))
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "UPDATE_LEAD_NOTES", required = true, content = @Content(examples = {
            @ExampleObject(value = Examples.ADD_UPDATE_CUSTOMER_NOTE_REQUEST),})))
    public BnplLeadNotesResponse updateCustomerNotes(@PathVariable UUID notePublicId,
                                                     @Valid @RequestBody CustomerNotesRequest customerNotesRequest,
                                                     @Parameter(hidden = true) Principal principal) {

        return bnplCrmService.updateLeadNotes(notePublicId, customerNotesRequest, principal);
    }

    @PatchMapping("/crm/leads/{leadPublicId}/assign/{assignedSalesAgentPublicId}")
    @Operation(summary = "assign sales agent to bnpl customer", description = "assign sales agent to bnpl customer",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK")
            }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(examples = {})))
    public void doAssignSalesAgent(@PathVariable UUID leadPublicId,
                                   @PathVariable UUID assignedSalesAgentPublicId,
                                   @Parameter(hidden = true) Principal principal) {
        bnplCrmService.doAssignSalesAgent(leadPublicId, assignedSalesAgentPublicId, principal);
    }

    @GetMapping("/crm/leads/assignedCustomers")
    @Operation(responses = {
            @ApiResponse(responseCode = "200", description = "Get assigned lead customer")})
    public Page<AssignedBnplCustomerResponse> listAssignedCustomers(@Parameter(hidden = true) Principal principal,
                                                                    @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {

        return bnplCrmService.listAssignedCustomers(principal, pageable);
    }

    // get creditLImitUtilization for parent customer and the child customers

    @GetMapping("/creditLimitUtilization/{parentCustomerPublicId}")
    @Operation(summary = "Get credit limit utilization", description = "Get credit limit utilization for parent customer and the child customers",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public CreditLimitUtilizationResponse getCreditLimitUtilization(@PathVariable UUID parentCustomerPublicId) {
        return bnplService.getCreditLimitUtilization(parentCustomerPublicId);
    }

    //TODO HAVE A BETTER WAY TO FETCH THIS
    @JsonView(BaseView.DetailedView.class)
    @GetMapping("/payment-items-wallet-deposit")
    @Operation(summary = "Get payment items deposit from wallet", description = "Get payment items deposit from wallet",
            responses = {@ApiResponse(responseCode = "200", description = "OK")})
    public List<PaymentItem> getPaymentItems(@RequestParam(name = "orderNumber") String orderNumber) {
        return bnplService.getPaymentItems(orderNumber);
    }

    // customer list with BNPL accounts

    @GetMapping("/enterpriseCustomers")
    @Operation(summary = "Get enterprise customers with outstanding credit items",
            description = "Get parent customers whose own or child customers' credit items are not paid or partially paid",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public BNPLCustomerResponsePageable getEnterpriseCustomers(
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return bnplService.getEnterpriseCustomers(pageable);
    }

    @GetMapping("/enterpriseCreditItems/{parentCustomerPublicId}")
    @Operation(summary = "Get enterprise credit-limit items",
            description = "Get all credit-limit items belonging to a parent customer and its child customers",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Unauthorized Request", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
            })
    public BNPLPaymentResponsePageable getEnterpriseCreditItems(
            @PathVariable UUID parentCustomerPublicId,
            @PageableDefault(size = 20, sort = "creationTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return bnplService.getEnterpriseCreditItems(parentCustomerPublicId, pageable);
    }
}
