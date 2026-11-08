package com.app.itibari.services;

import com.app.itibari.dataTypes.*;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.CustomerAction;
import com.app.itibari.models.request.CustomersRequest;
import com.app.itibari.models.request.DeliverVanSaleItemRequest;
import com.app.itibari.models.request.UpdateCustomerCallStatusRequest;
import com.app.itibari.models.request.UpdateCustomerLeadStatusRequest;
import com.app.itibari.models.response.*;
import com.app.itibari.repository.*;
import com.app.itibari.services.helper.BnplHelper;
import com.app.itibari.services.helper.CustomerServiceHelper;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.formatter.DateFormatter;
import com.app.itibari.utils.helpers.AgentActiveChecker;
import com.app.itibari.utils.helpers.PhoneNumberHelper;
import com.app.itibari.utils.reportHelper.ChurnedCustomerExportHelper;
import com.app.itibari.utils.reportHelper.SupplierCustomer;
import com.app.itibari.utils.specification.CustomerSpecification;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.text.DocumentException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static com.app.itibari.utils.helpers.PhoneNumberHelper.getLastNineDigits;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomersServiceImpl implements CustomersService {

    private final BnplHelper bnplHelper;
    private final CreditRuleRepository creditRuleRepository;
    private final CustomersRepository customersRepository;
    private final OrganizationRegionsRepository organizationRegionsRepository;
    private final OrganizationLocationsRepository organizationLocationsRepository;
    private final PersonRepository personRepository;
    private final SalesOrdersRepository salesOrdersRepository;
    private final CustomerServiceHelper customerServiceHelper;
    private final SalesAgentsRepository salesAgentsRepository;
    private final PhoneNumberHelper phoneNumberHelper;
    private final AgentActiveChecker agentActiveChecker;
    private final SupplierCustomer supplierCustomer;
    private final SupplierRepository supplierRepository;
    private final ChurnedCustomerExportHelper churnedCustomerExportHelper;
    private final HelpDeskCustomerRepository helpDeskCustomerRepository;
    private final CallStatusRepository callStatusRepository;
    private final EventsRepository eventsRepository;
    private final WalletRepository walletRepository;
    private final OrderAmendmentRepository orderAmendmentRepository;
    private final ChurnedCallHistoryRepository churnedCallHistoryRepository;
    private final LocationApprovalRepository locationApprovalRepository;
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    @Override
    public Customers createCustomer(@Valid CustomersRequest customersRequest, String name) {

        customersRequest.setPhoneNumber(normalizeCustomerPhoneNumber(customersRequest.getPhoneNumber()));
        customersRequest.setOwnerNumber(normalizeOwnerNumber(customersRequest.getOwnerNumber()));
        validateShopKeeperNumber(customersRequest, null);
        customerServiceHelper.validateCustomerRequest(customersRequest);
        Person person = null;
        if (!customersRequest.isSelfRegistered() && !customersRequest.isSaleAgentRegistered() && !customersRequest.isParentCustomerRegistered()) {
            Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
            if (personOptional.isPresent()) {
                person = personOptional.get();
            } else {
                throw new BadRequestRestApiException()
                        .developerMessage("Person with email " + name + " not found")
                        .userMessage("Person not found");
            }
        }
        Optional<Customers> existingCustomerByPhoneNumber = customersRepository.findByPhoneNumberAndDeletedIsFalse(customersRequest.getPhoneNumber());
        if (existingCustomerByPhoneNumber.isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer with phone number " + customersRequest.getPhoneNumber() + " already exists")
                    .userMessage("A customer with this phone number already exists");
        }
        Customers customer = new Customers();

        UUID salesAgentPublicId = null;
        String registeredBy = null;
        SalesAgents salesAgents = null;
        if (customersRequest.isSaleAgentRegistered()) {
            Optional<SalesAgents> optionalSalesAgents = salesAgentsRepository.findSalesAgentsByPhoneNumberAndDeletedIsFalse(name);

            if (optionalSalesAgents.isPresent()) {
                salesAgents = optionalSalesAgents.get();

                agentActiveChecker.checkIfAgentIsActive(salesAgents.getPublicId(), AgentType.SALES_AGENT);
                salesAgentPublicId = salesAgents.getPublicId();
                registeredBy = salesAgents.getName();
            } else {
                throw new BadRequestRestApiException()
                        .developerMessage("Sales Agent with phone number " + name + " not found")
                        .userMessage("Sales Agent not found");
            }
        } else {
            registeredBy = person != null ? person.getFullName() : "System";
        }
        if (null != customersRequest.getOrganizationRegionId()) {
            // Validate organization Region
            Optional<OrganizationRegions> organizationRegionsOptional = organizationRegionsRepository
                    .findByPublicIdAndDeletedIsFalse(customersRequest.getOrganizationRegionId());
            if (organizationRegionsOptional.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Organization Region with ID " + customersRequest.getOrganizationRegionId() + " not found")
                        .userMessage("Organization Region not found");
            }
            customer.setOrganizationRegions(organizationRegionsOptional.get());
        }

        if (null != customersRequest.getOrganizationLocationId()) {
            // Validate organization location
            Optional<OrganizationLocations> organizationLocationsOptional = organizationLocationsRepository
                    .findByPublicIdAndDeletedIsFalse(customersRequest.getOrganizationLocationId());
            if (organizationLocationsOptional.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Organization Region with ID " + customersRequest.getOrganizationRegionId() + " not found")
                        .userMessage("Organization Region not found");
            }
            customer.setOrganizationLocation(organizationLocationsOptional.get());
        }


        if (customersRequest.getParentCustomerPublicId() != null) {
            Optional<Customers> optionalParentCustomer = customersRepository.findByPublicIdAndDeletedIsFalse(customersRequest.getParentCustomerPublicId());
            if (optionalParentCustomer.isEmpty()) {
                throw new BadRequestRestApiException()
                        .userMessage("Parent Customer with ID " + customersRequest.getParentCustomerPublicId() + " not found")
                        .developerMessage("Parent Customer not found");
            }

            if (optionalParentCustomer.get().isHasParentCustomer() && optionalParentCustomer.get().getParentCustomer() != null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Customer already has a parent customer")
                        .userMessage("Customer already has a parent customer");
            }
            customer.setParentCustomer(optionalParentCustomer.get());
            customer.setCanChildCustomerLogin(customersRequest.getCanChildCustomerLogin());
            customer.setCanChildCustomerPlaceOrder(customersRequest.getCanChildCustomerPlaceOrder());
            optionalParentCustomer.get().setHasOtherStores(true);
            customersRepository.save(optionalParentCustomer.get());

        }

        customer.setName(customersRequest.getName());
        customer.setFirstName(customersRequest.getFirstName());
        customer.setLastName(customersRequest.getLastName());
        customer.setEmail(customersRequest.getEmail());
        customer.setNationalId(customersRequest.getNationalId());
        customer.setPhoneNumber(customersRequest.getPhoneNumber());
        customer.setRegistrationDate(LocalDate.now());  // Assuming registration date is today
        customer.setSourceChannel(customersRequest.getSourceChannel());
        customer.setLatitude(customersRequest.getLatitude());
        customer.setLongitude(customersRequest.getLongitude());
        customer.setAddress(customersRequest.getAddress());
        customer.setLandmark(customersRequest.getLandmark());
        customer.setTin(this.validateTin(customersRequest.getTin()));
        customer.setCustomerActiveStatus(CustomerActiveStatus.New);
        customer.setAgentPublicId(salesAgentPublicId);
        customer.setRegisteredBy(registeredBy);
        customer.setCustomerType(customersRequest.isSaleAgentRegistered() && customersRequest.getCustomerType() != null ? customersRequest.getCustomerType() : CustomerType.REGULAR);
        customer.setBnplVerificationStatus(CustomerBnplVerificationStatus.NOT_VERIFIED);
        customer.setOwnerNumber(customersRequest.getOwnerNumber());
        customer.setShopKeeperNumber(
                (customersRequest.getShopKeeperNumber() == null || customersRequest.getShopKeeperNumber().isBlank())
                        ? customersRequest.getPhoneNumber()
                        : normalizeOptionalPhoneNumber(customersRequest.getShopKeeperNumber())
        );


        if (person != null) {
            customer.setCreatedBy(person);
        }
        customer.setStoreName(customersRequest.getStoreName());

        Customers savedCustomer = customersRepository.save(customer);

        if (savedCustomer.getParentCustomer() != null && savedCustomer.getParentCustomer().getBnplVerificationStatus() == CustomerBnplVerificationStatus.VERIFIED) {
            bnplHelper.createWalletForChildCustomer(savedCustomer, person, getWallet(savedCustomer.getParentCustomer()));

        }
        runMapCustomerToSA(savedCustomer);

        if (customersRequest.isSaleAgentRegistered() && salesAgents != null) {
            salesAgents.addCustomer(savedCustomer);
            salesAgentsRepository.save(salesAgents);
        }

        if (savedCustomer.getCustomerType().equals(CustomerType.CORPORATE)) {
            customer.setCustomerType(CustomerType.REGULAR);
            customer.setAwaitingCustomerTypeChange(true);
            bnplHelper.processCorporateCustomer(savedCustomer, false);
        }
        // check for enterprise
        if (customersRequest.isHasOtherStores()) {
            customer.setCustomerType(CustomerType.ENTERPRISE);
        }
        return customersRepository.save(customer);
    }

    private Wallet getWallet(Customers parentCustomer) {
        Wallet wallet = walletRepository.findByCustomerAndDeletedIsFalse(parentCustomer);

        if (wallet == null) {
            throw new BadRequestRestApiException()
                    .userMessage("Parent Customer does not have a wallet")
                    .developerMessage("Parent Customer does not have a wallet");
        }
        return wallet;
    }

    private void runMapCustomerToSA(Customers savedCustomer) {
        OrganizationLocations organizationLocation = savedCustomer.getOrganizationLocation();
        if (organizationLocation != null) {
            List<SalesAgents> agents = salesAgentsRepository.findByOrganizationLocation(organizationLocation);
            if (!agents.isEmpty()) {
                SalesAgents assignedAgent = agents.get(0);
                savedCustomer.setServedBy(assignedAgent);
                customersRepository.save(savedCustomer);
            } else {
                throw new BadRequestRestApiException()
                        .developerMessage("No SalesAgents found for OrganizationLocation")
                        .userMessage("Sorry, No SalesAgents found for OrganizationLocation");
            }
        }
    }


    private String validateTin(String tin) {
        if (tin == null || tin.isEmpty()) {
            return null; // TIN is optional, so return null if not provided
        }

        // Example validation: TIN must be 9 digits long
        if (!tin.matches("\\d{9}")) {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid TIN format: " + tin)
                    .userMessage("TIN must be a 9-digit number");
        }

        if (customersRepository.existsByTinAndDeletedIsFalse(tin)) {
            throw new BadRequestRestApiException()
                    .developerMessage("TIN already exists: " + tin)
                    .userMessage("A customer with this TIN already exists");
        }

        if (tin.equalsIgnoreCase("123456789")) {
            throw new BadRequestRestApiException()
                    .developerMessage("TIN cannot be 123456789")
                    .userMessage("Invalid TIN");
        }

        return tin;
    }

    @Override
    public void updateCustomer(UUID customerPublicId, @Valid CustomersRequest customersRequest, String name) {
        customersRequest.setPhoneNumber(normalizeCustomerPhoneNumber(customersRequest.getPhoneNumber()));
        customersRequest.setOwnerNumber(normalizeOwnerNumber(customersRequest.getOwnerNumber()));
//        if (Validators.allEqualNull(customersRequest.getOwnerNumber(), customersRequest.getShopKeeperNumber())) {
//            throw new BadRequestRestApiException()
//                    .developerMessage("Owner number and/or shopkeeper numbers are mandatory")
//                    .userMessage("Sorry!. Owner number and/or shopkeeper numbers are mandatory");
//        }
        Optional<Customers> customerOptional = customersRepository.findByPublicIdAndDeletedIsFalse(customerPublicId);
        if (customerOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer with public ID " + customerPublicId + " not found")
                    .userMessage("Customer not found");
        }
        Customers customer = customerOptional.get();

        String savedPhoneNumber = PhoneNumberHelper.toInternationalFormat(customer.getPhoneNumber());
        if (!customersRequest.getPhoneNumber().equals(savedPhoneNumber)
                && customersRepository.existsByPhoneNumberAndDeletedIsFalseAndParentCustomerIsNull(
                        customersRequest.getPhoneNumber())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer with phone number " + customersRequest.getPhoneNumber() + " already exists")
                    .userMessage("A customer with this phone number already exists");
        }

        validateShopKeeperNumber(customersRequest, customer);

        if (customersRequest.getCustomerType() == null) {
            customersRequest.setCustomerType(
                    customer.getCustomerType() == CustomerType.CORPORATE
                            ? CustomerType.CORPORATE
                            : CustomerType.REGULAR);
        }

        if (customer.getCustomerType().equals(CustomerType.REGULAR) && customersRequest.getCustomerType().equals(CustomerType.CORPORATE) && customer.isAwaitingCustomerTypeChange()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer type cannot be changed")
                    .userMessage("Sorry!. Customer type change in progress");
        }

        if (customer.getCustomerType().equals(CustomerType.CORPORATE) && customersRequest.getCustomerType().equals(CustomerType.REGULAR) && customer.isAwaitingCustomerTypeChange()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer type cannot be changed")
                    .userMessage("Sorry!. Customer type change in progress");
        }

        boolean changeFromCooperateToRegular = customer.getCustomerType() == CustomerType.CORPORATE && customersRequest.getCustomerType() == CustomerType.REGULAR;

        //customerServiceHelper.validateCustomerRequest(customersRequest);
        Person person = null;
        boolean isSelfRegisteredOrSaleAgentRegistered = customersRequest.isSelfRegistered() || customersRequest.isSaleAgentRegistered() || customersRequest.isParentCustomerRegistered();
        if (!isSelfRegisteredOrSaleAgentRegistered) {
            Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
            if (personOptional.isPresent()) {
                person = personOptional.get();
            } else {
                throw new BadRequestRestApiException()
                        .developerMessage("Person with email " + name + " not found")
                        .userMessage("Person not found");
            }
        }

        if (null != customersRequest.getOrganizationRegionId()) {
            Optional<OrganizationRegions> organizationRegionsOptional = organizationRegionsRepository
                    .findByPublicIdAndDeletedIsFalse(customersRequest.getOrganizationRegionId());
            if (organizationRegionsOptional.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Organization Region with ID " + customersRequest.getOrganizationRegionId() + " not found")
                        .userMessage("Organization Region not found");
            }
            customer.setOrganizationRegions(organizationRegionsOptional.get());
        }

        if (null != customersRequest.getOrganizationLocationId()) {
            // Validate organization location
            Optional<OrganizationLocations> organizationLocationsOptional = organizationLocationsRepository
                    .findByPublicIdAndDeletedIsFalse(customersRequest.getOrganizationLocationId());
            if (organizationLocationsOptional.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Organization Region with ID " + customersRequest.getOrganizationRegionId() + " not found")
                        .userMessage("Organization Region not found");
            }
            customer.setOrganizationLocation(organizationLocationsOptional.get());
        }

        if (customersRequest.getParentCustomerPublicId() != null) {
            Optional<Customers> optionalParentCustomer = customersRepository.findByPublicIdAndDeletedIsFalse(customersRequest.getParentCustomerPublicId());
            if (optionalParentCustomer.isEmpty()) {
                throw new BadRequestRestApiException()
                        .userMessage("Parent Customer with ID " + customersRequest.getParentCustomerPublicId() + " not found")
                        .developerMessage("Parent Customer not found");
            }
            if (optionalParentCustomer.get().isHasParentCustomer() && optionalParentCustomer.get().getParentCustomer() != null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Customer already has a parent customer")
                        .userMessage("Customer already has a parent customer");
            }
            customer.setParentCustomer(optionalParentCustomer.get());
            customer.setCanChildCustomerLogin(customersRequest.getCanChildCustomerLogin());
            customer.setCanChildCustomerPlaceOrder(customersRequest.getCanChildCustomerPlaceOrder());
            optionalParentCustomer.get().setHasOtherStores(true);
            customersRepository.save(optionalParentCustomer.get());

        }

        // Update the customer
        customer.setName(customersRequest.getName());
        customer.setFirstName(customersRequest.getFirstName());
        customer.setLastName(customersRequest.getLastName());
        customer.setEmail(customersRequest.getEmail());
        customer.setNationalId(customersRequest.getNationalId());
        customer.setPhoneNumber(customersRequest.getPhoneNumber());
        customer.setSourceChannel(customersRequest.getSourceChannel());
        customer.setAddress(customersRequest.getAddress());
        customer.setLongitude(customersRequest.getLongitude());
        customer.setLatitude(customersRequest.getLatitude());
        customer.setLandmark(customersRequest.getLandmark());
        customer.setOwnerNumber(customersRequest.getOwnerNumber());
        customer.setShopKeeperNumber(
                (customersRequest.getShopKeeperNumber() == null || customersRequest.getShopKeeperNumber().isBlank())
                        ? customersRequest.getPhoneNumber()
                        : normalizeOptionalPhoneNumber(customersRequest.getShopKeeperNumber())
        );


        // check for null to avoid overwriting existing TIN with null if not provided in update request
        if (customersRequest.getTin() != null) {
            customer.setTin(customersRequest.getTin().equalsIgnoreCase(customer.getTin()) ? customer.getTin() : this.validateTin(customersRequest.getTin()));
        }

        customer.setCustomerType(customersRequest.isSaleAgentRegistered() && customersRequest.getCustomerType() != null ? customersRequest.getCustomerType() : CustomerType.REGULAR);

        if (person != null) {
            customer.setUpdatedBy(person);
        }
        customer.setUpdateDate(LocalDateTime.now());
        customer.setStoreName(customersRequest.getStoreName());
        Customers saved = customersRepository.save(customer);

        if (saved.getParentCustomer() != null && saved.getParentCustomer().getBnplVerificationStatus() == CustomerBnplVerificationStatus.VERIFIED) {
            bnplHelper.createWalletForChildCustomer(saved, person, getWallet(saved.getParentCustomer()));

        }
        runUpdateCxCustomer(saved);

        if (saved.getCustomerType().equals(CustomerType.CORPORATE) && !saved.isBnplAccountCreated() || changeFromCooperateToRegular) {
            saved.setCustomerType(changeFromCooperateToRegular ? CustomerType.CORPORATE : CustomerType.REGULAR);
            saved.setAwaitingCustomerTypeChange(true);
            bnplHelper.processCorporateCustomer(saved, changeFromCooperateToRegular);
        }

        // check for enterprise
        if (customersRequest.isHasOtherStores()) {
            customer.setCustomerType(CustomerType.ENTERPRISE);
        }

        customersRepository.save(saved);
    }

    private void validateShopKeeperNumber(CustomersRequest request, Customers existingCustomer) {
        String shopKeeperNumber = request.getShopKeeperNumber() == null || request.getShopKeeperNumber().isBlank()
                ? request.getPhoneNumber()
                : PhoneNumberHelper.toInternationalFormat(request.getShopKeeperNumber());
        request.setShopKeeperNumber(shopKeeperNumber);
        if (existingCustomer != null
                && shopKeeperNumber.equals(normalizeOptionalPhoneNumber(existingCustomer.getShopKeeperNumber()))) {
            return;
        }
        if (customersRepository.existsByShopKeeperNumberAndDeletedIsFalse(shopKeeperNumber)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer with shopkeeper number " + shopKeeperNumber + " already exists")
                    .userMessage("A customer with this shopkeeper number already exists");
        }
    }

    private String normalizeOwnerNumber(String ownerNumber) {
        if (ownerNumber == null || ownerNumber.isBlank()) {
            return ownerNumber;
        }
        String normalizedOwnerNumber = PhoneNumberHelper.toInternationalFormat(ownerNumber);
        if (!normalizedOwnerNumber.matches("\\+255\\d{9}")) {
            throw new BadRequestRestApiException()
                    .developerMessage("Owner number must have country code +255 followed by 9 digits")
                    .userMessage("Please provide a valid Tanzanian owner phone number");
        }
        return normalizedOwnerNumber;
    }

    private String normalizeOptionalPhoneNumber(String phoneNumber) {
        return phoneNumber == null || phoneNumber.isBlank()
                ? phoneNumber
                : PhoneNumberHelper.toInternationalFormat(phoneNumber);
    }

    private String normalizeCustomerPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Phone number is mandatory")
                    .userMessage("Sorry, phone number is required");
        }

        String normalizedPhoneNumber = PhoneNumberHelper.toInternationalFormat(phoneNumber);
        if (!normalizedPhoneNumber.matches("\\+255\\d{9}")) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer phone number must have country code +255 followed by 9 digits")
                    .userMessage("Please provide a valid Tanzanian phone number");
        }
        return normalizedPhoneNumber;
    }

    private void runUpdateCxCustomer(Customers customer) {
        HelpDeskCustomer helpDeskCustomer = helpDeskCustomerRepository.findTopByExistingCustomerPublicIdAndDeletedIsFalse(customer.getPublicId()).orElse(null);
        if (helpDeskCustomer != null) {
            helpDeskCustomer.setUpdateDate(LocalDateTime.now());

            helpDeskCustomer.setName(customer.getName());
            helpDeskCustomer.setPhoneNumber(customer.getPhoneNumber());
            helpDeskCustomer.setDistrictId(customer.getOrganizationRegions().getPublicId().toString());
            helpDeskCustomer.setWardId(customer.getOrganizationLocation().getPublicId().toString());
            helpDeskCustomer.setPostalAddress(customer.getAddress());
            helpDeskCustomer.setStoreName(customer.getStoreName());
            helpDeskCustomerRepository.save(helpDeskCustomer);
        }
    }

    @Override
    public Customers getCustomer(UUID customerPublicId) {

        Optional<Customers> customerOptional = customersRepository
                .findByPublicIdAndDeletedIsFalse(customerPublicId);
        if (customerOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer with public ID " + customerPublicId + " not found")
                    .userMessage("Customer not found");
        }
        return customerOptional.get();
    }

    @Override
    public Page<Customers> getAllCustomers(String searchParam,
                                           String name,
                                           String email,
                                           String nationalId,
                                           String phoneNumber,
                                           SourceChannelEnum sourceChannel,
                                           UUID organizationLocationId,
                                           UUID organizationRegionId,
                                           UUID registeredBy,
                                           List<UUID> locations,
                                           LocalDate startDate,
                                           LocalDate endDate,
                                           UUID servedBy,
                                           UUID parentCustomerPublicId,
                                           UUID salesAgentPublicId,
                                           Boolean isFromSA,
                                           Principal principal,
                                           Boolean active,
                                           Pageable pageable) {


        if (isFromSA != null && isFromSA) {
            String agentName = principal.getName();

            Optional<SalesAgents> salesAgentsOptional = salesAgentsRepository.findByPhoneNumberAndDeletedIsFalse(agentName);
            if (salesAgentsOptional.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Sales agent not found")
                        .userMessage("Sales agent found");
            }

            SalesAgents salesAgents1 = salesAgentsOptional.get();

            List<OrganizationLocations> organizationLocations = salesAgents1.getOrganizationLocations();

            locations = organizationLocations.stream()
                    .map(OrganizationLocations::getPublicId).distinct().collect(Collectors.toList());
        }

        if (active == null) {
            active = Boolean.TRUE;
        }

        SalesAgents salesAgents = salesAgentsRepository.findByPublicIdAndDeletedIsFalse(servedBy).orElse(null);

        Specification<Customers> spec = Specification
                .where(CustomerSpecification.searchParamMatches(searchParam)) // Search by multiple fields
                .and(CustomerSpecification.hasNameLike(name))  // Filter by name
                .and(CustomerSpecification.hasEmailLike(email)) // Filter by email
                .and(CustomerSpecification.hasNationalIdLike(nationalId)) // Filter by nationalId
                .and(CustomerSpecification.hasPhoneNumberLike(phoneNumber)) // Filter by phoneNumber
                .and(CustomerSpecification.hasSourceChannel(sourceChannel)) // Filter by sourceChannel
                .and(CustomerSpecification.hasOrganizationLocation(organizationLocationId)) // Filter by organizationLocationId
                .and(CustomerSpecification.hasOrganizationRegion(organizationRegionId)) // Filter by organizationRegionId
                .and(CustomerSpecification.hasRegisteredBy(registeredBy))
                .and(CustomerSpecification.isNotDeleted())
                .and(CustomerSpecification.isActive(active))
                .and(CustomerSpecification.hasParentCustomer(parentCustomerPublicId))
                .and(CustomerSpecification.hasServedBy(salesAgents)) // Filter by list of organization locations
                .and(CustomerSpecification.hasOrganizationLocations(locations)) // Filter by list of organization locations
                .and(CustomerSpecification.registrationDateBetween(startDate, endDate)); // Filter by registrationDate range


        Page<Customers> customersPage = customersRepository.findAll(spec, pageable);
        hydrateDeliveredOrderCounts(customersPage.getContent());

        return customersPage;
    }

    private void hydrateDeliveredOrderCounts(List<Customers> customers) {
        if (customers == null || customers.isEmpty()) {
            return;
        }

        List<UUID> customerIds = customers.stream()
                .map(Customers::getPublicId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (customerIds.isEmpty()) {
            return;
        }

        Map<UUID, Long> deliveredOrderCounts = salesOrdersRepository
                .countOrdersByCustomerPublicIdsAndStatus(customerIds, LoadingStatusEnum.DELIVERED)
                .stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]
                ));

        customers.forEach(customer -> customer.setCountDeliveredOrders(
                deliveredOrderCounts.getOrDefault(customer.getPublicId(), 0L)
        ));
    }

    @Override
    public void deleteCustomer(UUID customerPublicId, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<Customers> customerOptional = customersRepository
                .findByPublicIdAndDeletedIsFalse(customerPublicId);
        if (customerOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer with public ID " + customerPublicId + " not found")
                    .userMessage("Customer not found");
        }

        // Mark the customer as deleted
        Customers customer = customerOptional.get();
        optionalPerson.ifPresent(customer::setUpdatedBy);
        customer.setDeleted(true);
        customer.setUpdateDate(LocalDateTime.now());
        customersRepository.save(customer);
    }

    public OrderStats getCustomerOrderStats(UUID customerPublicId) {

        Optional<Customers> customerOptional = customersRepository.findByPublicIdAndDeletedIsFalse(customerPublicId);
        if (customerOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer with public ID " + customerPublicId + " not found")
                    .userMessage("Customer not found");
        }

        long orderCount = salesOrdersRepository.countByCustomerId(customerPublicId);
        BigDecimal averageOrderValue = salesOrdersRepository.calculateAverageOrderValue(customerPublicId);

        return OrderStats.builder()
                .orderCount(orderCount)
                .averageOrderValue(averageOrderValue)
                .build();
    }

    @Override
    public Customers getCustomerProfile(String name) {

        Optional<Customers> optionalCustomers = customersRepository.findByPhoneNumberAndDeletedIsFalse(name);

        if (optionalCustomers.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer with phone number " + name + " not found")
                    .userMessage("Customer not found");
        }
        return optionalCustomers.get();
    }

    @Override
    public List<Customers> getCustomersBySalesAgent(UUID publicId, String from, String to, Pageable pageable) {

        Optional<Customers> optionalCustomers = customersRepository.findByPublicIdAndDeletedIsFalse(publicId);

        if (optionalCustomers.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer with phone number  not found")
                    .userMessage("Customer not found");
        }
        Customers customers = optionalCustomers.get();

        Specification<Customers> spec = Specification
                .where(CustomerSpecification.hasSalesAgent(customers.getPublicId()))
                .and(CustomerSpecification.registrationDateBetween(LocalDate.parse(from), LocalDate.parse(to)));
        return customersRepository.findAll(spec, pageable).getContent();
    }

    @Override
    public Customers createCustomerFromVanSale(DeliverVanSaleItemRequest deliverItemRequest, Person person) {

        if (deliverItemRequest.isExistingCustomer()) {
            Optional<Customers> existingCustomerOptional = customersRepository
                    .findByPublicIdAndDeletedIsFalse(deliverItemRequest.getCustomerPublicId());
            if (existingCustomerOptional.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Customer with public ID " + deliverItemRequest.getCustomerPublicId() + " not found")
                        .userMessage("Customer not found");
            }
            return existingCustomerOptional.get();
        }

        CustomersRequest customersRequest = deliverItemRequest.getCustomersRequest();
        customersRequest.setPhoneNumber(normalizeCustomerPhoneNumber(customersRequest.getPhoneNumber()));
        customersRequest.setOwnerNumber(normalizeOwnerNumber(customersRequest.getOwnerNumber()));
        validateShopKeeperNumber(customersRequest, null);

        // Check if the customer with the given phone number already exists
        Optional<Customers> existingCustomerByPhoneNumber = customersRepository.findByPhoneNumberAndDeletedIsFalse(customersRequest.getPhoneNumber());
        if (existingCustomerByPhoneNumber.isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer with phone number " + customersRequest.getPhoneNumber() + " already exists")
                    .userMessage("A customer with this phone number already exists");
        }

        Customers customer = new Customers();
        if (null != customersRequest.getOrganizationRegionId()) {
            // Validate organization Region
            Optional<OrganizationRegions> organizationRegionsOptional = organizationRegionsRepository
                    .findByPublicIdAndDeletedIsFalse(customersRequest.getOrganizationRegionId());
            if (organizationRegionsOptional.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Organization Region with ID " + customersRequest.getOrganizationRegionId() + " not found")
                        .userMessage("Organization Region not found");
            }
            customer.setOrganizationRegions(organizationRegionsOptional.get());
        }

        if (null != customersRequest.getOrganizationLocationId()) {
            // Validate organization location
            Optional<OrganizationLocations> organizationLocationsOptional = organizationLocationsRepository
                    .findByPublicIdAndDeletedIsFalse(customersRequest.getOrganizationLocationId());
            if (organizationLocationsOptional.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Organization Region with ID " + customersRequest.getOrganizationRegionId() + " not found")
                        .userMessage("Organization Region not found");
            }
            customer.setOrganizationLocation(organizationLocationsOptional.get());
        }

        customer.setName(customersRequest.getName());
        customer.setEmail(customersRequest.getEmail());
        customer.setNationalId(customersRequest.getNationalId());
        customer.setPhoneNumber(customersRequest.getPhoneNumber());
        customer.setRegistrationDate(LocalDate.now());
        customer.setOwnerNumber(customersRequest.getOwnerNumber());
        customer.setShopKeeperNumber(customersRequest.getShopKeeperNumber());
        customer.setSourceChannel(customersRequest.getSourceChannel());
        customer.setLatitude(customersRequest.getLatitude());
        customer.setLongitude(customersRequest.getLongitude());
        customer.setAddress(customersRequest.getAddress());
        customer.setLandmark(customersRequest.getLandmark());
        customer.setTin(customersRequest.getTin());
        customer.setBnplVerificationStatus(CustomerBnplVerificationStatus.NOT_VERIFIED);
        customer.setCustomerType(CustomerType.REGULAR);
        if (person != null) {
            customer.setCreatedBy(person);
        }
        return customersRepository.save(customer);
    }


    @Override
    public List<CustomerLocationResponse> getCustomerLocations() {

        List<CustomerLocationResponse> customerLocationResponses = new ArrayList<>();

        customersRepository.findAllByDeletedIsFalse().forEach(customer -> {
            CustomerLocationResponse response = new CustomerLocationResponse();
            response.setCustomerPublicId(customer.getPublicId());
            response.setCustomerName(customer.getName());
            response.setLatitude(customer.getLatitude());
            response.setLongitude(customer.getLongitude());
            response.setStatus(customer.getCustomerActiveStatus());
            customerLocationResponses.add(response);
        });

        return customerLocationResponses;
    }

    @Override
    public CustomerPageable getSupplierCustomers(UUID regionId, LocalDate startDate, LocalDate endDate,
                                                 Pageable pageable,
                                                 Principal principal) {

        Page<Customers> customersPage;

        if (regionId != null && startDate != null && endDate != null && !startDate.isAfter(endDate)) {
            OrganizationRegions organizationRegions =
                    organizationRegionsRepository.findByPublicIdAndDeletedIsFalse(regionId).orElse(null);
            LocalDateTime localStartDate = startDate.atStartOfDay();
            LocalDateTime localEndDate = endDate.atTime(LocalTime.MAX);
            customersPage = customersRepository.findByOrganizationRegionsAndCreationTimeBetween(
                    organizationRegions, localStartDate, localEndDate, pageable);
        } else if (regionId == null && startDate != null && endDate != null && !startDate.isAfter(endDate)) {
            LocalDateTime localStartDate = startDate.atStartOfDay();
            LocalDateTime localEndDate = endDate.atTime(LocalTime.MAX);
            customersPage = customersRepository.findByCreationTimeBetween(
                    localStartDate, localEndDate, pageable);
        } else {
            customersPage = customersRepository.findAll(pageable);
        }

        CustomerPageable customerPageable = new CustomerPageable();

        List<CustomerDTO> customerDTOList = new ArrayList<>();

        customersPage.getContent().forEach(customer -> {
            CustomerDTO customerDTO = new CustomerDTO();
            customerDTO.setName(customer.getName());
            customerDTO.setCustomerPublicId(customer.getPublicId());
            customerDTO.setRegion(customer.getOrganizationRegions() != null ? customer.getOrganizationRegions().getName() : null);
            customerDTO.setLocation(customer.getOrganizationLocation() != null ? customer.getOrganizationLocation().getName() : null);
            customerDTO.setZone(customer.getOrganizationLocation() != null ? customer.getOrganizationLocation().getName() : null);
            customerDTO.setLandmark(customer.getLandmark());
            customerDTO.setLatitude(customer.getLatitude());
            customerDTO.setLongitude(customer.getLongitude());
            customerDTO.setPhoneNumber(customer.isShowPhoneNumberInSupportChat() ? phoneNumberHelper.formatPhoneNumberForDisplay(customer.getPhoneNumber()) : "Hidden");
            customerDTOList.add(customerDTO);
        });

        PageImpl<CustomerDTO> customerDTOPage = new PageImpl<>(customerDTOList, pageable, customerDTOList.size());
        customerPageable.setContent(customerDTOPage.getContent());
        customerPageable.setFirst(customerDTOPage.isFirst());
        customerPageable.setLast(customerDTOPage.isLast());
        customerPageable.setPageNumber(customerDTOPage.getNumber());
        customerPageable.setPageSize(customerDTOPage.getSize());
        customerPageable.setTotalElements(customerDTOPage.getTotalElements());
        customerPageable.setTotalPages(customerDTOPage.getTotalPages());

        return customerPageable;
    }

    @Override
    public void showSupplierCustomerPhoneNumber(UUID customerPublicId, String name) {

        Customers customer = getCustomer(customerPublicId);
        customer.setShowPhoneNumberInSupportChat(true);
        customersRepository.save(customer);
    }

    @Override
    @Transactional
    public void fixCustomerDuplicate() {

        List<String> duplicatePhones = customersRepository.findDuplicatePhoneNumbers();

        for (String phone : duplicatePhones) {
            String lastNineDigits = getLastNineDigits(phone);
            List<Customers> customers = customersRepository.findByPhoneNumberLastNine(lastNineDigits);

            if (customers.size() <= 1) {
                continue;
            }

            Customers parent = customers.stream()
                    .min(Comparator.comparing(Customers::getId))
                    .orElseThrow(() -> new IllegalStateException("No parent customer found"));

            log.info("Merging {} duplicates for phone {}", customers.size(), phone);

            for (Customers duplicate : customers) {
                if (duplicate.equals(parent)) {
                    continue;
                }

                List<SalesOrders> salesOrderList = salesOrdersRepository.findByCustomerAndDeletedIsFalse(duplicate.getPublicId());

                if (salesOrderList == null || salesOrderList.isEmpty()) {
                    log.info("No orders found for duplicate customer {}", duplicate.getId());

                    log.info("Merging {} duplicates for phone {}", customers.size(), phone);
                } else {
                    for (SalesOrders order : salesOrderList) {
                        order.setCustomer(parent);
                    }
                    salesOrdersRepository.saveAll(salesOrderList);
                    log.info("Reassigned {} orders from duplicate {} to parent {}",
                            salesOrderList.size(), duplicate.getId(), parent.getId());
                }

                deleteCustomer(duplicate);
            }
        }
    }

    public void deleteCustomer(Customers customer) {

        log.info("Deleted duplicate customer {}", customer.getId());
        customer.setDeleted(true);
        customer.setUpdateDate(LocalDateTime.now());
        customersRepository.save(customer);
    }

    @Override
    public void changeCustomerType(UUID customerPublicId, CustomerType customerType, String name) {
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);


        Customers customer = getCustomer(customerPublicId);
        boolean changeFromCooperateToRegular = customerType == CustomerType.REGULAR;

        if (customer.isAwaitingCustomerTypeChange()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer type change in progress")
                    .userMessage("Customer type change in progress");
        }

        optionalPerson.ifPresent(customer::setUpdatedBy);
        customer.setUpdateDate(LocalDateTime.now());

        customer.setCustomerType(changeFromCooperateToRegular ? CustomerType.CORPORATE : CustomerType.REGULAR);
        customer.setAwaitingCustomerTypeChange(true);
        bnplHelper.processCorporateCustomer(customer, changeFromCooperateToRegular);

        customersRepository.save(customer);
    }

    @Override
    public ByteArrayOutputStream getAllSalesOrders(UUID regionId, UUID locationId,
                                                   String searchParam, StatementExportType exportType,
                                                   LocalDate startDate, LocalDate endDate, Pageable pageable, Principal principal) {
        List<Customers> content =
                getAllSupplierCustomers(regionId, locationId, searchParam, startDate, endDate, principal, pageable).getContent();
        return supplierCustomer.createPrintOut(content, exportType, principal.getName());
    }

    @Override
    public void customerActions(UUID customerPublicId, CustomerAction customerAction, String email) {
        Person loggedInUser = getLoggedInUser(email);
        Customers customer = getCustomer(customerPublicId);

        if (customerAction.actionType() == CustomerAction.ActionType.De_Activate) {
            if (customerAction.rejectionCode() == null || customerAction.rejectionCode().isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Missing rejection code.")
                        .userMessage("Sorry, Missing rejection reason");
            }


            customer.setActive(false);
            customer.setStatus(CustomerStatus.Pending_DeActivation);
            customer.setRejectionReason(customerAction.rejectionCode());
        }

        if (customerAction.actionType() == CustomerAction.ActionType.Approve_DeActivate) {
            customer.setStatus(CustomerStatus.De_Activated);
        }

        if (customerAction.actionType() == CustomerAction.ActionType.Activate) {
            customer.setStatus(CustomerStatus.Active);
            customer.setActive(true);
        }

        customer.setUpdatedBy(loggedInUser);
        customersRepository.save(customer);
    }

    public Person getLoggedInUser(String email) {
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(email);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person with email " + email + " not found")
                    .userMessage("Person not found");
        }
        return optionalPerson.get();
    }

    @Override
    public void recalculateAov() {

        List<Customers> customers = customersRepository.findAllByBnplAccountCreatedIsFalseAndDeletedIsFalse();

        for (Customers customer : customers) {
            if (customer.getCustomerType() != null) {
                if (customer.getCustomerType() == CustomerType.REGULAR) {
                    applyRegularCustomerAov(customer);
                } else if (customer.getCustomerType() == CustomerType.CORPORATE) {
                    customer.setLimitedOfferEligibility(true);
                }
            }
            customersRepository.save(customer);
        }
    }

    private void applyRegularCustomerAov(Customers customer) {
        BigDecimal aov = salesOrdersRepository.calculateAverageOrderValue(customer.getPublicId());
        if (aov != null && aov.compareTo(BigDecimal.ZERO) > 0) {
            customer.setPreliminaryValue(aov);
            Optional<CreditRule> optionalCreditRule = creditRuleRepository.findCreditRuleByCustomerTypeAndCreditLimitAndDeletedIsFalse(customer.getCustomerType(), true);
            if (optionalCreditRule.isPresent()) {
                CreditRule creditRule = optionalCreditRule.get();
                BigDecimal creditLimit = aov.multiply(creditRule.getCreditLimitPercentage()).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
                customer.setLimitedOfferEligibility(creditLimit.compareTo(new BigDecimal("50000")) >= 0);
            }
        } else {
            customer.setPreliminaryValue(BigDecimal.ZERO);
            customer.setLimitedOfferEligibility(false);
        }
    }

    @Override
    public void recalculateAovWithUIds(List<UUID> customerIds) {
        List<Customers> customers = new ArrayList<>();

        for (UUID customerId : customerIds) {
            Optional<Customers> customerOptional = customersRepository.findByPublicIdAndDeletedIsFalse(customerId);
            customerOptional.ifPresent(customers::add);
        }
        for (Customers customer : customers) {
            if (customer.getCustomerType() == CustomerType.REGULAR) {
                applyRegularCustomerAov(customer);
                customersRepository.save(customer);
            }
        }
    }

    @Override
    public Page<Customers> getAllSupplierCustomers(UUID regionId,
                                                   UUID locationId,
                                                   String searchParam,
                                                   LocalDate startDate,
                                                   LocalDate endDate,
                                                   Principal principal,
                                                   Pageable pageable) {

        String email = principal.getName();
        Person person = personRepository.findByEmailAndDeletedIsFalse(email).orElse(null);

        Suppliers suppliers = supplierRepository
                .findByPublicIdAndDeletedIsFalse(UUID.fromString(person.getSupplierPublicId()))
                .orElse(null);

        if (suppliers == null) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Customers> cq = cb.createQuery(Customers.class);

        Root<SalesOrders> salesOrderRoot = cq.from(SalesOrders.class);
        Join<SalesOrders, SalesOrderItems> itemsJoin = salesOrderRoot.join("salesOrderItems");
        Join<SalesOrderItems, Product> productJoin = itemsJoin.join("product");
        Join<Product, Suppliers> supplierJoin = productJoin.join("suppliers");
        Join<SalesOrders, Customers> customerJoin = salesOrderRoot.join("customer");

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(supplierJoin.get("publicId"), suppliers.getPublicId()));

        if (regionId != null) {
            predicates.add(cb.equal(customerJoin.get("organizationRegions").get("publicId"), regionId));
        }

        if (locationId != null) {
            predicates.add(cb.equal(customerJoin.get("organizationLocation").get("publicId"), locationId));
        }

        if (searchParam != null && !searchParam.trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(customerJoin.get("name")), "%" + searchParam.trim().toLowerCase() + "%"));
        }

        if (startDateTime != null) {
            predicates.add(cb.greaterThanOrEqualTo(salesOrderRoot.get("deliveryDate"), startDateTime));
        }
        if (endDateTime != null) {
            predicates.add(cb.lessThanOrEqualTo(salesOrderRoot.get("deliveryDate"), endDateTime));
        }

        cq.select(customerJoin).distinct(true).where(predicates.toArray(new Predicate[0]));

        TypedQuery<Customers> query = entityManager.createQuery(cq);

        // Apply pagination
        int totalRows = query.getResultList().size();
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Customers> customers = query.getResultList();
        return new PageImpl<>(customers, pageable, totalRows);
    }

    @Override
    @Transactional
    public void updateCustomerCallStatus(UUID customerPublicId, UpdateCustomerCallStatusRequest request, String principalName) {
        Person actionedBy = personRepository.findByEmailAndDeletedIsFalse(principalName).orElse(null);
        if (actionedBy == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("user not found")
                    .userMessage("user not found");
        }

        Customers customer = customersRepository.findByPublicIdAndDeletedIsFalse(customerPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Customer not found: " + customerPublicId)
                        .userMessage("Customer not found"));

        String catalogCode = request.getStatus().catalogCode();
        CallStatus callStatus = callStatusRepository
                .findByCodeIgnoreCaseAndActiveIsTrueAndDeletedIsFalse(catalogCode)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Active call status not found for outcome: " + catalogCode)
                        .userMessage("Call status not found or is no longer active for this outcome"));
        if (request.isOrderAmendment()) {
            if (request.getAmendmentPublicId() == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Amendment details missing")
                        .userMessage("Sorry, amendment details missing");
            }

            OrderAmendment orderAmendment = orderAmendmentRepository.findByPublicIdAndDeletedIsFalse(request.getAmendmentPublicId()).orElse(null);
            if (orderAmendment == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Amendment not found")
                        .userMessage("Sorry, amendment not found");
            }

            orderAmendment.setCallStatus(callStatus);
            orderAmendment.setUpdatedBy(actionedBy);
            OrderAmendment saveOrderAmendment = orderAmendmentRepository.save(orderAmendment);

            updateCallHistory(customer, actionedBy, callStatus.getCode(), request.isOrderAmendment(), false, saveOrderAmendment.getPublicId(), false);
        } else if (request.isChurned()) {
            updateCallHistory(customer, actionedBy, callStatus.getCode(), false, request.isChurned(), null, false);
        } else if (request.isLocationChange()) {
            LocationApproval locationApproval = locationApprovalRepository.findByPublicIdAndDeletedIsFalse(request.getLocationApprovalPublicId()).orElse(null);
            if (locationApproval == null) {
                throw new BadRequestRestApiException()
                        .developerMessage("Location approval not found")
                        .userMessage("Sorry, location approval not found");
            }
            locationApproval.setCallStatus(callStatus.getCode());
            locationApprovalRepository.save(locationApproval);
            updateCallHistory(customer, actionedBy, callStatus.getCode(), false, false, null, request.isLocationChange());
        } else {
            customer.setCallStatus(callStatus);
            customer.setLastContactDate(LocalDateTime.now());
            personRepository.findByEmailAndDeletedIsFalse(principalName)
                    .ifPresent(customer::setUpdatedBy);

            customersRepository.save(customer);
            updateCallHistory(customer, actionedBy, callStatus.getCode(), false, false, null, false);
            //TODO add bnpl
        }
    }

    @Override
    public Customers getCustomerByPhone(String customerPhoneNumber) {
        Customers customers = customersRepository.findByPhoneNumberIgnoreCaseAndDeletedIsFalse(customerPhoneNumber).orElse(null);
        if (customers == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer with phone number " + customerPhoneNumber + " not found")
                    .userMessage("Customer not found");
        }
        return customers;
    }

    @Override
    @Transactional
    public void updateCustomerLeadStatus(UUID customerPublicId, UpdateCustomerLeadStatusRequest request, String principalName) {
        Customers customer = customersRepository.findByPublicIdAndDeletedIsFalse(customerPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Customer not found: " + customerPublicId)
                        .userMessage("Customer not found"));
        customer.setLeadStatus(request.getLeadStatus());
        customersRepository.save(customer);
        Person actionedBy = principalName == null
                ? null
                : personRepository.findByEmailAndDeletedIsFalse(principalName).orElse(null);
        appendLeadStatusHistory(customer, actionedBy, request.getLeadStatus());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChurnedCustomer> getChurnedCustomer(
            ChurnedDateRangeFilter rangeFilter,
            CustomerType customerType,
            UUID callStatusPublicId,
            LocalDate startDate,
            LocalDate endDate,
            String searchParam,
            CustomerOutboundCallStatus callStatus,
            UUID departmentPublicId,
            ChurnedCustomerLeadStatus leadStatus,
            Pageable pageable) {

        if (rangeFilter == null) {
            rangeFilter = ChurnedDateRangeFilter.Range_30_120;
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Customers> cq = cb.createQuery(Customers.class);
        Root<Customers> root = cq.from(Customers.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.isFalse(root.get("deleted")));

        if (customerType != null) {
            predicates.add(cb.equal(root.get("customerType"), customerType));
        }

        if (callStatusPublicId != null) {
            predicates.add(cb.equal(root.get("callStatus").get("publicId"), callStatusPublicId));
        }

        if (callStatus != null) {
            CallStatus status = callStatusRepository
                    .findByCodeIgnoreCaseAndDeletedIsFalse(callStatus.catalogCode())
                    .orElse(null);
            predicates.add(cb.equal(root.get("callStatus"), status));
        }

        if (leadStatus != null) {
            predicates.add(cb.equal(root.get("leadStatus"), leadStatus));
        }

        if (departmentPublicId != null) {
            predicates.add(cb.equal(root.get("customerNotes")
                    .get("reasonCode")
                    .get("department")
                    .get("publicId"), departmentPublicId));
        }

        LocalDateTime baseDate = endDate == null ? LocalDateTime.now() : endDate.atStartOfDay();
        LocalDateTime startDateTime = baseDate.minusDays(rangeFilter.getMaxDays());
        LocalDateTime endDateTime = baseDate.minusDays(rangeFilter.getMinDays());
        predicates.add(cb.between(root.get("lastOrderDate"), startDateTime, endDateTime));

        if (searchParam != null && searchParam.trim().length() >= 3) {
            String search = "%" + searchParam.toUpperCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.upper(root.get("name")), search),
                    cb.like(cb.upper(root.get("firstName")), search),
                    cb.like(cb.upper(root.get("lastName")), search),
                    cb.like(cb.upper(root.get("phoneNumber")), search),
                    cb.like(cb.upper(root.get("storeName")), search)
            ));
        }

        cq.select(root).where(cb.and(predicates.toArray(new Predicate[0])));

        Expression<Integer> untaggedFirst = cb.<Integer>selectCase()
                .when(cb.isNull(root.get("callStatus")), 0)
                .otherwise(1);

        Expression<Integer> leadStatusOrder = cb.<Integer>selectCase()
                .when(cb.isNull(root.get("leadStatus")), 0)
                .when(cb.equal(root.get("leadStatus"), ChurnedCustomerLeadStatus.GREEN), 1)
                .when(cb.equal(root.get("leadStatus"), ChurnedCustomerLeadStatus.YELLOW), 2)
                .when(cb.equal(root.get("leadStatus"), ChurnedCustomerLeadStatus.RED), 3)
                .otherwise(4);

        if (callStatusPublicId != null) {
            cq.orderBy(cb.asc(untaggedFirst), cb.asc(leadStatusOrder), cb.asc(root.get("callBackDate")));
        } else {
            cq.orderBy(cb.asc(untaggedFirst), cb.asc(leadStatusOrder), cb.asc(root.get("lastOrderDate")));
        }

        List<Customers> customers = entityManager.createQuery(cq)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        List<ChurnedCustomer> churnedCustomerList = customers.stream()
                .map(c -> new ChurnedCustomer(
                        c.getPublicId(),
                        c.getName(),
                        c.getPhoneNumber(),
                        c.getLastOrderDate(),
                        c.getServedBy() != null ? c.getServedBy().getName() : null,
                        c.getLastContactDate(),
                        c.getOrganizationRegions() != null ? c.getOrganizationRegions().getName() : null,
                        c.getOrganizationLocation() != null ? c.getOrganizationLocation().getName() : null,
                        c.getCustomerType(),
                        c.getCallStatus() != null ? new CallStatusSummary(
                                c.getCallStatus().getPublicId(),
                                c.getCallStatus().getCode(),
                                c.getCallStatus().getDescription()
                        ) : null,
                        c.getLeadStatus(),
                        c.getStoreName(),
                        c.getFirstName(),
                        c.getLastName(),
                        c.getCustomerNotes().stream()
                                .max(Comparator.comparing(CustomerNotes::getCreationTime))
                                .map(n -> new NoteSummary(
                                        n.getReasonCode() != null ? n.getReasonCode().getCode() : null,
                                        n.getReasonCode() != null ? n.getReasonCode().getDescription() : null,
                                        n.getReasonCode() != null && n.getReasonCode().getDepartment() != null
                                                ? n.getReasonCode().getDepartment().getName()
                                                : null
                                ))
                                .stream()
                                .toList(),
                        c.getPhoneNumber(),
                        c.getShopKeeperNumber(),
                        c.getCallMadeBy(),
                        c.getPreviousOrderDate(),
                        c.getFollowUpDate()
                ))
                .toList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Customers> countRoot = countQuery.from(Customers.class);

        List<Predicate> countPredicates = rebuildPredicates(
                cb,
                countRoot,
                countRoot.join("callStatus", JoinType.LEFT),
                rangeFilter,
                customerType,
                callStatusPublicId,
                startDate,
                endDate,
                searchParam,
                callStatus,
                departmentPublicId,
                leadStatus
        );

        countQuery.select(cb.countDistinct(countRoot))
                .where(cb.and(countPredicates.toArray(new Predicate[0])));

        long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(churnedCustomerList, pageable, total);
    }


    private Order resolveChurnSort(CriteriaBuilder cb, Root<Customers> root,
                                   Join<Customers, CallStatus> callStatusJoin, Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            org.springframework.data.domain.Sort.Order first = pageable.getSort().iterator().next();
            String property = first.getProperty();
            boolean ascending = first.isAscending();

            if ("callStatus".equalsIgnoreCase(property) || "callStatusCode".equalsIgnoreCase(property)) {
                return ascending
                        ? cb.asc(cb.coalesce(callStatusJoin.get("code"), ""))
                        : cb.desc(cb.coalesce(callStatusJoin.get("code"), ""));
            }
            if ("lastContactDate".equalsIgnoreCase(property)) {
                return ascending
                        ? cb.asc(root.get("lastContactDate"))
                        : cb.desc(root.get("lastContactDate"));
            }
            if ("name".equalsIgnoreCase(property)) {
                return ascending
                        ? cb.asc(root.get("name"))
                        : cb.desc(root.get("name"));
            }
        }
        return cb.desc(root.get("lastOrderDate"));
    }

    private List<Predicate> rebuildPredicates(CriteriaBuilder cb, Root<Customers> root,
                                              Join<Customers, CallStatus> callStatusJoin,
                                              ChurnedDateRangeFilter rangeFilter, CustomerType customerType,
                                              UUID callStatusPublicId, LocalDate startDate, LocalDate endDate,
                                              String searchParam, CustomerOutboundCallStatus callStatus,
                                              UUID departmentPublicId, ChurnedCustomerLeadStatus leadStatus) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.isFalse(root.get("deleted")));

        if (customerType != null) {
            predicates.add(cb.equal(root.get("customerType"), customerType));
        }
        if (callStatusPublicId != null) {
            predicates.add(cb.equal(callStatusJoin.get("publicId"), callStatusPublicId));
        }

        if (callStatus != null) {
            CallStatus status = callStatusRepository.findByCodeIgnoreCaseAndDeletedIsFalse(callStatus.catalogCode()).orElse(null);
            predicates.add(cb.equal(root.get("callStatus"), status));
        }

        if (leadStatus != null) {
            predicates.add(cb.equal(root.get("leadStatus"), leadStatus));
        }

        if (departmentPublicId != null) {
            predicates.add(cb.equal(root.get("customerNotes").get("reasonCode").get("department").get("publicId"), departmentPublicId));
        }

        LocalDateTime baseDate = endDate == null ? LocalDateTime.now() : endDate.atStartOfDay();

        LocalDateTime startDateTime = baseDate.minusDays(rangeFilter.getMaxDays());
        LocalDateTime endDateTime = baseDate.minusDays(rangeFilter.getMinDays());
        predicates.add(cb.between(root.get("lastOrderDate"), startDateTime, endDateTime));

        if (searchParam != null && searchParam.trim().length() >= 3) {
            String search = "%" + searchParam.toUpperCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.upper(root.get("name")), search),
                    cb.like(cb.upper(root.get("firstName")), search),
                    cb.like(cb.upper(root.get("lastName")), search),
                    cb.like(cb.upper(root.get("phoneNumber")), search),
                    cb.like(cb.upper(root.get("storeName")), search)
            ));
        }
        return predicates;
    }

    @Override
    public ByteArrayOutputStream runExportChurnedCustomers(ChurnedDateRangeFilter rangeFilter, CustomerType customerType,
                                                           UUID callStatusPublicId, String name,
                                                           StatementExportType statementExportType,
                                                           LocalDate startDate, LocalDate endDate,
                                                           String searchParam, CustomerOutboundCallStatus callStatus,
                                                           UUID departmentPublicId, ChurnedCustomerLeadStatus leadStatus,
                                                           Pageable pageable) throws DocumentException, IOException {
        List<ChurnedCustomer> fetchedList = getChurnedCustomer(
                rangeFilter, customerType, callStatusPublicId, startDate, endDate, searchParam, callStatus,
                departmentPublicId, leadStatus, pageable).getContent();
        if (statementExportType == StatementExportType.EXCEL) {
            return churnedCustomerExportHelper.createExport(fetchedList, name, null);
        } else {
            return churnedCustomerExportHelper.createPdf(fetchedList, name, null);
        }
    }

    @Override
    public Page<ChurnedCustomer> getNonChurnedInActiveCustomers(
            CustomerType customerType,
            UUID callStatusPublicId,
            LocalDate startDate,
            LocalDate endDate,
            String searchParam,
            Boolean inActive,
            CustomerOutboundCallStatus callStatus,
            UUID departmentPublicId,
            Pageable pageable) {

        if (inActive != null && inActive == Boolean.TRUE) {
            return fetchCustomersWhoHaveNeverPlacedAnOrder(customerType, callStatusPublicId,
                    startDate, endDate, searchParam, Boolean.TRUE, callStatus, departmentPublicId, pageable);
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Customers> cq = cb.createQuery(Customers.class);
        Root<Customers> root = cq.from(Customers.class);
        Join<Customers, OrganizationRegions> district = root.join("organizationRegions", JoinType.LEFT);
        Join<Customers, OrganizationLocations> ward = root.join("organizationLocation", JoinType.LEFT);
        Join<Customers, SalesAgents> agents = root.join("servedBy", JoinType.LEFT);
        Join<Customers, CallStatus> callStatusJoin = root.join("callStatus", JoinType.LEFT);

        List<Predicate> predicates = buildNonChurnedPredicates(
                cb, root, callStatusJoin,
                customerType, callStatusPublicId,
                startDate, endDate, searchParam,
                ChurnedDateRangeFilter.Range_30_120, callStatus);

        cq.select(root).where(cb.and(predicates.toArray(new Predicate[0])));
        cq.orderBy(resolveChurnSort(cb, root, callStatusJoin, pageable));

        List<Customers> customers = entityManager.createQuery(cq)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        // Transform to DTO including notes
        List<ChurnedCustomer> churnedCustomerList = customers.stream()
                .map(c -> new ChurnedCustomer(
                        c.getPublicId(),
                        c.getName(),
                        c.getPhoneNumber(),
                        c.getLastOrderDate(),
                        c.getServedBy() != null ? c.getServedBy().getName() : null,
                        c.getLastContactDate(),
                        c.getOrganizationRegions() != null ? c.getOrganizationRegions().getName() : null,
                        c.getOrganizationLocation() != null ? c.getOrganizationLocation().getName() : null,
                        c.getCustomerType(),
                        c.getCallStatus() != null ? new CallStatusSummary(
                                c.getCallStatus().getPublicId(),
                                c.getCallStatus().getCode(),
                                c.getCallStatus().getDescription()
                        ) : null,
                        c.getLeadStatus(),
                        c.getStoreName(),
                        c.getFirstName(),
                        c.getLastName(),
                        c.getCustomerNotes().stream()
                                .max(Comparator.comparing(CustomerNotes::getCreationTime))
                                .map(n -> new NoteSummary(
                                        n.getReasonCode() != null ? n.getReasonCode().getCode() : null,
                                        n.getReasonCode() != null ? n.getReasonCode().getDescription() : null,
                                        n.getReasonCode() != null && n.getReasonCode().getDepartment() != null
                                                ? n.getReasonCode().getDepartment().getName()
                                                : null
                                ))
                                .stream()
                                .toList(),
                        c.getPhoneNumber(),
                        c.getShopKeeperNumber(),
                        c.getCallMadeBy(),
                        c.getPreviousOrderDate(),
                        c.getFollowUpDate()
                ))
                .toList();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Customers> countRoot = countQuery.from(Customers.class);
        Join<Customers, CallStatus> countCallStatusJoin = countRoot.join("callStatus", JoinType.LEFT);

        List<Predicate> countPredicates = buildNonChurnedPredicates(
                cb, countRoot, countCallStatusJoin,
                customerType, callStatusPublicId,
                startDate, endDate, searchParam,
                ChurnedDateRangeFilter.Range_30_120, callStatus);

        countQuery.select(cb.countDistinct(countRoot))
                .where(cb.and(countPredicates.toArray(new Predicate[0])));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(churnedCustomerList, pageable, total);
    }

    @Override
    public ByteArrayOutputStream runExportNonChurnedInActiveCustomers(Boolean inActive, CustomerType customerType, UUID callStatusPublicId, String name, StatementExportType exportType, LocalDate startDate, LocalDate endDate, String searchParam, CustomerOutboundCallStatus callStatus, UUID departmentPublicId, Pageable pageable) throws DocumentException, IOException {
        Pageable exportPageable = PageRequest.of(0, Integer.MAX_VALUE, pageable.getSort());
        List<ChurnedCustomer> fetchedList = getNonChurnedInActiveCustomers(
                customerType,
                callStatusPublicId,
                startDate,
                endDate,
                searchParam,
                inActive,
                callStatus,
                departmentPublicId,
                exportPageable).getContent();

        if (inActive == null) {
            inActive = Boolean.FALSE;
        }
        if (exportType == StatementExportType.EXCEL) {
            return churnedCustomerExportHelper.createExport(fetchedList, name, inActive);
        } else {
            return churnedCustomerExportHelper.createPdf(fetchedList, name, inActive);
        }
    }


    private Page<ChurnedCustomer> fetchCustomersWhoHaveNeverPlacedAnOrder(
            CustomerType customerType,
            UUID callStatusPublicId,
            LocalDate startDate,
            LocalDate endDate,
            String searchParam,
            Boolean inActive,
            CustomerOutboundCallStatus callStatus,
            UUID departmentPublicId,
            Pageable pageable) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Customers> cq = cb.createQuery(Customers.class);
        Root<Customers> root = cq.from(Customers.class);
        Join<Customers, OrganizationRegions> district = root.join("organizationRegions", JoinType.LEFT);
        Join<Customers, OrganizationLocations> ward = root.join("organizationLocation", JoinType.LEFT);
        Join<Customers, SalesAgents> agents = root.join("servedBy", JoinType.LEFT);
        Join<Customers, CallStatus> callStatusJoin = root.join("callStatus", JoinType.LEFT);

        List<Predicate> predicates = buildInActiveCustomersPredicates(
                cb, root, callStatusJoin,
                customerType, callStatusPublicId, searchParam, callStatus, departmentPublicId);

        cq.select(root).where(cb.and(predicates.toArray(new Predicate[0])));
        cq.orderBy(resolveChurnSort(cb, root, callStatusJoin, pageable));

        List<Customers> customers = entityManager.createQuery(cq)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        // Transform to DTO including notes
        List<ChurnedCustomer> churnedCustomerList = customers.stream()
                .map(c -> new ChurnedCustomer(
                        c.getPublicId(),
                        c.getName(),
                        c.getPhoneNumber(),
                        c.getLastOrderDate(),
                        c.getServedBy() != null ? c.getServedBy().getName() : null,
                        c.getLastContactDate(),
                        c.getOrganizationRegions() != null ? c.getOrganizationRegions().getName() : null,
                        c.getOrganizationLocation() != null ? c.getOrganizationLocation().getName() : null,
                        c.getCustomerType(),
                        c.getCallStatus() != null ? new CallStatusSummary(
                                c.getCallStatus().getPublicId(),
                                c.getCallStatus().getCode(),
                                c.getCallStatus().getDescription()
                        ) : null,
                        c.getLeadStatus(),
                        c.getStoreName(),
                        c.getFirstName(),
                        c.getLastName(),
                        c.getCustomerNotes().stream()
                                .max(Comparator.comparing(CustomerNotes::getCreationTime))
                                .map(n -> new NoteSummary(
                                        n.getReasonCode() != null ? n.getReasonCode().getCode() : null,
                                        n.getReasonCode() != null ? n.getReasonCode().getDescription() : null,
                                        n.getReasonCode() != null && n.getReasonCode().getDepartment() != null
                                                ? n.getReasonCode().getDepartment().getName()
                                                : null
                                ))
                                .stream()
                                .toList(),
                        c.getPhoneNumber(),
                        c.getShopKeeperNumber(),
                        c.getCallMadeBy(),
                        c.getPreviousOrderDate(),
                        c.getFollowUpDate()
                ))
                .toList();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Customers> countRoot = countQuery.from(Customers.class);
        Join<Customers, CallStatus> countCallStatusJoin = countRoot.join("callStatus", JoinType.LEFT);

        List<Predicate> countPredicates = buildInActiveCustomersPredicates(
                cb, countRoot, countCallStatusJoin,
                customerType, callStatusPublicId, searchParam, callStatus, departmentPublicId);

        countQuery.select(cb.countDistinct(countRoot))
                .where(cb.and(countPredicates.toArray(new Predicate[0])));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(churnedCustomerList, pageable, total);
    }


    private List<Predicate> buildNonChurnedPredicates(
            CriteriaBuilder cb,
            Root<Customers> root,
            Join<Customers, CallStatus> callStatusJoin,
            CustomerType customerType,
            UUID callStatusPublicId,
            LocalDate startDate,
            LocalDate endDate,
            String searchParam,
            ChurnedDateRangeFilter rangeFilter, CustomerOutboundCallStatus callStatus) {

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.isFalse(root.get("deleted")));

        if (customerType != null) {
            predicates.add(cb.equal(root.get("customerType"), customerType));
        }

        if (callStatusPublicId != null) {
            predicates.add(cb.equal(callStatusJoin.get("publicId"), callStatusPublicId));
        }

        if (callStatus != null) {
            CallStatus status = callStatusRepository.findByCodeIgnoreCaseAndDeletedIsFalse(callStatus.catalogCode()).orElse(null);
            predicates.add(cb.equal(root.get("callStatus"), status));
        }

        LocalDateTime baseDate = endDate == null ? LocalDateTime.now() : endDate.atStartOfDay();
        LocalDateTime startDateTime = baseDate.minusDays(rangeFilter.getMaxDays());
        predicates.add(cb.lessThan(root.get("lastOrderDate"), startDateTime));

        if (searchParam != null && searchParam.trim().length() >= 3) {
            String search = "%" + searchParam.toUpperCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.upper(root.get("name")), search),
                    cb.like(cb.upper(root.get("firstName")), search),
                    cb.like(cb.upper(root.get("lastName")), search),
                    cb.like(cb.upper(root.get("phoneNumber")), search)
            ));
        }

        return predicates;
    }

    private List<Predicate> buildInActiveCustomersPredicates(
            CriteriaBuilder cb,
            Root<Customers> root,
            Join<Customers, CallStatus> callStatusJoin,
            CustomerType customerType,
            UUID callStatusPublicId,
            String searchParam, CustomerOutboundCallStatus callStatus, UUID departmentPublicId) {

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.isFalse(root.get("deleted")));

        if (customerType != null) {
            predicates.add(cb.equal(root.get("customerType"), customerType));
        }

        if (callStatusPublicId != null) {
            predicates.add(cb.equal(callStatusJoin.get("publicId"), callStatusPublicId));
        }

        if (callStatus != null) {
            CallStatus status = callStatusRepository.findByCodeIgnoreCaseAndDeletedIsFalse(callStatus.catalogCode()).orElse(null);
            predicates.add(cb.equal(root.get("callStatus"), status));
        }

        if (departmentPublicId != null) {
            predicates.add(cb.equal(root.get("customerNotes").get("reasonCode").get("department").get("publicId"), departmentPublicId));
        }

        predicates.add(cb.isNull(root.get("lastOrderDate")));

        if (searchParam != null && searchParam.trim().length() >= 3) {
            String search = "%" + searchParam.toUpperCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.upper(root.get("name")), search),
                    cb.like(cb.upper(root.get("firstName")), search),
                    cb.like(cb.upper(root.get("lastName")), search),
                    cb.like(cb.upper(root.get("phoneNumber")), search)
            ));
        }

        return predicates;
    }

    @Override
    public void activateDeactivateChildCustomer(UUID customerPublicId, Boolean activate, String name) {
        Customers customer = customersRepository.findByPublicIdAndDeletedIsFalse(customerPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Customer not found: " + customerPublicId)
                        .userMessage("Customer not found"));

        //validate parent customer

        Customers parentCustomer = customersRepository.findByPhoneNumberIgnoreCaseAndDeletedIsFalse(name)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Parent customer not found: " + name)
                        .userMessage("Parent customer not found"));


        if (customer.getParentCustomer() == null || customer.getParentCustomer() != parentCustomer) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer is not a child of the parent customer: " + name)
                    .userMessage("Customer is not a child of the parent customer");
        }

        if (activate) {
            customer.setCanChildCustomerPlaceOrder(true);
            customer.setUpdatedBy(personRepository.findByEmailAndDeletedIsFalse(name).orElse(null));

        } else {
            customer.setCanChildCustomerPlaceOrder(false);
            customer.setUpdatedBy(personRepository.findByEmailAndDeletedIsFalse(name).orElse(null));
        }

        customersRepository.save(customer);


        eventsRepository.save(new Events(
                customer.getPublicId().toString(),
                activate ? "CUSTOMER_ACTIVATED_TO_PLACE_ORDER" : "CUSTOMER_BLOCKED_TO_PLACE_ORDER",
                LocalDateTime.now(),
                "Customer " + customer.getName() + " has been " + (activate ? "activated" : "deactivated") + " to place order by " + name,
                activate ? "ACTIVATE" : "DEACTIVATE",
                personRepository.findByEmailAndDeletedIsFalse(name).orElse(null)
        ));
    }

    @Override
    public Page<ChurnedCallHistory> getChurnedCallHistory(Pageable pageable) {
        return churnedCallHistoryRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChurnedCallHistory> getCustomerCallHistory(UUID customerPublicId, Pageable pageable) {
        customersRepository.findByPublicIdAndDeletedIsFalse(customerPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Customer not found: " + customerPublicId)
                        .userMessage("Customer not found"));
        Pageable newestFirst = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("creationTime"), Sort.Order.desc("id")));
        return churnedCallHistoryRepository.findByCustomerPublicId(customerPublicId.toString(), newestFirst);
    }

    private void appendLeadStatusHistory(Customers customer, Person actionedBy, ChurnedCustomerLeadStatus leadStatus) {
        ZoneId nairobiZone = ZoneId.of("Africa/Nairobi");
        LocalDateTime nairobiDate = LocalDateTime.now(nairobiZone);
        String taggedDate = DateFormatter.formatDateToString(nairobiDate.toLocalDate());

        ChurnedCallHistory history = new ChurnedCallHistory();
        history.setCustomerPublicId(customer.getPublicId().toString());
        history.setCustomerName(customer.getName());
        history.setShopName(customer.getStoreName());
        history.setPhoneNumber(customer.getPhoneNumber());
        history.setDistrict(customer.getOrganizationRegions() != null ? customer.getOrganizationRegions().getName() : null);
        history.setWard(customer.getOrganizationLocation() != null ? customer.getOrganizationLocation().getName() : null);
        history.setLastOrderDate(customer.getLastOrderDate() != null ? DateFormatter.formatDate(customer.getLastOrderDate()) : null);
        history.setDate(taggedDate);
        history.setCxCallDate(taggedDate);
        history.setCreationTime(nairobiDate);
        history.setLeadStatus(leadStatus);
        if (actionedBy != null) {
            history.setCxAgentPublicId(actionedBy.getPublicId());
            history.setCxAgentName(actionedBy.getFullName());
        }
        history.setChurned(false);
        history.setLocationChange(false);
        history.setOrderAmendment(false);
        history.setTelephone(false);
        history.setBuyNowPayLater(false);
        history.setOrderPlacedAfterRecommentedOutcome(false);
        history.setCallResponseStatus(false);
        churnedCallHistoryRepository.save(history);
    }

    private void updateCallHistory(Customers customer, Person actionedBy, String code, boolean amendment, boolean churned, UUID amendmentId, boolean locationChange) {
        ZoneId nairobiZone = ZoneId.of("Africa/Nairobi");
        LocalDateTime nairobiDate = LocalDateTime.now(nairobiZone);
        String taggedDate = DateFormatter.formatDateToString(nairobiDate.toLocalDate());

        ChurnedCallHistory churnedCallHistory = new ChurnedCallHistory();
        churnedCallHistory.setCustomerPublicId(customer.getPublicId().toString());
        churnedCallHistory.setCreationTime(nairobiDate);
        churnedCallHistory.setDate(taggedDate);
        churnedCallHistory.setCxCallDate(taggedDate);
        churnedCallHistory.setCxAgentPublicId(actionedBy.getPublicId());
        churnedCallHistory.setCxAgentName(actionedBy.getFullName());
        churnedCallHistory.setCustomerName(customer.getName());
        churnedCallHistory.setShopName(customer.getStoreName());
        churnedCallHistory.setDistrict(customer.getOrganizationRegions() != null ? customer.getOrganizationRegions().getName() : null);
        churnedCallHistory.setWard(customer.getOrganizationLocation() != null ? customer.getOrganizationLocation().getName() : null);
        churnedCallHistory.setPhoneNumber(customer.getPhoneNumber());
        churnedCallHistory.setLastOrderDate(customer.getLastOrderDate() != null ? DateFormatter.formatDate(customer.getLastOrderDate()) : null);

        churnedCallHistory.setCallStatus(code);
        churnedCallHistory.setCallStatusDateTime(nairobiDate);

        churnedCallHistory.setChurned(churned);
        churnedCallHistory.setOrderAmendment(amendment);
        churnedCallHistory.setLocationChange(locationChange);
        if (amendment) {
            churnedCallHistory.setAmendmentPublicId(amendmentId);
        }
        log.info("------check -call-amend-----{}", code);

        churnedCallHistory.setCallResponseStatus(true);
        churnedCallHistory.setTelephone(false);
        churnedCallHistory.setBuyNowPayLater(false);
        churnedCallHistory.setOrderPlacedAfterRecommentedOutcome(false);
        churnedCallHistoryRepository.save(churnedCallHistory);
    }
}
