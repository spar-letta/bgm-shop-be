package com.app.itibari.services;

import com.app.itibari.dataTypes.StatementExportType;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.AgentAction;
import com.app.itibari.models.request.SalesAgentsRequest;
import com.app.itibari.models.response.CustomerAnalysis;
import com.app.itibari.models.response.SalesAgentPerformance;
import com.app.itibari.repository.*;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.reportHelper.SalesAgentCustomer;
import com.app.itibari.utils.specification.SalesOrdersSpecification;
import com.itextpdf.io.source.ByteArrayOutputStream;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesAgentsServiceImpl implements SalesAgentsService {

    private final SalesAgentsRepository salesAgentsRepository;

    private final PersonRepository personRepository;

    private final OrganizationRegionsRepository organizationRegionsRepository;

    private final CustomersService customersService;

    private final SalesOrdersService salesOrdersService;

    private final SalesOrdersRepository salesOrdersRepository;

    private final OrganizationLocationsRepository organizationLocationsRepository;

    private final EventsRepository eventsRepository;

    private final DiscountApprovalRepository discountApprovalRepository;
    private final CustomersRepository customersRepository;
    private final SalesAgentCustomer salesAgentCustomer;

    @Override
    public SalesAgents createSalesAgent(SalesAgentsRequest salesAgentsRequest, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<SalesAgents> salesAgentsOptional = salesAgentsRepository.findByEmailAndDeletedIsFalse(salesAgentsRequest.getEmail());

        if (salesAgentsOptional.isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales agent with email " + salesAgentsOptional.get().getEmail() + " already exists")
                    .userMessage("Sorry. Sales agent with email already exists");
        }

        // phone number check
        Optional<SalesAgents> salesAgentsPhoneOptional = salesAgentsRepository.findByPhoneNumberAndDeletedIsFalse(salesAgentsRequest.getPhoneNumber());

        if (salesAgentsPhoneOptional.isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales agent with phone number " + salesAgentsPhoneOptional
                            .get()
                            .getPhoneNumber() + " already exists")
                    .userMessage("Sorry. Sales agent with phone number already exists");
        }

        Optional<OrganizationRegions> organizationRegionOptional = organizationRegionsRepository
                .findByPublicIdAndDeletedIsFalse(salesAgentsRequest.getOrganizationRegionId());

        if (organizationRegionOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Organization Region with public Id " + salesAgentsRequest.getOrganizationRegionId() + " not found")
                    .userMessage("Sorry. Organization Region not found! ");
        }

        OrganizationRegions organizationRegion = organizationRegionOptional.get();

        Person salesAgentManagerPerson = null;
        if (null != salesAgentsRequest.getSalesAgentManagerPersonId()) {
            Optional<Person> salesAgentsManagerPersonOptional = personRepository.findByPublicIdAndDeletedIsFalse(salesAgentsRequest.getSalesAgentManagerPersonId());

            if (salesAgentsManagerPersonOptional.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Sales Agent Manager Person with public Id " + salesAgentsRequest.getSalesAgentManagerPersonId() + " not found")
                        .userMessage("Sorry. Sales Agent Manager Person with public Id ");
            }
            salesAgentManagerPerson = salesAgentsManagerPersonOptional.get();
        }

        SalesAgents salesAgent = new SalesAgents();

        if (salesAgentsRequest.getLocationPublicIds() != null) {
            salesAgentsRequest.getLocationPublicIds().forEach(locationId -> {
                Optional<OrganizationLocations> organizationLocationsOptional = organizationLocationsRepository.findByOrganizationRegionAndPublicIdAndDeletedIsFalse(
                        organizationRegion,
                        locationId);

                if (organizationLocationsOptional.isEmpty()) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Organization Location with public Id " + locationId + " not found")
                            .userMessage("Sorry. Organization Location not found ");
                }
                OrganizationLocations location = organizationLocationsOptional.get();
                salesAgent.addLocation(location);
            });
        }

        salesAgent.setName(salesAgentsRequest.getName());
        salesAgent.setEmail(salesAgentsRequest.getEmail());
        salesAgent.setPhoneNumber(salesAgentsRequest.getPhoneNumber());
        salesAgent.setSalesAgentManager(salesAgentManagerPerson);
        salesAgent.setOrganizationRegion(organizationRegion);
        personOptional.ifPresent(salesAgent::setCreatedBy);
        salesAgent.setIdentificationType(salesAgentsRequest.getIdentificationType());
        salesAgent.setIdentificationNumber(salesAgentsRequest.getIdentificationNumber());

        return salesAgentsRepository.save(salesAgent);
    }

    @Override
    public void updateSalesAgent(UUID agentPublicId, SalesAgentsRequest salesAgentsRequest, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<SalesAgents> salesAgentOptional = salesAgentsRepository
                .findByPublicIdAndDeletedIsFalse(agentPublicId);

        if (salesAgentOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales agent with id " + agentPublicId + " not found")
                    .userMessage("Sorry. Sales agent with id " + agentPublicId + " not found");
        }

        Optional<OrganizationRegions> organizationRegionOptional = organizationRegionsRepository
                .findByPublicIdAndDeletedIsFalse(salesAgentsRequest.getOrganizationRegionId());

        if (organizationRegionOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Organization Region with public Id " + salesAgentsRequest.getOrganizationRegionId() + " not found")
                    .userMessage("Sorry. Organization Region not found with public Id " + salesAgentsRequest.getOrganizationRegionId());
        }

        OrganizationRegions organizationRegion = organizationRegionOptional.get();

        Person salesAgentManagerPerson = null;
        if (null != salesAgentsRequest.getSalesAgentManagerPersonId()) {
            Optional<Person> salesAgentsManagerPersonOptional = personRepository.findByPublicIdAndDeletedIsFalse(salesAgentsRequest.getSalesAgentManagerPersonId());

            if (salesAgentsManagerPersonOptional.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Sales Agent Manager Person with public Id " + salesAgentsRequest.getSalesAgentManagerPersonId() + " not found")
                        .userMessage("Sorry. Sales Agent Manager Person with public Id " + salesAgentsRequest.getSalesAgentManagerPersonId() + " not found");
            }
            salesAgentManagerPerson = salesAgentsManagerPersonOptional.get();
        }

        SalesAgents salesAgent = salesAgentOptional.get();

        if (salesAgentsRequest.getLocationPublicIds() != null) {
            salesAgent.getOrganizationLocations().clear();
            salesAgentsRequest.getLocationPublicIds().forEach(locationId -> {
                Optional<OrganizationLocations> organizationLocationsOptional = organizationLocationsRepository.findByOrganizationRegionAndPublicIdAndDeletedIsFalse(
                        organizationRegion,
                        locationId);

                if (organizationLocationsOptional.isEmpty()) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Organization Location with public  not found")
                            .userMessage("Sorry. Organization Location not found!");
                }
                OrganizationLocations location = organizationLocationsOptional.get();
                salesAgent.addLocation(location);
            });
        }

        salesAgent.setName(salesAgentsRequest.getName());
        salesAgent.setEmail(salesAgentsRequest.getEmail());
        salesAgent.setPhoneNumber(salesAgentsRequest.getPhoneNumber());
        salesAgent.setSalesAgentManager(salesAgentManagerPerson);
        salesAgent.setOrganizationRegion(organizationRegion);
        personOptional.ifPresent(salesAgent::setUpdatedBy);
        salesAgent.setUpdateDate(LocalDateTime.now());
        salesAgent.setIdentificationType(salesAgentsRequest.getIdentificationType());
        salesAgent.setIdentificationNumber(salesAgentsRequest.getIdentificationNumber());

        salesAgentsRepository.save(salesAgent);
    }

    @Override
    public SalesAgents getSalesAgent(UUID agentPublicId) {

        Optional<SalesAgents> salesAgentOptional = salesAgentsRepository
                .findByPublicIdAndDeletedIsFalse(agentPublicId);

        if (salesAgentOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales agent with id " + agentPublicId + " not found")
                    .userMessage("Sorry. Sales agent  not found");
        }

        salesAgentOptional.get().getCustomers().forEach(item -> log.info("-----detais {}", item));

        return salesAgentOptional.get();
    }

    @Override
    public Page<SalesAgents> getAllSalesAgents(String searchParam, UUID organisationRegionPublicId,
                                               UUID organisationLocationPublicId,
                                               Pageable pageable) {

        if (searchParam != null) {
            return salesAgentsRepository
                    .findByNameContainingIgnoreCaseAndDeletedIsFalseOrEmailContainingIgnoreCaseAndDeletedIsFalse(searchParam,
                            searchParam,
                            pageable);
        }
        if (organisationRegionPublicId != null && organisationLocationPublicId == null) {
            OrganizationRegions organizationRegions =
                    organizationRegionsRepository.findByPublicIdAndDeletedIsFalse(organisationRegionPublicId).orElse(null);
            return salesAgentsRepository.findByOrganizationRegion(organizationRegions, pageable);
        }
        if (organisationRegionPublicId == null && organisationLocationPublicId != null) {
            OrganizationLocations organizationLocations =
                    organizationLocationsRepository.findByPublicIdAndDeletedIsFalse(organisationLocationPublicId).orElse(null);
            return salesAgentsRepository.findByOrganizationLocationsIn(Arrays.asList(organizationLocations), pageable);
        }
        if (organisationRegionPublicId != null && organisationLocationPublicId != null) {
            OrganizationRegions organizationRegions =
                    organizationRegionsRepository.findByPublicIdAndDeletedIsFalse(organisationRegionPublicId).orElse(null);
            OrganizationLocations organizationLocations =
                    organizationLocationsRepository.findByPublicIdAndDeletedIsFalse(organisationLocationPublicId).orElse(null);

            return salesAgentsRepository.findByOrganizationRegionAndOrganizationLocationsIn(organizationRegions,
                    Arrays.asList(organizationLocations), pageable);
        } else {
            return salesAgentsRepository.findByDeletedIsFalse(pageable);
        }
    }

    @Override
    public void deleteSalesAgent(UUID agentPublicId, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<SalesAgents> salesAgentOptional = salesAgentsRepository
                .findByPublicIdAndDeletedIsFalse(agentPublicId);

        if (salesAgentOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales agent with id " + agentPublicId + " not found")
                    .userMessage("Sorry. Sales agent not found");
        }

        SalesAgents salesAgent = salesAgentOptional.get();
        personOptional.ifPresent(salesAgent::setUpdatedBy);
        salesAgent.setDeleted(true);
        salesAgent.setUpdateDate(LocalDateTime.now());

        salesAgentsRepository.save(salesAgent);
    }

    @Override
    public SalesAgents getSalesAgentProfile(String name) {

        Optional<SalesAgents> optionalSalesAgents = salesAgentsRepository.findSalesAgentsByPhoneNumberAndDeletedIsFalse(name);

        if (optionalSalesAgents.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales agent with phone number " + name + " not found")
                    .userMessage("Sorry. Sales agent with phone number not found");
        }

        return optionalSalesAgents.get();
    }

    @Override
    public SalesAgentPerformance getSalesAgentPerformance(UUID salesAgentPublicId, String from, String to, Pageable pageable) {

        Optional<SalesAgents> salesAgentOptional = salesAgentsRepository
                .findByPublicIdAndDeletedIsFalse(salesAgentPublicId);

        if (salesAgentOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales agent with id " + salesAgentPublicId + " not found")
                    .userMessage("Sorry. Sales agent with not found");
        }

        SalesAgents salesAgent = salesAgentOptional.get();

        SalesAgentPerformance salesAgentPerformance = new SalesAgentPerformance();

        List<CustomerAnalysis> customerAnalysisList = new ArrayList<>();

        List<Customers> customersList = customersService.getCustomersBySalesAgent(salesAgent.getPublicId(), from, to, pageable);

        if (!customersList.isEmpty()) {
            customersList.forEach(customers -> {
                CustomerAnalysis customerAnalysis = new CustomerAnalysis();
                customerAnalysis.setCustomerName(customers.getName());
                customerAnalysis.setCustomerPhone(customers.getPhoneNumber());
                customerAnalysis.setRegistrationCount(1);
                customerAnalysis.setTotalOrders(getOrdersCountByCustomer(customers.getPublicId(), from, to));
                customerAnalysis.setTotalAmount(getTotalAmountByCustomer(customers.getPublicId(), from, to));

                customerAnalysisList.add(customerAnalysis);
            });
        }

        Integer totalSales = customerAnalysisList.stream()
                .mapToInt(CustomerAnalysis::getRegistrationCount)
                .sum();
        BigDecimal totalSalesAmount = customerAnalysisList.stream()
                .map(CustomerAnalysis::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!customerAnalysisList.isEmpty()) {
            PageImpl<CustomerAnalysis> customerAnalysisPage = new PageImpl<>(customerAnalysisList, pageable, customerAnalysisList.size());
            salesAgentPerformance.setContent(customerAnalysisPage.getContent());
            salesAgentPerformance.setTotalElements(customerAnalysisPage.getTotalElements());
            salesAgentPerformance.setPageSize(customerAnalysisPage.getSize());
            salesAgentPerformance.setLast(customerAnalysisPage.isLast());
            salesAgentPerformance.setFirst(customerAnalysisPage.isFirst());
            salesAgentPerformance.setPageNumber(customerAnalysisPage.getNumber());
            salesAgentPerformance.setTotalSales(totalSales);
            salesAgentPerformance.setTotalSalesAmount(totalSalesAmount);
        }
        return salesAgentPerformance;
    }

    private BigDecimal getTotalAmountByCustomer(UUID publicId, String from, String to) {

        Specification<SalesOrders> spec = Specification
                .where(SalesOrdersSpecification.isNotDeleted())
                .and(SalesOrdersSpecification.hasStringDateFromTo(from, to))
                .and(SalesOrdersSpecification.hasCustomer(publicId));

        List<SalesOrders> salesOrdersList = salesOrdersRepository.findAll(spec, Pageable.unpaged()).getContent();

        if (salesOrdersList.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return salesOrdersList.stream()
                .map(SalesOrders::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long getOrdersCountByCustomer(UUID publicId, String from, String to) {

        Specification<SalesOrders> spec = Specification
                .where(SalesOrdersSpecification.isNotDeleted())
                .and(SalesOrdersSpecification.hasStringDateFromTo(from, to))
                .and(SalesOrdersSpecification.hasCustomer(publicId));

        List<SalesOrders> salesOrdersList = salesOrdersRepository.findAll(spec, Pageable.unpaged()).getContent();

        if (salesOrdersList.isEmpty()) {
            return 0;
        }
        return salesOrdersList.size();
    }

    @Override
    public void deactivateSalesAgent(UUID salesAgentPublicId, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        if (personOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person with email " + name + " not found")
                    .userMessage("Sorry. Person with email not found");
        }

        Optional<SalesAgents> salesAgentOptional = salesAgentsRepository
                .findByPublicIdAndDeletedIsFalse(salesAgentPublicId);
        if (salesAgentOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales agent with id " + salesAgentPublicId + " not found")
                    .userMessage("Sorry. Sales agent with not found");
        }
        SalesAgents salesAgent = salesAgentOptional.get();
        if (!salesAgent.isActive()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales agent with id " + salesAgentPublicId + " is already deactivated")
                    .userMessage("Sorry. Sales agent is already deactivated");
        }
        salesAgent.setActive(false);
        personOptional.ifPresent(salesAgent::setUpdatedBy);
        salesAgent.setUpdateDate(LocalDateTime.now());
        salesAgentsRepository.save(salesAgent);

        String description = "Sales Agent " + salesAgent.getName() + " has been deactivated by " + personOptional.get().getFullName();
        eventsRepository.save(new Events(salesAgentPublicId.toString(),
                "Sales Agent Deactivated",
                LocalDateTime.now(),
                description,
                "DEACTIVATION",
                personOptional.get()));
    }

    @Override
    public void activateSalesAgent(UUID salesAgentPublicId, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        if (personOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person with email " + name + " not found")
                    .userMessage("Sorry. Person with email not found");
        }

        Optional<SalesAgents> salesAgentOptional = salesAgentsRepository
                .findByPublicIdAndDeletedIsFalse(salesAgentPublicId);
        if (salesAgentOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales agent with id " + salesAgentPublicId + " not found")
                    .userMessage("Sorry. Sales agent with not found");
        }
        SalesAgents salesAgent = salesAgentOptional.get();
        if (salesAgent.isActive()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales agent with id " + salesAgentPublicId + " is already activated")
                    .userMessage("Sorry. Sales agent is already activated");
        }
        salesAgent.setActive(true);
        personOptional.ifPresent(salesAgent::setUpdatedBy);
        salesAgent.setUpdateDate(LocalDateTime.now());
        salesAgentsRepository.save(salesAgent);

        String description = "Sales Agent " + salesAgent.getName() + " has been activated by " + personOptional.get().getFullName();
        eventsRepository.save(new Events(salesAgentPublicId.toString(),
                "Sales Agent Activated",
                LocalDateTime.now(),
                description,
                "ACTIVATION",
                personOptional.get()));
    }

    @Override
    public Page<DiscountApproval> fetchSpecialRequests(LocalDate startDate, LocalDate endDate, String name, Pageable pageable) {

        Optional<SalesAgents> salesAgentsOptional = salesAgentsRepository.findSalesAgentsByPhoneNumberAndDeletedIsFalse(name);
        if (salesAgentsOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales agent with phone number " + name + " not found")
                    .userMessage("Sorry. Sales agent with phone number not found");
        }
        SalesAgents salesAgent = salesAgentsOptional.get();

        List<DiscountApproval> discountApprovalList = discountApprovalRepository.findDiscountApprovalByRequestedByAndDeletedIsFalse(
                salesAgent.getPublicId().toString(),
                pageable);

        if (discountApprovalList.isEmpty()) {
            return Page.empty(pageable);
        }

        List<DiscountApproval> filteredList = discountApprovalList.stream()
                .filter(da -> {
                    boolean afterStart = (startDate == null) || (!da.getCreationTime().toLocalDate().isBefore(startDate));
                    boolean beforeEnd = (endDate == null) || (!da.getCreationTime().toLocalDate().isAfter(endDate));
                    return afterStart && beforeEnd;
                })
                .toList();

        return new PageImpl<>(filteredList, pageable, filteredList.size());
    }

    @Override
    @Transactional
    public void actionSalesAgent(UUID salesAgentPublicId, AgentAction agentAction, String name) {
        Person loggedInUser = getLoggedInUser(name);
        SalesAgents salesAgent = getSalesAgent(salesAgentPublicId);

        switch (agentAction.actionType()) {
            case Map_Customer -> handleMapCustomer(agentAction, salesAgent, loggedInUser);
            case De_Activate_Agent -> handleDeactivateAgent(agentAction, salesAgent, loggedInUser);
            default -> throw new BadRequestRestApiException()
                    .developerMessage("Unsupported action type")
                    .userMessage("Sorry. Action not supported");
        }
    }

    @Override
    public ByteArrayOutputStream generateSalesAgentCustomerReportExport(UUID organisationLocationPublicId, UUID organisationRegionPublicId, String searchParam, StatementExportType statementExportType, String name, Pageable pageable) {
        List<SalesAgents> salesList = getAllSalesAgents(searchParam, organisationRegionPublicId,
                organisationLocationPublicId,
                pageable).getContent();

        return salesAgentCustomer.createReport(salesList, name);
    }

    @Override
    public List<SalesAgents> getAllSalesAgentsMiniData(String searchParam) {
        Sort sortByNameAsc = Sort.by(Sort.Direction.ASC, "name");

        if (searchParam != null && !searchParam.trim().isEmpty()) {
            return salesAgentsRepository
                    .findByNameContainingIgnoreCaseAndDeletedIsFalse(searchParam, sortByNameAsc);
        } else {
            return salesAgentsRepository
                    .findSalesAgentsByDeletedIsFalse(sortByNameAsc);
        }
    }

    private void handleMapCustomer(AgentAction agentAction, SalesAgents salesAgent, Person loggedInUser) {
        if (agentAction.customerPublicIds().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer Ids must not be empty")
                    .userMessage("Sorry. Select a customer");
        }

        List<Customers> customers = agentAction.customerPublicIds().stream()
                .map(id -> customersRepository.findByPublicIdAndDeletedIsFalse(id)
                        .orElseThrow(() -> new BadRequestRestApiException()
                                .developerMessage("Customer not found")
                                .userMessage("Sorry. Customer not found")))
                .toList();

        for (Customers customer : customers) {
            SalesAgents currentAgent = customer.getServedBy();

            if (currentAgent != null && !currentAgent.getId().equals(salesAgent.getId())) {
                currentAgent.removeCustomer(customer);
            }

            if (currentAgent == null || !currentAgent.getId().equals(salesAgent.getId())) {
                salesAgent.addCustomer(customer);
            }
        }

        salesAgent.setUpdatedBy(loggedInUser);
        salesAgentsRepository.save(salesAgent);
    }

    private void handleDeactivateAgent(AgentAction agentAction, SalesAgents salesAgent, Person loggedInUser) {
        UUID receiverId = agentAction.salesAgentToReceiveCustomerPublicId();
        if (receiverId == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Missing parameters for transfer")
                    .userMessage("Sorry. Select sales agent to transfer customer to.");
        }

        SalesAgents receiverAgent = getSalesAgent(receiverId);
        List<Customers> customers = customersRepository.findByServedBy(salesAgent);

        for (Customers customer : customers) {
            salesAgent.removeCustomer(customer);
            receiverAgent.addCustomer(customer);
        }

        salesAgent.setUpdatedBy(loggedInUser);
        salesAgent.setActive(false);
        receiverAgent.setUpdatedBy(loggedInUser);

        salesAgentsRepository.saveAll(List.of(salesAgent, receiverAgent));
    }


    public Person getLoggedInUser(String name) {
        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        if (personOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person with email " + name + " not found")
                    .userMessage("Sorry. Person with email not found");
        }

        return personOptional.get();
    }
}
