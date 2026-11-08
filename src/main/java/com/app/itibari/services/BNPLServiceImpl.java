package com.app.itibari.services;

import com.app.itibari.configuration.RoleCheckService;
import com.app.itibari.dataTypes.*;
import com.app.itibari.dataTypes.Module;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.AllCustomerPurchaseTrend;
import com.app.itibari.models.request.BNPLOnboarding;
import com.app.itibari.models.response.*;
import com.app.itibari.repository.*;
import com.app.itibari.services.helper.BnplHelper;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.helpers.Validators;
import com.app.itibari.utils.specification.CustomerSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 *
 * this bnpl is buy now pay later service implementation on progress,
 */


@Service
@Slf4j
@RequiredArgsConstructor
public class BNPLServiceImpl implements BNPLService {
    private final WalletRepository walletRepository;
    private final CustomersRepository customersRepository;
    private final BnplHelper bnplHelper;
    private final DocumentRuleRepository documentRuleRepository;
    private final FileDataRepository fileDataRepository;
    private final PaymentItemRepository paymentItemRepository;
    private final CreditLimitItemRepository creditLimitItemRepository;
    private final TransactionItemRepository transactionItemRepository;
    private final PersonRepository personRepository;
    private final RejectionCodeRepository rejectionCodeRepository;
    private final CustomerFeedBackRepository customerFeedBackRepository;
    private final CreditRuleRepository creditRuleRepository;
    private final CorporateCustomerBnplApprovalRepository corporateCustomerBnplApprovalRepository;
    private final SalesAgentsRepository salesAgentsRepository;
    private final RoleCheckService roleCheckService;
    private final EventsRepository eventsRepository;

    @Override
    public BNPLCustomerResponsePageable getEnterpriseCustomers(Pageable pageable) {
        List<PaymentStatusEnum> outstandingStatuses = List.of(
                PaymentStatusEnum.NOT_PAID,
                PaymentStatusEnum.PARTIALLY_PAID
        );

        Page<Customers> enterpriseCustomers = customersRepository
                .findEnterpriseCustomersWithOutstandingCreditItems(outstandingStatuses, pageable);
        List<Wallet> enterpriseWallets = enterpriseCustomers.getContent().stream()
                .map(walletRepository::findByCustomerAndDeletedIsFalse)
                .filter(Objects::nonNull)
                .toList();

        BNPLCustomerResponsePageable response = new BNPLCustomerResponsePageable();
        response.setTotalElements(enterpriseCustomers.getTotalElements());
        response.setPageSize(enterpriseCustomers.getSize());
        response.setTotalPages(enterpriseCustomers.getTotalPages());
        response.setLast(enterpriseCustomers.isLast());
        response.setFirst(enterpriseCustomers.isFirst());
        response.setPageNumber(enterpriseCustomers.getNumber());
        response.setContent(bnplHelper.convertToBNPLCustomerResponseDTO(enterpriseWallets));
        return response;
    }

    @Override
    public BNPLPaymentResponsePageable getEnterpriseCreditItems(UUID parentCustomerPublicId, Pageable pageable) {
        Customers parentCustomer = customersRepository.findByPublicIdAndDeletedIsFalse(parentCustomerPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Parent customer not found")
                        .userMessage("Parent customer not found"));

        BNPLPaymentResponsePageable response = new BNPLPaymentResponsePageable();
        Page<CreditLimitItem> creditLimitItems = creditLimitItemRepository
                .findEnterpriseCreditItemsByBatchStatus(parentCustomer, Batched.NOT_BATCHED, pageable);
        response.setTotalElements(creditLimitItems.getTotalElements());
        response.setPageSize(creditLimitItems.getSize());
        response.setTotalPages(creditLimitItems.getTotalPages());
        response.setLast(creditLimitItems.isLast());
        response.setFirst(creditLimitItems.isFirst());
        response.setPageNumber(creditLimitItems.getNumber());
        response.setContent(bnplHelper.convertToBNPLPaymentResponse(creditLimitItems.getContent()));
        return response;
    }

    @Override
    public BNPLCustomerResponsePageable getBNPLCustomers(String search, CreditStatus creditStatus, UUID customerPublicId, CustomerType customerType, UUID regionPublicId, Pageable pageable) {

        Page<BNPLCustomerResponseDTO> bnplCustomerPage = bnplHelper.getBNPLCustomers(search, creditStatus, customerPublicId, customerType, regionPublicId, pageable);
        return toBNPLCustomerResponsePageable(bnplCustomerPage);

    }

    @Override
    public BNPLCustomerResponsePageable getBNPLCustomersWithChildCustomers(String search, CreditStatus creditStatus, UUID customerPublicId, CustomerType customerType, UUID regionPublicId, Pageable pageable) {

        Page<BNPLCustomerResponseDTO> bnplCustomerPage = bnplHelper.getBNPLCustomersWithChildCustomers(search, creditStatus, customerPublicId, customerType, regionPublicId, pageable);
        return toBNPLCustomerResponsePageable(bnplCustomerPage);

    }

    private BNPLCustomerResponsePageable toBNPLCustomerResponsePageable(Page<BNPLCustomerResponseDTO> bnplCustomerPage) {
        BNPLCustomerResponsePageable response = new BNPLCustomerResponsePageable();
        response.setTotalElements(bnplCustomerPage.getTotalElements());
        response.setPageSize(bnplCustomerPage.getSize());
        response.setTotalPages(bnplCustomerPage.getTotalPages());
        response.setLast(bnplCustomerPage.isLast());
        response.setFirst(bnplCustomerPage.isFirst());
        response.setPageNumber(bnplCustomerPage.getNumber());
        response.setContent(bnplCustomerPage.getContent());
        return response;
    }

    @Override
    public List<CreditSalesDTO> getCreditSalesData() {
        return bnplHelper.generateCreditSalesPerRegion();
    }

