package com.app.itibari.services;

import com.app.itibari.dataTypes.CustomerType;
import com.app.itibari.models.entities.Customers;
import com.app.itibari.models.request.BNPLOnboarding;
import com.app.itibari.repository.*;
import com.app.itibari.services.helper.BnplHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BNPLServiceImplTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private CustomersRepository customersRepository;
    @Mock
    private BnplHelper bnplHelper;
    @Mock
    private DocumentRuleRepository documentRuleRepository;
    @Mock
    private FileDataRepository fileDataRepository;
    @Mock
    private PaymentItemRepository paymentItemRepository;
    @Mock
    private CreditLimitItemRepository creditLimitItemRepository;
    @Mock
    private TransactionItemRepository transactionItemRepository;
    @Mock
    private PersonRepository personRepository;
    @Mock
    private RejectionCodeRepository rejectionCodeRepository;
    @Mock
    private CustomerFeedBackRepository customerFeedBackRepository;
    @Mock
    private CreditRuleRepository creditRuleRepository;
    @Mock
    private CorporateCustomerBnplApprovalRepository corporateCustomerBnplApprovalRepository;

    @InjectMocks
    private BNPLServiceImpl bnplService;

    @Test
    public void onboardCustomerToBNPL_shouldUseExistingAovAndCreditLimit_whenRecalculationReturnsZero() {
        UUID customerPublicId = UUID.randomUUID();
        Customers customer = new Customers();
        customer.setPublicId(customerPublicId);
        customer.setCustomerType(CustomerType.REGULAR);
        customer.setPreliminaryValue(new BigDecimal("240000"));
        customer.setCreditLimit(new BigDecimal("84000"));

        BNPLOnboarding request = buildOnboardingRequest(customerPublicId);

        when(customersRepository.findByPublicIdAndDeletedIsFalse(customerPublicId)).thenReturn(Optional.of(customer));
        when(documentRuleRepository.findDocumentRuleByDocumentTypeAndDocumentRequiredIsTrueAndDeletedIsFalse(any())).thenReturn(Collections.emptyList());
        when(bnplHelper.getAovForCustomer(customer)).thenReturn(BigDecimal.ZERO);
        when(bnplHelper.getCreditLimitForCustomer(eq(CustomerType.REGULAR), eq(new BigDecimal("240000")))).thenReturn(BigDecimal.ZERO);
        when(customersRepository.save(any(Customers.class))).thenAnswer(invocation -> invocation.getArgument(0));

        bnplService.onboardCustomerToBNPL(request, "admin@itibari.io");

        assertEquals(new BigDecimal("240000"), customer.getPreliminaryValue());
        assertEquals(new BigDecimal("84000"), customer.getCreditLimit());
        verify(bnplHelper).createWalletForCustomer(customer, "admin@itibari.io", new BigDecimal("240000"), request, new BigDecimal("84000"));
    }

    @Test
    public void onboardCustomerToBNPL_shouldUseRecalculatedAovAndCreditLimit_whenAvailable() {
        UUID customerPublicId = UUID.randomUUID();
        Customers customer = new Customers();
        customer.setPublicId(customerPublicId);
        customer.setCustomerType(CustomerType.REGULAR);
        customer.setPreliminaryValue(new BigDecimal("120000"));
        customer.setCreditLimit(new BigDecimal("42000"));

        BNPLOnboarding request = buildOnboardingRequest(customerPublicId);

        when(customersRepository.findByPublicIdAndDeletedIsFalse(customerPublicId)).thenReturn(Optional.of(customer));
        when(documentRuleRepository.findDocumentRuleByDocumentTypeAndDocumentRequiredIsTrueAndDeletedIsFalse(any())).thenReturn(Collections.emptyList());
        when(bnplHelper.getAovForCustomer(customer)).thenReturn(new BigDecimal("300000"));
        when(bnplHelper.getCreditLimitForCustomer(eq(CustomerType.REGULAR), eq(new BigDecimal("300000")))).thenReturn(new BigDecimal("105000"));
        when(customersRepository.save(any(Customers.class))).thenAnswer(invocation -> invocation.getArgument(0));

        bnplService.onboardCustomerToBNPL(request, "admin@itibari.io");

        assertEquals(new BigDecimal("300000"), customer.getPreliminaryValue());
        assertEquals(new BigDecimal("105000"), customer.getCreditLimit());
        verify(bnplHelper).createWalletForCustomer(customer, "admin@itibari.io", new BigDecimal("300000"), request, new BigDecimal("105000"));
    }

    private BNPLOnboarding buildOnboardingRequest(UUID customerPublicId) {
        BNPLOnboarding request = new BNPLOnboarding();
        request.setCustomerPublicId(customerPublicId);
        request.setCustomerID("CUST-1001");
        request.setTinNumber("123456789");
        request.setBusinessLicenseNumber("BL-12345");
        request.setGuarantorName("Guarantor Name");
        request.setGuarantorPhoneNumber("+255700000001");
        return request;
    }
}

