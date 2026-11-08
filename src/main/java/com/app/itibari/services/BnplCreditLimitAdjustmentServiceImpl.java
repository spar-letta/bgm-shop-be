package com.app.itibari.services;

import com.app.itibari.dataTypes.BnplCreditLimitAdjustmentType;
import com.app.itibari.dataTypes.PaymentStatusEnum;
import com.app.itibari.models.entities.BnplCreditLimitAdjustment;
import com.app.itibari.models.entities.CreditLimitItem;
import com.app.itibari.models.entities.Customers;
import com.app.itibari.models.entities.Wallet;
import com.app.itibari.models.response.BnplCreditLimitAdjustmentResponse;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.repository.BnplCreditLimitAdjustmentRepository;
import com.app.itibari.repository.CreditLimitItemRepository;
import com.app.itibari.repository.CustomersRepository;
import com.app.itibari.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BnplCreditLimitAdjustmentServiceImpl implements BnplCreditLimitAdjustmentService {

    private static final BigDecimal ADJUSTMENT_PERCENTAGE = new BigDecimal("20");
    private static final BigDecimal DEDUCTION_FACTOR = new BigDecimal("0.80");
    private static final BigDecimal INCREASE_FACTOR = new BigDecimal("1.20");
    private static final int REQUIRED_STREAK_SIZE = 3;

    private final WalletRepository walletRepository;
    private final CustomersRepository customersRepository;
    private final CreditLimitItemRepository creditLimitItemRepository;
    private final BnplCreditLimitAdjustmentRepository adjustmentRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<BnplCreditLimitAdjustmentResponse> fetchAdjustments(LocalDate startDate, LocalDate endDate,
                                                                  UUID customerPublicId, String searchParam,
                                                                  Pageable pageable) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestRestApiException()
                    .developerMessage("startDate must be on or before endDate")
                    .userMessage("Start date must be on or before end date");
        }
        String search = searchParam == null || searchParam.isBlank()
                ? null : "%" + searchParam.trim().toLowerCase(Locale.ROOT) + "%";
        return adjustmentRepository.findAdjustments(
                        startDate == null ? null : startDate.atStartOfDay(),
                        endDate == null ? null : endDate.plusDays(1).atStartOfDay(),
                        customerPublicId, search, pageable)
                .map(this::toResponse);
    }

    private BnplCreditLimitAdjustmentResponse toResponse(BnplCreditLimitAdjustment adjustment) {
        Customers customer = adjustment.getCustomer();
        Wallet wallet = adjustment.getWallet();
        return BnplCreditLimitAdjustmentResponse.builder()
                .publicId(adjustment.getPublicId())
                .customerPublicId(customer == null ? null : customer.getPublicId())
                .customerName(customer == null ? null : customer.getName())
                .customerPhoneNumber(customer == null ? null : customer.getPhoneNumber())
                .walletPublicId(wallet == null ? null : wallet.getPublicId())
                .adjustmentType(adjustment.getAdjustmentType())
                .percentage(adjustment.getPercentage())
                .oldCreditLimit(adjustment.getOldCreditLimit())
                .newCreditLimit(adjustment.getNewCreditLimit())
                .oldCreditLimitBalance(adjustment.getOldCreditLimitBalance())
                .newCreditLimitBalance(adjustment.getNewCreditLimitBalance())
                .periodStart(adjustment.getPeriodStart())
                .periodEnd(adjustment.getPeriodEnd())
                .reason(adjustment.getReason())
                .creationTime(adjustment.getCreationTime())
                .build();
    }

    @Override
    @Transactional
    public int adjustCreditLimits() {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate periodStart = currentMonth.minusMonths(2).atDay(1);
        LocalDate periodEnd = currentMonth.atEndOfMonth();

        List<Wallet> wallets = walletRepository.findRootBnplWallets();

        int adjusted = 0;
        for (Wallet wallet : wallets) {
            if (adjustCreditLimit(wallet, periodStart, periodEnd, today)) {
                adjusted++;
            }
        }

        log.info("Adjusted BNPL credit limits for {} wallet(s)", adjusted);
        return adjusted;
    }

    private boolean adjustCreditLimit(Wallet wallet, LocalDate periodStart, LocalDate periodEnd, LocalDate today) {
        List<CreditLimitItem> recentItems = creditLimitItemRepository
                .findRecentCreditItemsForFundingWallet(wallet, periodStart, today, PageRequest.of(0, REQUIRED_STREAK_SIZE))
                .getContent();

        if (recentItems.size() < REQUIRED_STREAK_SIZE) {
            return false;
        }

        BnplCreditLimitAdjustmentType adjustmentType = resolveAdjustmentType(recentItems, today);
        if (adjustmentType == null) {
            return false;
        }

        Customers customer = wallet.getCustomer();
        if (adjustmentRepository.existsByCustomerAndAdjustmentTypeAndPeriodStartAndPeriodEndAndDeletedIsFalse(
                customer, adjustmentType, periodStart, periodEnd)) {
            return false;
        }

        BigDecimal oldCreditLimit = ObjectUtils.defaultIfNull(customer.getCreditLimit(), BigDecimal.ZERO);
        if (oldCreditLimit.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        BigDecimal newCreditLimit = oldCreditLimit
                .multiply(adjustmentType == BnplCreditLimitAdjustmentType.DEDUCTION ? DEDUCTION_FACTOR : INCREASE_FACTOR)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal oldCreditLimitBalance = ObjectUtils.defaultIfNull(wallet.getCreditLimitBalance(), BigDecimal.ZERO);
        BigDecimal balanceDelta = newCreditLimit.subtract(oldCreditLimit);
        BigDecimal newCreditLimitBalance = oldCreditLimitBalance.add(balanceDelta).max(BigDecimal.ZERO);

        customer.setCreditLimit(newCreditLimit);
        wallet.setCreditLimitBalance(newCreditLimitBalance);
        customersRepository.save(customer);
        walletRepository.save(wallet);

        adjustmentRepository.save(buildAdjustment(
                customer,
                wallet,
                adjustmentType,
                oldCreditLimit,
                newCreditLimit,
                oldCreditLimitBalance,
                newCreditLimitBalance,
                periodStart,
                periodEnd,
                recentItems));

        log.info("BNPL credit limit {} applied for customer {} from {} to {}",
                adjustmentType, customer.getPublicId(), oldCreditLimit, newCreditLimit);
        return true;
    }

    private BnplCreditLimitAdjustmentType resolveAdjustmentType(List<CreditLimitItem> recentItems, LocalDate today) {
        boolean allDefaulted = recentItems.stream().allMatch(item -> isDefaulted(item, today));
        if (allDefaulted) {
            return BnplCreditLimitAdjustmentType.DEDUCTION;
        }

        boolean allHealthy = recentItems.stream().allMatch(this::isHealthyRepayment);
        if (allHealthy) {
            return BnplCreditLimitAdjustmentType.INCREASE;
        }

        return null;
    }

    private boolean isDefaulted(CreditLimitItem item, LocalDate today) {
        return item.getDueDate() != null
                && item.getDueDate().isBefore(today)
                && !item.isFullyPaid()
                && (item.getPaymentStatus() == PaymentStatusEnum.NOT_PAID
                || item.getPaymentStatus() == PaymentStatusEnum.PARTIALLY_PAID);
    }

    private boolean isHealthyRepayment(CreditLimitItem item) {
        return item.isFullyPaid() || item.getPaymentStatus() == PaymentStatusEnum.PAID;
    }

    private BnplCreditLimitAdjustment buildAdjustment(Customers customer,
                                                     Wallet wallet,
                                                     BnplCreditLimitAdjustmentType adjustmentType,
                                                     BigDecimal oldCreditLimit,
                                                     BigDecimal newCreditLimit,
                                                     BigDecimal oldCreditLimitBalance,
                                                     BigDecimal newCreditLimitBalance,
                                                     LocalDate periodStart,
                                                     LocalDate periodEnd,
                                                     List<CreditLimitItem> recentItems) {
        BnplCreditLimitAdjustment adjustment = new BnplCreditLimitAdjustment();
        adjustment.setCustomer(customer);
        adjustment.setWallet(wallet);
        adjustment.setAdjustmentType(adjustmentType);
        adjustment.setPercentage(ADJUSTMENT_PERCENTAGE);
        adjustment.setOldCreditLimit(oldCreditLimit);
        adjustment.setNewCreditLimit(newCreditLimit);
        adjustment.setOldCreditLimitBalance(oldCreditLimitBalance);
        adjustment.setNewCreditLimitBalance(newCreditLimitBalance);
        adjustment.setPeriodStart(periodStart);
        adjustment.setPeriodEnd(periodEnd);
        adjustment.setTriggeredByCreditItemIds(recentItems.stream()
                .map(CreditLimitItem::getPublicId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining(",")));
        adjustment.setReason(adjustmentType == BnplCreditLimitAdjustmentType.DEDUCTION
                ? "Three consecutive BNPL defaults in the current three-month assessment period"
                : "Three consecutive BNPL repayments without default in the current three-month assessment period");
        return adjustment;
    }
}