    @Override
    public BNPLEligibleCustomerPageable getEligibleCustomersForBNPLAccountCreation(String search, UUID customerPublicId, CustomerType customerType, UUID regionPublicId, Pageable pageable) {
        Page<BNPLEligibleCustomerDTO> eligibleCustomersPage = bnplHelper.getEligibleCustomersForBNPLAccountCreation(search, customerPublicId, customerType, regionPublicId, pageable);
        BNPLEligibleCustomerPageable response = new BNPLEligibleCustomerPageable();
        response.setTotalElements(eligibleCustomersPage.getTotalElements());
        response.setPageSize(eligibleCustomersPage.getSize());
        response.setTotalPages(eligibleCustomersPage.getTotalPages());
        response.setLast(eligibleCustomersPage.isLast());
        response.setFirst(eligibleCustomersPage.isFirst());
        response.setPageNumber(eligibleCustomersPage.getNumber());
        response.setContent(eligibleCustomersPage.getContent());
        return response;
    }

    @Override
    public BNPLEligibleCustomerPageable getChurnedBNPLCustomers(String search, UUID customerPublicId, CustomerType customerType, UUID regionPublicId, List<UUID> locations, String loggedUser, String fromDate, String toDate, Pageable pageable) {

        Page<Customers> churnedCustomersPage = getFilteredCustomers(search, customerType, regionPublicId, locations, loggedUser, fromDate, toDate, pageable);

        Page<BNPLEligibleCustomerDTO> churnedCustomerDTOPage = bnplHelper.convertToEligibleCustomerDTO(churnedCustomersPage);

        BNPLEligibleCustomerPageable response = new BNPLEligibleCustomerPageable();
        response.setTotalElements(churnedCustomerDTOPage.getTotalElements());
        response.setPageSize(churnedCustomerDTOPage.getSize());
        response.setTotalPages(churnedCustomerDTOPage.getTotalPages());
        response.setLast(churnedCustomerDTOPage.isLast());
        response.setFirst(churnedCustomerDTOPage.isFirst());
        response.setPageNumber(churnedCustomerDTOPage.getNumber());
        response.setContent(churnedCustomerDTOPage.getContent());
        return response;
    }

    @Override
    public BNPLEligibleCustomerPageable getChurnedCreditCustomers(String search, UUID customerPublicId, CustomerType customerType, UUID regionPublicId, List<UUID> locations, String loggedUser, String fromDate, String toDate, Pageable pageable) {
        Page<Customers> churnedCreditCustomersPage = getFilteredChurnedCreditCustomers(
                search, customerPublicId, customerType, regionPublicId, locations, loggedUser, fromDate, toDate, pageable);
        Page<BNPLEligibleCustomerDTO> churnedCreditCustomerDTOPage = bnplHelper.convertToEligibleCustomerDTO(churnedCreditCustomersPage);

        BNPLEligibleCustomerPageable response = new BNPLEligibleCustomerPageable();
        response.setTotalElements(churnedCreditCustomerDTOPage.getTotalElements());
        response.setPageSize(churnedCreditCustomerDTOPage.getSize());
        response.setTotalPages(churnedCreditCustomerDTOPage.getTotalPages());
        response.setLast(churnedCreditCustomerDTOPage.isLast());
        response.setFirst(churnedCreditCustomerDTOPage.isFirst());
        response.setPageNumber(churnedCreditCustomerDTOPage.getNumber());
        response.setContent(churnedCreditCustomerDTOPage.getContent());
        return response;
    }


    @Override
    public void onboardCustomerToBNPL(BNPLOnboarding bnplOnboardingRequest, String name) {

        Customers customer = customersRepository.findByPublicIdAndDeletedIsFalse(bnplOnboardingRequest.getCustomerPublicId())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Customer not found")
                        .userMessage("Customer not found"));

//        if (customer.getLimitedOfferEligibility() == null || !customer.getLimitedOfferEligibility()) {
//            throw new BadRequestRestApiException()
//                    .developerMessage("Customer is not eligible for BNPL onboarding")
//                    .userMessage("Customer is not eligible for BNPL onboarding");
//        }

        if (customer.isSelfOnboarded() && customer.getBnplVerificationStatus() == CustomerBnplVerificationStatus.AWAITING_VERIFICATION) {
            OnBoardingData onBoardingData = customer.getOnBoardingData();
            bnplOnboardingRequest.setCustomerID(onBoardingData.getCustomerID());
            bnplOnboardingRequest.setTinNumber(onBoardingData.getTinNumber());
            bnplOnboardingRequest.setBusinessLicenseNumber(onBoardingData.getBusinessLicenseNumber());
            bnplOnboardingRequest.setGuarantorName(onBoardingData.getGuarantorName());
            bnplOnboardingRequest.setGuarantorPhoneNumber(onBoardingData.getGuarantorPhoneNumber());
        }

        if (Validators.anyEqualNull(bnplOnboardingRequest.getCustomerID(), bnplOnboardingRequest.getCustomerPublicId(),
                bnplOnboardingRequest.getTinNumber(), bnplOnboardingRequest.getBusinessLicenseNumber(),
                bnplOnboardingRequest.getGuarantorName(), bnplOnboardingRequest.getGuarantorPhoneNumber())) {

            throw new BadRequestRestApiException()
                    .developerMessage("One or more required fields are null")
                    .userMessage("Required fields cannot be null");
        }


        if (customer.isBnplAccountCreated()) {
            throw new BadRequestRestApiException()
                    .developerMessage("BNPL account already created for this customer")
                    .userMessage("BNPL account already exists for this customer");
        }

        // check if all required files are uploaded and approved.

        List<DocumentRule> requiredDocuments = documentRuleRepository.findDocumentRuleByDocumentTypeAndDocumentRequiredIsTrueAndDeletedIsFalse(DocumentTypes.KYC_DOCUMENT);
        for (DocumentRule docRule : requiredDocuments) {
            List<FileData> fileDataList = fileDataRepository.findAllByReferenceIdAndReferenceTypeAndDocumentRuleAndDeletedIsFalse(
                    customer.getPublicId().toString(),
                    ReferenceType.KYC_DOCUMENT.name(),
                    docRule);
            boolean documentExists = !fileDataList.isEmpty();
            boolean documentApproved = fileDataList.stream().anyMatch(FileData::isApproved);

            if (!documentExists) {
                throw new BadRequestRestApiException()
                        .developerMessage("Required document " + docRule.getDocumentName() + " is missing or not approved")
                        .userMessage("Please upload and get approval for the required document: " + docRule.getDocumentName());
            }

            if (!documentApproved) {
                throw new BadRequestRestApiException()
                        .developerMessage("Required document " + docRule.getDocumentName() + " is not approved")
                        .userMessage("Please get approval for the required document: " + docRule.getDocumentName());
            }
        }

