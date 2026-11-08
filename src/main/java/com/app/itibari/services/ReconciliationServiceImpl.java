package com.app.itibari.services;

import com.app.itibari.dataTypes.*;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.CashCreditReconAdjustmentRequest;
import com.app.itibari.models.request.DebitNoteRequest;
import com.app.itibari.models.request.ReconciliationRequest;
import com.app.itibari.models.response.*;
import com.app.itibari.repository.*;
import com.app.itibari.services.helper.LogisticsHelper;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.helpers.CreditReconHelper;
import com.app.itibari.utils.helpers.Validators;
import com.app.itibari.utils.specification.DebitNoteSpecification;
import com.app.itibari.utils.specification.ReconciliationSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationServiceImpl implements ReconciliationService {

    private final ReconciliationRepository reconciliationRepository;

    private final PersonRepository personRepository;

    private final ProductReconRepository productReconRepository;

    private final DeliveryAgentsRepository deliveryAgentsRepository;
    private final LogisticsHelper logisticsHelper;

    private final DebitNoteRepository debitNoteRepository;
    private final RoutePlanningRepository routePlanningRepository;
    private final CreditReconHelper creditReconHelper;
    private final CreditReconRepository creditReconRepository;
    private final PaymentItemRepository paymentItemRepository;
    private final CreditLimitItemRepository creditLimitItemRepository;
    private final CreditReconItemRepository creditReconItemRepository;
    private final EventsRepository eventsRepository;
    private final CashCreditReconReportRepository cashCreditReconReportRepository;
    private final CashCreditReconReportLineRepository cashCreditReconReportLineRepository;
    private final CashCreditReconAdjustmentRepository cashCreditReconAdjustmentRepository;

    @Override
    public Page<Reconciliation> getAllReconciliations(String searchParam,
                                                      String deliveryAgentPublicId,
                                                      String deliveryId,
                                                      String deliveryStatus,
                                                      String vehicleNumber,
                                                      Boolean reconciled,
                                                      LocalDate from,
                                                      LocalDate to,
                                                      Boolean walkIn,
                                                      Pageable pageable) {

        Specification<Reconciliation> specification = Specification
                .where(ReconciliationSpecification.isNotDeleted())
                .and(ReconciliationSpecification.hasDeliveryAgentPublicId(deliveryAgentPublicId))
                .and(ReconciliationSpecification.hasVehicle(vehicleNumber))
                .and(ReconciliationSpecification.hasSearchTerm(searchParam))
                .and(ReconciliationSpecification.hasDeliveryStatus(deliveryStatus))
                .and(ReconciliationSpecification.isReconciled(reconciled))
                .and(ReconciliationSpecification.isWalkIn(walkIn))
                .and(ReconciliationSpecification.hasDeliveryId(deliveryId))
                .and(ReconciliationSpecification.hasDateRange(from, to));

        return reconciliationRepository.findAll(specification, pageable);
    }

    @Override
    public Page<ProductItem> getReconciliationProducts(UUID reconciliationId, String searchParam, Pageable pageable) {

        Optional<Reconciliation> optionalReconciliation = reconciliationRepository.findByPublicIdAndDeletedIsFalse(reconciliationId);

        if (optionalReconciliation.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("No such reconciliation")
                    .userMessage("No such reconciliation bruv");
        }

        Reconciliation reconciliation = optionalReconciliation.get();
        ProductRecon productRecon = productReconRepository
                .findByPublicIdAndDeletedIsFalse(UUID.fromString(reconciliation.getProductReconPublicId()))
                .orElse(null);

        if (productRecon == null) {
            return Page.empty();
        }

        List<ProductItem> productItems = productRecon.getProductItems();
        return new PageImpl<>(productItems, pageable, productItems.size());
    }

    @Override
    public void reconcile(ReconciliationRequest reconciliationRequest, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        Optional<Reconciliation> optionalReconciliation = reconciliationRepository.findByPublicIdAndDeletedIsFalse(reconciliationRequest.getPublicId());

        if (optionalReconciliation.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("No such reconciliation")
                    .userMessage("No such reconciliation bruv");
        }
        Reconciliation reconciliation = optionalReconciliation.get();
        assertReconciliationEditable(reconciliation);

        if (reconciliation.isReconciled()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Reconciliation already done")
                    .userMessage("Reconciliation already done, cannot be updated");
        }

        Optional<ProductRecon> optionalProductRecon = productReconRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(reconciliation.getProductReconPublicId()));

        if (optionalProductRecon.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("No such product reconciliation")
                    .userMessage("No such product reconciliation found");
        }

        ProductRecon productRecon = optionalProductRecon.get();

        if (productRecon.getStatus() != ReconStatus.Reconciled) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product reconciliation not reconciled")
                    .userMessage("Product reconciliation not reconciled, cannot be updated");
        }

        optionalPerson.ifPresent(reconciliation::setUpdatedBy);
        reconciliation.setUpdateDate(LocalDateTime.now());
        reconciliation.setReconciliationAmount(reconciliationRequest.getAmount());
        reconciliation.setReconciled(true);
        reconciliation.setDeliveryStatus("RECONCILED");
        reconciliationRepository.save(reconciliation);

        // return product to stock if not sold

        this.updateStock(productRecon.getProductItems(), productRecon.getWarehousePublicId());
        releaseDeliveryAgent(reconciliation);
    }

    private void releaseDeliveryAgent(Reconciliation reconciliation) {
        if (!reconciliation.isWalkIn() && reconciliation.getDeliveryAgentPublicId() != null) {
            logisticsHelper.updateAgentAvailability(UUID.fromString(reconciliation.getDeliveryAgentPublicId()), true);
        }
    }

    private void updateStock(List<ProductItem> productItems, String warehousePublicId) {

        ///  already taken care of in the product recon service when marking as reconciled
//        Optional<Warehouse> optionalWarehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(warehousePublicId));
//
//        if (optionalWarehouse.isEmpty()) {
//            log.info("Warehouse not found");
//            return;
//        }
//        Warehouse warehouse = optionalWarehouse.get();
//
//        Optional<Inventory> optionalInventory = inventoryRepository.findByWarehouseAndDeletedIsFalse(warehouse);
//
//        if (optionalInventory.isEmpty()) {
//            log.info("Inventory not found for warehouse: {}", warehouse.getWarehouseName());
//            return;
//        }
//
//        Inventory inventory = optionalInventory.get();
//
//        for (ProductItem productItem : productItems) {
//            if (productItem.getQuantityReturned() > 0) {
//                productSkuHelper.updateProductStock(productItem.getProductPublicId(), (int) productItem.getQuantityReturned(), UpdateType.Decrement, StockType.ReservedStock, false);
//                Optional<Product> product = productRepository.findByPublicIdAndDeletedIsFalse(productItem.getProductPublicId());
//                if (product.isPresent()) {
//                    inventoryHelper.updateInventory(product.get(), warehouse.getPublicId(), productItem.getQuantityReturned(), StockType.ReservedStock, UpdateType.Decrement, false);
//                    log.info("Updated stock for product: {} in warehouse: {}", productItem.getProductName(), warehouse.getWarehouseName());
//                } else {
//                    log.warn("Product not found for publicId: {}", productItem.getProductPublicId());
//                }
//            } else {
//                log.info("No stock to update for product: {} in warehouse: {}", productItem.getProductName(), warehouse.getWarehouseName());
//            }
//        }
    }

    @Override
    @Transactional
    public DebitNotes createDebitNote(DebitNoteRequest debitNoteRequest, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("No such person")
                    .userMessage("No such person found");
        }

        Person person = optionalPerson.get();

        Optional<Reconciliation> optionalReconciliation = reconciliationRepository.findByPublicIdAndDeletedIsFalse(debitNoteRequest.getReconciliationPublicId());

        if (optionalReconciliation.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("No such reconciliation")
                    .userMessage("No such reconciliation found");
        }

        Reconciliation reconciliation = optionalReconciliation.get();
        assertReconciliationEditable(reconciliation);

