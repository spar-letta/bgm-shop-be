package com.app.itibari.services.helper;

import com.app.itibari.dataTypes.CreditStatus;
import com.app.itibari.dataTypes.PaymentStatusEnum;
import com.app.itibari.dataTypes.WalletItemStatus;
import com.app.itibari.models.entities.CreditLimitItem;
import com.app.itibari.models.entities.Customers;
import com.app.itibari.models.entities.PaymentItem;
import com.app.itibari.models.entities.Wallet;
import com.app.itibari.repository.*;
import com.app.itibari.services.CounterService;
import com.app.itibari.services.ItibariSmsService;
import com.app.itibari.utils.helpers.CreditReconHelper;
import com.app.itibari.utils.helpers.WebhookClient;
import jakarta.persistence.EntityManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BnplHelperTest {

    @Mock
    private OrganizationRegionsRepository organizationRegionsRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private SalesOrdersRepository salesOrdersRepository;
    @Mock
    private CreditRuleRepository creditRuleRepository;
    @Mock
    private PersonRepository personRepository;
    @Mock
    private CreditLimitItemRepository creditLimitItemRepository;
    @Mock
    private TransactionItemRepository transactionItemRepository;
    @Mock
    private PaymentItemRepository paymentItemRepository;
    @Mock
    private CounterService counterService;
    @Mock
    private CustomersRepository customersRepository;
    @Mock
    private FileDataRepository fileDataRepository;
    @Mock
    private EntityManager entityManager;
    @Mock
    private ItibariSmsService itibariSmsService;
    @Mock
    private CreditReconHelper creditReconHelper;
    @Mock
    private CorporateCustomerBnplApprovalRepository corporateCustomerBnplApprovalRepository;
    @Mock
    private NotificationHelper notificationHelper;
    @Mock
    private EventsRepository eventsRepository;
    @Mock
    private CreditReconRepository creditReconRepository;
    @Mock
    private WebhookClient webhookClient;

    @InjectMocks
    private BnplHelper bnplHelper;

    @Test
    public void processDepositToWallet_marksPaid_whenPartialPaymentsReachTotal() {
        Customers customer = new Customers();
        customer.setPublicId(UUID.randomUUID());
        customer.setCreditLimit(new BigDecimal("1000"));

        Wallet wallet = new Wallet();
        wallet.setCustomer(customer);
        wallet.setWalletBalance(new BigDecimal("200"));
        wallet.setCreditLimitBalance(new BigDecimal("500"));

        CreditLimitItem creditLimitItem = new CreditLimitItem();
        creditLimitItem.setPublicId(UUID.randomUUID());
        creditLimitItem.setWallet(wallet);
        creditLimitItem.setOrderNumber("ORD-1001");
        creditLimitItem.setAmount(new BigDecimal("100"));
        creditLimitItem.setInterestAmount(BigDecimal.ZERO);
        creditLimitItem.setFineAmount(BigDecimal.ZERO);
        creditLimitItem.setPaidAmount(BigDecimal.ZERO);
        creditLimitItem.setPaymentStatus(PaymentStatusEnum.PARTIALLY_PAID);
        creditLimitItem.setDueDate(LocalDate.now().minusDays(1));
        creditLimitItem.setCreditStatus(CreditStatus.Healthy);

        PaymentItem priorPayment = new PaymentItem();
        priorPayment.setAmountPaid(new BigDecimal("30"));
        priorPayment.setAmountPaidFromWallet(new BigDecimal("20"));

        when(creditLimitItemRepository.findPendingCreditItemsNative(
                anyString(), eq(WalletItemStatus.ACTIVE.name()),
                anyList()
        )).thenReturn(List.of(creditLimitItem));

        when(paymentItemRepository.findByOrderNumberIgnoreCaseAndStatusAndDeletedIsFalse("ORD-1001", WalletItemStatus.PAID))
                .thenReturn(List.of(priorPayment));

        when(creditLimitItemRepository.save(any(CreditLimitItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentItemRepository.save(any(PaymentItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(counterService.getNextCounter(any())).thenReturn(Math.toIntExact(1L));
        when(salesOrdersRepository.findBySalesOrderNumberAndDeletedIsFalse("ORD-1001"))
                .thenReturn(Optional.empty());

        bnplHelper.processDepositToWallet(
                wallet,
                new BigDecimal("50"),
                null,
                false,
                false,
                "tester@itibari.io"
        );

        ArgumentCaptor<CreditLimitItem> creditCaptor = ArgumentCaptor.forClass(CreditLimitItem.class);
        verify(creditLimitItemRepository, atLeastOnce()).save(creditCaptor.capture());
        CreditLimitItem finalSaved = creditCaptor.getValue();

        assertEquals(PaymentStatusEnum.PAID, finalSaved.getPaymentStatus());
        assertTrue(finalSaved.isFullyPaid());
        assertEquals(new BigDecimal("100"), finalSaved.getPaidAmount());
        assertEquals(BigDecimal.ZERO, finalSaved.getBalanceAmount());

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository, atLeastOnce()).save(walletCaptor.capture());
        Wallet finalWallet = walletCaptor.getValue();
        assertEquals(new BigDecimal("1000"), finalWallet.getCreditLimitBalance());

        verify(creditReconHelper).updateReconAmount(any(CreditLimitItem.class), eq("ORD-1001"));
    }

    @Test
    public void processDepositToWallet_addsOverpaymentToWalletExcessAmountPaid() {
        Customers customer = new Customers();
        customer.setPublicId(UUID.randomUUID());
        customer.setCreditLimit(new BigDecimal("1000"));

        Wallet wallet = new Wallet();
        wallet.setCustomer(customer);
        wallet.setWalletBalance(new BigDecimal("200"));
        wallet.setExcessAmountPaid(new BigDecimal("200"));
        wallet.setCreditLimitBalance(new BigDecimal("500"));

        CreditLimitItem creditLimitItem = creditLimitItem("ORD-OVERPAY", wallet, new BigDecimal("100"));

        when(creditLimitItemRepository.findPendingCreditItemsNative(anyString(), eq(WalletItemStatus.ACTIVE.name()), anyList()))
                .thenReturn(List.of(creditLimitItem));
        when(paymentItemRepository.findByOrderNumberIgnoreCaseAndStatusAndDeletedIsFalse("ORD-OVERPAY", WalletItemStatus.PAID))
                .thenReturn(List.of());
        when(creditLimitItemRepository.save(any(CreditLimitItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentItemRepository.save(any(PaymentItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(counterService.getNextCounter(any())).thenReturn(Math.toIntExact(1L));
        when(salesOrdersRepository.findBySalesOrderNumberAndDeletedIsFalse("ORD-OVERPAY"))
                .thenReturn(Optional.empty());

        bnplHelper.processDepositToWallet(wallet, new BigDecimal("130"), null, false, false, "tester@itibari.io");

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        Wallet savedWallet = walletCaptor.getValue();

        assertEquals(new BigDecimal("230"), savedWallet.getWalletBalance());
        assertEquals(new BigDecimal("230"), savedWallet.getExcessAmountPaid());

        ArgumentCaptor<CreditLimitItem> creditCaptor = ArgumentCaptor.forClass(CreditLimitItem.class);
        verify(creditLimitItemRepository, atLeastOnce()).save(creditCaptor.capture());
        assertEquals(new BigDecimal("30"), creditCaptor.getValue().getExcessAmountPaid());
    }

    @Test
    public void processDepositToWallet_reducesWalletExcessAmountPaidWhenPaidFromWallet() {
        Customers customer = new Customers();
        customer.setPublicId(UUID.randomUUID());
        customer.setCreditLimit(new BigDecimal("1000"));

        Wallet wallet = new Wallet();
        wallet.setCustomer(customer);
        wallet.setWalletBalance(new BigDecimal("80"));
        wallet.setExcessAmountPaid(new BigDecimal("80"));
        wallet.setCreditLimitBalance(new BigDecimal("500"));

        CreditLimitItem creditLimitItem = creditLimitItem("ORD-WALLET", wallet, new BigDecimal("100"));

        when(creditLimitItemRepository.findPendingCreditItemsNative(anyString(), eq(WalletItemStatus.ACTIVE.name()), anyList()))
                .thenReturn(List.of(creditLimitItem));
        when(paymentItemRepository.findByOrderNumberIgnoreCaseAndStatusAndDeletedIsFalse("ORD-WALLET", WalletItemStatus.PAID))
                .thenReturn(List.of());
        when(creditLimitItemRepository.save(any(CreditLimitItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentItemRepository.save(any(PaymentItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(counterService.getNextCounter(any())).thenReturn(Math.toIntExact(1L));
        when(salesOrdersRepository.findBySalesOrderNumberAndDeletedIsFalse("ORD-WALLET"))
                .thenReturn(Optional.empty());

        bnplHelper.processDepositToWallet(wallet, new BigDecimal("40"), null, true, false, "tester@itibari.io");

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        Wallet savedWallet = walletCaptor.getValue();

        assertEquals(new BigDecimal("20"), savedWallet.getWalletBalance());
        assertEquals(new BigDecimal("20"), savedWallet.getExcessAmountPaid());

        ArgumentCaptor<PaymentItem> paymentCaptor = ArgumentCaptor.forClass(PaymentItem.class);
        verify(paymentItemRepository).save(paymentCaptor.capture());
        assertEquals(new BigDecimal("60"), paymentCaptor.getValue().getAmountPaidFromWallet());
    }

    private CreditLimitItem creditLimitItem(String orderNumber, Wallet wallet, BigDecimal amount) {
        CreditLimitItem creditLimitItem = new CreditLimitItem();
        creditLimitItem.setPublicId(UUID.randomUUID());
        creditLimitItem.setWallet(wallet);
        creditLimitItem.setOrderNumber(orderNumber);
        creditLimitItem.setAmount(amount);
        creditLimitItem.setInterestAmount(BigDecimal.ZERO);
        creditLimitItem.setFineAmount(BigDecimal.ZERO);
        creditLimitItem.setPaidAmount(BigDecimal.ZERO);
        creditLimitItem.setPaymentStatus(PaymentStatusEnum.NOT_PAID);
        creditLimitItem.setDueDate(LocalDate.now().minusDays(1));
        creditLimitItem.setCreditStatus(CreditStatus.Healthy);
        return creditLimitItem;
    }
}