        BigDecimal aov = resolveAovForOnboarding(customer);
        BigDecimal creditLimit = resolveCreditLimitForOnboarding(customer, aov);

        customer.setBnplAccountCreated(true);
        customer.setOnBoardingDate(LocalDate.now());
        customer.setPreliminaryValue(aov);
        customer.setCreditLimit(creditLimit);
        customer.setBnplVerificationStatus(CustomerBnplVerificationStatus.VERIFIED);
        Customers savedCustomer = customersRepository.save(customer);

        log.info("========Customer {} has been onboarded to BNPL successfully with credit limit {}====", customer.getName(), customer.getCreditLimit());

        bnplHelper.createWalletForCustomer(savedCustomer, name, aov, bnplOnboardingRequest, creditLimit);

    }

    private BigDecimal resolveAovForOnboarding(Customers customer) {
        BigDecimal recalculatedAov = Optional.ofNullable(bnplHelper.getAovForCustomer(customer)).orElse(BigDecimal.ZERO);
        BigDecimal existingAov = Optional.ofNullable(customer.getPreliminaryValue()).orElse(BigDecimal.ZERO);

        if (recalculatedAov.compareTo(BigDecimal.ZERO) > 0) {
            return recalculatedAov;
        }
        if (existingAov.compareTo(BigDecimal.ZERO) > 0) {
            return existingAov;
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal resolveCreditLimitForOnboarding(Customers customer, BigDecimal resolvedAov) {
        BigDecimal recalculatedCreditLimit = Optional.ofNullable(this.getCustomerCreditLimit(customer, resolvedAov)).orElse(BigDecimal.ZERO);
        BigDecimal existingCreditLimit = Optional.ofNullable(customer.getCreditLimit()).orElse(BigDecimal.ZERO);

        if (recalculatedCreditLimit.compareTo(BigDecimal.ZERO) > 0) {
            return recalculatedCreditLimit;
        }
        if (existingCreditLimit.compareTo(BigDecimal.ZERO) > 0) {
            return existingCreditLimit;
        }
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getCustomerCreditLimit(Customers customer, BigDecimal aov) {

        return bnplHelper.getCreditLimitForCustomer(customer.getCustomerType(), aov);
    }

    @Override
    public Page<WalletResponseDTO> getBNPLWallets(String search, CustomerType customerType, UUID customerPublicId, String startDate, String dueDate, CreditStatus creditStatus, Pageable pageable) {
        return bnplHelper.getBNPLWallets(search, customerType, customerPublicId, startDate, dueDate, creditStatus, pageable);
    }

    @Override
    public Page<PaymentItem> getBNPLWalletPayments(UUID walletId, Pageable pageable) {
        Wallet wallet = walletRepository.findByPublicIdAndDeletedIsFalse(walletId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Wallet not found")
                        .userMessage("Wallet not found"));

        return paymentItemRepository.findByWalletAndDeletedIsFalse(wallet, pageable);
    }

    @Override
    public Page<CreditLimitItem> getBNPLWalletCredits(UUID walletId, UUID parentCustomerPublicId, Pageable pageable) {
        Customers creditScopeCustomer;
        if (parentCustomerPublicId != null) {
            creditScopeCustomer = customersRepository.findByPublicIdAndDeletedIsFalse(parentCustomerPublicId)
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Parent customer not found")
                            .userMessage("Parent customer not found"));
        } else {
            Wallet wallet = walletRepository.findByPublicIdAndDeletedIsFalse(walletId)
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Wallet not found")
                            .userMessage("Wallet not found"));
            creditScopeCustomer = wallet.getCustomer();
        }

        return creditLimitItemRepository.findEnterpriseCreditItems(creditScopeCustomer, pageable);
    }

    @Override
    public BNPLCreditControlDTO getBNPLPaymentDashboardSummary() {
        return bnplHelper.generateBNPLPaymentDashboardSummary();
    }


    @Override
    public BNPLPaymentResponsePageable getBNPLCreditPaymentsData(String search, UUID customerPublicId, PaymentStatusEnum paymentStatus, CustomerType customerType, UUID regionPublicId, String startDate, String dueDate, UUID parentCustomerPublicId, Batched batched, Pageable pageable) {
        Page<BNPLPaymentResponse> bnplPaymentResponsePage = bnplHelper.getBNPLCreditPaymentsData(search, customerPublicId, paymentStatus, customerType, regionPublicId, startDate, dueDate, parentCustomerPublicId, batched, pageable);
        BNPLPaymentResponsePageable response = new BNPLPaymentResponsePageable();
        response.setTotalElements(bnplPaymentResponsePage.getTotalElements());
        response.setPageSize(bnplPaymentResponsePage.getSize());
        response.setTotalPages(bnplPaymentResponsePage.getTotalPages());
        response.setLast(bnplPaymentResponsePage.isLast());
        response.setFirst(bnplPaymentResponsePage.isFirst());
        response.setPageNumber(bnplPaymentResponsePage.getNumber());
        response.setContent(bnplPaymentResponsePage.getContent());
        return response;
    }

    @Override
    public List<CustomerPurchaseTrend> getCustomerPurchaseTrend(UUID customerPublicId, Integer year) {
        Customers customer = customersRepository.findByPublicIdAndDeletedIsFalse(customerPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Customer not found")
                        .userMessage("Customer not found"));

        if (year == null) {
            year = LocalDate.now().getYear();
        }

        return bnplHelper.generateCustomerPurchaseTrend(customer, year);
    }

    @Override
    public Page<TransactionItem> getBNPLWalletTransactionItems(UUID walletId, Pageable pageable) {
        Wallet wallet = walletRepository.findByPublicIdAndDeletedIsFalse(walletId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Wallet not found")
                        .userMessage("Wallet not found"));

        return transactionItemRepository.findTransactionItemsByWalletAndDeletedIsFalse(wallet, pageable);
    }

    @Override
    public void performActionsOnBNPLWallet(UUID customerPublicId, WalletAction action, UUID reasonPublicId, BigDecimal amount, String name) {
        Customers customer = customersRepository.findByPublicIdAndDeletedIsFalse(customerPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Customer not found")
                        .userMessage("Customer not found"));

        Wallet wallet = walletRepository.findByCustomerAndDeletedIsFalse(customer);
        if (wallet == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Wallet not found for customer")
                    .userMessage("Wallet not found for customer");
        }

        Person performedBy = personRepository.findByEmailAndDeletedIsFalse(name)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Person performing action not found")
                        .userMessage("Person performing action not found"));

        if (action == WalletAction.Blacklist) {

            if (reasonPublicId == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Reason for blacklisting is required")
                        .userMessage("Please provide a reason for blacklisting the wallet");
            }

            RejectionsCode reason = rejectionCodeRepository.findRejectionsCodeByPublicIdAndDeletedIsFalse(reasonPublicId)
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Reason code not found")
                            .userMessage("Reason code not found"));

            if (reason.getModule() != Module.BnplBlacklistReason) {
                throw new BadRequestRestApiException()
                        .developerMessage("Invalid reason code for blacklisting")
                        .userMessage("The provided reason code is not valid for blacklisting");
            }
            wallet.setCreditStatus(CreditStatus.Suspended);
            wallet.setBlacklistReason(reason);
        } else if (action == WalletAction.Whitelist) {
            wallet.setCreditStatus(CreditStatus.Healthy);

        } else if (action == WalletAction.RecalculateLimit) {
            if (amount == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Amount for credit limit recalculation is required")
                        .userMessage("Please provide an amount for credit limit recalculation");
            }

            if (wallet.getUnPaidAmount().compareTo(amount) > 0) {
                throw new BadRequestRestApiException()
                        .developerMessage("Amount for credit limit recalculation is out of range")
                        .userMessage("Amount for credit limit recalculation is out of range");
            }

            BigDecimal amountDue = wallet.getUnPaidAmount();
            BigDecimal newBalance = amount.subtract(amountDue);
            wallet.setCreditLimitBalance(newBalance);
            customer.setCreditLimit(amount);
            customersRepository.save(wallet.getCustomer());
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid wallet action")
                    .userMessage("Invalid wallet action");
        }
        wallet.setUpdatedBy(performedBy);
        wallet.setUpdateDate(LocalDateTime.now());
        walletRepository.save(wallet);
    }

    @Override
    public void makeDepositToBNPLWallet(UUID customerPublicId, BigDecimal amount, List<String> receipts, Boolean payViaWallet, String name) {
        Customers customer = customersRepository.findByPublicIdAndDeletedIsFalse(customerPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Customer not found")
                        .userMessage("Customer not found"));

        Wallet wallet = walletRepository.findByCustomerAndDeletedIsFalse(customer);
        if (wallet == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Wallet not found for customer")
                    .userMessage("Wallet not found for customer");
        }

        boolean hasReceipts = receipts != null && !receipts.isEmpty();

        bnplHelper.processDepositToWallet(wallet, amount, receipts, payViaWallet, hasReceipts, name);
    }

    @Override
    public void raiseCustomerFeedbackTicket(UUID customerPublicId, String issueDescription, UUID feedbackReasonPublicId, String name) {

        Customers customer = customersRepository.findByPublicIdAndDeletedIsFalse(customerPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Customer not found")
                        .userMessage("Customer not found"));

        Wallet wallet = walletRepository.findByCustomerAndDeletedIsFalse(customer);

        if (wallet == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Wallet not found for customer")
                    .userMessage("Wallet not found for customer, cannot raise feedback ticket");
        }

        Optional<RejectionsCode> feedbackReasonOpt = rejectionCodeRepository.findRejectionsCodeByPublicIdAndDeletedIsFalse(feedbackReasonPublicId);
        if (feedbackReasonOpt.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Feedback reason not found")
                    .userMessage("Feedback reason not found");
        }

        RejectionsCode feedbackReason = feedbackReasonOpt.get();

        CustomerFeedBack ticket = new CustomerFeedBack();
        ticket.setCustomer(customer);
        ticket.setFeedbackText(issueDescription);
        ticket.setRejectionCode(feedbackReason);
        ticket.setCreationTime(LocalDateTime.now());

        customerFeedBackRepository.save(ticket);

    }

    @Override
    public Page<CustomerFeedBack> getCustomerFeedbackTickets(UUID customerPublicId, Pageable pageable) {

        if (customerPublicId != null) {
            Customers customer = customersRepository.findByPublicIdAndDeletedIsFalse(customerPublicId)
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Customer not found")
                            .userMessage("Customer not found"));
            return customerFeedBackRepository.findByCustomerAndDeletedIsFalse(customer, pageable);
        }

        return customerFeedBackRepository.findAllByDeletedIsFalse(pageable);

    }

    @Override
    public void customerSelfOnBoard(BNPLOnboarding bnplOnboardingRequest, String name) {

        if (Validators.anyEqualNull(bnplOnboardingRequest.getCustomerID(), bnplOnboardingRequest.getCustomerPublicId(),
                bnplOnboardingRequest.getTinNumber(), bnplOnboardingRequest.getBusinessLicenseNumber(),
                bnplOnboardingRequest.getGuarantorName(), bnplOnboardingRequest.getGuarantorPhoneNumber())) {

            throw new BadRequestRestApiException()
                    .developerMessage("One or more required fields are null")
                    .userMessage("Required fields cannot be null");
        }

        Customers customer = customersRepository.findByPublicIdAndDeletedIsFalse(bnplOnboardingRequest.getCustomerPublicId())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Customer not found")
                        .userMessage("Customer not found"));

        if (customer.getLimitedOfferEligibility() == null || !customer.getLimitedOfferEligibility()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer is not eligible for BNPL onboarding")
                    .userMessage("Customer is not eligible for BNPL onboarding");
        }

        if (customer.isBnplAccountCreated()) {
            throw new BadRequestRestApiException()
                    .developerMessage("BNPL account already created for this customer")
                    .userMessage("BNPL account already exists for this customer");
        }


        // check if all required files are uploaded
        List<DocumentRule> requiredDocuments = documentRuleRepository.findDocumentRuleByDocumentTypeAndDocumentRequiredIsTrueAndDeletedIsFalse(DocumentTypes.KYC_DOCUMENT);
        for (DocumentRule docRule : requiredDocuments) {
            List<FileData> fileDataList = fileDataRepository.findAllByReferenceIdAndReferenceTypeAndDocumentRuleAndDeletedIsFalse(
                    customer.getPublicId().toString(),
                    ReferenceType.KYC_DOCUMENT.name(),
                    docRule);
            boolean documentExists = !fileDataList.isEmpty();

            if (!documentExists) {
                throw new BadRequestRestApiException()
                        .developerMessage("Required document " + docRule.getDocumentName() + " is missing or not approved")
                        .userMessage("Please upload and get approval for the required document: " + docRule.getDocumentName());
            }
        }


        customer.setSelfOnboarded(true);

        OnBoardingData onBoardingData = new OnBoardingData();
        onBoardingData.setTinNumber(bnplOnboardingRequest.getTinNumber());
        onBoardingData.setBusinessLicenseNumber(bnplOnboardingRequest.getBusinessLicenseNumber());
        onBoardingData.setGuarantorName(bnplOnboardingRequest.getGuarantorName());
        onBoardingData.setGuarantorPhoneNumber(bnplOnboardingRequest.getGuarantorPhoneNumber());
        onBoardingData.setCustomerID(bnplOnboardingRequest.getCustomerID());
        customer.setOnBoardingData(onBoardingData);
        customer.setBnplVerificationStatus(CustomerBnplVerificationStatus.AWAITING_VERIFICATION);

        customersRepository.save(customer);

    }

    @Override
    public CreditLimitResponse getCustomerCreditLimitForCustomer(UUID customerId, String name) {
        Customers customer = customersRepository.findByPublicIdAndDeletedIsFalse(customerId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Customer not found")
                        .userMessage("Customer not found"));

        Customers creditOwner = resolveCreditLimitOwner(customer);
        List<String> scopeReferenceIds = resolveCreditScopeReferenceIds(creditOwner);

        Optional<CreditRule> optionalCreditRuleInterestRate = creditRuleRepository.findCreditRuleByCustomerTypeAndInterestRateAndDeletedIsFalse(
                creditOwner.getCustomerType(),
                true
        );

        Optional<CreditRule> optionalCreditRuleRepaymentPeriod = creditRuleRepository.findCreditRuleByCustomerTypeAndRepaymentTermAndDeletedIsFalse(
                creditOwner.getCustomerType(),
                true
        );

        CreditLimitResponse response;


        if (creditOwner.isBnplAccountCreated()) {
            Wallet wallet = walletRepository.findByCustomerAndDeletedIsFalse(creditOwner);
            if (wallet == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Wallet not found for customer")
                        .userMessage("Wallet not found for customer");
            }
            List<CreditLimitItem> creditLimitItems = creditLimitItemRepository.findAllByReferenceIdInAndDeletedIsFalse(scopeReferenceIds)
                    .stream()
                    .filter(item -> item.getStatus() == WalletItemStatus.ACTIVE)
                    .filter(item -> item.getPaymentStatus() != PaymentStatusEnum.PAID)
                    .filter(item -> !item.isFullyPaid())
                    .filter(item -> !item.isCancelled())
                    .toList();
            boolean hasActiveCreditLimit = !creditLimitItems.isEmpty();

            List<CreditLimitItem> overduePayments = creditLimitItems.stream()
                    .filter(item -> item.getPaymentStatus() != PaymentStatusEnum.PAID)
                    .filter(item -> item.getDueDate() != null && item.getDueDate().isBefore(LocalDate.now()))
                    .toList();


            response = new CreditLimitResponse();
            response.setHasCreditLimit(true);
            response.setCreditLimit(creditOwner.getCreditLimit());
            response.setHasActiveOrder(hasActiveCreditLimit);
            response.setHasOverduePayment(!overduePayments.isEmpty());
            response.setCreditInterestRate(optionalCreditRuleInterestRate.isPresent() ? optionalCreditRuleInterestRate.get().getInterestRatePercentage() : BigDecimal.ZERO);
            response.setRepaymentTermInDays(optionalCreditRuleRepaymentPeriod.isPresent() ? optionalCreditRuleRepaymentPeriod.get().getRepaymentTermDays() : 0);
            response.setOnBoardedToBNPL(true);
        } else {

            BigDecimal aov = bnplHelper.getAovForCustomer(creditOwner);
            BigDecimal creditLimit = this.getCustomerCreditLimit(creditOwner, aov);

            response = new CreditLimitResponse();
            response.setHasCreditLimit(true);
            response.setCreditLimit(creditLimit);
            response.setHasActiveOrder(false);
            response.setHasOverduePayment(false);
            response.setCreditInterestRate(optionalCreditRuleInterestRate.isPresent() ? optionalCreditRuleInterestRate.get().getInterestRatePercentage() : BigDecimal.ZERO);
            response.setRepaymentTermInDays(optionalCreditRuleRepaymentPeriod.isPresent() ? optionalCreditRuleRepaymentPeriod.get().getRepaymentTermDays() : 0);
            response.setOnBoardedToBNPL(false);
        }

        return response;
    }

    @Override
    public CreditStatusDashboardDTO getCreditStatusDashboard() {
        return bnplHelper.generateCreditStatusDashboard();
    }

    @Override
    public Page<BNPLCustomerResponseDTO> getBNPLCustomerTop() {
        Page<Wallet> topWallets = walletRepository.findAllByDeletedIsFalse(Pageable.unpaged());
        List<Wallet> sortedWallets = topWallets.getContent().stream()
                .sorted((w1, w2) -> w2.getTotalCreditAmount().compareTo(w1.getTotalCreditAmount()))
                .limit(5)
                .toList();
        return bnplHelper.convertToBNPLCustomerResponseDTOs(sortedWallets);
    }

    @Override
    public Page<CreditLimitItem> getCustomerUnPaidCreditItems(String name, Pageable pageable) {
        Optional<Customers> optionalCustomer = customersRepository.findByPhoneNumberAndDeletedIsFalse(name);
        if (optionalCustomer.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer not found")
                    .userMessage("Customer not found");
        }

        Customers customer = optionalCustomer.get();
        log.info("Fetching unpaid credit items for customer: {}", customer.getName());
        Wallet wallet = walletRepository.findByCustomerAndDeletedIsFalse(customer);
        if (wallet != null) {
            log.info("Wallet found for customer: {}", customer.getName());
            List<PaymentStatusEnum> unpaidStatuses = List.of(PaymentStatusEnum.UN_PAID, PaymentStatusEnum.PARTIALLY_PAID);
            List<CreditLimitItem> items = creditLimitItemRepository.findAllByReferenceIdAndDeletedIsFalse(
                    customer.getPublicId().toString()
            );
            items = items.stream()
                    .filter(CreditLimitItem::isOrderDelivered)
                    .filter(item -> item.getPaymentStatus() != PaymentStatusEnum.PAID)
                    .toList();
            log.info("Found {} unpaid credit items for customer: {}", items.size(), customer.getName());
            return new PageImpl<>(items, pageable, items.size());
        } else {
            log.info("No wallet found for customer: {}", customer.getName());
            return Page.empty(pageable);
        }
    }

    @Override
    public Page<TransactionItem> getCustomerPaymentTransactionsHistory(String name, Pageable pageable) {
        Optional<Customers> optionalCustomer = customersRepository.findByPhoneNumberAndDeletedIsFalse(name);
        if (optionalCustomer.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer not found")
                    .userMessage("Customer not found");
        }

        Customers customer = optionalCustomer.get();
        Wallet wallet = walletRepository.findByCustomerAndDeletedIsFalse(customer);
        if (wallet != null) {
            return transactionItemRepository.findTransactionItemsByWalletAndTransactionTypeAndDeletedIsFalse(
                    wallet,
                    TransactionType.DEPOSIT,
                    pageable);
        } else {
            return Page.empty(pageable);
        }
    }

    @Override
    public List<AllCustomerPurchaseTrend> getCustomerPurchaseTrends(Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }

        return bnplHelper.generateCustomerPurchaseTrends(year);
    }

    @Override
    public BnplLoanManagementPageable getBNPLLoanManagementData(String search, UUID customerPublicId, String startDate, String dueDate, PaymentStatusEnum paymentStatus, UUID parentCustomerPublicId, Pageable pageable) {
        Page<BnplLoanManagement> loanManagementPage = bnplHelper.getBNPLLoanManagementData(search, customerPublicId, startDate, dueDate, paymentStatus, parentCustomerPublicId, pageable);
        BnplLoanManagementPageable response = new BnplLoanManagementPageable();
        response.setTotalElements(loanManagementPage.getTotalElements());
        response.setPageSize(loanManagementPage.getSize());
        response.setTotalPages(loanManagementPage.getTotalPages());
        response.setLast(loanManagementPage.isLast());
        response.setFirst(loanManagementPage.isFirst());
        response.setPageNumber(loanManagementPage.getNumber());
        response.setContent(loanManagementPage.getContent());
        return response;
    }

    private Customers resolveCreditLimitOwner(Customers customer) {
        if (customer != null && customer.getParentCustomer() != null) {
            return customer.getParentCustomer();
        }
        return customer;
    }

    private List<String> resolveCreditScopeReferenceIds(Customers creditOwner) {
        List<String> referenceIds = new java.util.ArrayList<>();
        referenceIds.add(creditOwner.getPublicId().toString());

        List<Customers> childCustomers = customersRepository.findAllByParentCustomerAndDeletedIsFalse(creditOwner);
        childCustomers.stream()
                .map(Customers::getPublicId)
                .filter(java.util.Objects::nonNull)
                .map(UUID::toString)
                .forEach(referenceIds::add);

        return referenceIds;
    }

    @Override
    public void sendDuePaymentReminders(UUID creditPublicId, String name) {
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person performing action not found")
                    .userMessage("Person performing action not found");
        }
        bnplHelper.processDuePaymentReminder(creditPublicId, optionalPerson.get());
    }

    @Override
    public void customerCreditPay(UUID referenceId, String orderNumber, Boolean payViaWallet, Boolean hasFile, String name) {
        Customers customer = customersRepository.findByPublicIdAndDeletedIsFalse(referenceId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Customer not found")
                        .userMessage("Customer not found"));

        Wallet wallet = walletRepository.findByCustomerAndDeletedIsFalse(customer);
        if (wallet == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Wallet not found for customer")
                    .userMessage("Wallet not found for customer");
        }

        Wallet fundingWallet = resolveFundingWallet(wallet);

        if (fundingWallet.getWalletBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestRestApiException()
                    .developerMessage("Insufficient wallet balance")
                    .userMessage("Your wallet balance is insufficient to make this payment");
        }
        bnplHelper.processCustomerCreditPayment(wallet, orderNumber, payViaWallet, hasFile, name);
    }

    @Override
    public Page<CorporateCustomerBnplApproval> getCorporateCustomerBnplApproval(String searchParam, CorporateApprovalStatus corporateApprovalStatus, String name, Pageable pageable) {
        return bnplHelper.getCorporateCustomerBnplApproval(searchParam, corporateApprovalStatus, pageable);
    }

    @Override
    public void actionOnCorporateCustomerBnplApproval(UUID publicId, ApprovalAction approvalAction, String name) {
        Optional<CorporateCustomerBnplApproval> optionalCorporateCustomerBnplApproval = corporateCustomerBnplApprovalRepository.findCorporateCustomerBnplApprovalByPublicIdAndDeletedIsFalse(publicId);
        if (optionalCorporateCustomerBnplApproval.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Corporate customer bnpl approval not found")
                    .userMessage("Corporate customer bnpl approval not found");
        }
        CorporateCustomerBnplApproval corporateCustomerBnplApproval = optionalCorporateCustomerBnplApproval.get();

        if (corporateCustomerBnplApproval.getApprovalStatus() != CorporateApprovalStatus.PENDING_APPROVAL) {
            throw new BadRequestRestApiException()
                    .developerMessage("Approval request already actioned on")
                    .userMessage("Approval request already actioned on");
        }

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person performing action not found")
                    .userMessage("Person performing action not found");
        }
        Person person = optionalPerson.get();

        boolean approve = approvalAction == ApprovalAction.APPROVE;
        corporateCustomerBnplApproval.setApprovalStatus(approve ? CorporateApprovalStatus.APPROVED : CorporateApprovalStatus.REJECTED);
        corporateCustomerBnplApproval.setActionedOn(LocalDate.now());
        corporateCustomerBnplApproval.setActionedBy(person.getFullName());
        corporateCustomerBnplApproval.setUpdatedBy(person);
        corporateCustomerBnplApproval.setUpdateDate(LocalDateTime.now());

        CorporateCustomerBnplApproval saved = corporateCustomerBnplApprovalRepository.save(corporateCustomerBnplApproval);


        // then create wallet for customer

        if (approve) {
            bnplHelper.createCorporateWallet(saved.getCustomer(), saved.isChangeToRegular());
            customersRepository.save(saved.getCustomer());

        } else {
            // if rejected, send notification to customer about rejection
            saved.getCustomer().setAwaitingCustomerTypeChange(false);
            customersRepository.save(saved.getCustomer());


            //  bnplHelper.sendCorporateApprovalRejectionNotification(saved.getCustomer());
        }

    }

    @Override
    public void recalculateCreditLimitForCustomer(UUID customerPublicId, String name) {
        Customers customer = customersRepository.findByPublicIdAndDeletedIsFalse(customerPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Customer not found")
                        .userMessage("Customer not found"));

        BigDecimal aov = resolveAovForOnboarding(customer);
        BigDecimal newCreditLimit = resolveCreditLimitForOnboarding(customer, aov);

        customer.setCreditLimit(newCreditLimit);
        customer.setPreliminaryValue(aov);
        customersRepository.save(customer);


        Wallet wallet = walletRepository.findByCustomerAndDeletedIsFalse(customer);
        if (wallet != null) {
            wallet.setCreditLimitBalance(newCreditLimit.subtract(wallet.getUnPaidAmount()));
            wallet.setUpdatedBy(personRepository.findByEmailAndDeletedIsFalse(name).orElse(null));
            wallet.setUpdateDate(LocalDateTime.now());
            walletRepository.save(wallet);
        }

    }

    @Override
    public void reverseCreditPayment(UUID creditLimitPublicId, UUID reasonPublicId, String name) {
        Optional<CreditLimitItem> optionalCreditLimitItem = creditLimitItemRepository.findByPublicIdAndDeletedIsFalse(creditLimitPublicId);
        if (optionalCreditLimitItem.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Credit limit item not found")
                    .userMessage("Credit limit item not found");
        }
        CreditLimitItem creditLimitItem = optionalCreditLimitItem.get();


        RejectionsCode reason = rejectionCodeRepository.findRejectionsCodeByPublicIdAndDeletedIsFalse(reasonPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Reason code not found")
                        .userMessage("Reason code not found"));

        if (reason.getModule() != Module.BnplPaymentReversalReason) {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid reason code for payment reversal")
                    .userMessage("The provided reason code is not valid for payment reversal");
        }

        bnplHelper.processCreditPaymentReversal(creditLimitItem, reason, name);
    }

    @Override
    public void payCreditFromWallet(UUID creditLimitPublicId, BigDecimal amount, String name) {


        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person not found")
                    .userMessage("Sorry! Person not found");
        }

        Optional<CreditLimitItem> optionalCreditLimitItem = creditLimitItemRepository.findByPublicIdAndDeletedIsFalse(creditLimitPublicId);
        if (optionalCreditLimitItem.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Credit limit item not found")
                    .userMessage("Credit limit item not found");
        }
        CreditLimitItem creditLimitItem = optionalCreditLimitItem.get();

        if (creditLimitItem.getPaymentStatus() == PaymentStatusEnum.PAID) {
            throw new BadRequestRestApiException()
                    .developerMessage("Credit limit item is already paid")
                    .userMessage("This credit limit item is already paid");
        }

        Wallet wallet = resolveFundingWallet(creditLimitItem.getWallet());
        if (wallet.getWalletBalance().compareTo(amount) < 0) {
            throw new BadRequestRestApiException()
                    .developerMessage("Insufficient wallet balance")
                    .userMessage("Your wallet balance is insufficient to make this payment");
        }

        if (amount.compareTo(wallet.getWalletBalance()) > 0) {
            throw new BadRequestRestApiException()
                    .developerMessage("Insufficient wallet balance")
                    .userMessage("Your wallet balance is insufficient to make this payment");
        }

        // if unpaid amount is less than the amount trying to pay, throw error
        if (creditLimitItem.getBalanceAmount().compareTo(amount) < 0) {
            throw new BadRequestRestApiException()
                    .developerMessage("Payment amount exceeds unpaid amount")
                    .userMessage("The amount you are trying to pay exceeds the unpaid amount for this credit item");
        }

        bnplHelper.processCreditPaymentFromWallet(creditLimitItem, amount, optionalPerson.get());
    }

    private Page<Customers> getFilteredCustomers(String searchParam, CustomerType customerType, UUID regionPublicId, List<UUID> locations, String agentName, String fromDate, String toDate, Pageable pageable) {

        Boolean isFromSA = checkRequestIsFromSalesAgent();

        SalesAgents salesAgents = null;
        if (isFromSA) {

            log.info("==========Searching for customers from SA==========");

            Optional<SalesAgents> salesAgentsOptional = salesAgentsRepository.findByPhoneNumberAndDeletedIsFalse(agentName);
            if (salesAgentsOptional.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Sales agent not found")
                        .userMessage("Sales agent not found");
            }

            salesAgents = salesAgentsOptional.get();

            List<OrganizationLocations> organizationLocations = salesAgents.getOrganizationLocations();

            locations = organizationLocations.stream()
                    .map(OrganizationLocations::getPublicId).distinct().collect(Collectors.toList());

            log.info("=====Sales agent {} has access to locations: {}=======", salesAgents.getName(), locations);
        }


        LocalDate startDate = null;

        LocalDate endDate = null;

        if (fromDate != null) {
            try {
                startDate = LocalDate.parse(fromDate);
            } catch (DateTimeParseException e) {
                throw new BadRequestRestApiException()
                        .developerMessage("Invalid from date format, expected format is YYYY-MM-DD")
                        .userMessage("Invalid from date format, expected format is YYYY-MM-DD");
            }
        }

        if (toDate != null) {
            try {
                endDate = LocalDate.parse(toDate);
            } catch (DateTimeParseException e) {
                throw new BadRequestRestApiException()
                        .developerMessage("Invalid to date format, expected format is YYYY-MM-DD")
                        .userMessage("Invalid to date format, expected format is YYYY-MM-DD");
            }
        }

        Specification<Customers> spec = Specification
                .where(CustomerSpecification.searchParamMatches(searchParam))
                .and(CustomerSpecification.isNotDeleted())
                .and(CustomerSpecification.hasServedBy(salesAgents))
                .and(CustomerSpecification.hasOrganizationRegion(regionPublicId))
                .and(CustomerSpecification.hasCustomerType(customerType))
                .and(CustomerSpecification.hasCustomerInactive(CustomerActiveStatus.Inactive))
                .and(CustomerSpecification.hasOrganizationLocations(locations))
                .and(CustomerSpecification.registrationDateBetween(startDate, endDate));


        return customersRepository.findAll(spec, pageable);
    }

    private Page<Customers> getFilteredChurnedCreditCustomers(String searchParam, UUID customerPublicId, CustomerType customerType, UUID regionPublicId, List<UUID> locations, String agentName, String fromDate, String toDate, Pageable pageable) {
        SalesAgents salesAgent = null;
        if (checkRequestIsFromSalesAgent()) {
            salesAgent = salesAgentsRepository.findByPhoneNumberAndDeletedIsFalse(agentName)
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Sales agent not found")
                            .userMessage("Sales agent not found"));

            locations = salesAgent.getOrganizationLocations().stream()
                    .map(OrganizationLocations::getPublicId)
                    .distinct()
                    .collect(Collectors.toList());
        }

        LocalDate startDate = parseDate(fromDate, "from");
        LocalDate endDate = parseDate(toDate, "to");

        Specification<Customers> spec = Specification
                .where(CustomerSpecification.searchParamMatches(searchParam))
                .and(CustomerSpecification.isNotDeleted())
                .and(CustomerSpecification.hasPublicId(customerPublicId))
                .and(CustomerSpecification.hasServedBy(salesAgent))
                .and(CustomerSpecification.hasOrganizationRegion(regionPublicId))
                .and(CustomerSpecification.hasCustomerType(customerType))
                .and(CustomerSpecification.hasOrganizationLocations(locations))
                .and(CustomerSpecification.registrationDateBetween(startDate, endDate))
                .and(CustomerSpecification.hasEverPlacedCreditOrderButNoRecentCreditOrder(LocalDateTime.now().minusDays(30)));

        return customersRepository.findAll(spec, pageable);
    }

    private LocalDate parseDate(String date, String parameterName) {
        if (date == null) {
            return null;
        }

        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid " + parameterName + " date format, expected format is YYYY-MM-DD")
                    .userMessage("Invalid " + parameterName + " date format, expected format is YYYY-MM-DD");
        }
    }

    @Override
    public CreditLimitUtilizationResponse getCreditLimitUtilization(UUID parentCustomerPublicId) {

        Customers parentCustomer = customersRepository.findByPublicIdAndDeletedIsFalse(parentCustomerPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Parent customer not found")
                        .userMessage("Parent customer not found"));

        Wallet wallet = walletRepository.findByCustomerAndDeletedIsFalse(parentCustomer);
        if (wallet == null) {
            return new CreditLimitUtilizationResponse();
        }


        BigDecimal totalCreditLimit = parentCustomer.getCreditLimit();
        List<String> scopeReferenceIds = resolveCreditScopeReferenceIds(parentCustomer);

        BigDecimal utilizedCreditLimit = creditLimitItemRepository.findAllByReferenceIdInAndDeletedIsFalse(scopeReferenceIds)
                .stream()
                .filter(item -> item.getStatus() == WalletItemStatus.ACTIVE)
                .filter(item -> !item.isCancelled())
                .filter(item -> item.getPaymentStatus() == PaymentStatusEnum.NOT_PAID || item.getPaymentStatus() == PaymentStatusEnum.PARTIALLY_PAID)
                .map(CreditLimitItem::getBalanceAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal availableCreditLimit = totalCreditLimit.subtract(utilizedCreditLimit);

        CreditLimitUtilizationResponse response = new CreditLimitUtilizationResponse();
        response.setCreditLimit(totalCreditLimit);
        response.setUtilizedAmount(utilizedCreditLimit);
        response.setAvailableCreditLimit(availableCreditLimit);
        if (totalCreditLimit.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal utilizationPercentage = utilizedCreditLimit.multiply(new BigDecimal("100")).divide(totalCreditLimit, 2, RoundingMode.HALF_UP);
            response.setUtilizationPercentage(utilizationPercentage);
        } else {
            response.setUtilizationPercentage(BigDecimal.ZERO);
        }

        return response;
    }

    @Override
    public List<PaymentItem> getPaymentItems(String orderNumber) {
        return paymentItemRepository.findByOrderNumberIgnoreCaseAndPaymentMethodIgnoreCaseAndDeletedIsFalse(orderNumber, "BNPL Wallet Deposit from Wallet");
    }

    private Boolean checkRequestIsFromSalesAgent() {
        String role = "ROLE_SALES_AGENT";

        return roleCheckService.hasOneRole(role);

    }

    private Wallet resolveFundingWallet(Wallet wallet) {
        if (wallet != null && wallet.getParentWallet() != null) {
            return wallet.getParentWallet();
        }
        return wallet;
    }
}