//        if (debitNoteRequest.getAmount().compareTo(reconciliation.getDepositAmount()) != 0) {
//            throw new BadRequestRestApiException()
//                    .developerMessage("Debit note amount must be equal to reconciliation deposit amount")
//                    .userMessage(String.format("Debit note amount must be equal to reconciliation outstanding amount: %s",
//                            reconciliation.getOutstandingAmount()));
//        }

        Optional<DeliveryAgents> optionalDeliveryAgent = deliveryAgentsRepository.findByPublicIdForUpdate(debitNoteRequest.getDeliveryAgentPublicId());

        if (optionalDeliveryAgent.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("No such delivery agent")
                    .userMessage("No such delivery agent found");
        }
        DeliveryAgents deliveryAgent = optionalDeliveryAgent.get();

        if (!reconciliation.getDeliveryAgentPublicId().equals(deliveryAgent.getPublicId().toString())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Reconciliation does not belong to this delivery agent")
                    .userMessage("Reconciliation does not belong to this delivery agent");
        }

        if (debitNoteRepository.existsForDeliveryAgentAndRoute(deliveryAgent.getPublicId(),
                DebitReferenceType.DeliveryAgent, reconciliation.getPublicId().toString(),
                reconciliation.getRoutePublicId())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Debit note already exists for delivery agent " + deliveryAgent.getPublicId()
                            + " and route " + reconciliation.getRoutePublicId())
                    .userMessage("A debit note already exists for this delivery agent and route");
        }

        DebitNotes debitNotes = new DebitNotes();
        debitNotes.setAmount(debitNoteRequest.getAmount());
        debitNotes.setDescription(debitNoteRequest.getDescription());
        debitNotes.setCreatedBy(person);
        debitNotes.setCreationTime(LocalDateTime.now());
        debitNotes.setReferenceId(deliveryAgent.getPublicId());
        debitNotes.setReferenceType(DebitReferenceType.DeliveryAgent);
        debitNotes.setBalance(debitNoteRequest.getAmount());
        debitNotes.setReconciled(false);
        debitNotes.setReconciliationPublicId(reconciliation.getPublicId().toString());
        debitNotes.setRoute(reconciliation.getDeliveryId());
        debitNotes.setRoutePublicId(reconciliation.getRoutePublicId());

        populateDebitNoteAgentDetails(debitNotes, deliveryAgent);
        DebitNotes savedDebitNote = debitNoteRepository.save(debitNotes);

        List<DebitNotes> existingDebitNotes = debitNoteRepository.findByReferenceIdAndReconciledIsFalseAndReferenceTypeAndDeletedIsFalse(
                deliveryAgent.getPublicId(),
                DebitReferenceType.DeliveryAgent);

        if (existingDebitNotes.size() == 1) {
            deliveryAgent.setDebtZone(DebtZone.GreenZone);
        } else if (existingDebitNotes.size() > 2) {
            deliveryAgent.setDebtZone(DebtZone.YellowZone);
        } else {
            deliveryAgent.setDebtZone(DebtZone.RedZone);
        }

        deliveryAgentsRepository.save(deliveryAgent);
        log.info("Debit note created for delivery agent: {} with amount: {}", deliveryAgent.getFullName(), debitNoteRequest.getAmount());

        // reconcile the reconciliation
        Optional<ProductRecon> optionalProductRecon = productReconRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(reconciliation.getProductReconPublicId()));

        reconciliation.setReconciled(true);
        reconciliation.setUpdateDate(LocalDateTime.now());
        reconciliation.setDeliveryStatus("RECONCILED");
        reconciliation.setUpdatedBy(person);
        reconciliationRepository.save(reconciliation);

        optionalProductRecon.ifPresent(productRecon -> {
            this.updateStock(productRecon.getProductItems(), productRecon.getWarehousePublicId());
        });
        releaseDeliveryAgent(reconciliation);

        populateDebitNoteAgentDetails(savedDebitNote, deliveryAgent);
        return debitNoteRepository.save(savedDebitNote);
    }

    @Override
    public Page<DebitNotes> getDebitNotes(UUID deliveryAgentPublicId,
                                          String searchParam,
                                          String reconciliationPublicId,
                                          Boolean reconciled,
                                          LocalDate from,
                                          LocalDate to,
                                          String routeNumber,
                                          Pageable pageable) {

        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Debit note date range starts after it ends")
                    .userMessage("The from date must be on or before the to date");
        }

        Specification<DebitNotes> specification = Specification
                .where(DebitNoteSpecification.isNotDeleted())
                .and(DebitNoteSpecification.hasDeliveryAgentPublicId(deliveryAgentPublicId))
                .and(DebitNoteSpecification.hasSearchTerm(searchParam))
                .and(DebitNoteSpecification.hasReconciliationPublicId(reconciliationPublicId))
                .and(DebitNoteSpecification.isReconciled(reconciled))
                .and(DebitNoteSpecification.hasDateRange(from, to))
                .and(DebitNoteSpecification.hasRouteNumber(routeNumber));

        Page<DebitNotes> notes = debitNoteRepository.findAll(specification, pageable);
        List<UUID> agentIds = notes.getContent().stream()
                .map(DebitNotes::getDeliveryAgentPublicId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!agentIds.isEmpty()) {
            Map<UUID, DeliveryAgents> agents = new HashMap<>();
            deliveryAgentsRepository.findByPublicIdInAndDeletedIsFalse(agentIds)
                    .forEach(agent -> agents.put(agent.getPublicId(), agent));
            notes.forEach(note -> populateDebitNoteAgentDetails(note, agents.get(note.getDeliveryAgentPublicId())));
        }
        return notes;
    }

    @Override
    public DebitNotes getDebitNoteById(UUID publicId) {

        Optional<DebitNotes> optionalDebitNote = debitNoteRepository.findByPublicIdAndDeletedIsFalse(publicId);

        if (optionalDebitNote.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("No such debit note")
                    .userMessage("No such debit note found");
        }

        DebitNotes debitNote = optionalDebitNote.get();
        if (debitNote.getDeliveryAgentPublicId() != null) {
            deliveryAgentsRepository.findByPublicIdAndDeletedIsFalse(debitNote.getDeliveryAgentPublicId())
                    .ifPresent(agent -> populateDebitNoteAgentDetails(debitNote, agent));
        }
        return debitNote;
    }

    private void populateDebitNoteAgentDetails(DebitNotes debitNote, DeliveryAgents deliveryAgent) {
        if (deliveryAgent != null) {
            debitNote.setDeliveryAgentPublicId(deliveryAgent.getPublicId());
            debitNote.setDeliveryAgentName(deliveryAgent.getFullName());
            debitNote.setDebtLevel(deliveryAgent.getDebtZone());
        }
    }

    @Override
    @Transactional
    public void updateDebitNote(UUID publicId, BigDecimal amount, String name) {

        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestRestApiException()
                    .developerMessage("Debit note payment amount must be positive")
                    .userMessage("Debit note payment amount must be positive");
        }
        DebitNotes debitNote = debitNoteRepository.findByPublicIdForUpdate(publicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Debit note not found")
                        .userMessage("Debit note not found"));
        if (debitNote.getReferenceType() != DebitReferenceType.DeliveryAgent) {
            throw new BadRequestRestApiException()
                    .developerMessage("Payment requires a delivery agent debit note")
                    .userMessage("Payment requires a delivery agent debit note");
        }
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("No such person")
                    .userMessage("No such person found");
        }

        Person person = optionalPerson.get();
        if (Boolean.TRUE.equals(debitNote.getReconciled())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Debit note already reconciled")
                    .userMessage("Debit note already reconciled, cannot be updated");
        }

        if (debitNote.getBalance().compareTo(amount) < 0) {
            throw new BadRequestRestApiException()
                    .developerMessage("Amount cannot be greater than balance")
                    .userMessage("Amount cannot be greater than balance");
        }

        Reconciliation reconciliation = reconciliationRepository
                .findByPublicIdForUpdate(UUID.fromString(debitNote.getReconciliationPublicId()))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Reconciliation not found for debit note")
                        .userMessage("Reconciliation not found for debit note"));
        assertReconciliationEditable(reconciliation);

        boolean isReconciled = debitNote.getBalance().subtract(amount).compareTo(BigDecimal.ZERO) == 0;

        debitNote.setBalance(debitNote.getBalance().subtract(amount));
        debitNote.setUpdatedBy(person);
        debitNote.setUpdateDate(LocalDateTime.now());
        debitNote.setReconciled(isReconciled);
        DebitNotes saved = debitNoteRepository.save(debitNote);

        DeliveryAgents deliveryAgent = deliveryAgentsRepository.findByPublicIdAndDeletedIsFalse(saved.getReferenceId()).orElse(null);

        if (deliveryAgent == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("No such delivery agent")
                    .userMessage("No such delivery agent found");
        }

        List<DebitNotes> existingDebitNotes = debitNoteRepository.findByReferenceIdAndReconciledIsFalseAndReferenceTypeAndDeletedIsFalse(
                deliveryAgent.getPublicId(),
                DebitReferenceType.DeliveryAgent);

        if (existingDebitNotes.size() == 1) {
            deliveryAgent.setDebtZone(DebtZone.GreenZone);
        } else if (existingDebitNotes.size() > 2) {
            deliveryAgent.setDebtZone(DebtZone.YellowZone);
        } else {
            deliveryAgent.setDebtZone(DebtZone.RedZone);
        }

        deliveryAgentsRepository.save(deliveryAgent);

        reconciliation.setDepositAmount(zero(reconciliation.getDepositAmount()).add(amount));
        reconciliation.setDebitNotePaidAmount(zero(reconciliation.getDebitNotePaidAmount()).add(amount));
        reconciliation.setUpdatedBy(person);
        reconciliation.setUpdateDate(LocalDateTime.now());
        reconciliationRepository.save(reconciliation);

        routePlanningRepository.findByRouteIdAndDeletedIsFalse(reconciliation.getDeliveryId())
                .ifPresent(route -> {
                    route.setBalance(reconciliation.getDeliveryAmount()
                            .subtract(reconciliation.getDepositAmount().add(zero(reconciliation.getCreditAmount()))));
                    routePlanningRepository.save(route);
                });

        if (saved.getReconciled()) {
            saved.setWhiteListedBy(person.getFullName());
            saved.setWhiteListingDate(LocalDateTime.now());
        }
        populateDebitNoteAgentDetails(saved, deliveryAgent);
        debitNoteRepository.save(saved);
    }

    @Override
    public void deleteDebitNote(UUID publicId, String name) {

        DebitNotes debitNote = getDebitNoteById(publicId);

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("No such person")
                    .userMessage("No such person found");
        }

        Person person = optionalPerson.get();

        if (!debitNote.getReconciled()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Debit note not reconciled")
                    .userMessage("Debit note not reconciled, cannot be deleted");
        }

        debitNote.setDeleted(true);
        debitNote.setUpdateDate(LocalDateTime.now());
        debitNote.setUpdatedBy(person);

        debitNoteRepository.save(debitNote);
    }

    @Override
    public List<ReconOtpInfo> getOrderNumbersAndOtps(UUID reconciliationId) {
        Optional<Reconciliation> optionalReconciliation = reconciliationRepository.findByPublicIdAndDeletedIsFalse(reconciliationId);

        if (optionalReconciliation.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("No such reconciliation")
                    .userMessage("No such reconciliation bruv");
        }

        Reconciliation reconciliation = optionalReconciliation.get();

        if (reconciliation.getCreditAmount().compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        RoutePlanning routePlanning = routePlanningRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(reconciliation.getRoutePublicId()))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("No such route planning")
                        .userMessage("No such route planning found"));

        List<SalesOrders> creditOrders = routePlanning.getSalesOrders().stream()
                .filter(SalesOrders::isCreditApplied)
                .filter(so -> so.getSalesOrderStatus() == LoadingStatusEnum.DELIVERED)
                .toList();

        if (creditOrders.isEmpty()) {
            return null;
        }

        List<ReconOtpInfo> reconOtpInfos = new ArrayList<>();
        for (SalesOrders salesOrder : creditOrders) {
            ReconOtpInfo reconOtpInfo = new ReconOtpInfo();
            reconOtpInfo.setOrderNumber(salesOrder.getSalesOrderNumber());
            reconOtpInfo.setOtpCode(salesOrder.getCreditOneTimePassword());
            reconOtpInfo.setCustomerName(salesOrder.getCustomer().getName());
            reconOtpInfo.setCustomerPublicId(salesOrder.getCustomer().getPublicId().toString());
            reconOtpInfos.add(reconOtpInfo);
        }

        return reconOtpInfos;
    }

    @Override
    public Page<CreditRecon> getAllCreditRecons(String searchParam, String deliveryAgentPublicId, String salesAgentPublicId, String customerPublicId,
                                                String orderNumber, LocalDate from, LocalDate to, Pageable pageable) {

        return creditReconHelper.getAllCreditRecons(searchParam, deliveryAgentPublicId, salesAgentPublicId, customerPublicId, orderNumber, from, to, pageable
        );

    }


    @Override
    public Page<CreditReconItem> getCreditReconItems(UUID creditReconId, String searchParam, Pageable pageable) {
        Optional<CreditRecon> optionalCreditRecon = creditReconRepository.findCreditReconByPublicIdAndDeletedIsFalse(creditReconId);
        if (optionalCreditRecon.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("No such credit reconciliation")
                    .userMessage("No such credit reconciliation found");
        }

        return creditReconHelper.getCreditReconItems(optionalCreditRecon.get(), searchParam, pageable);
    }

    @Override
    public void reconcileCreditRecon(UUID creditReconId, String comment, String name) {

        if (comment == null || comment.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Reconciliation comment is required")
                    .userMessage("Reconciliation comment is required");
        }

        Optional<CreditRecon> optionalCreditRecon = creditReconRepository.findCreditReconByPublicIdAndDeletedIsFalse(creditReconId);
        if (optionalCreditRecon.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("No such credit reconciliation")
                    .userMessage("No such credit reconciliation found");
        }

        CreditRecon creditRecon = optionalCreditRecon.get();

        if (creditRecon.isReconciled()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Credit reconciliation already reconciled")
                    .userMessage("Credit reconciliation already reconciled, cannot be updated");
        }

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("No such person")
                    .userMessage("No such person found");
        }

        Person person = optionalPerson.get();

        creditRecon.setReconciled(true);
        creditRecon.setReconciliationComment(comment);
        creditRecon.setUpdatedBy(person);
        creditRecon.setUpdateDate(LocalDateTime.now());
        creditReconRepository.save(creditRecon);

        List<CreditReconItem> creditReconItems = creditRecon.getCreditReconItems();

        for (CreditReconItem item : creditReconItems) {
            if (!item.getReconciled()) {
                item.setReconciled(true);
                creditReconItemRepository.save(item);
            }
        }

        updateCreditLimitItem(creditRecon.getOrderNumber(), comment);

        eventsRepository.save(new Events(creditReconId.toString(), "Reconciled", LocalDateTime.now(), comment, "RECONCILE", person));
    }

    private void updateCreditLimitItem(String orderNumber, String comment) {
        Optional<CreditLimitItem> optionalCreditLimitItem = creditLimitItemRepository.findByOrderNumberAndDeletedIsFalse(orderNumber);
        if (optionalCreditLimitItem.isPresent()) {
            CreditLimitItem creditLimitItem = optionalCreditLimitItem.get();
            creditLimitItem.setReconciled(true);
            creditLimitItem.setReconciliationComment(comment);
            creditLimitItemRepository.save(creditLimitItem);
        }
    }

    @Override
    @Transactional
    public CashCreditReconReportDTO generateCashCreditReconReport(LocalDate reportDate, String name) {
        return generateCashCreditReconReport(reportDate, getPerson(name));
    }

    private CashCreditReconReportDTO generateCashCreditReconReport(LocalDate reportDate, Person person) {
        CashCreditReconReport report = cashCreditReconReportRepository
                .findFirstByReportDateAndReportStatusNotAndDeletedIsFalseOrderByCreationTimeDesc(
                        reportDate,
                        CashCreditReconReportStatus.CANCELLED)
                .orElseGet(CashCreditReconReport::new);

        if (report.getId() != null && report.getReportStatus() == CashCreditReconReportStatus.RECONCILED) {
            return toDto(report);
        }

        if (report.getId() == null) {
            report.setReportDate(reportDate);
            report.setReportStatus(CashCreditReconReportStatus.GENERATED);
            report.setGeneratedAt(LocalDateTime.now());
            report.setCreatedBy(person);
        }
        report.setUpdatedBy(person);

        CashCreditReconReport savedReport = cashCreditReconReportRepository.save(report);
        rebuildReportLines(savedReport, person);
        recalculateReportTotals(savedReport);

        eventsRepository.save(new Events(savedReport.getPublicId().toString(),
                "CASH CREDIT RECON REPORT GENERATED",
                LocalDateTime.now(),
                "Cash and credit reconciliation report generated for " + reportDate,
                "GENERATED",
                person));

        return toDto(savedReport);
    }

    @Override
    public CashCreditReconReportDTO getCashCreditReconReport(UUID publicId) {
        return toDto(getCashCreditReport(publicId));
    }

    @Override
    @Transactional
    public CashCreditReconReportDTO refreshCashCreditReconReport(UUID publicId, String name) {
        Person person = getPerson(name);
        CashCreditReconReport report = cashCreditReconReportRepository.findByPublicIdAndDeletedIsFalseForUpdate(publicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Cash credit reconciliation report not found")
                        .userMessage("Report not found"));
        assertReportEditable(report);

        rebuildReportLines(report, person);
        recalculateReportTotals(report);

        eventsRepository.save(new Events(report.getPublicId().toString(),
                "CASH CREDIT RECON REPORT REFRESHED",
                LocalDateTime.now(),
                "Cash and credit reconciliation report refreshed",
                "REFRESHED",
                person));

        return toDto(report);
    }

    @Override
    public CashCreditReconReportDTO updateCashCreditReconAdjustment(UUID reportPublicId,
                                                                    UUID linePublicId,
                                                                    CashCreditReconAdjustmentRequest request,
                                                                    String name) {
        Person person = getPerson(name);
        CashCreditReconReport report = getCashCreditReport(reportPublicId);
        assertReportEditable(report);

        if (request.getType() == null || request.getAmount() == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Adjustment type and amount are required")
                    .userMessage("Adjustment type and amount are required");
        }

        CashCreditReconReportLine line = cashCreditReconReportLineRepository.findByPublicIdAndDeletedIsFalse(linePublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Cash credit recon report line not found")
                        .userMessage("Report line not found"));

        if (!line.getReport().getPublicId().equals(report.getPublicId())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Report line does not belong to selected report")
                    .userMessage("Report line does not belong to selected report");
        }

        CashCreditReconAdjustment adjustment = cashCreditReconAdjustmentRepository
                .findByReportLineAndAdjustmentTypeAndDeletedIsFalse(line, request.getType())
                .orElseGet(CashCreditReconAdjustment::new);

        if (adjustment.getId() == null) {
            adjustment.setReportLine(line);
            adjustment.setAdjustmentType(request.getType());
            adjustment.setCreatedBy(person);
        }
        adjustment.setAmount(request.getAmount());
        adjustment.setNote(request.getNote());
        adjustment.setAttachmentUrl(request.getAttachmentUrl());
        adjustment.setApprovalStatus(CashCreditReconAdjustmentStatus.PENDING);
        adjustment.setReviewComment(null);
        adjustment.setUpdatedBy(person);
        cashCreditReconAdjustmentRepository.save(adjustment);

        applyAdjustments(line);
        cashCreditReconReportLineRepository.save(line);
        recalculateReportTotals(report);

        eventsRepository.save(new Events(report.getPublicId().toString(),
                "CASH CREDIT RECON REPORT ADJUSTED",
                LocalDateTime.now(),
                "Adjusted " + request.getType() + " for " + line.getDeliveryAgentName(),
                "ADJUSTED",
                person));

        return toDto(report);
    }

    @Override
    public CashCreditReconReportDTO reconcileCashCreditReconReport(UUID publicId, String comment, String name) {
        Person person = getPerson(name);
        CashCreditReconReport report = getCashCreditReport(publicId);
        assertReportEditable(report);

        if (comment == null || comment.isBlank()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Reconciliation comment is required")
                    .userMessage("Reconciliation comment is required");
        }

        recalculateReportTotals(report);
        report.setReportStatus(CashCreditReconReportStatus.RECONCILED);
        report.setReconciledAt(LocalDateTime.now());
        report.setReconciledBy(person.getFullName());
        report.setReconciliationComment(comment);
        report.setUpdatedBy(person);
        CashCreditReconReport savedReport = cashCreditReconReportRepository.save(report);

        for (CashCreditReconReportLine line : cashCreditReconReportLineRepository.findByReportAndDeletedIsFalse(savedReport)) {
            lockSourceRecords(line, person);
        }

        eventsRepository.save(new Events(savedReport.getPublicId().toString(),
                "CASH CREDIT RECON REPORT RECONCILED",
                LocalDateTime.now(),
                comment,
                "RECONCILED",
                person));

        return toDto(savedReport);
    }

    @Override
    public Page<Events> getCashCreditReconReportEvents(UUID publicId, Pageable pageable) {
        CashCreditReconReport report = getCashCreditReport(publicId);
        return eventsRepository.findByReferenceIdIgnoreCase(report.getPublicId().toString(), pageable);
    }

    private void rebuildReportLines(CashCreditReconReport report, Person person) {
        List<RoutePlanning> routes = routePlanningRepository.findByCreationTimeBetweenAndDeletedIsFalse(
                report.getReportDate().atStartOfDay(),
                report.getReportDate().atTime(LocalTime.MAX));
        Set<String> activeRouteIds = new HashSet<>();

        for (RoutePlanning route : routes) {
            activeRouteIds.add(route.getPublicId().toString());
            CashCreditReconReportLine line = cashCreditReconReportLineRepository
                    .findByReportAndRoutePublicIdAndDeletedIsFalse(report, route.getPublicId().toString())
                    .orElseGet(CashCreditReconReportLine::new);

            if (line.getId() == null) {
                line.setReport(report);
                line.setRoutePublicId(route.getPublicId().toString());
                line.setCreatedBy(person);
            }

            Reconciliation reconciliation = reconciliationRepository
                    .findByRoutePublicIdAndDeletedIsFalse(route.getPublicId().toString())
                    .orElse(null);
            ProductRecon productRecon = productReconRepository
                    .findProductReconByDeliveryIdIgnoreCaseAndDeletedIsFalse(route.getRouteId())
                    .orElse(null);

            line.setRouteId(route.getRouteId());
            line.setDeliveryAgentPublicId(route.getDeliveryAgentId());
            line.setDeliveryAgentName(route.getDeliveryAgentName());
            line.setVehicleNumber(route.getVehicleName());
            line.setGmvDelivered(reconciliation != null ? zero(reconciliation.getDeliveryAmount()) : BigDecimal.ZERO);
            line.setDiscounts(reconciliation != null ? zero(reconciliation.getTotalDiscount().subtract(reconciliation.getCreditDiscount())) : BigDecimal.ZERO);
            line.setCreditDiscounts(reconciliation != null ? zero(reconciliation.getCreditDiscount()) : BigDecimal.ZERO);
            line.setCreditOrders(reconciliation != null ? zero(reconciliation.getCreditAmount()) : BigDecimal.ZERO);
            line.setDepositedAmount(reconciliation != null ? zero(reconciliation.getDepositAmount()) : BigDecimal.ZERO);
            line.setSourceReconciliationPublicId(reconciliation != null ? reconciliation.getPublicId().toString() : null);
            line.setSourceProductReconPublicId(productRecon != null ? productRecon.getPublicId().toString() : null);
            line.setUpdatedBy(person);

            // Adjustment queries require a persistent report line. New lines do not
            // have an identifier until they are saved, so querying with them causes
            // Hibernate to throw TransientObjectException.
            if (line.getId() == null) {
                line = cashCreditReconReportLineRepository.save(line);
            }

            applyAdjustments(line);
            cashCreditReconReportLineRepository.save(line);
        }

        for (CashCreditReconReportLine existingLine : cashCreditReconReportLineRepository.findByReportAndDeletedIsFalse(report)) {
            if (!activeRouteIds.contains(existingLine.getRoutePublicId())) {
                existingLine.setDeleted(true);
                existingLine.setUpdatedBy(person);
                cashCreditReconReportLineRepository.save(existingLine);
            }
        }
    }

    private void applyAdjustments(CashCreditReconReportLine line) {
        BigDecimal complementaries = BigDecimal.ZERO;
        BigDecimal loadingOffloading = BigDecimal.ZERO;
        BigDecimal parkingFee = BigDecimal.ZERO;
        BigDecimal ferryCost = BigDecimal.ZERO;
        BigDecimal metaOutstanding = BigDecimal.ZERO;

        for (CashCreditReconAdjustment adjustment : cashCreditReconAdjustmentRepository.findByReportLineAndDeletedIsFalse(line)) {
            if (adjustment.getApprovalStatus() != CashCreditReconAdjustmentStatus.APPROVED) {
                continue;
            }
            BigDecimal amount = zero(adjustment.getAmount());
            if (adjustment.getAdjustmentType() == CashCreditReconAdjustmentType.COMPLEMENTARY) {
                complementaries = complementaries.add(amount);
            } else if (adjustment.getAdjustmentType() == CashCreditReconAdjustmentType.LOADING_AND_OFFLOADING) {
                loadingOffloading = loadingOffloading.add(amount);
            } else if (adjustment.getAdjustmentType() == CashCreditReconAdjustmentType.PARKING_FEE) {
                parkingFee = parkingFee.add(amount);
            } else if (adjustment.getAdjustmentType() == CashCreditReconAdjustmentType.FERRY_COST) {
                ferryCost = ferryCost.add(amount);
            } else if (adjustment.getAdjustmentType() == CashCreditReconAdjustmentType.META_OUTSTANDING) {
                metaOutstanding = metaOutstanding.add(amount);
            }
        }

        line.setComplementaries(complementaries);
        line.setLoadingOffloading(loadingOffloading);
        line.setParkingFee(parkingFee);
        line.setFerryCost(ferryCost);
        line.setMetaOutstanding(metaOutstanding);

        BigDecimal netGmv = zero(line.getGmvDelivered())
                .subtract(zero(line.getDiscounts()))
                .subtract(complementaries);
        BigDecimal expectedCash = netGmv
                .subtract(zero(line.getCreditOrders()))
                .subtract(loadingOffloading)
                .subtract(parkingFee)
                .subtract(ferryCost);

        BigDecimal difference = zero(line.getDepositedAmount()).subtract(expectedCash).add(line.getCreditDiscounts());

        line.setNetGmv(netGmv);
        line.setDifference(difference);
        if (difference.compareTo(BigDecimal.ZERO) > 0) {
            line.setLineStatus(CashCreditReconLineStatus.OVER);
        } else if (difference.compareTo(BigDecimal.ZERO) < 0) {
            line.setLineStatus(CashCreditReconLineStatus.UNDER);
        } else {
            line.setLineStatus(CashCreditReconLineStatus.BALANCED);
        }
    }

    private void recalculateReportTotals(CashCreditReconReport report) {
        List<CashCreditReconReportLine> lines = cashCreditReconReportLineRepository.findByReportAndDeletedIsFalse(report);

        report.setTotalGmvDelivered(sum(lines, CashCreditReconReportLine::getGmvDelivered));
        report.setTotalDiscounts(sum(lines, CashCreditReconReportLine::getDiscounts));
        report.setTotalComplementaries(sum(lines, CashCreditReconReportLine::getComplementaries));
        report.setTotalNetGmv(sum(lines, CashCreditReconReportLine::getNetGmv));
        report.setTotalLoadingOffloading(sum(lines, CashCreditReconReportLine::getLoadingOffloading));
        report.setTotalCreditOrders(sum(lines, CashCreditReconReportLine::getCreditOrders));
        report.setTotalParkingFee(sum(lines, CashCreditReconReportLine::getParkingFee));
        report.setTotalFerryCost(sum(lines, CashCreditReconReportLine::getFerryCost));
        report.setTotalCreditDiscounts(sum(lines, CashCreditReconReportLine::getCreditDiscounts));
        report.setTotalDepositedAmount(sum(lines, CashCreditReconReportLine::getDepositedAmount));
        report.setTotalDifference(sumDifference(lines, CashCreditReconReportLine::getDifference));
        report.setTotalMetaOutstanding(sum(lines, CashCreditReconReportLine::getMetaOutstanding));
        cashCreditReconReportRepository.save(report);
    }

    private void lockSourceRecords(CashCreditReconReportLine line, Person person) {
        if (line.getSourceReconciliationPublicId() != null) {
            reconciliationRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(line.getSourceReconciliationPublicId()))
                    .ifPresent(reconciliation -> {
                        reconciliation.setLocked(true);
                        reconciliation.setUpdatedBy(person);
                        reconciliationRepository.save(reconciliation);
                    });
        }

        if (line.getSourceProductReconPublicId() != null) {
            productReconRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(line.getSourceProductReconPublicId()))
                    .ifPresent(productRecon -> {
                        productRecon.setLocked(true);
                        productRecon.setUpdatedBy(person);
                        productReconRepository.save(productRecon);
                    });
        }
    }

    private CashCreditReconReport getCashCreditReport(UUID publicId) {
        return cashCreditReconReportRepository.findByPublicIdAndDeletedIsFalse(publicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Cash credit reconciliation report not found")
                        .userMessage("Report not found"));
    }

    private void assertReportEditable(CashCreditReconReport report) {
        if (report.getReportStatus() == CashCreditReconReportStatus.RECONCILED) {
            throw new BadRequestRestApiException()
                    .developerMessage("Cash credit reconciliation report is already reconciled")
                    .userMessage("Report is already reconciled and cannot be updated");
        }
    }

    private void assertReconciliationEditable(Reconciliation reconciliation) {
        boolean lockedByReport = reconciliation.getPublicId() != null
                && cashCreditReconReportLineRepository.existsBySourceReconciliationPublicIdAndReport_ReportStatusAndDeletedIsFalse(
                reconciliation.getPublicId().toString(),
                CashCreditReconReportStatus.RECONCILED);

        if (reconciliation.isLocked() || lockedByReport) {
            throw new BadRequestRestApiException()
                    .developerMessage("Reconciliation is locked by a reconciled cash credit report")
                    .userMessage("This reconciliation is locked by a reconciled cash and credit report");
        }
    }

    private Person findPerson(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return personRepository.findByEmailAndDeletedIsFalse(name).orElse(null);
    }

    private Person getPerson(String name) {
        Person person = findPerson(name);
        if (person == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("No such person: " + name)
                    .userMessage("No such person found");
        }
        return person;
    }

    private CashCreditReconReportDTO toDto(CashCreditReconReport report) {
        List<CashCreditReconReportLineDTO> lines = cashCreditReconReportLineRepository.findByReportAndDeletedIsFalse(report)
                .stream()
                .map(this::toLineDto)
                .sorted(Comparator.comparing(
                        CashCreditReconReportLineDTO::getDeliveryAgentName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        return CashCreditReconReportDTO.builder()
                .publicId(report.getPublicId())
                .reportDate(report.getReportDate())
                .status(report.getReportStatus())
                .generatedAt(report.getGeneratedAt())
                .reconciledAt(report.getReconciledAt())
                .reconciledBy(report.getReconciledBy())
                .reconciliationComment(report.getReconciliationComment())
                .totalGmvDelivered(zero(report.getTotalGmvDelivered()))
                .totalDiscounts(zero(report.getTotalDiscounts()))
                .totalCreditDiscounts(zero(report.getTotalCreditDiscounts()))
                .totalComplementaries(zero(report.getTotalComplementaries()))
                .totalNetGmv(zero(report.getTotalNetGmv()))
                .totalLoadingOffloading(zero(report.getTotalLoadingOffloading()))
                .totalCreditOrders(zero(report.getTotalCreditOrders()))
                .totalParkingFee(zero(report.getTotalParkingFee()))
                .totalFerryCost(zero(report.getTotalFerryCost()))
                .totalDepositedAmount(zero(report.getTotalDepositedAmount()))
                .totalDifference(zero(report.getTotalDifference()))
                .totalMetaOutstanding(zero(report.getTotalMetaOutstanding()))
                .lines(lines)
                .build();
    }

    private CashCreditReconReportLineDTO toLineDto(CashCreditReconReportLine line) {
        List<CashCreditReconAdjustmentDTO> adjustments = cashCreditReconAdjustmentRepository.findByReportLineAndDeletedIsFalse(line)
                .stream()
                .map(adjustment -> CashCreditReconAdjustmentDTO.builder()
                        .publicId(adjustment.getPublicId())
                        .type(adjustment.getAdjustmentType())
                        .amount(zero(adjustment.getAmount()))
                        .note(adjustment.getNote())
                        .approvalStatus(adjustment.getApprovalStatus())
                        .reviewComment(adjustment.getReviewComment())
                        .attachmentUrl(adjustment.getAttachmentUrl())
                        .build())
                .toList();

        return CashCreditReconReportLineDTO.builder()
                .publicId(line.getPublicId())
                .routePublicId(line.getRoutePublicId())
                .routeId(line.getRouteId())
                .deliveryAgentPublicId(line.getDeliveryAgentPublicId())
                .deliveryAgentName(line.getDeliveryAgentName())
                .vehicleNumber(line.getVehicleNumber())
                .gmvDelivered(zero(line.getGmvDelivered()))
                .discounts(zero(line.getDiscounts()))
                .creditDiscounts(line.getCreditDiscounts())
                .complementaries(zero(line.getComplementaries()))
                .netGmv(zero(line.getNetGmv()))
                .loadingOffloading(zero(line.getLoadingOffloading()))
                .creditOrders(zero(line.getCreditOrders()))
                .parkingFee(zero(line.getParkingFee()))
                .ferryCost(zero(line.getFerryCost()))
                .depositedAmount(zero(line.getDepositedAmount()))
                .difference(zero(line.getDifference()))
                .metaOutstanding(zero(line.getMetaOutstanding()))
                .status(line.getLineStatus())
                .sourceReconciliationPublicId(line.getSourceReconciliationPublicId())
                .sourceProductReconPublicId(line.getSourceProductReconPublicId())
                .adjustments(adjustments)
                .build();
    }

    private BigDecimal sum(List<CashCreditReconReportLine> lines,
                           java.util.function.Function<CashCreditReconReportLine, BigDecimal> mapper) {
        return lines.stream().map(mapper).map(this::zero).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumDifference(List<CashCreditReconReportLine> lines,
                                     java.util.function.Function<CashCreditReconReportLine, BigDecimal> mapper) {
        BigDecimal amount = lines.stream().map(mapper).map(this::zero).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal creditDiscount = lines.stream().map(CashCreditReconReportLine::getCreditDiscounts).map(this::zero).reduce(BigDecimal.ZERO, BigDecimal::add);
        return amount.subtract(creditDiscount);
    }

    private BigDecimal zero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    @Override
    @Transactional
    public String addCashCreditReconAdjustment(CashCreditReconAdjustmentRequest request, String name) {
        if (Validators.anyEqualNull(request.getType(), request.getAmount(), request.getRoutePublicId())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Adjustment type, amount and route public ID are required")
                    .userMessage("Adjustment type, amount and route public ID are required");
        }
        if (request.getAmount().signum() < 0) {
            throw new BadRequestRestApiException()
                    .developerMessage("A non-negative expense amount is required")
                    .userMessage("A non-negative expense amount is required");
        }

        RoutePlanning routePlanning = routePlanningRepository.findByPublicId(request.getRoutePublicId());

        if (routePlanning == null || Boolean.TRUE.equals(routePlanning.getDeleted())) {
            throw new BadRequestRestApiException()
                    .developerMessage("No such route planning")
                    .userMessage("No such route planning found");
        }

        Person person = findPerson(name);
        CashCreditReconReportLine cashCreditReconReportLine = cashCreditReconReportLineRepository.findByDeliveryAgentPublicIdAndRoutePublicIdAndDeletedIsFalse(routePlanning.getDeliveryAgentId(), routePlanning.getPublicId().toString())
                .orElse(null);

        if (cashCreditReconReportLine == null) {
            generateCashCreditReconReport(routePlanning.getCreationTime().toLocalDate(), person);
            cashCreditReconReportLine = cashCreditReconReportLineRepository
                    .findByDeliveryAgentPublicIdAndRoutePublicIdAndDeletedIsFalse(
                            routePlanning.getDeliveryAgentId(), routePlanning.getPublicId().toString())
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Cash credit reconciliation report line not found after report generation for route "
                                    + routePlanning.getPublicId())
                            .userMessage("Unable to create a reconciliation report line for this route"));
        }

        assertReportEditable(cashCreditReconReportLine.getReport());
        // create adjustment and add to this

        CashCreditReconAdjustment cashCreditReconAdjustment = new CashCreditReconAdjustment();
        cashCreditReconAdjustment.setAdjustmentType(request.getType());
        cashCreditReconAdjustment.setAmount(request.getAmount());
        cashCreditReconAdjustment.setNote(request.getNote());
        cashCreditReconAdjustment.setAttachmentUrl(request.getAttachmentUrl());
        cashCreditReconAdjustment.setReportLine(cashCreditReconReportLine);
        cashCreditReconAdjustment.setApprovalStatus(CashCreditReconAdjustmentStatus.PENDING);
        cashCreditReconAdjustment.setCreatedBy(person);
        cashCreditReconAdjustment.setUpdatedBy(person);
        cashCreditReconAdjustment.setCreationTime(LocalDateTime.now());

        CashCreditReconAdjustment saved = cashCreditReconAdjustmentRepository.save(cashCreditReconAdjustment);

        cashCreditReconReportLine.getAdjustments().add(saved);
        cashCreditReconReportLineRepository.save(cashCreditReconReportLine);

        eventsRepository.save(new Events(cashCreditReconReportLine.getReport().getPublicId().toString(),
                "LOGISTICS EXPENSE SUBMITTED", LocalDateTime.now(),
                "Submitted " + request.getType() + " expense of " + request.getAmount()
                        + " for " + cashCreditReconReportLine.getDeliveryAgentName(),
                "PENDING", person));

        return saved.getPublicId().toString();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CashCreditReconPendingAdjustmentsDTO> getCashCreditReconAdjustments(String deliveryAgentAgent,
                                                                                    String searchParam,
                                                                                    LocalDate from,
                                                                                    LocalDate to,
                                                                                    Pageable pageable) {

        LocalDateTime fromDateTime = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = to != null ? to.atTime(23, 59, 59) : null;
        String routeSearch = searchParam == null || searchParam.isBlank() ? null : searchParam.trim();

        return cashCreditReconReportLineRepository
                .findLinesWithAdjustments(deliveryAgentAgent, routeSearch, fromDateTime, toDateTime, pageable)
                .map(this::toPendingAdjustmentsDto);
    }

    @Override
    public CashCreditReconAdjustmentDTO reviewCashCreditReconAdjustment(UUID adjustmentPublicId,
                                                                        CashCreditReconAdjustmentStatus status,
                                                                        String comment,
                                                                        String name) {
        if (status != CashCreditReconAdjustmentStatus.APPROVED && status != CashCreditReconAdjustmentStatus.REJECTED) {
            throw new BadRequestRestApiException().developerMessage("Invalid logistics expense review status")
                    .userMessage("Only approval or rejection is allowed");
        }
        Person person = getPerson(name);
        CashCreditReconAdjustment adjustment = cashCreditReconAdjustmentRepository.findByPublicIdAndDeletedIsFalse(adjustmentPublicId)
                .orElseThrow(() -> new BadRequestRestApiException().developerMessage("Logistics expense not found")
                        .userMessage("Logistics expense not found"));
        assertReportEditable(adjustment.getReportLine().getReport());
        if (adjustment.getApprovalStatus() != CashCreditReconAdjustmentStatus.PENDING) {
            throw new BadRequestRestApiException().developerMessage("Logistics expense has already been reviewed")
                    .userMessage("Only pending logistics expenses can be reviewed");
        }
        adjustment.setApprovalStatus(status);
        adjustment.setReviewComment(comment);
        adjustment.setUpdatedBy(person);
        cashCreditReconAdjustmentRepository.save(adjustment);

        CashCreditReconReportLine line = adjustment.getReportLine();
        applyAdjustments(line);
        cashCreditReconReportLineRepository.save(line);
        if (status == CashCreditReconAdjustmentStatus.APPROVED) {
            synchronizeReconciliationExpenseAmount(line, person);
        }
        recalculateReportTotals(line.getReport());
        eventsRepository.save(new Events(line.getReport().getPublicId().toString(), "LOGISTICS EXPENSE " + status,
                LocalDateTime.now(), "Expense " + adjustment.getAdjustmentType() + " of " + zero(adjustment.getAmount())
                + " was " + status.name().toLowerCase() + (comment == null || comment.isBlank() ? "" : ": " + comment),
                status.name(), person));
        return toAdjustmentDto(adjustment);
    }

    /**
     * Keeps the operational reconciliation in sync with the approved logistics
     * expenses recorded against its cash/credit reconciliation report line.
     */
    private void synchronizeReconciliationExpenseAmount(CashCreditReconReportLine line, Person person) {
        BigDecimal approvedExpenses = cashCreditReconAdjustmentRepository.findByReportLineAndDeletedIsFalse(line).stream()
                .filter(adjustment -> adjustment.getApprovalStatus() == CashCreditReconAdjustmentStatus.APPROVED)
                .map(CashCreditReconAdjustment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        reconciliationRepository.findByRoutePublicIdAndDeletedIsFalse(line.getRoutePublicId())
                .ifPresent(reconciliation -> {
                    reconciliation.setExpenseAmount(approvedExpenses);
                    reconciliation.setUpdatedBy(person);
                    reconciliation.setUpdateDate(LocalDateTime.now());
                    reconciliationRepository.save(reconciliation);
                });
    }

    @Override
    public CashCreditReconAdjustmentDTO updateCashCreditReconAdjustmentAmount(UUID adjustmentPublicId,
                                                                              BigDecimal amount,
                                                                              String comment,
                                                                              String name) {
        if (amount == null || amount.signum() < 0) {
            throw new BadRequestRestApiException().developerMessage("A non-negative expense amount is required")
                    .userMessage("A non-negative expense amount is required");
        }
        Person person = getPerson(name);
        CashCreditReconAdjustment adjustment = cashCreditReconAdjustmentRepository.findByPublicIdAndDeletedIsFalse(adjustmentPublicId)
                .orElseThrow(() -> new BadRequestRestApiException().developerMessage("Logistics expense not found")
                        .userMessage("Logistics expense not found"));
        CashCreditReconReportLine line = adjustment.getReportLine();
        assertReportEditable(line.getReport());
        if (adjustment.getApprovalStatus() != CashCreditReconAdjustmentStatus.PENDING) {
            throw new BadRequestRestApiException().developerMessage("Reviewed logistics expenses cannot be edited")
                    .userMessage("Only pending logistics expenses can be edited");
        }

        BigDecimal previousAmount = zero(adjustment.getAmount());
        adjustment.setAmount(amount);
        if (comment != null && !comment.isBlank()) {
            adjustment.setReviewComment(comment);
        }
        adjustment.setUpdatedBy(person);
        cashCreditReconAdjustmentRepository.save(adjustment);

        eventsRepository.save(new Events(line.getReport().getPublicId().toString(), "LOGISTICS EXPENSE AMOUNT EDITED",
                LocalDateTime.now(), "Changed " + adjustment.getAdjustmentType() + " from " + previousAmount + " to " + amount
                + (comment == null || comment.isBlank() ? "" : ": " + comment), "AMOUNT_EDITED", person));
        return toAdjustmentDto(adjustment);
    }

    private CashCreditReconAdjustmentDTO toAdjustmentDto(CashCreditReconAdjustment adjustment) {
        return CashCreditReconAdjustmentDTO.builder().publicId(adjustment.getPublicId())
                .type(adjustment.getAdjustmentType()).amount(zero(adjustment.getAmount()))
                .note(adjustment.getNote()).approvalStatus(adjustment.getApprovalStatus())
                .creationTime(adjustment.getCreationTime())
                .reviewComment(adjustment.getReviewComment()).attachmentUrl(adjustment.getAttachmentUrl()).build();
    }

    private CashCreditReconPendingAdjustmentsDTO toPendingAdjustmentsDto(CashCreditReconReportLine line) {
        List<CashCreditReconAdjustmentDTO> adjustments = cashCreditReconAdjustmentRepository
                .findByReportLineAndDeletedIsFalse(line)
                .stream()
                .map(this::toAdjustmentDto)
                .toList();
        boolean reconciled = line.getReport().getReportStatus() == CashCreditReconReportStatus.RECONCILED;
        boolean allApproved = adjustments.stream().allMatch(adjustment -> adjustment.getApprovalStatus() == CashCreditReconAdjustmentStatus.APPROVED);
        boolean allPending = adjustments.stream().allMatch(adjustment -> adjustment.getApprovalStatus() == CashCreditReconAdjustmentStatus.PENDING);
        boolean allRejected = adjustments.stream().allMatch(adjustment -> adjustment.getApprovalStatus() == CashCreditReconAdjustmentStatus.REJECTED);


        return CashCreditReconPendingAdjustmentsDTO.builder()
                .reportLinePublicId(line.getPublicId())
                .routePublicId(line.getRoutePublicId())
                .routeId(line.getRouteId())
                .deliveryAgentPublicId(line.getDeliveryAgentPublicId())
                .deliveryAgentName(line.getDeliveryAgentName())
                .reportDate(line.getReport().getReportDate())
                .vehicleNumber(line.getVehicleNumber())
                .reconciled(reconciled)
                .pendingAdjustments(adjustments)
                .adjustments(adjustments)
                .status(getStatus(allApproved, allRejected, allPending))
                .build();
    }

    private String getStatus(boolean allApproved, boolean allRejected, boolean allPending) {
        if (allApproved) return CashCreditReconAdjustmentStatus.APPROVED.name();
        if (allPending) return CashCreditReconAdjustmentStatus.PENDING.name();
        if (allRejected) return CashCreditReconAdjustmentStatus.REJECTED.name();
        return CashCreditReconAdjustmentStatus.PARTIALLY_APPROVED.name();
    }
}
