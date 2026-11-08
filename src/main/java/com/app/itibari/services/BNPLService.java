package com.app.itibari.services;

import com.app.itibari.dataTypes.*;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.AllCustomerPurchaseTrend;
import com.app.itibari.models.request.BNPLOnboarding;
import com.app.itibari.models.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface BNPLService {
    

    List<CreditSalesDTO> getCreditSalesData();
    

    void onboardCustomerToBNPL(BNPLOnboarding bnplOnboardingRequest, String name);

    BigDecimal getCustomerCreditLimit(Customers customer, BigDecimal aov);

    Page<WalletResponseDTO> getBNPLWallets(String search, CustomerType customerType, UUID customerPublicId, String startDate, String dueDate, CreditStatus creditStatus, Pageable pageable);

    Page<PaymentItem> getBNPLWalletPayments(UUID walletId, Pageable pageable);

    Page<CreditLimitItem> getBNPLWalletCredits(UUID walletId,UUID parentCustomerPublicId, Pageable pageable);

    BNPLCreditControlDTO getBNPLPaymentDashboardSummary();


    List<CustomerPurchaseTrend> getCustomerPurchaseTrend(UUID customerPublicId, Integer year);

    Page<TransactionItem> getBNPLWalletTransactionItems(UUID walletId, Pageable pageable);

    void performActionsOnBNPLWallet(UUID customerPublicId, WalletAction action, UUID reasonPublicId, BigDecimal amount, String name);

    void makeDepositToBNPLWallet(UUID customerPublicId, BigDecimal amount, List<String> receipts,Boolean payViaWallet, String name);

    void raiseCustomerFeedbackTicket(UUID customerPublicId, String issueDescription, UUID feedbackReasonPublicId, String name);

    Page<CustomerFeedBack> getCustomerFeedbackTickets(UUID customerPublicId, Pageable pageable);

    void customerSelfOnBoard(BNPLOnboarding bnplOnboardingRequest, String name);

    CreditLimitResponse getCustomerCreditLimitForCustomer(UUID customerId, String name);

    CreditStatusDashboardDTO getCreditStatusDashboard();

    Page<BNPLCustomerResponseDTO> getBNPLCustomerTop();

    Page<CreditLimitItem> getCustomerUnPaidCreditItems(String name, Pageable pageable);

    Page<TransactionItem> getCustomerPaymentTransactionsHistory(String name, Pageable pageable);

    List<AllCustomerPurchaseTrend> getCustomerPurchaseTrends(Integer year);


    BnplLoanManagementPageable getBNPLLoanManagementData(String search, UUID customerPublicId, String startDate, String dueDate, PaymentStatusEnum paymentStatus, UUID parentCustomerPublicId, Pageable pageable);

    void sendDuePaymentReminders(UUID creditPublicId, String name);

    BNPLCustomerResponsePageable getBNPLCustomers(String search, CreditStatus creditStatus, UUID customerPublicId, CustomerType customerType, UUID regionPublicId, Pageable pageable);

    BNPLCustomerResponsePageable getBNPLCustomersWithChildCustomers(String search, CreditStatus creditStatus, UUID customerPublicId, CustomerType customerType, UUID regionPublicId, Pageable pageable);

    BNPLEligibleCustomerPageable getEligibleCustomersForBNPLAccountCreation(String search, UUID customerPublicId, CustomerType customerType, UUID regionPublicId, Pageable pageable);

    BNPLPaymentResponsePageable getBNPLCreditPaymentsData(String search, UUID customerPublicId, PaymentStatusEnum paymentStatus, CustomerType customerType, UUID regionPublicId, String startDate, String dueDate, UUID parentCustomerPublicId,Batched batched, Pageable pageable);

    void customerCreditPay(UUID referenceId, String orderNumber, Boolean payViaWallet, Boolean hasFile, String name);


    Page<CorporateCustomerBnplApproval> getCorporateCustomerBnplApproval(String searchParam, CorporateApprovalStatus corporateApprovalStatus, String name, Pageable pageable);

    void actionOnCorporateCustomerBnplApproval(UUID publicId, ApprovalAction approvalAction, String name);

    void recalculateCreditLimitForCustomer(UUID customerPublicId, String name);

    BNPLEligibleCustomerPageable getChurnedBNPLCustomers(String search, UUID customerPublicId, CustomerType customerType, UUID regionPublicId, List<UUID> locations,String loggedUser, String fromDate, String toDate, Pageable pageable);

    BNPLEligibleCustomerPageable getChurnedCreditCustomers(String search, UUID customerPublicId, CustomerType customerType, UUID regionPublicId, List<UUID> locations, String loggedUser, String fromDate, String toDate, Pageable pageable);

    void reverseCreditPayment(UUID creditLimitPublicId, UUID reasonPublicId, String name);

    void payCreditFromWallet(UUID creditLimitPublicId, BigDecimal amount, String name);

    CreditLimitUtilizationResponse getCreditLimitUtilization(UUID parentCustomerPublicId);

    List<PaymentItem> getPaymentItems(String orderNumber);

    BNPLCustomerResponsePageable getEnterpriseCustomers(Pageable pageable);

    BNPLPaymentResponsePageable getEnterpriseCreditItems(UUID parentCustomerPublicId, Pageable pageable);
}
