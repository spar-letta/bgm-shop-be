package com.app.itibari.services;

import com.app.itibari.dataTypes.BnplCreditLimitAdjustmentType;
import com.app.itibari.dataTypes.PaymentStatusEnum;
import com.app.itibari.models.entities.BnplCreditLimitAdjustment;
import com.app.itibari.models.entities.CreditLimitItem;
import com.app.itibari.models.entities.Customers;
import com.app.itibari.models.entities.Wallet;
import com.app.itibari.repository.BnplCreditLimitAdjustmentRepository;
import com.app.itibari.repository.CreditLimitItemRepository;
import com.app.itibari.repository.CustomersRepository;
import com.app.itibari.repository.WalletRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BnplCreditLimitAdjustmentServiceImplTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private CustomersRepository customersRepository;
    @Mock
    private CreditLimitItemRepository creditLimitItemRepository;
    @Mock
    private BnplCreditLimitAdjustmentRepository adjustmentRepository;

    @InjectMocks
    private BnplCreditLimitAdjustmentServiceImpl service;

    @Test
    public void fetchAdjustmentsIncludesWholeEndDateAndReturnsPublicReferences() {
        LocalDate date = LocalDate.of(2026, 9, 14);
        Wallet wallet = wallet(new BigDecimal("1000"), new BigDecimal("600"));
        wallet.setPublicId(UUID.randomUUID());
        BnplCreditLimitAdjustment adjustment = new BnplCreditLimitAdjustment();
        adjustment.setPublicId(UUID.randomUUID());
        adjustment.setCustomer(wallet.getCustomer());
        adjustment.setWallet(wallet);
        adjustment.setNewCreditLimit(new BigDecimal("1200"));
        PageRequest pageable = PageRequest.of(1, 5);
        when(adjustmentRepository.findAdjustments(date.atStartOfDay(), date.plusDays(1).atStartOfDay(),
                wallet.getCustomer().getPublicId(), "%john%", pageable))
                .thenReturn(new PageImpl<>(List.of(adjustment), pageable, 6));

        var result = service.fetchAdjustments(date, date, wallet.getCustomer().getPublicId(), " John ", pageable);

        assertEquals(6, result.getTotalElements());
        assertEquals(1, result.getNumber());
        assertEquals(adjustment.getPublicId(), result.getContent().get(0).getPublicId());
        assertEquals(wallet.getCustomer().getPublicId(), result.getContent().get(0).getCustomerPublicId());
        assertEquals(wallet.getPublicId(), result.getContent().get(0).getWalletPublicId());
        assertEquals(new BigDecimal("1200"), result.getContent().get(0).getNewCreditLimit());
    }

    @Test
    public void fetchAdjustmentsAllowsMissingFiltersAndBlankSearch() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(adjustmentRepository.findAdjustments(null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        assertEquals(0, service.fetchAdjustments(null, null, null, "  ", pageable).getTotalElements());
    }

    @Test
    public void fetchAdjustmentsRejectsReversedDateRange() {
        org.junit.Assert.assertThrows(
                com.app.itibari.utils.exceptions.rest.BadRequestRestApiException.class,
                () -> service.fetchAdjustments(LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 14),
                        null, null, PageRequest.of(0, 20)));
        verifyNoInteractions(adjustmentRepository);
    }

    @Test
    public void adjustCreditLimits_deductsTwentyPercentForThreeConsecutiveDefaults() {
        Wallet wallet = wallet(new BigDecimal("1000.00"), new BigDecimal("600.00"));
        List<CreditLimitItem> items = List.of(
                defaultedItem(1L),
                defaultedItem(2L),
                defaultedItem(3L)
        );

        when(walletRepository.findRootBnplWallets()).thenReturn(List.of(wallet));
        when(creditLimitItemRepository.findRecentCreditItemsForFundingWallet(eq(wallet), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(items));
        when(adjustmentRepository.existsByCustomerAndAdjustmentTypeAndPeriodStartAndPeriodEndAndDeletedIsFalse(
                eq(wallet.getCustomer()), eq(BnplCreditLimitAdjustmentType.DEDUCTION), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(false);
        when(customersRepository.save(any(Customers.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(adjustmentRepository.save(any(BnplCreditLimitAdjustment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int adjusted = service.adjustCreditLimits();

        assertEquals(1, adjusted);
        assertEquals(new BigDecimal("800.00"), wallet.getCustomer().getCreditLimit());
        assertEquals(new BigDecimal("400.00"), wallet.getCreditLimitBalance());

        ArgumentCaptor<BnplCreditLimitAdjustment> adjustmentCaptor = ArgumentCaptor.forClass(BnplCreditLimitAdjustment.class);
        verify(adjustmentRepository).save(adjustmentCaptor.capture());
        assertEquals(BnplCreditLimitAdjustmentType.DEDUCTION, adjustmentCaptor.getValue().getAdjustmentType());
        assertEquals(new BigDecimal("1000.00"), adjustmentCaptor.getValue().getOldCreditLimit());
        assertEquals(new BigDecimal("800.00"), adjustmentCaptor.getValue().getNewCreditLimit());
    }

    @Test
    public void adjustCreditLimits_increasesTwentyPercentForThreeConsecutiveHealthyRepayments() {
        Wallet wallet = wallet(new BigDecimal("1000.00"), new BigDecimal("600.00"));
        List<CreditLimitItem> items = List.of(
                paidItem(1L),
                paidItem(2L),
                paidItem(3L)
        );

        when(walletRepository.findRootBnplWallets()).thenReturn(List.of(wallet));
        when(creditLimitItemRepository.findRecentCreditItemsForFundingWallet(eq(wallet), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(items));
        when(adjustmentRepository.existsByCustomerAndAdjustmentTypeAndPeriodStartAndPeriodEndAndDeletedIsFalse(
                eq(wallet.getCustomer()), eq(BnplCreditLimitAdjustmentType.INCREASE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(false);
        when(customersRepository.save(any(Customers.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(adjustmentRepository.save(any(BnplCreditLimitAdjustment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int adjusted = service.adjustCreditLimits();

        assertEquals(1, adjusted);
        assertEquals(new BigDecimal("1200.00"), wallet.getCustomer().getCreditLimit());
        assertEquals(new BigDecimal("800.00"), wallet.getCreditLimitBalance());
    }

    @Test
    public void adjustCreditLimits_doesNotReapplyAdjustmentInSameThreeMonthPeriod() {
        Wallet wallet = wallet(new BigDecimal("1000.00"), new BigDecimal("600.00"));
        LocalDate periodStart = YearMonth.now().minusMonths(2).atDay(1);
        LocalDate periodEnd = YearMonth.now().atEndOfMonth();

        when(walletRepository.findRootBnplWallets()).thenReturn(List.of(wallet));
        when(creditLimitItemRepository.findRecentCreditItemsForFundingWallet(eq(wallet), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(defaultedItem(1L), defaultedItem(2L), defaultedItem(3L))));
        when(adjustmentRepository.existsByCustomerAndAdjustmentTypeAndPeriodStartAndPeriodEndAndDeletedIsFalse(
                wallet.getCustomer(), BnplCreditLimitAdjustmentType.DEDUCTION, periodStart, periodEnd))
                .thenReturn(true);

        int adjusted = service.adjustCreditLimits();

        assertEquals(0, adjusted);
        assertEquals(new BigDecimal("1000.00"), wallet.getCustomer().getCreditLimit());
        assertEquals(new BigDecimal("600.00"), wallet.getCreditLimitBalance());
        verify(adjustmentRepository, never()).save(any(BnplCreditLimitAdjustment.class));
    }

    @Test
    public void adjustCreditLimits_doesNothingForMixedStreak() {
        Wallet wallet = wallet(new BigDecimal("1000.00"), new BigDecimal("600.00"));

        when(walletRepository.findRootBnplWallets()).thenReturn(List.of(wallet));
        when(creditLimitItemRepository.findRecentCreditItemsForFundingWallet(eq(wallet), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(defaultedItem(1L), paidItem(2L), defaultedItem(3L))));

        int adjusted = service.adjustCreditLimits();

        assertEquals(0, adjusted);
        verify(customersRepository, never()).save(any(Customers.class));
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(adjustmentRepository, never()).save(any(BnplCreditLimitAdjustment.class));
    }

    private Wallet wallet(BigDecimal creditLimit, BigDecimal creditLimitBalance) {
        Customers customer = new Customers();
        customer.setId(10L);
        customer.setPublicId(UUID.randomUUID());
        customer.setBnplAccountCreated(true);
        customer.setCreditLimit(creditLimit);

        Wallet wallet = new Wallet();
        wallet.setId(20L);
        wallet.setPublicId(UUID.randomUUID());
        wallet.setCustomer(customer);
        wallet.setCreditLimitBalance(creditLimitBalance);
        return wallet;
    }

    private CreditLimitItem defaultedItem(Long id) {
        CreditLimitItem item = creditItem(id);
        item.setDueDate(LocalDate.now().minusDays(id));
        item.setPaymentStatus(PaymentStatusEnum.NOT_PAID);
        item.setFullyPaid(false);
        return item;
    }

    private CreditLimitItem paidItem(Long id) {
        CreditLimitItem item = creditItem(id);
        item.setDueDate(LocalDate.now().minusDays(id));
        item.setPaymentStatus(PaymentStatusEnum.PAID);
        item.setFullyPaid(true);
        return item;
    }

    private CreditLimitItem creditItem(Long id) {
        CreditLimitItem item = new CreditLimitItem();
        item.setId(id);
        item.setPublicId(UUID.randomUUID());
        return item;
    }
}
