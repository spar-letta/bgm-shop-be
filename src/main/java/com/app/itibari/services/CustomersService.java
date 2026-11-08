package com.app.itibari.services;

import com.app.itibari.dataTypes.ChurnedCustomerLeadStatus;
import com.app.itibari.dataTypes.ChurnedDateRangeFilter;
import com.app.itibari.dataTypes.CustomerOutboundCallStatus;
import com.app.itibari.dataTypes.CustomerType;
import com.app.itibari.dataTypes.SourceChannelEnum;
import com.app.itibari.dataTypes.StatementExportType;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.CustomerAction;
import com.app.itibari.models.request.CustomersRequest;
import com.app.itibari.models.request.DeliverVanSaleItemRequest;
import com.app.itibari.models.request.UpdateCustomerCallStatusRequest;
import com.app.itibari.models.request.UpdateCustomerLeadStatusRequest;
import com.app.itibari.models.response.ChurnedCustomer;
import com.app.itibari.models.response.CustomerLocationResponse;
import com.app.itibari.models.response.CustomerPageable;
import com.app.itibari.models.response.OrderStats;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.text.DocumentException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CustomersService {

    Customers createCustomer(@Valid CustomersRequest customersRequest, String name);

    void updateCustomer(UUID customerPublicId, @Valid CustomersRequest customersRequest, String name);

    Customers getCustomer(UUID customerPublicId);

    void deleteCustomer(UUID customerPublicId, String name);

    Page<Customers> getAllCustomers(String searchParam, String name, String email, String nationalId, String phoneNumber,
                                    SourceChannelEnum sourceChannel, UUID organizationLocationId, UUID organizationRegionId,
                                    UUID registeredBy, List<UUID> locations, LocalDate startDate, LocalDate endDate,
                                    UUID servedBy,UUID parentCustomerPublicId, UUID salesAgentPublicId, Boolean isFromSA, Principal principal,
                                    Boolean active,
                                    Pageable pageable);

    OrderStats getCustomerOrderStats(@NotNull UUID customerId);

    Customers getCustomerProfile(String name);

    List<Customers> getCustomersBySalesAgent(UUID publicId, String from, String to, Pageable pageable);

    Customers createCustomerFromVanSale(DeliverVanSaleItemRequest deliverItemRequest, Person person);


    List<CustomerLocationResponse> getCustomerLocations();

    CustomerPageable getSupplierCustomers(UUID regionId, LocalDate startDate, LocalDate endDate, Pageable pageable,
                                          Principal principal);

    void showSupplierCustomerPhoneNumber(UUID customerPublicId, String name);

    void fixCustomerDuplicate();

    void changeCustomerType(UUID customerPublicId, CustomerType customerType, String name);

    ByteArrayOutputStream getAllSalesOrders(UUID regionId, UUID locationId,
                                            String searchParam, StatementExportType exportType,
                                            LocalDate startDate, LocalDate endDate, Pageable pageable, Principal principal);

    void customerActions(UUID customerPublicId, CustomerAction customerAction, String name);

    void recalculateAov();

    void recalculateAovWithUIds(List<UUID> customerIds);

    Page<Customers> getAllSupplierCustomers(UUID regionId, UUID locationId, String searchParam, LocalDate startDate,
                                            LocalDate endDate,
                                            Principal principal, Pageable pageable);

    Customers getCustomerByPhone(String customerPhoneNumber);

    void updateCustomerCallStatus(UUID customerPublicId, UpdateCustomerCallStatusRequest request, String principalName);

    void updateCustomerLeadStatus(UUID customerPublicId, UpdateCustomerLeadStatusRequest request, String principalName);

    Page<ChurnedCustomer> getChurnedCustomer(ChurnedDateRangeFilter churnedDateRangeFilter, CustomerType customerType,
                                             UUID callStatusPublicId, LocalDate startDate,
                                             LocalDate endDate, String searchParam, CustomerOutboundCallStatus callStatus,
                                             UUID departmentPublicId, ChurnedCustomerLeadStatus leadStatus, Pageable pageable);

    ByteArrayOutputStream runExportChurnedCustomers(ChurnedDateRangeFilter rangeFilter, CustomerType customerType,
                                                    UUID callStatusPublicId, String name, StatementExportType statementExportType,
                                                    LocalDate startDate, LocalDate endDate, String searchParam, CustomerOutboundCallStatus callStatus,
                                                    UUID departmentPublicId, ChurnedCustomerLeadStatus leadStatus, Pageable pageable) throws DocumentException, IOException;

    Page<ChurnedCustomer> getNonChurnedInActiveCustomers(CustomerType customerType, UUID callStatusPublicId, LocalDate startDate, LocalDate endDate, String searchParam, Boolean inActive, CustomerOutboundCallStatus callStatus, UUID departmentPublicId, Pageable pageable);

    ByteArrayOutputStream runExportNonChurnedInActiveCustomers(Boolean inActive, CustomerType customerType, UUID callStatusPublicId, String name, StatementExportType exportType, LocalDate startDate, LocalDate endDate, String searchParam, CustomerOutboundCallStatus callStatus, UUID departmentPublicId, Pageable pageable) throws DocumentException, IOException;

    void activateDeactivateChildCustomer(UUID customerPublicId, Boolean activate, String name);

    Page<ChurnedCallHistory> getChurnedCallHistory(Pageable pageable);

    Page<ChurnedCallHistory> getCustomerCallHistory(UUID customerPublicId, Pageable pageable);
}
