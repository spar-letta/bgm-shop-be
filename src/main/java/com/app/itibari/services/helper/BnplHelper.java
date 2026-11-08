package com.app.itibari.services.helper;

import com.app.itibari.dataTypes.*;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.AllCustomerPurchaseTrend;
import com.app.itibari.models.request.BNPLOnboarding;
import com.app.itibari.models.response.*;
import com.app.itibari.repository.*;
import com.app.itibari.services.CounterService;
import com.app.itibari.services.ItibariSmsService;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.helpers.CreditReconHelper;
import com.app.itibari.utils.helpers.WebhookClient;
import com.app.itibari.utils.specification.CustomerSpecification;
import io.micrometer.common.util.StringUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BnplHelper {
    private final static String webhookUrl = "https://chat.googleapis.com/v1/spaces/AAQAADUTK_M/messages?key=AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI&token=dWQOlFNND7YtdZi8PIG4btqydQ94m8r8d-H3_iJY-v0";
    private final OrganizationRegionsRepository organizationRegionsRepository;
    private final WalletRepository walletRepository;
    private final SalesOrdersRepository salesOrdersRepository;
    private final CreditRuleRepository creditRuleRepository;
    private final PersonRepository personRepository;
    private final CreditLimitItemRepository creditLimitItemRepository;
    private final TransactionItemRepository transactionItemRepository;
    private final PaymentItemRepository paymentItemRepository;
    private final CounterService counterService;
    private final CustomersRepository customersRepository;
    private final FileDataRepository fileDataRepository;
    private final WebhookClient webhookClient;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final TumaSmsRepository tumaSmsRepository;

    @PersistenceContext
    private final EntityManager entityManager;
    private final ItibariSmsService itibariSmsService;
    private final CreditReconHelper creditReconHelper;
    private final CorporateCustomerBnplApprovalRepository corporateCustomerBnplApprovalRepository;
    private final NotificationHelper notificationHelper;
    private final EventsRepository eventsRepository;
    private final CreditReconRepository creditReconRepository;


    public List<CreditSalesDTO> generateCreditSalesPerRegion() {
        // group wallets per region and sum the credit sales, sort from large to small

        Map<OrganizationRegions, List<Wallet>> wallets = new HashMap<>();

        List<OrganizationRegions> regions = organizationRegionsRepository.findByDeletedIsFalse(Pageable.unpaged()).getContent();

        List<Wallet> allWallets = walletRepository.findAllActiveWalletsNative(Pageable.unpaged()).getContent();

        for (OrganizationRegions region : regions) {
            List<Wallet> walletsInRegion = allWallets.stream()
                    .filter(wallet -> wallet.getCustomer().getOrganizationRegions() != null)
                    .filter(wallet -> wallet.getCustomer().getOrganizationRegions().getPublicId().equals(region.getPublicId()))
                    .toList();
            wallets.put(region, walletsInRegion);
        }

        return wallets.entrySet().stream()
                .map(entry -> {
                    OrganizationRegions region = entry.getKey();
                    List<Wallet> walletsInRegion = entry.getValue();
                    BigDecimal totalCreditSales = walletsInRegion.stream()
                            .map(Wallet::getTotalCreditAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    CreditSalesDTO dto = new CreditSalesDTO();
                    dto.setRegion(region.getName());
                    dto.setCreditSalesAmount(totalCreditSales);
                    dto.setPublicId(region.getPublicId());
                    return dto;
                })
                .sorted((dto1, dto2) -> dto2.getCreditSalesAmount().compareTo(dto1.getCreditSalesAmount()))
                .toList();
    }

    public Page<BNPLEligibleCustomerDTO> convertToEligibleCustomerDTO(Page<Customers> customersPage) {
        List<BNPLEligibleCustomerDTO> dtos = new ArrayList<>();

        // Get all customers from the page
        List<Customers> customers = customersPage.getContent();

        // Fetch order statistics for all customers in one query
        Map<Long, CustomerOrderStatsDTO> orderStatsMap = getOrderStatsForCustomers(customers);
        Map<Long, LocalDateTime> latestCreditActivityDates = getLatestCreditActivityDatesForCustomers(customers);

        // Convert each customer to DTO using the pre-fetched stats
        for (Customers customer : customers) {
            BNPLEligibleCustomerDTO dto = convertSingleCustomerToDTO(customer, orderStatsMap, latestCreditActivityDates);
            dtos.add(dto);
        }

        return new PageImpl<>(dtos, customersPage.getPageable(), customersPage.getTotalElements());
    }

    private BNPLEligibleCustomerDTO convertSingleCustomerToDTO(Customers customer,
                                                               Map<Long, CustomerOrderStatsDTO> orderStatsMap,
                                                               Map<Long, LocalDateTime> latestCreditActivityDates) {
        BNPLEligibleCustomerDTO dto = new BNPLEligibleCustomerDTO();

        LocalDateTime creationTime = latestCreditActivityDates.get(customer.getId());

        CustomerOrderStatsDTO stats = orderStatsMap.getOrDefault(
                customer.getId(),
                new CustomerOrderStatsDTO(customer.getId(), 0L, 0L, BigDecimal.ZERO)
        );

        // Basic customer information
        BigDecimal aov = calculateAov(stats.gmv(), stats.totalOrders());
        BigDecimal creditLimit = getCreditLimitForCustomer(customer.getCustomerType(), aov);
        String dateString = formatter.format(customer.getCreationTime());

        dto.setAov(aov);
        dto.setCreditLimit(creditLimit);
        dto.setPublicId(customer.getPublicId());
        dto.setCustomerName(customer.getName());
        dto.setCustomerPhoneNumber(customer.getPhoneNumber());
        dto.setCustomerType(customer.getCustomerType());
        dto.setCashPurchaser(true);
        dto.setCustomerCreationDate(dateString);
        dto.setBnplOnboarded(customer.isBnplAccountCreated());
        dto.setLastActivityDate(creationTime != null ? formatter.format(creationTime) : null);
        dto.setPreviousOrderDate(customer.getPreviousOrderDate());

        // Handle self-onboarded data
        if (customer.isSelfOnboarded() && customer.getOnBoardingData() != null) {
            OnBoardingData onBoardingData = customer.getOnBoardingData();
            dto.setSelfOnboarded(true);
            dto.setCustomerID(onBoardingData.getCustomerID());
            dto.setTinNumber(onBoardingData.getTinNumber());
            dto.setBusinessLicenseNumber(onBoardingData.getBusinessLicenseNumber());
            dto.setGuarantorName(onBoardingData.getGuarantorName());
            dto.setGuarantorPhoneNumber(onBoardingData.getGuarantorPhoneNumber());
        }

        // Set order statistics from pre-fetched map
        dto.setDaysSinceLastOrder(stats.daysSinceLastOrder());
        dto.setTotalOrders(stats.totalOrders());
        dto.setGmv(stats.gmv());

        return dto;
    }

    public Map<Long, CustomerOrderStatsDTO> getOrderStatsForCustomers(List<Customers> customers) {
        if (customers == null || customers.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> customerIds = customers.stream()
                .map(Customers::getId)
                .collect(Collectors.toList());

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
        Root<SalesOrders> root = cq.from(SalesOrders.class);

        // Filter by customer IDs
        cq.where(
                root.get("customer").get("id").in(customerIds),
                cb.isFalse(root.get("deleted"))
        );

        // Fix: Properly type the Path for greatest function
        Path<LocalDateTime> creationTimePath = root.get("creationTime");
        Expression<LocalDateTime> maxCreationTime = cb.greatest(creationTimePath);

        // Fix: Properly type the count and sum expressions
        Expression<Long> orderCount = cb.count(root);
        Expression<BigDecimal> gmv = cb.sum(root.get("totalAmount"));

        cq.multiselect(
                root.get("customer").get("id"),
                maxCreationTime,
                orderCount,
                gmv
        );
        cq.groupBy(root.get("customer").get("id"));

        // Execute query
        List<Object[]> results = entityManager.createQuery(cq).getResultList();

        // Build map of customer ID to order stats
        Map<Long, CustomerOrderStatsDTO> statsMap = new HashMap<>();

        for (Object[] result : results) {
            Long customerId = (Long) result[0];
            LocalDateTime lastOrderDate = (LocalDateTime) result[1];
            Long totalOrders = (Long) result[2];
            BigDecimal totalGmv = (BigDecimal) result[3];

            long daysSinceLastOrder = calculateDaysSinceLastOrder(lastOrderDate);

            CustomerOrderStatsDTO stats = new CustomerOrderStatsDTO(
                    customerId,
                    daysSinceLastOrder,
                    totalOrders,
                    totalGmv
            );

            statsMap.put(customerId, stats);
        }

        return statsMap;
    }

    private Map<Long, LocalDateTime> getLatestCreditActivityDatesForCustomers(List<Customers> customers) {
        if (customers == null || customers.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> customerIds = customers.stream()
                .map(Customers::getId)
                .collect(Collectors.toList());

        return creditLimitItemRepository.findLatestCreationTimesByCustomerIds(customerIds).stream()
                .collect(Collectors.toMap(
                        result -> (Long) result[0],
                        result -> (LocalDateTime) result[1]
                ));
    }

    private BigDecimal calculateAov(BigDecimal totalOrderAmount, Long totalOrders) {
        if (totalOrderAmount == null || totalOrders == null || totalOrders == 0) {
            return BigDecimal.ZERO;
        }

        return totalOrderAmount.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);
    }

    private long calculateDaysSinceLastOrder(LocalDateTime lastOrderDate) {
        if (lastOrderDate == null) {
            return 0L;
        }
        return ChronoUnit.DAYS.between(lastOrderDate, LocalDateTime.now());
    }

    public BigDecimal getCreditLimitForCustomer(CustomerType customerType, BigDecimal aov) {
        Optional<CreditRule> optionalCreditRule = creditRuleRepository.findCreditRuleByCustomerTypeAndCreditLimitAndDeletedIsFalse(customerType, true);

        if (optionalCreditRule.isEmpty()) {
            return BigDecimal.ZERO;
        }
        CreditRule creditRule = optionalCreditRule.get();
        BigDecimal creditLimitPercentage = creditRule.getCreditLimitPercentage();
        return aov.multiply(creditLimitPercentage).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
    }

    public BigDecimal getAovForCustomer(Customers customer) {
        if (customer == null || customer.getPublicId() == null) {
            return BigDecimal.ZERO;
        }
        if (salesOrdersRepository.customerQualifiesForAOV(customer.getPublicId())) {
            return salesOrdersRepository.calculateAverageOrderValue(customer.getPublicId());
        }
        return BigDecimal.ZERO;
    }

    public void createWalletForCustomer(Customers savedCustomer, String name, BigDecimal aov, BNPLOnboarding bnplOnboardingRequest, BigDecimal creditLimit) {

        log.info("==========Creating wallet for customer: {}==========", savedCustomer.getName());
        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);

        Wallet wallet = new Wallet();
        wallet.setCustomer(savedCustomer);
        personOptional.ifPresent(wallet::setCreatedBy);
        wallet.setCreationTime(LocalDateTime.now());
        wallet.setCreditStatus(CreditStatus.Healthy);
        wallet.setCreditLimitBalance(creditLimit);
        wallet.setAverageOrderValue(aov);
        wallet.setCustomerID(bnplOnboardingRequest.getCustomerID());
        wallet.setTinNumber(bnplOnboardingRequest.getTinNumber());
        wallet.setBusinessLicenseNumber(bnplOnboardingRequest.getBusinessLicenseNumber());
        wallet.setGuarantorName(bnplOnboardingRequest.getGuarantorName());
        wallet.setGuarantorPhoneNumber(bnplOnboardingRequest.getGuarantorPhoneNumber());
        Wallet wallet1 = walletRepository.save(wallet);

        log.info("Wallet has been created successfully for customer: {}", savedCustomer.getName());

        List<Customers> customers = customersRepository.findAllByParentCustomerAndDeletedIsFalse(savedCustomer);


        // update already created customers.
        if (!customers.isEmpty()) {
            customers.forEach(childCustomer -> createWalletForChildCustomer(childCustomer, wallet1.getCreatedBy(), wallet1));
        }

    }

    public Page<WalletResponseDTO> convertToWalletResponse(Page<Wallet> walletPage) {
        return walletPage.map(wallet -> {
            WalletResponseDTO dto = new WalletResponseDTO();
            dto.setPublicId(wallet.getPublicId());
            dto.setCustomerName(wallet.getCustomer().getName());
            dto.setCreditLimit(wallet.getCustomer().getCreditLimit());
            dto.setCustomerType(wallet.getCustomer().getCustomerType());
            dto.setRegion(wallet.getCustomer().getOrganizationRegions() != null ? wallet.getCustomer().getOrganizationRegions().getName() : null);
            dto.setZone(wallet.getCustomer().getOrganizationLocation() != null ? wallet.getCustomer().getOrganizationLocation().getName() : null);
            dto.setOnBoardingDate(wallet.getCustomer().getOnBoardingDate());
            dto.setIdNumber(wallet.getCustomerID());
            dto.setAvailableBalance(wallet.getCreditLimitBalance());
            dto.setTotalCreditUsed(wallet.getTotalCreditAmount());
            dto.setTotalPaymentsMade(wallet.getTotalPaymentAmount());
            dto.setCreditStatus(wallet.getCreditStatus());
            dto.setCustomerID(wallet.getCustomerID());
            dto.setTinNumber(wallet.getTinNumber());
            dto.setBusinessLicenseNumber(wallet.getBusinessLicenseNumber());
            dto.setGuarantorName(wallet.getGuarantorName());
            dto.setGuarantorPhoneNumber(wallet.getGuarantorPhoneNumber());
            dto.setUnpaidAmount(wallet.getUnPaidAmount());
            dto.setWalletBalance(wallet.getExcessAmountPaid());
            dto.setTotalCreditTaken(wallet.getTotalCreditTaken());
            return dto;
        });
    }


    public BNPLCreditControlDTO generateBNPLPaymentDashboardSummary() {
        BNPLCreditControlDTO dto = new BNPLCreditControlDTO();

        List<Wallet> wallets = walletRepository.findAllActiveWalletsNative(Pageable.unpaged()).getContent();

        BigDecimal totalCreditIssued = wallets.stream()
                .map(Wallet::getTotalCreditAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaymentsReceived = wallets.stream()
                .map(Wallet::getTotalPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOutstandingBalance = wallets.stream()
                .map(wallet -> wallet.getCustomer().getCreditLimit().subtract(wallet.getCreditLimitBalance()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        BigDecimal totalDefaultedAmount = wallets.stream()
                .filter(wallet -> wallet.getCreditStatus() == CreditStatus.Defaulted)
                .map(wallet -> wallet.getTotalCreditAmount().subtract(wallet.getTotalTransactionAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal unPaidAmount = wallets.stream()
                .map(Wallet::getUnPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        dto.setOverallCreditTaken(totalCreditIssued);
        dto.setOverallPaidAmount(totalPaymentsReceived);
        dto.setOverallDueAmount(totalOutstandingBalance);
        dto.setOverallDefaultedAmount(totalDefaultedAmount);
        dto.setUnpaidAmount(unPaidAmount);

        return dto;
    }

    public Page<BNPLPaymentResponse> getBNPLCreditPaymentsData(String search, UUID customerPublicId, PaymentStatusEnum paymentStatus, CustomerType customerType, UUID regionPublicId, String startDate, String dueDate, UUID parentCustomerPublicId, Batched batched, Pageable pageable) {
        LocalDate start = null;
        LocalDate due = null;

        if (startDate != null && !startDate.isEmpty()) {
            start = LocalDate.parse(startDate);
        }
        if (dueDate != null && !dueDate.isEmpty()) {
            due = LocalDate.parse(dueDate);
        }

        Customers customer = null;
        if (customerPublicId != null) {
            customer = customersRepository.findByPublicIdAndDeletedIsFalse(customerPublicId).orElse(null);
        }

        if (customerPublicId == null && parentCustomerPublicId != null) {
            customer = customersRepository.findByPublicIdAndDeletedIsFalse(parentCustomerPublicId).orElse(null);
        }

        // entity manager query

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

// Main query
        CriteriaQuery<CreditLimitItem> cq = cb.createQuery(CreditLimitItem.class);
        Root<CreditLimitItem> root = cq.from(CreditLimitItem.class);
        cq.distinct(true);
        cq.where(cb.and(buildPaymentPredicates(cb, root, search, customer, start, due, paymentStatus, customerType, regionPublicId, parentCustomerPublicId, batched).toArray(new Predicate[0])));
        cq.orderBy(cb.desc(root.get("id")));

        TypedQuery<CreditLimitItem> query = entityManager.createQuery(cq)
                .setMaxResults(pageable.getPageSize())
                .setFirstResult((int) pageable.getOffset());  // better than pageNumber * size

        List<CreditLimitItem> queryResultList = query.getResultList();

// Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<CreditLimitItem> countRoot = countQuery.from(CreditLimitItem.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(cb.and(buildPaymentPredicates(cb, countRoot, search, customer, start, due, paymentStatus, customerType, regionPublicId, parentCustomerPublicId, batched).toArray(new Predicate[0])));

        Long count = entityManager.createQuery(countQuery).getSingleResult();

        List<BNPLPaymentResponse> queryDTOList = convertToBNPLPaymentResponse(queryResultList);
        return new PageImpl<>(queryDTOList, pageable, count);

    }

    public List<BNPLPaymentResponse> convertToBNPLPaymentResponse(List<CreditLimitItem> queryResultList) {
        List<BNPLPaymentResponse> queryDTOList = new ArrayList<>();
        queryResultList.forEach(creditLimitItem -> {
            Wallet fundingWallet = resolveFundingWallet(creditLimitItem.getWallet());
            BNPLPaymentResponse dto = new BNPLPaymentResponse();
            dto.setPublicId(creditLimitItem.getWallet().getCustomer().getPublicId());
            dto.setCreditLimitPublicId(creditLimitItem.getPublicId());
            dto.setCustomerName(creditLimitItem.getWallet().getCustomer().getName());
            dto.setCustomerPhoneNumber(String.valueOf(creditLimitItem.getWallet().getCustomer().getPhoneNumber()));
            dto.setActiveStatus(creditLimitItem.getWallet().getCustomer().getCustomerActiveStatus());
            dto.setCustomerType(creditLimitItem.getWallet().getCustomer().getCustomerType());
            dto.setCreditAmount(creditLimitItem.getAmount());
            dto.setInterestRate(creditLimitItem.getInterestRate());
            dto.setInterestAmount(creditLimitItem.getInterestAmount());
            dto.setPayableAmount(creditLimitItem.getBalanceAmount());
            dto.setPaidAmount(creditLimitItem.getPaidAmount());
            dto.setPaymentStatus(creditLimitItem.getPaymentStatus());
            dto.setDate(creditLimitItem.getDateTaken());
            dto.setPaymentReference(creditLimitItem.getPaymentReference());
            dto.setCreditStatus(creditLimitItem.getCreditStatus());
            dto.setDuration(creditLimitItem.getDurationDays());
            dto.setOverdueDays(creditLimitItem.getOverdueDays());
            dto.setCreditLimit(fundingWallet.getCustomer().getCreditLimit());
            dto.setCreditBalance(fundingWallet.getCreditLimitBalance());
            dto.setDateTaken(creditLimitItem.getDateTaken());
            dto.setDueDate(creditLimitItem.getDueDate());
            dto.setOrderNumber(creditLimitItem.getOrderNumber());
            dto.setOrderDelivered(creditLimitItem.isOrderDelivered());
            dto.setAmountPaidFromWallet(creditLimitItem.getAmountPaidFromWallet());
            dto.setWalletBalance(fundingWallet.getWalletBalance());
            dto.setEnterpriseCredit(creditLimitItem.getWallet().getCustomer().isHasOtherStores() || creditLimitItem.getWallet().getCustomer().isHasParentCustomer());
            dto.setBatched(creditLimitItem.getBatched());
            dto.setWalletPublicId(creditLimitItem.getWallet().getPublicId().toString());
            queryDTOList.add(dto);
        });
        return queryDTOList;
    }

    private List<Predicate> buildPaymentPredicates(CriteriaBuilder cb, Root<CreditLimitItem> currentRoot, String search, Customers customer, LocalDate start, LocalDate due, PaymentStatusEnum paymentStatus, CustomerType customerType, UUID regionPublicId, UUID parentCustomerPublicId, Batched batched) {
        List<Predicate> andPredicates = new ArrayList<>();

        andPredicates.add(cb.equal(currentRoot.get("deleted"), Boolean.FALSE));
        andPredicates.add(cb.equal(currentRoot.get("cancelled"), Boolean.FALSE));

        if (start != null && due != null && start.isBefore(due)) {
            andPredicates.add(cb.between(currentRoot.get("dateTaken"), start, due));
        }

        if (customer != null) {
            andPredicates.add(cb.equal(currentRoot.get("wallet").get("customer"), customer));
        }

        if (paymentStatus != null) {
            andPredicates.add(cb.equal(currentRoot.get("paymentStatus"), paymentStatus));
        }

        if (batched != null) {
            andPredicates.add(cb.equal(currentRoot.get("batched"), batched));
        }

        if (customerType != null) {
            andPredicates.add(cb.equal(currentRoot.get("wallet").get("customer").get("customerType"), customerType));
        }

        if (regionPublicId != null) {
            andPredicates.add(cb.equal(currentRoot.get("wallet").get("customer").get("organizationRegions").get("publicId"), regionPublicId));
        }

        if (parentCustomerPublicId != null) {
            List<UUID> creditScopeCustomerIds = resolveCreditScopeCustomerIds(parentCustomerPublicId);
            andPredicates.add(currentRoot.get("wallet").get("customer").get("publicId").in(creditScopeCustomerIds));
        }


        if (search != null && search.trim().length() >= 3) {
            String upperSearch = search.toUpperCase();

            List<Predicate> orPredicates = new ArrayList<>();
            //orPredicates.add(cb.like(cb.upper(currentRoot.get("wallet").get("customer").get("name")), "%" + upperSearch + "%"));
            orPredicates.add(cb.like(cb.upper(currentRoot.get("orderNumber")), "%" + upperSearch + "%"));
            orPredicates.add(cb.like(cb.upper(currentRoot.get("paymentStatus").as(String.class)), "%" + upperSearch + "%"));
            orPredicates.add(cb.like(cb.upper(currentRoot.get("wallet").get("customer").get("phoneNumber")), "%" + upperSearch + "%"));

            andPredicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
        }

        return andPredicates;
    }

    public void createUpdateCreditItems(SalesOrders order, Person person) {

        Wallet wallet = walletRepository.findByCustomerAndDeletedIsFalse(order.getCustomer());

        if (wallet == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("No wallet found for customer " + order.getCustomer().getName())
                    .userMessage("No wallet found for customer " + order.getCustomer().getName());
        }

        Wallet fundingWallet = resolveFundingWallet(wallet);

        BigDecimal creditAmount = order.getTotalAmountPayable() != null ? order.getTotalAmountPayable() : BigDecimal.ZERO;

        log.info("[DISCOUNT-TRACE] createUpdateCreditItems: order={} status={} totalAmount={} totalDiscount={} creditAmount(net of discount)={}",
                order.getSalesOrderNumber(), order.getSalesOrderStatus(), order.getTotalAmount(),
                order.getTotalDiscount(), creditAmount);

        CreditLimitItem creditLimitItem = createUpdateCreditLimitItem(order, creditAmount, wallet, fundingWallet, person);

        createUpdateTransactionItem(creditLimitItem, creditAmount, wallet);
    }


    private CreditLimitItem createUpdateCreditLimitItem(SalesOrders order, BigDecimal creditAmount, Wallet wallet, Wallet fundingWallet, Person person) {

        CreditLimitItem creditLimitItem = creditLimitItemRepository.findByOrderNumberAndDeletedIsFalse(
                order.getSalesOrderNumber()
        ).filter(item -> item.getStatus() == WalletItemStatus.ACTIVE).orElseGet(CreditLimitItem::new);


        Optional<CreditRule> optionalCreditRuleInterestRate = creditRuleRepository.findCreditRuleByCustomerTypeAndInterestRateAndDeletedIsFalse(
                wallet.getCustomer().getCustomerType(),
                true
        );

        Optional<CreditRule> optionalCreditRuleRepaymentPeriod = creditRuleRepository.findCreditRuleByCustomerTypeAndRepaymentTermAndDeletedIsFalse(
                wallet.getCustomer().getCustomerType(),
                true
        );

        if (optionalCreditRuleRepaymentPeriod.isPresent()) {
            CreditRule creditRuleRepaymentPeriod = optionalCreditRuleRepaymentPeriod.get();
            if (creditLimitItem.getDueDate() == null) {
                creditLimitItem.setDueDate(LocalDateTime.now().toLocalDate().plusDays(creditRuleRepaymentPeriod.getRepaymentTermDays() + 1));
            }

        } else {
            if (creditLimitItem.getDueDate() == null) {
                creditLimitItem.setDueDate(LocalDateTime.now().toLocalDate().plusDays(wallet.getCustomer().getCustomerType() == CustomerType.CORPORATE ? 3 : 12));
            }

        }

        Optional<CreditRule> optionalCreditRuleFine = creditRuleRepository.findCreditRuleByCustomerTypeAndOverdueFineAndDeletedIsFalse(
                wallet.getCustomer().getCustomerType(),
                true
        );

        if (optionalCreditRuleFine.isPresent()) {
            CreditRule creditRuleFine = optionalCreditRuleFine.get();
            creditLimitItem.setFinePercentage(creditRuleFine.getOverdueFinePercentage());
            creditLimitItem.setHasFine(true);
        }


        BigDecimal walletBalance = fundingWallet.getCreditLimitBalance().add(creditLimitItem.getAmount()).subtract(creditAmount);

        BigDecimal interestAmount = BigDecimal.ZERO;
        BigDecimal interestRate = BigDecimal.ZERO;
        if (optionalCreditRuleInterestRate.isPresent()) {
            CreditRule creditRuleInterestRate = optionalCreditRuleInterestRate.get();
            BigDecimal interestRatePercentage = (creditLimitItem.getInterestRate() != null && creditLimitItem.getInterestRate().compareTo(BigDecimal.ZERO) > 0) ? creditLimitItem.getInterestRate() : creditRuleInterestRate.getInterestRatePercentage();
            interestAmount = creditAmount.multiply(interestRatePercentage).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
            interestRate = interestRatePercentage;
        }

        // update order

        order.setInterestRate(interestRate);
        order.setInterestAmount(interestAmount);
        order.setCreditAmount(creditAmount);

        fundingWallet.setCreditLimitBalance(walletBalance);
        fundingWallet.setWalletBalance(walletBalance);
        walletRepository.save(fundingWallet);


        creditLimitItem.setWallet(wallet);
        creditLimitItem.setReferenceId(order.getCustomer().getPublicId().toString());
        creditLimitItem.setDescription("Credit taken for order " + order.getSalesOrderNumber());
        creditLimitItem.setCreditStatus(CreditStatus.Healthy);
        creditLimitItem.setAmount(creditAmount);
        creditLimitItem.setOrderNumber(order.getSalesOrderNumber());
        creditLimitItem.setDateTaken(creditLimitItem.getDateTaken() == null ? LocalDateTime.now().toLocalDate() : creditLimitItem.getDateTaken());
        creditLimitItem.setPaymentStatus(PaymentStatusEnum.NOT_PAID);
        creditLimitItem.setInterestAmount(interestAmount);
        creditLimitItem.setInterestRate((creditLimitItem.getInterestRate() != null && creditLimitItem.getInterestRate().compareTo(BigDecimal.ZERO) > 0) ? creditLimitItem.getInterestRate() : interestRate);
        creditLimitItem.setStatus(WalletItemStatus.ACTIVE);
        creditLimitItem.setBalanceAmount(creditAmount);
        creditLimitItem.setCreatedBy(person);
        creditLimitItem.setCreationTime(LocalDateTime.now());
        creditLimitItem.setOrderPublicId(order.getPublicId().toString());
        creditLimitItem.setParentCustomerPublicId(order.getCustomer().isHasParentCustomer() ? String.valueOf(order.getCustomer().getParentCustomer().getPublicId()) : null);
        CreditLimitItem saveCreditLimitItem = creditLimitItemRepository.save(creditLimitItem);

        log.info("===CreditLimitItem has been created/updated successfully for order===: {} {}", order.getSalesOrderNumber(), order.getCustomer().getName());
        if (saveCreditLimitItem.getDueDate() != null) {
            updateSalesOrderDueDate(order, saveCreditLimitItem.getDueDate());
        }
        return saveCreditLimitItem;
    }

    private void updateSalesOrderDueDate(SalesOrders order, LocalDate dueDate) {
        order.setDueDate(dueDate);
        salesOrdersRepository.save(order);
    }

    private void createUpdateTransactionItem(CreditLimitItem creditLimitItem, BigDecimal creditAmount, Wallet wallet) {

        TransactionItem transactionItem = transactionItemRepository.findByTransactionTypeAndOrderNumberAndStatusAndDeletedIsFalse(
                TransactionType.PURCHASE,
                creditLimitItem.getOrderNumber(),
                WalletItemStatus.ACTIVE
        ).orElseGet(TransactionItem::new);

        transactionItem.setTransactionType(TransactionType.PURCHASE);
        transactionItem.setAmount(creditAmount);
        transactionItem.setDescription("Transaction for credit taken on order " + creditLimitItem.getOrderNumber());
        transactionItem.setReferenceId(creditLimitItem.getReferenceId());
        transactionItem.setOrderNumber(creditLimitItem.getOrderNumber());
        transactionItem.setWallet(wallet);
        transactionItem.setCreatedBy(creditLimitItem.getCreatedBy());
        transactionItem.setCreationTime(LocalDateTime.now());
        transactionItem.setStatus(WalletItemStatus.ACTIVE);

        transactionItemRepository.save(transactionItem);
    }

    public void cancelCreditItemsForOrder(SalesOrders salesOrder, String name) {

        Optional<CreditLimitItem> optionalCreditLimitItem = creditLimitItemRepository.findByOrderNumberAndDeletedIsFalse(
                salesOrder.getSalesOrderNumber()
        );

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalCreditLimitItem.isPresent()) {
            CreditLimitItem creditLimitItem = optionalCreditLimitItem.get();
            creditLimitItem.setStatus(WalletItemStatus.CANCELLED);
            creditLimitItem.setCancelled(true);
            creditLimitItem.setUpdatedBy(personOptional != null && personOptional.isPresent() ? personOptional.get() : null);
            creditLimitItem.setUpdateDate(LocalDateTime.now());
            creditLimitItemRepository.save(creditLimitItem);


            Optional<TransactionItem> optionalTransactionItem = transactionItemRepository.findByTransactionTypeAndOrderNumberAndStatusAndDeletedIsFalse(
                    TransactionType.PURCHASE,
                    salesOrder.getSalesOrderNumber(),
                    WalletItemStatus.ACTIVE
            );

            if (optionalTransactionItem.isPresent()) {
                TransactionItem transactionItem = optionalTransactionItem.get();
                transactionItem.setStatus(WalletItemStatus.CANCELLED);
                transactionItem.setUpdatedBy(personOptional != null && personOptional.isPresent() ? personOptional.get() : null);
                transactionItem.setUpdateDate(LocalDateTime.now());
                transactionItemRepository.save(transactionItem);
            }

            Wallet wallet = resolveFundingWallet(creditLimitItem.getWallet());
            wallet.setCreditLimitBalance(wallet.getCreditLimitBalance().add(creditLimitItem.getAmount()));
            walletRepository.save(wallet);
        }
    }

    public List<CustomerPurchaseTrend> generateCustomerPurchaseTrend(Customers customer, Integer year) {
        List<CustomerPurchaseTrend> trends = new ArrayList<>();

        List<SalesOrders> orders = salesOrdersRepository.findAllByCustomerAndSalesOrderStatusAndDeletedIsFalse(
                customer,
                LoadingStatusEnum.DELIVERED
        );

        // display all 12 months even with no purchases
        // the month Jan - Dec


        for (int month = 1; month <= 12; month++) {
            int finalMonth = month;
            BigDecimal totalRevenue = orders.stream()
                    .filter(order -> order.getDeliveryDate() != null)
                    .filter(order -> order.getDeliveryDate().getYear() == year)
                    .filter(order -> order.getDeliveryDate().getMonthValue() == finalMonth)
                    .map(SalesOrders::getTotalAmountPayable)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            CustomerPurchaseTrend trend = new CustomerPurchaseTrend();
            trend.setYear(year);
            trend.setMonth(month);
            trend.setTotalRevenue(totalRevenue);
            String monthLabel = LocalDateTime.of(year, month, 1, 0, 0).getMonth().name().substring(0, 3);
            trend.setLabel(monthLabel + " " + year);
            trends.add(trend);
        }

        return trends;

    }


    public void processDepositToWallet(Wallet wallet, BigDecimal amount, List<String> receipts, Boolean payViaWallet, Boolean hasReceipts, String name) {
        Wallet fundingWallet = resolveFundingWallet(wallet);

        String paymentApprovedBy = String.format("System at %s", formatter.format(LocalDateTime.now()));
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        if (optionalPerson.isPresent()) {
            paymentApprovedBy = String.format("%s at %s", optionalPerson.get().getFullName(), formatter.format(LocalDateTime.now()));
        }

        List<CreditLimitItem> creditLimitItems = creditLimitItemRepository
                .findPendingCreditItemsNative(
                        wallet.getCustomer().getPublicId().toString(),
                        WalletItemStatus.ACTIVE.name(),
                        List.of(PaymentStatusEnum.NOT_PAID.name(), PaymentStatusEnum.PARTIALLY_PAID.name())
                );

        if (creditLimitItems.size() != 1) {
            throw new BadRequestRestApiException()
                    .developerMessage("Cannot process deposit when multiple or no credit items are pending payment")
                    .userMessage("Cannot process deposit when multiple or no credit items are pending payment");
        }

        CreditLimitItem creditLimitItem = creditLimitItems.get(0);

        if (hasReceipts && (payViaWallet == null || !payViaWallet)) {
            payViaWallet = creditLimitItem.isPayViaWallet();
        }

        List<PaymentItem> paymentItems = paymentItemRepository.findByOrderNumberIgnoreCaseAndStatusAndDeletedIsFalse(creditLimitItem.getOrderNumber(), WalletItemStatus.PAID);

        BigDecimal totalPaidAmount = paymentItems.stream()
                .map(item -> ObjectUtils.defaultIfNull(item.getAmountPaid(), BigDecimal.ZERO)
                        .add(ObjectUtils.defaultIfNull(item.getAmountPaidFromWallet(), BigDecimal.ZERO)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAmountPayable = creditLimitItem.getAmount().add(
                ObjectUtils.defaultIfNull(creditLimitItem.getInterestAmount(), BigDecimal.ZERO)
        ).add(ObjectUtils.defaultIfNull(creditLimitItem.getFineAmount(), BigDecimal.ZERO));

        BigDecimal totalAmountToBePaid = totalPaidAmount.add(amount);

        BigDecimal excessAmount = BigDecimal.ZERO;

        if (totalAmountToBePaid.compareTo(totalAmountPayable) >= 0) {
            excessAmount = totalAmountToBePaid.subtract(totalAmountPayable);
        }

        BigDecimal walletBalance = ObjectUtils.defaultIfNull(fundingWallet.getWalletBalance(), BigDecimal.ZERO);
        BigDecimal walletExcessAmountPaid = ObjectUtils.defaultIfNull(fundingWallet.getExcessAmountPaid(), BigDecimal.ZERO);
        BigDecimal amountToDeductFromWallet = BigDecimal.ZERO;
        BigDecimal walletNewBalance = walletBalance;
        BigDecimal walletNewExcessAmountPaid = walletExcessAmountPaid;
        BigDecimal walletAmountPaid = BigDecimal.ZERO;

        if (Boolean.TRUE.equals(payViaWallet) && excessAmount.compareTo(BigDecimal.ZERO) <= 0) {
            amountToDeductFromWallet = totalAmountPayable.subtract(totalAmountToBePaid);
        }

        if (amountToDeductFromWallet.compareTo(BigDecimal.ZERO) > 0) {
            if (walletBalance.compareTo(amountToDeductFromWallet) >= 0) {
                walletAmountPaid = amountToDeductFromWallet;
                walletNewBalance = walletBalance.subtract(amountToDeductFromWallet);
            } else {
                walletAmountPaid = walletBalance;
                walletNewBalance = BigDecimal.ZERO;
            }
            walletNewExcessAmountPaid = subtractWithoutNegative(walletExcessAmountPaid, walletAmountPaid);
        }

        if (excessAmount.compareTo(BigDecimal.ZERO) > 0) {
            walletNewBalance = walletBalance.add(excessAmount);
            walletNewExcessAmountPaid = walletExcessAmountPaid.add(excessAmount);
        }

        totalAmountToBePaid = totalAmountToBePaid.add(walletAmountPaid);
        boolean fullyPaid = totalAmountToBePaid.compareTo(totalAmountPayable) >= 0;


        creditLimitItem.setExcessAmountPaid(excessAmount);
        creditLimitItem.setPaidAmount(totalAmountToBePaid);
        creditLimitItem.setHasExcessPayment(excessAmount.compareTo(BigDecimal.ZERO) > 0);
        creditLimitItem.setAmountPaidFromWallet(walletAmountPaid);
        creditLimitItem.setApprovedBy(paymentApprovedBy);


        CreditLimitItem savedCreditLimitItem = creditLimitItemRepository.save(creditLimitItem);

        if (!fullyPaid) {
            savedCreditLimitItem.setPaymentStatus(PaymentStatusEnum.PARTIALLY_PAID);
        } else {
            savedCreditLimitItem.setPaymentStatus(PaymentStatusEnum.PAID);
            savedCreditLimitItem.setFullyPaid(true);
            updateOrderPaymentStatusToPaid(savedCreditLimitItem.getOrderPublicId(), savedCreditLimitItem.getOrderNumber());
        }


        CreditLimitItem finalCreditLimitItem = creditLimitItemRepository.save(savedCreditLimitItem);

        BigDecimal settledAmount;
        BigDecimal customerCreditLimit = ObjectUtils.defaultIfNull(fundingWallet.getCustomer().getCreditLimit(), BigDecimal.ZERO);

        if (finalCreditLimitItem.isFullyPaid()) {
            settledAmount = finalCreditLimitItem.getAmount();
        } else {
            settledAmount = finalCreditLimitItem.getPaidAmount();
        }

        fundingWallet.setWalletBalance(walletNewBalance);
        fundingWallet.setExcessAmountPaid(walletNewExcessAmountPaid);
        fundingWallet.setCreditLimitBalance(finalCreditLimitItem.isFullyPaid() ? customerCreditLimit : customerCreditLimit.subtract(settledAmount));

        String receiptNumbers = null;
        if (hasReceipts) {
            if (receipts == null || receipts.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Receipts are required when hasReceipts is true")
                        .userMessage("Receipts are required when hasReceipts is true");
            }
            receiptNumbers = String.join(",", receipts);
        }


        createTransaction(amount.add(walletAmountPaid), finalCreditLimitItem);

        PaymentItem paymentItem = createPaymentItem(finalCreditLimitItem, amount, walletAmountPaid, paymentApprovedBy, receiptNumbers);

        finalCreditLimitItem.setPaymentReference(paymentItem.getPaymentReference());
        creditLimitItemRepository.save(finalCreditLimitItem);

        walletRepository.save(fundingWallet);


        if (hasReceipts) {
            updateFileReceipt(receipts, wallet.getCustomer().getPublicId(), creditLimitItem.getOrderNumber());
        }

        creditReconHelper.updateReconAmount(finalCreditLimitItem, finalCreditLimitItem.getOrderNumber());

        String customerNotificationMessage = String.format("Your payment of %s for order %s has been received and processed. Your current credit is %s.", amount, creditLimitItem.getOrderNumber(), finalCreditLimitItem.isFullyPaid() ? "fully settled" : "partially settled with an outstanding balance of " + finalCreditLimitItem.getBalanceAmount());

        itibariSmsService.send(finalCreditLimitItem.getWallet().getCustomer().getPhoneNumber(), customerNotificationMessage);

        String description = String.format("Payment of %s received for order %s. Amount paid from wallet: %s. Excess amount: %s. Payment status: %s.",
                amount,
                creditLimitItem.getOrderNumber(),
                walletAmountPaid,
                excessAmount,
                finalCreditLimitItem.getPaymentStatus().name());

        eventsRepository.save(new Events(creditLimitItem.getPublicId().toString(), "Payment", LocalDateTime.now(), description, "CREDIT-PAYMENT", personRepository.findByEmailAndDeletedIsFalse(name).orElse(null)));

    }

    private BigDecimal subtractWithoutNegative(BigDecimal amount, BigDecimal deduction) {
        BigDecimal result = ObjectUtils.defaultIfNull(amount, BigDecimal.ZERO)
                .subtract(ObjectUtils.defaultIfNull(deduction, BigDecimal.ZERO));
        return result.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : result;
    }

    private void sendWebHookAlert(SalesOrders salesOrders) {
        CreditLimitItem creditLimitItem = creditLimitItemRepository.findByOrderNumberAndDeletedIsFalse(salesOrders.getSalesOrderNumber()).orElse(null);
        String referenceNumber = creditLimitItem != null ? creditLimitItem.getPaymentReference() : "-";
        String status = salesOrders.getPaymentStatus() == PaymentStatusEnum.PAID ? "Fully" : "PARTIALLY";
        String payload = String.format(
                "{ \"text\": \"Payment has been received for Order %s from %s. The payment reference number is %s. The payment has been settled %s, and the order payment status %s has been updated accordingly.\" }",
                salesOrders.getSalesOrderNumber(),
                salesOrders.getCustomer().getName(),
                referenceNumber,
                status,
                salesOrders.getPaymentStatus().name());

        if (!salesOrders.getSalesOrderNumber().equals("ORD101009")) {
            webhookClient.sendMessage(webhookUrl, payload);
        }
    }

    private void updateFileReceipt(List<String> receipts, UUID publicId, String orderNumber) {
        List<FileData> fileDataList = fileDataRepository.findByReferenceTypeAndReferenceIdAndDescriptionIgnoreCaseAndDeletedIsFalse(
                ReferenceType.CREDIT_PAYMENT.name(),
                publicId.toString(),
                orderNumber
        );
        for (FileData fileData : fileDataList) {
            if (fileData.getReceipt() != null) {
                boolean match = receipts.stream().anyMatch(receipt -> receipt.equalsIgnoreCase(fileData.getReceipt()));
                if (match) {
                    fileData.setProcessed(true);
                    fileDataRepository.save(fileData);
                }
            }
        }
    }

    private void updateOrderPaymentStatusToPaid(String orderPublicId, String orderNumber) {
        Optional<SalesOrders> optionalSalesOrder = Optional.empty();

        if (StringUtils.isNotBlank(orderPublicId)) {
            try {
                optionalSalesOrder = salesOrdersRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(orderPublicId));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid orderPublicId format while updating payment status: {}", orderPublicId);
            }
        }

        if (optionalSalesOrder.isEmpty() && StringUtils.isNotBlank(orderNumber)) {
            optionalSalesOrder = salesOrdersRepository.findBySalesOrderNumberAndDeletedIsFalse(orderNumber);
        }

        optionalSalesOrder.ifPresent(salesOrders -> {
            salesOrders.setPaymentStatus(PaymentStatusEnum.PAID);
            salesOrders.setPaymentDate(LocalDateTime.now());

            String description = String.format("Payment received for order %s. Total amount paid: %s. Payment status updated to PAID.",
                    salesOrders.getSalesOrderNumber(),
                    salesOrders.getTotalAmountPayable());

            eventsRepository.save(new Events(orderPublicId,
                    "ORDER PAID",
                    LocalDateTime.now(),
                    description,
                    "Order Paid",
                    null));

            SalesOrders saveSalesOrder = salesOrdersRepository.save(salesOrders);
            sendWebHookAlert(saveSalesOrder);
        });
    }

    private void createTransaction(BigDecimal amount, CreditLimitItem creditLimitItem) {

        Optional<TransactionItem> optionalTransactionItem = transactionItemRepository.findByTransactionTypeAndOrderNumberAndStatusAndDeletedIsFalse(
                TransactionType.PURCHASE,
                creditLimitItem.getOrderNumber(),
                WalletItemStatus.ACTIVE
        );

        if (optionalTransactionItem.isPresent()) {
            TransactionItem transactionItem = optionalTransactionItem.get();
            transactionItem.setStatus(WalletItemStatus.PAID);
            transactionItemRepository.save(transactionItem);
        }

        TransactionItem transactionItem = new TransactionItem();
        transactionItem.setTransactionType(TransactionType.DEPOSIT);
        transactionItem.setAmount(amount);
        transactionItem.setDescription("Transaction for deposit made on credit item for order " + creditLimitItem.getOrderNumber());
        transactionItem.setReferenceId(creditLimitItem.getReferenceId());
        transactionItem.setOrderNumber(creditLimitItem.getOrderNumber());
        transactionItem.setWallet(creditLimitItem.getWallet());
        transactionItem.setCreatedBy(creditLimitItem.getCreatedBy());
        transactionItem.setCreationTime(LocalDateTime.now());
        transactionItem.setStatus(WalletItemStatus.DEPOSITED);

        transactionItemRepository.save(transactionItem);
    }

    private PaymentItem createPaymentItem(CreditLimitItem saved, BigDecimal amount, BigDecimal amountPaidFromWallet, String approvedBy, String receiptNumber) {

        PaymentItem paymentItem = new PaymentItem();
        paymentItem.setWallet(saved.getWallet());
        paymentItem.setAmountPaid(amount);
        paymentItem.setDescription("Payment made for credit item on order " + saved.getOrderNumber());
        paymentItem.setOrderNumber(saved.getOrderNumber());
        paymentItem.setReferenceId(saved.getReferenceId());
        paymentItem.setPaymentMethod("BNPL Wallet Deposit");
        paymentItem.setPaymentReference("PMT-" + counterService.getNextCounter(CounterType.CREDIT_PAYMENT));
        paymentItem.setStatus(WalletItemStatus.PAID);
        paymentItem.setInterestRate(saved.getInterestRate());
        paymentItem.setTotalInterest(saved.getInterestAmount());
        paymentItem.setReceiptNumber(receiptNumber);
        paymentItem.setAmountPaidFromWallet(amountPaidFromWallet);
        paymentItem.setApprovedBy(approvedBy);
        paymentItem.setExcessAmountPaid(saved.getExcessAmountPaid());

        return paymentItemRepository.save(paymentItem);
    }

    /**
     * this method validates the deposit amount for a given customer referenceId and orderNumber. checks previous receipts, aggregate amounts, etc
     *
     * @param amount
     * @param referenceId
     * @param orderNumber
     */
    public void validateAmount(String amount, UUID referenceId, String orderNumber) {

        SalesOrders orders = salesOrdersRepository.findBySalesOrderNumberAndDeletedIsFalse(orderNumber)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Order not found for order number: " + orderNumber)
                        .userMessage("Order not found for order number: " + orderNumber));

        if (!orders.getCustomer().getPublicId().equals(referenceId)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Order reference not found for order number: " + orderNumber)
                    .userMessage("Sorry!. You can only make payment for orders associated with customer account");
        }

        BigDecimal amountPaidSoFar = BigDecimal.ZERO;
        log.info("Validating deposit amount: {} for referenceId: {}", amount, referenceId);

        // find fileData by referenceId and orderNumber

        List<FileData> fileDataList = fileDataRepository.findByReferenceTypeAndReferenceIdAndDescriptionIgnoreCaseAndDeletedIsFalse(
                ReferenceType.CREDIT_PAYMENT.name(),
                referenceId.toString(),
                orderNumber
        );

        fileDataList = fileDataList.stream()
                .filter(fileData -> (fileData.getDescription() != null && ObjectUtils.isNotEmpty(fileData.getDescription().trim()) && fileData.getDescription().equalsIgnoreCase(orderNumber)))
                .filter(fileData -> fileData.getAmount() != null)
                .filter(fileData -> fileData.getRejectionReason() == null || fileData.getRejectionReason().isEmpty())
                .toList();


        if (!fileDataList.isEmpty()) {
            log.info("=========Found {} previous payment records for referenceId: {} and orderNumber: {}========", fileDataList.size(), referenceId, orderNumber);
            // log file ids and amounts for traceability
            fileDataList.forEach(fileData ->
                    log.info("=======Previous payment record - File ID: {}, Amount: {}, Receipt: {}========", fileData.getPublicId(), fileData.getAmount(), fileData.getReceipt()));

            amountPaidSoFar = fileDataList.stream()
                    .map(FileData::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            log.info("Found previous payments for referenceId: {} and orderNumber: {}. Total amount paid so far: {}", referenceId, orderNumber, amountPaidSoFar);
        }

        BigDecimal depositAmount;
        try {
            depositAmount = new BigDecimal(amount);
        } catch (NumberFormatException e) {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid amount format")
                    .userMessage("Invalid amount format");
        }

        depositAmount = depositAmount.add(amountPaidSoFar);

        if (depositAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestRestApiException()
                    .developerMessage("Deposit amount must be greater than zero")
                    .userMessage("Deposit amount must be greater than zero");
        }

        Customers customer = customersRepository.findByPublicIdAndDeletedIsFalse(referenceId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Customer not found")
                        .userMessage("Customer not found"));

        Wallet wallet = walletRepository.findActiveByCustomerIdNative(customer.getId()).orElse(null);

        if (wallet == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Wallet not found for customer")
                    .userMessage("Wallet not found for customer");
        }


        List<CreditLimitItem> creditLimitItems = wallet.getCreditLimitItems().stream()
                .filter(creditLimitItem -> !creditLimitItem.isCancelled())
                .filter(creditLimitItem -> !creditLimitItem.getDeleted())
                .filter(creditLimitItem -> creditLimitItem.getStatus() == WalletItemStatus.ACTIVE)
                .filter(creditLimitItem -> creditLimitItem.getPaymentStatus() == PaymentStatusEnum.NOT_PAID || creditLimitItem.getPaymentStatus() == PaymentStatusEnum.PARTIALLY_PAID)
                .toList();

        if (creditLimitItems.size() != 1) {
            throw new BadRequestRestApiException()
                    .developerMessage("Cannot process deposit when multiple or no credit items are pending payment")
                    .userMessage("Sorry, we are unable to process your payment since you dont have any pending credit payments or you have multiple pending credit payments.");
        }

    }

    public CreditStatusDashboardDTO generateCreditStatusDashboard() {
        CreditStatusDashboardDTO dto = new CreditStatusDashboardDTO();

        List<Wallet> wallets = walletRepository.findAllActiveWalletsNative(Pageable.unpaged()).getContent();

        dto.setCustomersOnCredit(wallets.size());

        BigDecimal overallCreditLimit = wallets.stream()
                .map(wallet -> wallet.getCustomer().getCreditLimit())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageCreditLimit = BigDecimal.ZERO;

        if (!wallets.isEmpty()) {
            averageCreditLimit = overallCreditLimit.divide(BigDecimal.valueOf(wallets.size()), RoundingMode.HALF_UP);
        }

        dto.setAverageCreditLimit(averageCreditLimit);

        int totalCreditOrders = creditLimitItemRepository.countByDeletedIsFalse();
        dto.setTotalCreditOrders(totalCreditOrders);

        int healthyCreditCustomers = (int) wallets.stream()
                .filter(wallet -> wallet.getCreditStatus() == CreditStatus.Healthy)
                .count();
        dto.setHealthyCreditCustomers(healthyCreditCustomers);

        int churnedCreditCustomers = 0;  // for now set to 0 as no definition provided
        dto.setChurnedCreditCustomers(churnedCreditCustomers);

        int defaulterCreditCustomers = (int) wallets.stream()
                .filter(wallet -> wallet.getCreditStatus() == CreditStatus.Defaulted)
                .count();
        dto.setDefaulterCreditCustomers(defaulterCreditCustomers);

        int blacklistedCreditCustomers = (int) wallets.stream()
                .filter(wallet -> wallet.getCreditStatus() == CreditStatus.Suspended)
                .count();
        dto.setBlacklistedCreditCustomers(blacklistedCreditCustomers);

        return dto;
    }

    public Page<BNPLCustomerResponseDTO> convertToBNPLCustomerResponseDTOs(List<Wallet> topWallets) {

        List<BNPLCustomerResponseDTO> dtos = new ArrayList<>();
        for (Wallet wallet : topWallets) {
            dtos.add(convertToBNPLCustomerResponseDTO(wallet, false));
        }
        return new PageImpl<>(dtos);
    }

    public List<AllCustomerPurchaseTrend> generateCustomerPurchaseTrends(Integer year) {
        List<AllCustomerPurchaseTrend> trends = new ArrayList<>();

        List<SalesOrders> orders = salesOrdersRepository.findSalesOrdersBySalesOrderStatusAndCreditAppliedIsTrueAndDeletedIsFalse(
                LoadingStatusEnum.DELIVERED
        );

        // display all 12 months even with no purchases
        // the month Jan - Dec

        for (int month = 1; month <= 12; month++) {
            int finalMonth = month;
            BigDecimal totalRevenue = orders.stream()
                    .filter(order -> order.getDeliveryDate() != null)
                    .filter(order -> order.getDeliveryDate().getYear() == year)
                    .filter(order -> order.getDeliveryDate().getMonthValue() == finalMonth)
                    .map(SalesOrders::getTotalAmountPayable)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            AllCustomerPurchaseTrend trend = new AllCustomerPurchaseTrend();
            trend.setYear(year);
            trend.setMonth(month);
            trend.setTotalRevenue(totalRevenue);
            String monthLabel = LocalDateTime.of(year, month, 1, 0, 0).getMonth().name().substring(0, 3);
            trend.setLabel(monthLabel + " " + year);
            trends.add(trend);
        }

        return trends;
    }

    public Page<BnplLoanManagement> getBNPLLoanManagementData(String search, UUID customerPublicId, String startDate, String dueDate, PaymentStatusEnum paymentStatus, UUID parentCustomerPublicId, Pageable pageable) {

        LocalDate start = null;
        LocalDate due = null;

        if (startDate != null && !startDate.isEmpty()) {
            start = LocalDate.parse(startDate);
        }
        if (dueDate != null && !dueDate.isEmpty()) {
            due = LocalDate.parse(dueDate);
        }

        Customers customer = null;
        if (customerPublicId != null) {
            customer = customersRepository.findByPublicIdAndDeletedIsFalse(customerPublicId).orElse(null);
        }

        // entity manager query

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

// Main query
        CriteriaQuery<CreditLimitItem> cq = cb.createQuery(CreditLimitItem.class);
        Root<CreditLimitItem> root = cq.from(CreditLimitItem.class);
        cq.distinct(true);
        cq.where(cb.and(buildPredicates(cb, root, search, customer, start, due, paymentStatus, parentCustomerPublicId).toArray(new Predicate[0])));
        cq.orderBy(cb.desc(root.get("id")));

        TypedQuery<CreditLimitItem> query = entityManager.createQuery(cq)
                .setMaxResults(pageable.getPageSize())
                .setFirstResult((int) pageable.getOffset());  // better than pageNumber * size

        List<CreditLimitItem> queryResultList = query.getResultList();

// Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<CreditLimitItem> countRoot = countQuery.from(CreditLimitItem.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(cb.and(buildPredicates(cb, countRoot, search, customer, start, due, paymentStatus, parentCustomerPublicId).toArray(new Predicate[0])));

        Long count = entityManager.createQuery(countQuery).getSingleResult();

        List<BnplLoanManagement> queryDTOList = getBNPLLoanManagementDTO(queryResultList);
        return new PageImpl<>(queryDTOList, pageable, count);
    }

    private List<BnplLoanManagement> getBNPLLoanManagementDTO(List<CreditLimitItem> queryResultList) {

        List<BnplLoanManagement> dtos = new ArrayList<>();
        for (CreditLimitItem creditLimitItem : queryResultList) {
            BnplLoanManagement dto = new BnplLoanManagement();
            dto.setCreditPublicId(creditLimitItem.getPublicId().toString());
            dto.setCustomerPublicId(creditLimitItem.getWallet().getCustomer().getPublicId().toString());
            dto.setOrderPublicId(creditLimitItem.getOrderPublicId());
            dto.setOrderNumber(creditLimitItem.getOrderNumber());
            dto.setCustomerName(creditLimitItem.getWallet().getCustomer().getName());
            dto.setCustomerActiveStatus(creditLimitItem.getWallet().getCustomer().getCustomerActiveStatus() != null ? creditLimitItem.getWallet().getCustomer().getCustomerActiveStatus().name() : "New");
            dto.setCustomerPhoneNumber(creditLimitItem.getWallet().getCustomer().getPhoneNumber());
            dto.setCustomerEmail(creditLimitItem.getWallet().getCustomer().getEmail());
            dto.setCreditTakenAmount(creditLimitItem.getAmount());
            dto.setInterestRate(creditLimitItem.getInterestRate());
            dto.setInterestAmount(creditLimitItem.getInterestAmount());
            dto.setDueAmount(creditLimitItem.getBalanceAmount());
            dto.setCreditDate(creditLimitItem.getDateTaken() != null ? creditLimitItem.getDateTaken().toString() : null);
            dto.setPaymentDueDate(creditLimitItem.getDueDate() != null ? creditLimitItem.getDueDate().toString() : null);
            dto.setPaymentStatus(creditLimitItem.getPaymentStatus() != null ? creditLimitItem.getPaymentStatus().name() : null);
            dto.setReminderSent(creditLimitItem.isReminderSent());
            dto.setDefaulted(creditLimitItem.getCreditStatus() == CreditStatus.Defaulted);
            dto.setPaymentDate(creditLimitItem.getPaymentReference() != null ? String.valueOf(creditLimitItem.getUpdateDate().toLocalDate()) : null);
            dto.setAmountPaidFromWallet(creditLimitItem.getAmountPaidFromWallet());

            dtos.add(dto);
        }
        return dtos;
    }


    private List<Predicate> buildPredicates(CriteriaBuilder cb,
                                            Root<CreditLimitItem> currentRoot,
                                            String search,
                                            Customers customer,
                                            LocalDate start,
                                            LocalDate due,
                                            PaymentStatusEnum paymentStatus,
                                            UUID parentCustomerPublicId) {

        List<Predicate> andPredicates = new ArrayList<>();

        andPredicates.add(cb.equal(currentRoot.get("cancelled"), Boolean.FALSE));
        andPredicates.add(cb.equal(currentRoot.get("orderDelivered"), Boolean.TRUE));
        andPredicates.add(cb.equal(currentRoot.get("deleted"), Boolean.FALSE));

        if (start != null && due != null && start.isBefore(due)) {
            andPredicates.add(cb.between(currentRoot.get("dateTaken"), start, due));
        }

        if (customer != null) {
            andPredicates.add(cb.equal(currentRoot.get("wallet").get("customer"), customer));
        }

        if (paymentStatus != null) {
            andPredicates.add(cb.equal(currentRoot.get("paymentStatus"), paymentStatus));
        }

        if (parentCustomerPublicId != null) {
            List<UUID> creditScopeCustomerIds = resolveCreditScopeCustomerIds(parentCustomerPublicId);
            andPredicates.add(currentRoot.get("wallet").get("customer").get("publicId").in(creditScopeCustomerIds));
        }

        if (search != null && search.trim().length() >= 3) {
            String upperSearch = search.toUpperCase();

            List<Predicate> orPredicates = new ArrayList<>();
            orPredicates.add(cb.like(cb.upper(currentRoot.get("wallet").get("customer").get("name")), "%" + upperSearch + "%"));
            orPredicates.add(cb.like(cb.upper(currentRoot.get("orderNumber")), "%" + upperSearch + "%"));
            orPredicates.add(cb.like(cb.upper(currentRoot.get("paymentStatus").as(String.class)), "%" + upperSearch + "%"));
            orPredicates.add(cb.like(cb.upper(currentRoot.get("wallet").get("customer").get("phoneNumber")), "%" + upperSearch + "%"));

            andPredicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
        }

        return andPredicates;
    }

    private List<UUID> resolveCreditScopeCustomerIds(UUID parentCustomerPublicId) {
        Customers parentCustomer = customersRepository.findByPublicIdAndDeletedIsFalse(parentCustomerPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Parent customer not found")
                        .userMessage("Parent customer not found"));

        List<UUID> customerIds = new ArrayList<>();
        customerIds.add(parentCustomer.getPublicId());

        customersRepository.findAllByParentCustomerAndDeletedIsFalse(parentCustomer).stream()
                .map(Customers::getPublicId)
                .filter(Objects::nonNull)
                .forEach(customerIds::add);

        return customerIds;
    }

    public void processDuePaymentReminder(UUID creditPublicId, Person person) {
        Optional<CreditLimitItem> optionalCreditLimitItem = creditLimitItemRepository.findByPublicIdAndDeletedIsFalse(creditPublicId);

        if (optionalCreditLimitItem.isPresent()) {
            CreditLimitItem creditLimitItem = optionalCreditLimitItem.get();

            if (creditLimitItem.getBalanceAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestRestApiException()
                        .developerMessage("Credit limit item has no outstanding balance")
                        .userMessage("Credit limit item has no outstanding balance");
            }
            String message = String.format(
                    "Dear %s, your  credit for order %s  of %s is due on %s." +
                            " Please ensure timely payment to avoid penalties.",
                    creditLimitItem.getWallet().getCustomer().getName(),
                    creditLimitItem.getOrderNumber(),
                    creditLimitItem.getBalanceAmount().toString(),
                    creditLimitItem.getDueDate().toString()
            );
            itibariSmsService.send(creditLimitItem.getWallet().getCustomer().getPhoneNumber(), message);

            saveToDb(creditLimitItem.getWallet().getCustomer().getPhoneNumber(), message, creditLimitItem.getWallet().getCustomer().getName());

            creditLimitItem.setReminderSent(true);
            creditLimitItem.setUpdatedBy(person);
            creditLimitItem.setUpdateDate(LocalDateTime.now());
            creditLimitItemRepository.save(creditLimitItem);
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("Credit limit item not found")
                    .userMessage("Credit limit item not found");
        }
    }

    private void saveToDb(String phoneNumber, String message, String name) {
        TumaSms tumaSms = new TumaSms();
        tumaSms.setTextMessage(message);
        tumaSms.setPhoneNumber(phoneNumber);
        tumaSms.setRecipientName(name);
        tumaSms.setIsSuccess(Boolean.TRUE);
        tumaSms.setIsCustomer(Boolean.TRUE);
        tumaSms.setBuyNowPayLater(true);
        tumaSmsRepository.save(tumaSms);
    }

    public void sendScheduledPaymentReminders() {
        LocalDate today = LocalDate.now();
        List<LocalDate> reminderDueDates = List.of(today.plusDays(1), today, today.minusDays(2));
        List<PaymentStatusEnum> paymentStatuses = List.of(PaymentStatusEnum.NOT_PAID, PaymentStatusEnum.PARTIALLY_PAID);

        List<CreditLimitItem> creditLimitItems = creditLimitItemRepository.findPaymentReminderCreditItems(reminderDueDates, paymentStatuses);
        log.info("Found {} BNPL credit items for scheduled payment reminders", creditLimitItems.size());

        for (CreditLimitItem creditLimitItem : creditLimitItems) {
            try {
                sendScheduledPaymentReminder(creditLimitItem, today);
            } catch (Exception e) {
                log.error("Failed to send BNPL payment reminder for credit item {}", creditLimitItem.getPublicId(), e);
            }
        }
    }

    private void sendScheduledPaymentReminder(CreditLimitItem creditLimitItem, LocalDate today) {
        if (creditLimitItem.getBalanceAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Skipping BNPL payment reminder for credit item {} because balance is not outstanding", creditLimitItem.getPublicId());
            return;
        }

        Wallet wallet = creditLimitItem.getWallet();
        if (wallet == null || wallet.getCustomer() == null) {
            log.warn("Skipping BNPL payment reminder for credit item {} because wallet/customer is missing", creditLimitItem.getPublicId());
            return;
        }

        Customers customer = wallet.getCustomer();
        if (customer.getPhoneNumber() == null || customer.getPhoneNumber().isBlank()) {
            log.warn("Skipping BNPL payment reminder for credit item {} because customer {} has no phone number",
                    creditLimitItem.getPublicId(), customer.getPublicId());
            return;
        }

        String message = buildScheduledPaymentReminderMessage(creditLimitItem, customer, today);
        log.info("Sending BNPL payment reminder for credit item {} to {}", creditLimitItem.getPublicId(), customer.getPhoneNumber());
        itibariSmsService.send(customer.getPhoneNumber(), message);
        saveToDb(customer.getPhoneNumber(), message, customer.getName());
    }

    private String buildScheduledPaymentReminderMessage(CreditLimitItem creditLimitItem, Customers customer, LocalDate today) {
        long daysFromDueDate = ChronoUnit.DAYS.between(today, creditLimitItem.getDueDate());
        String amount = creditLimitItem.getBalanceAmount().toPlainString();

        if (daysFromDueDate == 1) {
            //Habari Shop Name, tunakukumbusha kuwa kesho ni siku ya kulipa rejesho lako la TZS ...... Lipa kupitia CRDB A/C 0150958172600. Asante.
            return String.format("Habari %s, tunakukumbusha kuwa kesho ni siku ya kulipa rejesho lako la TZS %s. Lipa kupitia CRDB A/C 0150958172600. Asante.",
                    customer.getName(), amount);
        }

        //Habari Shop Name, tunakukumbusha kuwa kesho ni leo ya kulipa rejesho lako la TZS ...... Lipa kupitia CRDB A/C 0150958172600. Asante.
        if (daysFromDueDate == 0) {
            return String.format("Habari %s, tunakukumbusha kuwa kesho ni leo ya kulipa rejesho lako la TZS %s. Lipa kupitia CRDB A/C 0150958172600. Asante.",
                    customer.getName(), amount);
        }

        //Habari Shop Name, rejesho lako limechelewa kwa siku .... Kiasi ni TZS ...... Lipa sasa kupitia CRDB A/C 0150958172600 kuepuka tozo za ziada. Asante.
        return String.format("Habari %s, rejesho lako limechelewa kwa siku %s. Kiasi ni TZS %s. Lipa sasa kupitia CRDB A/C 0150958172600 kuepuka tozo za ziada. Asante.",
                customer.getName(), daysFromDueDate, amount);
    }

    public Page<BNPLCustomerResponseDTO> getBNPLCustomers(String search, CreditStatus creditStatus, UUID customerPublicId, CustomerType customerType, UUID regionPublicId, Pageable pageable) {
        return getFlatBNPLCustomers(search, creditStatus, customerPublicId, customerType, regionPublicId, pageable);
    }

    public Page<BNPLCustomerResponseDTO> getBNPLCustomersWithChildCustomers(String search, CreditStatus creditStatus, UUID customerPublicId, CustomerType customerType, UUID regionPublicId, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

// Main query
        CriteriaQuery<Customers> cq = cb.createQuery(Customers.class);
        Root<Customers> root = cq.from(Customers.class);
        cq.distinct(true);
        cq.where(cb.and(buildParentCustomerPredicates(cb, root, search, creditStatus, customerPublicId, customerType, regionPublicId).toArray(new Predicate[0])));
        cq.orderBy(cb.desc(root.get("id")));

        TypedQuery<Customers> query = entityManager.createQuery(cq)
                .setMaxResults(pageable.getPageSize())
                .setFirstResult((int) pageable.getOffset());

        List<Customers> queryResultList = query.getResultList();

// Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Customers> countRoot = countQuery.from(Customers.class);
        countQuery.select(cb.countDistinct(countRoot));
        countQuery.where(cb.and(buildParentCustomerPredicates(cb, countRoot, search, creditStatus, customerPublicId, customerType, regionPublicId).toArray(new Predicate[0])));

        Long count = entityManager.createQuery(countQuery).getSingleResult();

        List<BNPLCustomerResponseDTO> queryDTOList = queryResultList.stream()
                .map(customer -> walletRepository.findActiveByCustomerIdNative(customer.getId()).orElse(null))
                .filter(Objects::nonNull)
                .map(wallet -> convertToBNPLCustomerResponseDTO(wallet, true))
                .toList();
        return new PageImpl<>(queryDTOList, pageable, count);
    }

    private Page<BNPLCustomerResponseDTO> getFlatBNPLCustomers(String search, CreditStatus creditStatus, UUID customerPublicId, CustomerType customerType, UUID regionPublicId, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

// Main query
        CriteriaQuery<Wallet> cq = cb.createQuery(Wallet.class);
        Root<Wallet> root = cq.from(Wallet.class);
        cq.distinct(true);
        cq.where(cb.and(buildWalletPredicates(cb, root, search, creditStatus, customerPublicId, customerType, regionPublicId).toArray(new Predicate[0])));
        cq.orderBy(cb.desc(root.get("id")));

        TypedQuery<Wallet> query = entityManager.createQuery(cq)
                .setMaxResults(pageable.getPageSize())
                .setFirstResult((int) pageable.getOffset());  // better than pageNumber * size

        List<Wallet> queryResultList = query.getResultList();

// Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Wallet> countRoot = countQuery.from(Wallet.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(cb.and(buildWalletPredicates(cb, countRoot, search, creditStatus, customerPublicId, customerType, regionPublicId).toArray(new Predicate[0])));

        Long count = entityManager.createQuery(countQuery).getSingleResult();

        List<BNPLCustomerResponseDTO> queryDTOList = convertToBNPLCustomerResponseDTO(queryResultList);
        return new PageImpl<>(queryDTOList, pageable, count);
    }

    private List<Predicate> buildWalletPredicates(CriteriaBuilder cb, Root<Wallet> countRoot, String search, CreditStatus creditStatus, UUID customerPublicId, CustomerType customerType, UUID regionPublicId) {
        List<Predicate> andPredicates = new ArrayList<>();

        andPredicates.add(cb.equal(countRoot.get("deleted"), Boolean.FALSE));

        if (creditStatus != null) {
            andPredicates.add(cb.equal(countRoot.get("creditStatus"), creditStatus));
        }

        if (customerPublicId != null) {
            Customers customer = customersRepository.findByPublicIdAndDeletedIsFalse(customerPublicId)
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Customer not found")
                            .userMessage("Customer not found"));
            andPredicates.add(cb.equal(countRoot.get("customer"), customer));
        }

        if (customerType != null) {
            andPredicates.add(cb.equal(countRoot.get("customer").get("customerType"), customerType));
        }

        if (regionPublicId != null) {
            OrganizationRegions region = organizationRegionsRepository.findByPublicIdAndDeletedIsFalse(regionPublicId)
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Region not found")
                            .userMessage("Region not found"));
            andPredicates.add(cb.equal(countRoot.get("customer").get("organizationRegions"), region));
        }

        if (search != null && search.trim().length() >= 3) {
            String upperSearch = search.toUpperCase();

            List<Predicate> orPredicates = new ArrayList<>();
            orPredicates.add(cb.like(cb.upper(countRoot.get("customer").get("name")), "%" + upperSearch + "%"));
            orPredicates.add(cb.like(cb.upper(countRoot.get("customer").get("phoneNumber")), "%" + upperSearch + "%"));

            andPredicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
        }

        return andPredicates;
    }

    private List<Predicate> buildParentCustomerPredicates(CriteriaBuilder cb, Root<Customers> root, String search, CreditStatus creditStatus, UUID customerPublicId, CustomerType customerType, UUID regionPublicId) {
        List<Predicate> andPredicates = new ArrayList<>();

        andPredicates.add(cb.equal(root.get("deleted"), Boolean.FALSE));
        andPredicates.add(cb.isNull(root.get("parentCustomer")));

        if (customerPublicId != null) {
            andPredicates.add(cb.equal(root.get("publicId"), customerPublicId));
        }

        if (customerType != null) {
            andPredicates.add(cb.equal(root.get("customerType"), customerType));
        }

        if (regionPublicId != null) {
            OrganizationRegions region = organizationRegionsRepository.findByPublicIdAndDeletedIsFalse(regionPublicId)
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Region not found")
                            .userMessage("Region not found"));
            andPredicates.add(cb.equal(root.get("organizationRegions"), region));
        }

        if (search != null && search.trim().length() >= 3) {
            String upperSearch = search.toUpperCase();

            List<Predicate> orPredicates = new ArrayList<>();
            orPredicates.add(cb.like(cb.upper(root.get("name")), "%" + upperSearch + "%"));
            orPredicates.add(cb.like(cb.upper(root.get("phoneNumber")), "%" + upperSearch + "%"));

            andPredicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
        }

        Subquery<Long> walletSubquery = cb.createQuery().subquery(Long.class);
        Root<Wallet> walletRoot = walletSubquery.from(Wallet.class);
        List<Predicate> walletPredicates = new ArrayList<>();
        walletPredicates.add(cb.equal(walletRoot.get("deleted"), Boolean.FALSE));
        walletPredicates.add(cb.equal(walletRoot.get("customer"), root));
        if (creditStatus != null) {
            walletPredicates.add(cb.equal(walletRoot.get("creditStatus"), creditStatus));
        }
        walletSubquery.select(walletRoot.get("id"));
        walletSubquery.where(cb.and(walletPredicates.toArray(new Predicate[0])));
        andPredicates.add(cb.exists(walletSubquery));

        return andPredicates;
    }

    public List<BNPLCustomerResponseDTO> convertToBNPLCustomerResponseDTO(List<Wallet> queryResultList) {
        return convertToBNPLCustomerResponseDTO(queryResultList, false);
    }

    private List<BNPLCustomerResponseDTO> convertToBNPLCustomerResponseDTO(List<Wallet> queryResultList, boolean includeChildCustomers) {
        List<BNPLCustomerResponseDTO> dtos = new ArrayList<>();
        for (Wallet wallet : queryResultList) {
            dtos.add(convertToBNPLCustomerResponseDTO(wallet, includeChildCustomers));
        }
        return dtos;

    }

    private BNPLCustomerResponseDTO convertToBNPLCustomerResponseDTO(Wallet wallet, boolean includeChildCustomers) {
        BNPLCustomerResponseDTO dto = new BNPLCustomerResponseDTO();
        Customers customer = wallet.getCustomer();
        dto.setPublicId(customer.getPublicId());
        dto.setCustomerName(customer.getName());
        dto.setCustomerType(String.valueOf(customer.getCustomerType()));
        dto.setPhoneNumber(customer.getPhoneNumber());
        dto.setRegistrationDate(customer.getOnBoardingDate());
        dto.setCreditLimit(customer.getCreditLimit());
        dto.setRegion(customer.getOrganizationRegions() != null ? customer.getOrganizationRegions().getName() : null);
        dto.setZone(customer.getOrganizationLocation() != null ? customer.getOrganizationLocation().getName() : null);
        dto.setCreditStatus(wallet.getCreditStatus());
        dto.setAov(wallet.getAverageOrderValue());
        dto.setLimitUtilizationPercentage(wallet.getLimitUsage());
        dto.setWalletBalance(wallet.getWalletBalance());

        Customers parentCustomer = customer.getParentCustomer();
        dto.setIsParent(parentCustomer == null);
        if (parentCustomer != null) {
            dto.setParentCustomerName(parentCustomer.getName());
            dto.setParentCustomerPublicId(parentCustomer.getPublicId());
        }

        if (includeChildCustomers) {
            List<BNPLCustomerResponseDTO> childCustomers = customersRepository.findAllByParentCustomerAndDeletedIsFalse(customer).stream()
                    .map(childCustomer -> walletRepository.findActiveByCustomerIdNative(childCustomer.getId()).orElse(null))
                    .filter(Objects::nonNull)
                    .map(childWallet -> convertToBNPLCustomerResponseDTO(childWallet, false))
                    .toList();
            if (!childCustomers.isEmpty()) {
                dto.setChildCustomers(childCustomers);
            }
        }

        return dto;
    }

    public Page<BNPLEligibleCustomerDTO> getEligibleCustomersForBNPLAccountCreation(String search, UUID customerPublicId, CustomerType customerType, UUID regionPublicId, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        LocalDateTime periodEnd = LocalDateTime.now(ZoneId.of("Africa/Nairobi"));
        // Main query
        CriteriaQuery<Customers> cq = cb.createQuery(Customers.class);
        Root<Customers> root = cq.from(Customers.class);
        cq.distinct(true);
        cq.where(cb.and(buildEligibleCustomerPredicates(cb, cq, root, search, customerPublicId, customerType, regionPublicId, periodEnd).toArray(new Predicate[0])));
        cq.orderBy(cb.desc(root.get("id")));

        TypedQuery<Customers> query = entityManager.createQuery(cq)
                .setMaxResults(pageable.getPageSize())
                .setFirstResult((int) pageable.getOffset());  // better than pageNumber * size

        List<Customers> queryResultList = query.getResultList();

// Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Customers> countRoot = countQuery.from(Customers.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(cb.and(buildEligibleCustomerPredicates(cb, countQuery, countRoot, search, customerPublicId, customerType, regionPublicId, periodEnd).toArray(new Predicate[0])));

        Long count = entityManager.createQuery(countQuery).getSingleResult();

        List<BNPLEligibleCustomerDTO> queryDTOList = getBNPLEligibleCustomerResponseDTO(queryResultList);
        return new PageImpl<>(queryDTOList, pageable, count);

    }


    private List<Predicate> buildEligibleCustomerPredicates(CriteriaBuilder cb, CriteriaQuery<?> query, Root<Customers> countRoot, String search, UUID customerPublicId, CustomerType customerType, UUID regionPublicId, LocalDateTime periodEnd) {
        List<Predicate> andPredicates = new ArrayList<>();

        andPredicates.add(cb.equal(countRoot.get("deleted"), Boolean.FALSE));
        andPredicates.add(CustomerSpecification
                .hasNotOnboardedCustomerWithThreeOrdersInEachRecentThirtyDayPeriod(periodEnd)
                .toPredicate(countRoot, query, cb));


        if (customerPublicId != null) {
            andPredicates.add(cb.equal(countRoot.get("publicId"), customerPublicId));
        }

        if (regionPublicId != null) {
            OrganizationRegions region = organizationRegionsRepository.findByPublicIdAndDeletedIsFalse(regionPublicId)
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Region not found")
                            .userMessage("Region not found"));
            andPredicates.add(cb.equal(countRoot.get("organizationRegions"), region));
        }


        if (search != null && search.trim().length() >= 3) {
            String upperSearch = search.toUpperCase();

            List<Predicate> orPredicates = new ArrayList<>();
            orPredicates.add(cb.like(cb.upper(countRoot.get("name")), "%" + upperSearch + "%"));
            orPredicates.add(cb.like(cb.upper(countRoot.get("phoneNumber")), "%" + upperSearch + "%"));
            orPredicates.add(cb.like(cb.upper(countRoot.get("storeName")), "%" + upperSearch + "%"));

            andPredicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
        }

        return andPredicates;
    }

    private List<BNPLEligibleCustomerDTO> getBNPLEligibleCustomerResponseDTO(List<Customers> queryResultList) {

        List<BNPLEligibleCustomerDTO> dtos = new ArrayList<>();
        for (Customers customer : queryResultList) {
            BNPLEligibleCustomerDTO dto = new BNPLEligibleCustomerDTO();

            BigDecimal aov = customer.getPreliminaryValue();


            BigDecimal creditLimit = getCreditLimitForCustomer(customer.getCustomerType(), aov);


            dto.setAov(aov);
            dto.setCreditLimit(creditLimit);
            dto.setPublicId(customer.getPublicId());
            dto.setCustomerName(customer.getName());
            dto.setCustomerPhoneNumber(customer.getPhoneNumber());
            dto.setCustomerType(customer.getCustomerType());
            dto.setCashPurchaser(true);
            if (customer.isSelfOnboarded()) {
                OnBoardingData onBoardingData = customer.getOnBoardingData();
                dto.setSelfOnboarded(true);
                dto.setCustomerID(onBoardingData.getCustomerID());
                dto.setTinNumber(onBoardingData.getTinNumber());
                dto.setBusinessLicenseNumber(onBoardingData.getBusinessLicenseNumber());
                dto.setGuarantorName(onBoardingData.getGuarantorName());
                dto.setGuarantorPhoneNumber(onBoardingData.getGuarantorPhoneNumber());
            }
            dtos.add(dto);

        }
        return dtos;
    }

    public void processCustomerCreditPayment(Wallet wallet, String orderNumber, Boolean payViaWallet, Boolean hasFile, String name) {

        if (payViaWallet == Boolean.TRUE) {
            // starts with ORD

            if (!orderNumber.startsWith("ORD")) {
                throw new BadRequestRestApiException()
                        .developerMessage("Invalid orderNumber format. orderNumber should start with 'ORD'")
                        .userMessage("Invalid orderNumber format. orderNumber should start with 'ORD'");
            }
            processCustomerCreditPaymentViaWallet(wallet, orderNumber, hasFile);
        }
    }


    private void processCustomerCreditPaymentViaWallet(Wallet wallet, String orderNumber, Boolean hasFile) {

        Optional<CreditLimitItem> optionalCreditLimitItem = creditLimitItemRepository.findByOrderNumberAndDeletedIsFalse(orderNumber);

        if (optionalCreditLimitItem.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("No active credit item found for the given order number")
                    .userMessage("No active credit item found for the given order number");
        }

        CreditLimitItem creditLimitItem = optionalCreditLimitItem.get();

        if (hasFile == Boolean.TRUE) {
            creditLimitItem.setPayViaWallet(Boolean.TRUE);
            creditLimitItemRepository.save(creditLimitItem);
        } else {
            processDepositToWallet(wallet, BigDecimal.ZERO, null, Boolean.TRUE, Boolean.FALSE, null);

        }

    }

    private BigDecimal calculateFine(CreditLimitItem creditLimitItem) {
        if (creditLimitItem == null || !creditLimitItem.isHasFine()) {
            return BigDecimal.ZERO;
        }

        BigDecimal finePercentage = Optional.ofNullable(creditLimitItem.getFinePercentage()).orElse(BigDecimal.ZERO);
        if (finePercentage.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal outstandingWithoutFine = Optional.ofNullable(creditLimitItem.getAmount()).orElse(BigDecimal.ZERO)
                .add(Optional.ofNullable(creditLimitItem.getInterestAmount()).orElse(BigDecimal.ZERO))
                .subtract(Optional.ofNullable(creditLimitItem.getPaidAmount()).orElse(BigDecimal.ZERO));

        if (outstandingWithoutFine.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return outstandingWithoutFine.multiply(finePercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public void recalculateBnplFine() {
        List<CreditLimitItem> overdueCreditItems = creditLimitItemRepository.findOverdueUnpaidCreditItemsNative();

        log.info("=========Found {} overdue credit items that require fine recalculation=========", overdueCreditItems.size());

        for (CreditLimitItem creditLimitItem : overdueCreditItems) {
            BigDecimal fineAmount = calculateFine(creditLimitItem);

            BigDecimal currentFine = Optional.ofNullable(creditLimitItem.getFineAmount())
                    .orElse(BigDecimal.ZERO);
            creditLimitItem.setFineAmount(currentFine.add(fineAmount));

//            creditLimitItem.setFineAmount(creditLimitItem.getFineAmount().add(fineAmount));
            creditLimitItemRepository.save(creditLimitItem);

            log.info("=========Recalculated fine for CreditLimitItem with Order NO: {}. New fine amount: {}=========", creditLimitItem.getOrderNumber(), creditLimitItem.getFineAmount());
        }
    }

    public void createCorporateWallet(Customers customer,boolean changeToRegular) {

        if (customer.isAwaitingCustomerTypeChange()) {
            customer.setCustomerType(changeToRegular?CustomerType.REGULAR:CustomerType.CORPORATE);
            customersRepository.save(customer);
        }
        if(changeToRegular){
            customer.setAwaitingCustomerTypeChange(false);
            customersRepository.save(customer);
        }

        customer.setBnplAccountCreated(true);
        customer.setOnBoardingDate(LocalDate.now());
        customer.setCreditLimit(customer.getPreliminaryValue());
        customer.setBnplVerificationStatus(CustomerBnplVerificationStatus.VERIFIED);
        customer.setAwaitingCustomerTypeChange(false);


        BNPLOnboarding bnplOnboarding = new BNPLOnboarding();

        bnplOnboarding.setCustomerPublicId(customer.getPublicId());
        bnplOnboarding.setCustomerID(customer.getOnBoardingData() != null ? customer.getOnBoardingData().getCustomerID() : null);
        bnplOnboarding.setTinNumber(customer.getTin());
        bnplOnboarding.setBusinessLicenseNumber(customer.getOnBoardingData() != null ? customer.getOnBoardingData().getBusinessLicenseNumber() : null);
        bnplOnboarding.setGuarantorName(customer.getOnBoardingData() != null ? customer.getOnBoardingData().getGuarantorName() : null);
        bnplOnboarding.setGuarantorPhoneNumber(customer.getOnBoardingData() != null ? customer.getOnBoardingData().getGuarantorPhoneNumber() : null);


        Customers saved = customersRepository.save(customer);

        this.createWalletForCustomer(saved, null, saved.getPreliminaryValue(), bnplOnboarding, saved.getCreditLimit());
        log.info("=========Created wallet for customer: {} with publicId: {}=========", customer.getName(), customer.getPublicId());
    }

    public void processCorporateCustomer(Customers savedCustomer,boolean changeToRegular) {
        // updates
        CorporateCustomerBnplApproval corporateCustomerBnplApproval = new CorporateCustomerBnplApproval();
        corporateCustomerBnplApproval.setCustomer(savedCustomer);
        corporateCustomerBnplApproval.setApprovalStatus(CorporateApprovalStatus.PENDING_APPROVAL);
        corporateCustomerBnplApproval.setCreatedBy(savedCustomer.getCreatedBy());
        corporateCustomerBnplApproval.setCreationTime(LocalDateTime.now());
        corporateCustomerBnplApproval.setChangeToRegular(changeToRegular);


        corporateCustomerBnplApprovalRepository.save(corporateCustomerBnplApproval);
    }

    @Transactional(readOnly = true)
    public Page<CorporateCustomerBnplApproval> getCorporateCustomerBnplApproval(String searchParam, CorporateApprovalStatus corporateApprovalStatus, Pageable pageable) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        // Main query
        CriteriaQuery<CorporateCustomerBnplApproval> cq = cb.createQuery(CorporateCustomerBnplApproval.class);
        Root<CorporateCustomerBnplApproval> root = cq.from(CorporateCustomerBnplApproval.class);
        cq.distinct(true);
        cq.where(cb.and(buildCorporateCustomerBnplApprovalPredicates(cb, root, searchParam, corporateApprovalStatus).toArray(new Predicate[0])));
        cq.orderBy(cb.desc(root.get("id")));
        TypedQuery<CorporateCustomerBnplApproval> query = entityManager.createQuery(cq)
                .setMaxResults(pageable.getPageSize())
                .setFirstResult((int) pageable.getOffset());
        List<CorporateCustomerBnplApproval> queryResultList = query.getResultList();
        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<CorporateCustomerBnplApproval> countRoot = countQuery.from(CorporateCustomerBnplApproval.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(cb.and(buildCorporateCustomerBnplApprovalPredicates(cb, countRoot, searchParam, corporateApprovalStatus).toArray(new Predicate[0])));
        Long count = entityManager.createQuery(countQuery).getSingleResult();
        return new PageImpl<>(queryResultList, pageable, count);
    }

    private List<Predicate> buildCorporateCustomerBnplApprovalPredicates(CriteriaBuilder cb, Root<CorporateCustomerBnplApproval> countRoot, String searchParam, CorporateApprovalStatus status) {
        List<Predicate> predicates = new java.util.ArrayList<>();

        if (StringUtils.isNotBlank(searchParam) && searchParam.trim().length() > 3) {
            String likePattern = "%" + searchParam.trim().toUpperCase() + "%";
            predicates.add(cb.like(cb.upper(countRoot.get("customer").get("name")), likePattern));
        }

        if (status != null) {
            predicates.add(cb.equal(countRoot.get("approvalStatus"), status));
        }

        return predicates;
    }

    public Page<WalletResponseDTO> getBNPLWallets(String search, CustomerType customerType, UUID customerPublicId, String startDate, String dueDate, CreditStatus creditStatus, Pageable pageable) {
        LocalDate start = null;
        LocalDate due = null;

        if (startDate != null && !startDate.isEmpty()) {
            start = LocalDate.parse(startDate);
        }
        if (dueDate != null && !dueDate.isEmpty()) {
            due = LocalDate.parse(dueDate);
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Wallet> cq = cb.createQuery(Wallet.class);
        Root<Wallet> root = cq.from(Wallet.class);
        cq.distinct(true);
        cq.where(cb.and(buildBNPLWalletPredicates(cb, root, search, customerType, customerPublicId, start, due, creditStatus).toArray(new Predicate[0])));
        cq.orderBy(cb.desc(root.get("id")));

        TypedQuery<Wallet> query = entityManager.createQuery(cq)
                .setMaxResults(pageable.getPageSize())
                .setFirstResult((int) pageable.getOffset());

        List<Wallet> queryResultList = query.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Wallet> countRoot = countQuery.from(Wallet.class);
        countQuery.select(cb.countDistinct(countRoot));
        countQuery.where(cb.and(buildBNPLWalletPredicates(cb, countRoot, search, customerType, customerPublicId, start, due, creditStatus).toArray(new Predicate[0])));

        Long count = entityManager.createQuery(countQuery).getSingleResult();

        return convertToWalletResponse(new PageImpl<>(queryResultList, pageable, count));
    }

    private List<Predicate> buildBNPLWalletPredicates(CriteriaBuilder cb, Root<Wallet> root,
                                                      String search, CustomerType customerType, UUID customerPublicId,
                                                      LocalDate start, LocalDate due, CreditStatus creditStatus) {
        List<Predicate> andPredicates = new ArrayList<>();

        // Explicitly join the customer relationship
        Join<Wallet, Customers> customerJoin = root.join("customer", JoinType.INNER);

        andPredicates.add(cb.equal(root.get("deleted"), Boolean.FALSE));

        if (customerType != null) {
            andPredicates.add(cb.equal(customerJoin.get("customerType"), customerType));
        }

        if (customerPublicId != null) {
            andPredicates.add(cb.equal(customerJoin.get("publicId"), customerPublicId));
        }

        if (creditStatus != null) {
            andPredicates.add(cb.equal(root.get("creditStatus"), creditStatus));
        }

        if (start != null && due != null && !start.isAfter(due)) {
            andPredicates.add(cb.between(customerJoin.get("onBoardingDate"), start, due));
        }

        if (search != null && search.trim().length() >= 3) {
            String upperSearch = search.trim().toUpperCase();
            List<Predicate> orPredicates = new ArrayList<>();
            orPredicates.add(cb.like(cb.upper(customerJoin.get("name")), "%" + upperSearch + "%"));
            orPredicates.add(cb.like(cb.upper(customerJoin.get("phoneNumber")), "%" + upperSearch + "%"));
            andPredicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
        }

        return andPredicates;
    }

    public void processCreditPaymentReversal(CreditLimitItem creditLimitItem, RejectionsCode reason, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person not found with email: " + name)
                    .userMessage("Person not found");
        }

        Person person = optionalPerson.get();

        List<PaymentItem> paymentItems = paymentItemRepository.findByOrderNumberIgnoreCaseAndStatusAndDeletedIsFalse(creditLimitItem.getOrderNumber(), WalletItemStatus.PAID);

        if (paymentItems != null && !paymentItems.isEmpty()) {
            for (PaymentItem paymentItem : paymentItems) {

                String description = String.format("Reversal of payment item with ID: %s for order number: %s due to reason: %s",
                        paymentItem.getPublicId(),
                        paymentItem.getOrderNumber(),
                        reason != null ? reason.getDescription() : "No reason provided");

                paymentItem.setStatus(WalletItemStatus.REVERSED);
                paymentItem.setUpdatedBy(person);
                paymentItem.setUpdateDate(LocalDateTime.now());
                paymentItem.setDescription(description);
                paymentItemRepository.save(paymentItem);
            }
        }

        creditLimitItem.setBalanceAmount(creditLimitItem.getBalanceAmount().add(creditLimitItem.getPaidAmount()));
        creditLimitItem.setPaidAmount(BigDecimal.ZERO);
        creditLimitItem.setAmountPaidFromWallet(BigDecimal.ZERO);
        creditLimitItem.setWalletBalance(creditLimitItem.getWalletBalance().add(creditLimitItem.getAmountPaidFromWallet()));
        creditLimitItem.setPaymentStatus(PaymentStatusEnum.NOT_PAID);
        creditLimitItem.setUpdatedBy(person);
        creditLimitItem.setUpdateDate(LocalDateTime.now());
        CreditLimitItem saved = creditLimitItemRepository.save(creditLimitItem);

        // update order.transaction history with reversal info

        TransactionItem transactionItem = new TransactionItem();
        // reversal transaction
        transactionItem.setAmount(creditLimitItem.getAmountPaidFromWallet());
        transactionItem.setTransactionType(TransactionType.REVERSAL);
        transactionItem.setOrderNumber(creditLimitItem.getOrderNumber());
        transactionItem.setDescription(String.format("Reversal of payment for credit limit item with order number: %s due to reason: %s", creditLimitItem.getOrderNumber(), reason != null ? reason.getDescription() : "No reason provided"));
        transactionItem.setCreatedBy(person);
        transactionItem.setCreationTime(LocalDateTime.now());

        transactionItem.setUpdatedBy(person);
        transactionItem.setUpdateDate(LocalDateTime.now());

        transactionItemRepository.save(transactionItem);


        Optional<CreditRecon> optionalCreditRecon = creditReconRepository.findCreditReconByOrderPublicIdAndDeletedIsFalse(creditLimitItem.getOrderPublicId());

        if (optionalCreditRecon.isPresent()) {
            CreditRecon creditRecon = optionalCreditRecon.get();
            creditRecon.setPaymentStatus(PaymentStatusEnum.NOT_PAID);
            creditRecon.setPaidAmount(BigDecimal.ZERO);
            creditRecon.setPaidDateTime(null);
            creditRecon.setBalanceAmount(saved.getBalanceAmount());
            creditRecon.setDepositAmount(BigDecimal.ZERO);

            creditReconRepository.save(creditRecon);
        }


        Optional<SalesOrders> optionalSalesOrder = salesOrdersRepository.findBySalesOrderNumberAndDeletedIsFalse(creditLimitItem.getOrderNumber());

        if (optionalSalesOrder.isPresent()) {
            SalesOrders order = optionalSalesOrder.get();
            order.setPaymentStatus(PaymentStatusEnum.NOT_PAID);
            salesOrdersRepository.save(order);
        }


    }

    public void processCreditPaymentFromWallet(CreditLimitItem creditLimitItem, BigDecimal amount, Person person) {
        Wallet fundingWallet = resolveFundingWallet(creditLimitItem.getWallet());

        creditLimitItem.setPaidAmount(creditLimitItem.getPaidAmount().add(amount));
        creditLimitItem.setBalanceAmount(creditLimitItem.getBalanceAmount().subtract(amount));
        creditLimitItem.setAmountPaidFromWallet(creditLimitItem.getAmountPaidFromWallet().add(amount));
        creditLimitItem.setWalletBalance(ObjectUtils.defaultIfNull(fundingWallet.getWalletBalance(), BigDecimal.ZERO).subtract(amount));

        CreditLimitItem saved = creditLimitItemRepository.save(creditLimitItem);

        BigDecimal allAmount = saved.getAmount().add(saved.getInterestAmount())
                .add(Optional.ofNullable(saved.getFineAmount()).orElse(BigDecimal.ZERO));

        if (saved.getPaidAmount().compareTo(allAmount) >= 0) {
            saved.setPaymentStatus(PaymentStatusEnum.PAID);
            saved.setFullyPaid(true);

        } else {
            saved.setPaymentStatus(PaymentStatusEnum.PARTIALLY_PAID);
        }
        CreditLimitItem item = creditLimitItemRepository.save(saved);

        BigDecimal customerCreditLimit = ObjectUtils.defaultIfNull(fundingWallet.getCustomer().getCreditLimit(), BigDecimal.ZERO);
        BigDecimal settledAmount = item.isFullyPaid() ? item.getAmount() : item.getPaidAmount();

        fundingWallet.setWalletBalance(ObjectUtils.defaultIfNull(fundingWallet.getWalletBalance(), BigDecimal.ZERO).subtract(amount));
        fundingWallet.setExcessAmountPaid(subtractWithoutNegative(fundingWallet.getExcessAmountPaid(), amount));
        fundingWallet.setCreditLimitBalance(item.isFullyPaid() ? customerCreditLimit : customerCreditLimit.subtract(settledAmount));
        walletRepository.save(fundingWallet);

        TransactionItem transactionItem = new TransactionItem();
        // reversal transaction
        transactionItem.setAmount(saved.getAmountPaidFromWallet());
        transactionItem.setTransactionType(TransactionType.DEPOSIT);
        transactionItem.setOrderNumber(creditLimitItem.getOrderNumber());
        transactionItem.setDescription(String.format("Deposit of %s from wallet for credit limit item with order number: %s from wallet", amount.toString(), creditLimitItem.getOrderNumber()));
        transactionItem.setCreatedBy(person);
        transactionItem.setCreationTime(LocalDateTime.now());

        transactionItem.setUpdatedBy(person);
        transactionItem.setUpdateDate(LocalDateTime.now());

        transactionItemRepository.save(transactionItem);


        PaymentItem paymentItem = new PaymentItem();
        paymentItem.setWallet(saved.getWallet());
        paymentItem.setAmountPaid(amount);
        paymentItem.setDescription("Payment made for credit item on order " + saved.getOrderNumber());
        paymentItem.setOrderNumber(saved.getOrderNumber());
        paymentItem.setReferenceId(saved.getReferenceId());
        paymentItem.setPaymentMethod("BNPL Wallet Deposit from Wallet");
        paymentItem.setPaymentReference("PMT-" + counterService.getNextCounter(CounterType.CREDIT_PAYMENT));
        paymentItem.setStatus(WalletItemStatus.PAID);
        paymentItem.setInterestRate(saved.getInterestRate());
        paymentItem.setTotalInterest(saved.getInterestAmount());
        paymentItem.setReceiptNumber("RCPT-" + counterService.getNextCounter(CounterType.CREDIT_PAYMENT_RECEIPT));
        paymentItem.setAmountPaidFromWallet(amount);
        paymentItem.setApprovedBy(person.getFullName());
        paymentItem.setExcessAmountPaid(saved.getExcessAmountPaid());

        paymentItemRepository.save(paymentItem);

        creditReconHelper.updateReconAmount(item, item.getOrderNumber());


        if (item.isFullyPaid()) {
            updateOrderPaymentStatusToPaid(item.getOrderPublicId(), item.getOrderNumber());
        }

    }

    private Wallet resolveFundingWallet(Wallet wallet) {
        if (wallet != null && wallet.getParentWallet() != null) {
            return wallet.getParentWallet();
        }
        return wallet;
    }


    public void createWalletForChildCustomer(Customers childCustomer, Person person, Wallet parentWallet) {

        log.info("==========Creating wallet for customer: {}==========", childCustomer.getName());


        /// new customer automatically aov and limit is zero.
        BigDecimal aov = BigDecimal.ZERO;

        BigDecimal creditLimit = BigDecimal.ZERO;

        Wallet wallet = new Wallet();
        wallet.setCustomer(childCustomer);
        wallet.setCreatedBy(person);
        wallet.setCreationTime(LocalDateTime.now());
        wallet.setCreditStatus(CreditStatus.Healthy);
        wallet.setCreditLimitBalance(creditLimit);
        wallet.setAverageOrderValue(aov);
        wallet.setCustomerID(parentWallet.getCustomerID());
        wallet.setTinNumber(parentWallet.getTinNumber());
        wallet.setBusinessLicenseNumber(parentWallet.getBusinessLicenseNumber());
        wallet.setGuarantorName(parentWallet.getGuarantorName());
        wallet.setGuarantorPhoneNumber(parentWallet.getGuarantorPhoneNumber());
        wallet.setParentWallet(parentWallet);
        walletRepository.save(wallet);

        log.info("==Wallet has been created successfully for customer: {}", childCustomer.getName());

        // update child customer

        childCustomer.setBnplAccountCreated(true);
        childCustomer.setOnBoardingDate(LocalDate.now());
        childCustomer.setCreditLimit(aov);
        childCustomer.setBnplVerificationStatus(CustomerBnplVerificationStatus.VERIFIED);
        childCustomer.setAwaitingCustomerTypeChange(false);

        customersRepository.save(childCustomer);

    }
}
