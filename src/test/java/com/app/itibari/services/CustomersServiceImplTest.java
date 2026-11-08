package com.app.itibari.services;

import com.app.itibari.dataTypes.CustomerType;
import com.app.itibari.models.entities.Customers;
import com.app.itibari.models.request.CustomersRequest;
import com.app.itibari.models.request.DeliverVanSaleItemRequest;
import com.app.itibari.repository.CustomersRepository;
import com.app.itibari.repository.HelpDeskCustomerRepository;
import com.app.itibari.services.helper.CustomerServiceHelper;
import com.app.itibari.utils.helpers.PhoneNumberHelper;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CustomersServiceImplTest {

    @Mock
    private CustomersRepository customersRepository;
    @Mock
    private HelpDeskCustomerRepository helpDeskCustomerRepository;
    @Mock
    private CustomerServiceHelper customerServiceHelper;
    @InjectMocks
    private CustomersServiceImpl service;

    @Test
    public void createRejectsDuplicateShopKeeperNumberInEquivalentFormats() {
        when(customersRepository.existsByShopKeeperNumberAndDeletedIsFalse("+255714394473"))
                .thenReturn(true);
        for (String shopKeeperNumber : new String[]{"0714394473", "+255714394473", "255714394473", "714394473"}) {
            CustomersRequest request = request("0714394472");
            request.setShopKeeperNumber(shopKeeperNumber);

            BadRequestRestApiException exception = assertThrows(BadRequestRestApiException.class,
                    () -> service.createCustomer(request, "customer"));
            assertEquals("A customer with this shopkeeper number already exists", exception.getRestApiError().getUserMessage());

            DeliverVanSaleItemRequest vanSaleRequest = new DeliverVanSaleItemRequest();
            vanSaleRequest.setCustomersRequest(request);
            assertThrows(BadRequestRestApiException.class,
                    () -> service.createCustomerFromVanSale(vanSaleRequest, null));
        }
        verify(customersRepository, never()).save(any(Customers.class));
        verifyNoInteractions(customerServiceHelper);
    }

    @Test
    public void childShopCreationRejectsShopKeeperNumberAlreadyAssignedToAnotherShop() {
        when(customersRepository.existsByShopKeeperNumberAndDeletedIsFalse("+255714394473"))
                .thenReturn(true);
        CustomersRequest request = request("0714394472");
        request.setParentCustomerPublicId(UUID.randomUUID());
        request.setShopKeeperNumber("0714394473");

        assertThrows(BadRequestRestApiException.class, () -> service.createCustomer(request, "customer"));

        verify(customersRepository).existsByShopKeeperNumberAndDeletedIsFalse("+255714394473");
        verify(customersRepository, never()).save(any(Customers.class));
    }

    @Test
    public void createChecksDuplicateShopKeeperWhenNumberDefaultsToMainPhone() {
        when(customersRepository.existsByShopKeeperNumberAndDeletedIsFalse("+255714394472"))
                .thenReturn(true);
        CustomersRequest request = request("0714394472");
        request.setShopKeeperNumber("   ");

        assertThrows(BadRequestRestApiException.class, () -> service.createCustomer(request, "customer"));

        assertEquals("+255714394472", request.getShopKeeperNumber());
        verify(customersRepository, never()).save(any(Customers.class));
    }

    @Test
    public void updateRejectsDuplicateShopKeeperBeforeChangingCustomer() {
        UUID publicId = UUID.randomUUID();
        Customers customer = customer(publicId, "+255714394472");
        customer.setParentCustomer(new Customers());
        customer.setShopKeeperNumber("+255714394474");
        when(customersRepository.findByPublicIdAndDeletedIsFalse(publicId)).thenReturn(Optional.of(customer));
        when(customersRepository.existsByShopKeeperNumberAndDeletedIsFalse("+255714394473"))
                .thenReturn(true);
        CustomersRequest request = request("0714394472");
        request.setShopKeeperNumber("0714394473");

        assertThrows(BadRequestRestApiException.class, () -> service.updateCustomer(publicId, request, "customer"));

        assertEquals("+255714394474", customer.getShopKeeperNumber());
        verify(customersRepository, never()).save(any(Customers.class));
        verifyNoInteractions(helpDeskCustomerRepository);
    }

    @Test
    public void updateAllowsKeepingOwnShopKeeperNumberInLocalFormat() {
        UUID publicId = UUID.randomUUID();
        Customers customer = customer(publicId, "+255714394472");
        customer.setShopKeeperNumber("+255714394473");
        when(customersRepository.findByPublicIdAndDeletedIsFalse(publicId)).thenReturn(Optional.of(customer));
        when(customersRepository.save(customer)).thenReturn(customer);
        when(helpDeskCustomerRepository.findTopByExistingCustomerPublicIdAndDeletedIsFalse(publicId))
                .thenReturn(Optional.empty());
        CustomersRequest request = request("0714394472");
        request.setShopKeeperNumber("0714394473");

        service.updateCustomer(publicId, request, "customer");

        assertEquals("+255714394473", customer.getShopKeeperNumber());
        verify(customersRepository, never()).existsByShopKeeperNumberAndDeletedIsFalse(any());
        verify(customersRepository, times(2)).save(customer);
    }

    @Test
    public void updateRejectsAnotherCustomersPhoneBeforeChangingOrSavingCustomer() {
        UUID publicId = UUID.randomUUID();
        Customers customer = customer(publicId, "+255712345678");
        when(customersRepository.findByPublicIdAndDeletedIsFalse(publicId)).thenReturn(Optional.of(customer));
        when(customersRepository.existsByPhoneNumberAndDeletedIsFalseAndParentCustomerIsNull("+255798765432"))
                .thenReturn(true);

        BadRequestRestApiException exception = assertThrows(BadRequestRestApiException.class,
                () -> service.updateCustomer(publicId, request("0798765432"), "customer"));

        assertEquals("A customer with this phone number already exists", exception.getRestApiError().getUserMessage());
        assertEquals("+255712345678", customer.getPhoneNumber());
        verify(customersRepository, never()).save(any(Customers.class));
        verifyNoInteractions(helpDeskCustomerRepository);
    }

    @Test
    public void updateAllowsKeepingOwnPhoneNumber() {
        for (String phoneNumber : new String[]{"+255712345678", "0712345678", "255712345678", "712345678", " 0712-345-678 "}) {
            assertSuccessfulUpdate("+255712345678", phoneNumber);
        }
    }

    @Test
    public void updateNormalizesPreviouslySavedLocalPhoneNumber() {
        assertSuccessfulUpdate("0712345678", "+255712345678");
    }

    @Test
    public void updateAllowsChangingToUnusedPhoneNumber() {
        for (String phoneNumber : new String[]{"+255798765432", "0798765432", "255798765432", "798765432"}) {
            assertSuccessfulUpdate("+255712345678", phoneNumber);
        }
    }

    @Test
    public void updateRejectsMissingPhoneNumber() {
        for (String phoneNumber : new String[]{null, "", "   "}) {
            assertThrows(BadRequestRestApiException.class,
                    () -> service.updateCustomer(UUID.randomUUID(), request(phoneNumber), "customer"));
        }
        verifyNoInteractions(customersRepository, helpDeskCustomerRepository);
    }

    @Test
    public void createNormalizesPhoneBeforeValidationAndSaving() {
        when(customersRepository.save(any(Customers.class))).thenAnswer(invocation -> invocation.getArgument(0));
        for (String phoneNumber : new String[]{"0714394472", "+255714394472", "255714394472", "714394472"}) {
            CustomersRequest request = request(phoneNumber);
            request.setOwnerNumber(phoneNumber);
            request.setShopKeeperNumber(phoneNumber);

            Customers saved = service.createCustomer(request, "customer");

            assertEquals("+255714394472", saved.getPhoneNumber());
            assertEquals("+255714394472", saved.getOwnerNumber());
            assertEquals("+255714394472", saved.getShopKeeperNumber());
            verify(customerServiceHelper).validateCustomerRequest(request);
            assertEquals("+255714394472", request.getPhoneNumber());
        }
        verify(customersRepository, times(4)).findByPhoneNumberAndDeletedIsFalse("+255714394472");
    }

    @Test
    public void createPreservesMissingOwnerAndUsesMainPhoneForMissingShopKeeper() {
        when(customersRepository.save(any(Customers.class))).thenAnswer(invocation -> invocation.getArgument(0));
        for (String optionalNumber : new String[]{null, "", "   "}) {
            CustomersRequest request = request("0714394472");
            request.setOwnerNumber(optionalNumber);
            request.setShopKeeperNumber(optionalNumber);

            Customers saved = service.createCustomer(request, "customer");

            assertEquals(optionalNumber, saved.getOwnerNumber());
            assertEquals("+255714394472", saved.getShopKeeperNumber());
        }
    }

    @Test
    public void createRejectsExistingPhoneInEquivalentFormats() {
        when(customersRepository.findByPhoneNumberAndDeletedIsFalse("+255714394472"))
                .thenReturn(Optional.of(customer(UUID.randomUUID(), "+255714394472")));
        for (String phoneNumber : new String[]{"0714394472", "+255714394472", "255714394472", "714394472"}) {
            assertThrows(BadRequestRestApiException.class,
                    () -> service.createCustomer(request(phoneNumber), "customer"));
        }
        verify(customersRepository, never()).save(any(Customers.class));
    }

    @Test
    public void vanSaleCreationNormalizesPhoneBeforeSaving() {
        when(customersRepository.save(any(Customers.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DeliverVanSaleItemRequest request = new DeliverVanSaleItemRequest();
        request.setCustomersRequest(request("0714394472"));
        request.getCustomersRequest().setOwnerNumber("0714394473");

        Customers saved = service.createCustomerFromVanSale(request, null);
        assertEquals("+255714394472", saved.getPhoneNumber());
        assertEquals("+255714394473", saved.getOwnerNumber());
        verify(customersRepository).findByPhoneNumberAndDeletedIsFalse("+255714394472");
    }

    @Test
    public void createAndUpdateRejectInvalidOwnerNumbersBeforeAccessingRepository() {
        for (String ownerNumber : new String[]{"07123", "07143944722", "owner", "+25571439447x", "+254714394472"}) {
            CustomersRequest request = request("0714394472");
            request.setOwnerNumber(ownerNumber);

            BadRequestRestApiException exception = assertThrows(BadRequestRestApiException.class,
                    () -> service.createCustomer(request, "customer"));
            assertEquals("Please provide a valid Tanzanian owner phone number", exception.getRestApiError().getUserMessage());
            assertThrows(BadRequestRestApiException.class,
                    () -> service.updateCustomer(UUID.randomUUID(), request, "customer"));

            DeliverVanSaleItemRequest vanSaleRequest = new DeliverVanSaleItemRequest();
            vanSaleRequest.setCustomersRequest(request);
            assertThrows(BadRequestRestApiException.class,
                    () -> service.createCustomerFromVanSale(vanSaleRequest, null));
        }
        verifyNoInteractions(customersRepository, customerServiceHelper);
    }

    @Test
    public void createAndUpdateRejectInvalidPhoneNumbersBeforeAccessingRepository() {
        for (String phoneNumber : new String[]{"07123", "07143944722", "phone", "+25571439447x", "+254714394472"}) {
            assertThrows(BadRequestRestApiException.class,
                    () -> service.createCustomer(request(phoneNumber), "customer"));
            assertThrows(BadRequestRestApiException.class,
                    () -> service.updateCustomer(UUID.randomUUID(), request(phoneNumber), "customer"));
        }
        verifyNoInteractions(customersRepository, customerServiceHelper);
    }

    private void assertSuccessfulUpdate(String savedPhoneNumber, String phoneNumber) {
        clearInvocations(customersRepository, helpDeskCustomerRepository);
        UUID publicId = UUID.randomUUID();
        Customers customer = customer(publicId, savedPhoneNumber);
        when(customersRepository.findByPublicIdAndDeletedIsFalse(publicId)).thenReturn(Optional.of(customer));
        when(customersRepository.save(customer)).thenReturn(customer);
        when(helpDeskCustomerRepository.findTopByExistingCustomerPublicIdAndDeletedIsFalse(publicId))
                .thenReturn(Optional.empty());

        CustomersRequest request = request(phoneNumber);
        request.setOwnerNumber("0714394472");
        request.setShopKeeperNumber("255714394473");
        service.updateCustomer(publicId, request, "customer");

        String normalizedPhoneNumber = PhoneNumberHelper.toInternationalFormat(phoneNumber);
        if (normalizedPhoneNumber.equals(PhoneNumberHelper.toInternationalFormat(savedPhoneNumber))) {
            verify(customersRepository, never()).existsByPhoneNumberAndDeletedIsFalseAndParentCustomerIsNull(any());
        } else {
            verify(customersRepository).existsByPhoneNumberAndDeletedIsFalseAndParentCustomerIsNull(normalizedPhoneNumber);
        }
        verify(customersRepository, times(2)).save(customer);
        assertEquals(normalizedPhoneNumber, customer.getPhoneNumber());
        assertEquals("+255714394472", customer.getOwnerNumber());
        assertEquals("+255714394473", customer.getShopKeeperNumber());
    }

    private Customers customer(UUID publicId, String phoneNumber) {
        Customers customer = new Customers();
        customer.setPublicId(publicId);
        customer.setPhoneNumber(phoneNumber);
        customer.setCustomerType(CustomerType.REGULAR);
        return customer;
    }

    private CustomersRequest request(String phoneNumber) {
        return CustomersRequest.builder().phoneNumber(phoneNumber).selfRegistered(true).build();
    }
}
