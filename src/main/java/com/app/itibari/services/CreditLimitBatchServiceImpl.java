package com.app.itibari.services;

import com.app.itibari.dataTypes.BatchStatus;
import com.app.itibari.dataTypes.Batched;
import com.app.itibari.dataTypes.CounterType;
import com.app.itibari.dataTypes.PaymentStatusEnum;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.CreditBatchAction;
import com.app.itibari.models.request.CreditBatchRequest;
import com.app.itibari.models.response.BNPLPaymentResponse;
import com.app.itibari.models.response.BatchResponsePageable;
import com.app.itibari.models.response.CreditBatchResponse;
import com.app.itibari.repository.*;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.helpers.Validators;
import com.app.itibari.utils.specification.CreditBatchSpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CreditLimitBatchServiceImpl implements CreditLimitBatchService {
    private final CreditLimitBatchRepository creditLimitBatchRepository;
    private final PersonRepository personRepository;
    private final CustomersRepository customersRepository;
    private final CreditLimitItemRepository creditLimitItemRepository;
    private final CounterService counterService;
    private final EventsRepository eventsRepository;

    public CreditLimitBatchServiceImpl(CreditLimitBatchRepository creditLimitBatchRepository, PersonRepository personRepository, CustomersRepository customersRepository, CreditLimitItemRepository creditLimitItemRepository, CounterService counterService, EventsRepository eventsRepository) {
        this.creditLimitBatchRepository = creditLimitBatchRepository;
        this.personRepository = personRepository;
        this.customersRepository = customersRepository;
        this.creditLimitItemRepository = creditLimitItemRepository;
        this.counterService = counterService;
        this.eventsRepository = eventsRepository;
    }

    @Override
    @Transactional
    public CreditBatchResponse createCreditBatch(CreditBatchRequest creditBatchRequest, String name) {

        if (Validators.anyEqualNull(creditBatchRequest.getCreditItemPublicIds(), creditBatchRequest.getParentCustomerPublicId())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Missing required parameters for CreditBatchRequest")
                    .userMessage("Missing required parameters for CreditBatchRequest");
        }
        if (creditBatchRequest.getCreditItemPublicIds().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Missing required parameters for CreditBatchRequest")
                    .userMessage("Missing required parameters for CreditBatchRequest");
        }

        Person loggedUser = validateLoggedInUser(name);
        Customers parentCustomer = validateParentCustomer(creditBatchRequest.getParentCustomerPublicId());

        Set<CreditLimitItem> creditLimitItems = validateCreditLimitItems(creditBatchRequest, parentCustomer);

        BigDecimal amount = getTotalAmount(creditLimitItems);
        BigDecimal paidAmount = getPaidAmount(creditLimitItems);
        BigDecimal balance = amount.subtract(paidAmount);

        CreditLimitBatches batch = new CreditLimitBatches();
        batch.setParentCustomer(parentCustomer);
        batch.setBatchNumber(String.format("CB-%S", counterService.getNextCounter(CounterType.CREDIT_BATCH)));
        batch.setCreditLimitItems(creditLimitItems);
        batch.setBatchStatus(BatchStatus.PENDING);
        batch.setPaidAmount(paidAmount);
        batch.setTotalAmount(amount);
        batch.setBalanceAmount(balance);
        batch.setCreatedBy(loggedUser);
        batch.setCreationTime(LocalDateTime.now());

        CreditLimitBatches saved = creditLimitBatchRepository.save(batch);

        this.updateCreditItems(saved.getPublicId(), saved.getCreditLimitItems());

        eventsRepository.save(new Events(saved.getPublicId().toString(), "BATCH CREATED", LocalDateTime.now(), "Batch created", "CREATE", loggedUser));

        return toCreditBatchResponse(saved);

    }

    @Override
    @Transactional
    public void creditBatchActions(UUID publicId, CreditBatchAction creditBatchAction, String name) {

        if (creditBatchAction == null || creditBatchAction.getBatchAction() == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Batch action is required")
                    .userMessage("Batch action is required");
        }

        CreditLimitBatches batch = validateCreditBatch(publicId);
        Person loggedUser = validateLoggedInUser(name);

        switch (creditBatchAction.getBatchAction()) {
            case DELETE_BATCH -> {
                requireStatus(batch, BatchStatus.PENDING, "Batch must be in PENDING status to delete");
                batch.getCreditLimitItems().forEach(this::markItemUnbatched);
                batch.getCreditLimitItems().clear();
                batch.setDeleted(true);
                batch.setUpdatedBy(loggedUser);
                creditLimitBatchRepository.save(batch);

                eventsRepository.save(new Events(batch.getPublicId().toString(), "BATCH DELETED", LocalDateTime.now(), "Batch deleted", "DELETE", loggedUser));
            }
            case ADD_CREDIT_ITEMS -> {
                if (creditBatchAction.getCreditItemPublicIds() == null || creditBatchAction.getCreditItemPublicIds().isEmpty()) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Missing required parameters for CreditBatchAction")
                            .userMessage("Missing required parameters for CreditBatchAction");
                }

                requireStatus(batch, BatchStatus.PENDING, "Batch status must be PENDING");

                Set<CreditLimitItem> newItems = validateCreditLimitItems(creditBatchAction.getCreditItemPublicIds(), batch.getParentCustomer());
                batch.getCreditLimitItems().addAll(newItems);

                BigDecimal addTotal = getTotalAmount(batch.getCreditLimitItems());
                BigDecimal addPaid = getPaidAmount(batch.getCreditLimitItems());
                batch.setTotalAmount(addTotal);
                batch.setPaidAmount(addPaid);
                batch.setBalanceAmount(addTotal.subtract(addPaid));
                batch.setUpdatedBy(loggedUser);

                CreditLimitBatches addSaved = creditLimitBatchRepository.save(batch);
                this.updateCreditItems(addSaved.getPublicId(), newItems);

                eventsRepository.save(new Events(batch.getPublicId().toString(), "BATCH UPDATED", LocalDateTime.now(), "Batch items Added", "UPDATE", loggedUser));
            }
            case REMOVE_CREDIT_ITEMS -> {
                if (creditBatchAction.getCreditItemPublicIds() == null || creditBatchAction.getCreditItemPublicIds().isEmpty()) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Missing required parameters for CreditBatchAction")
                            .userMessage("Missing required parameters for CreditBatchAction");
                }
                requireStatus(batch, BatchStatus.PENDING, "Batch status must be PENDING");

                Set<CreditLimitItem> itemsToRemove = new HashSet<>();
                creditBatchAction.getCreditItemPublicIds().forEach(itemId -> {
                    CreditLimitItem item = creditLimitItemRepository.findByPublicIdAndDeletedIsFalse(itemId)
                            .orElseThrow(() -> new BadRequestRestApiException()
                                    .developerMessage("Credit Limit Item Not Found")
                                    .userMessage("Credit Limit Item Not Found"));
                    if (!batch.getCreditLimitItems().contains(item)) {
                        throw new BadRequestRestApiException()
                                .developerMessage("Credit Limit Item does not belong to this batch")
                                .userMessage("Credit Limit Item does not belong to this batch");
                    }
                    itemsToRemove.add(item);
                });

                batch.getCreditLimitItems().removeAll(itemsToRemove);

                BigDecimal removeTotal = getTotalAmount(batch.getCreditLimitItems());
                BigDecimal removePaid = getPaidAmount(batch.getCreditLimitItems());
                batch.setTotalAmount(removeTotal);
                batch.setPaidAmount(removePaid);
                batch.setBalanceAmount(removeTotal.subtract(removePaid));
                batch.setUpdatedBy(loggedUser);

                creditLimitBatchRepository.save(batch);

                itemsToRemove.forEach(this::markItemUnbatched);

                eventsRepository.save(new Events(batch.getPublicId().toString(), "BATCH UPDATED", LocalDateTime.now(), "Batch items Removed", "UPDATE", loggedUser));
            }
            case APPROVE_BATCH -> {
                requireStatus(batch, BatchStatus.PENDING, "Batch must be in PENDING status to approve");

                batch.setBatchStatus(BatchStatus.APPROVED);
                batch.setUpdatedBy(loggedUser);
                creditLimitBatchRepository.save(batch);

                eventsRepository.save(new Events(batch.getPublicId().toString(), "BATCH APPROVED", LocalDateTime.now(), "Batch Approved", "APPROVE", loggedUser));
            }
            case GENERATE_INVOICE -> {
                requireStatus(batch, BatchStatus.APPROVED, "Batch must be APPROVED before invoice generation");
                if (batch.getCreditLimitItems().isEmpty()) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Batch has no credit items")
                            .userMessage("Batch has no credit items");
                }

                batch.setInvoiceNumber(generateInvoiceNumber(batch));
                batch.setInvoiceGeneratedDate(LocalDate.now());
                batch.setBatchStatus(BatchStatus.INVOICE_GENERATED);
                batch.setUpdatedBy(loggedUser);
                creditLimitBatchRepository.save(batch);

                eventsRepository.save(new Events(batch.getPublicId().toString(), "BATCH INVOICE GENERATED", LocalDateTime.now(), "Batch invoice generated", "GENERATE_INVOICE", loggedUser));
            }
            case ALLOCATE_RECONCILE, PAY_BATCH -> {
                allocateAndReconcile(batch, creditBatchAction, loggedUser);
            }
            default -> throw new BadRequestRestApiException()
                    .developerMessage("Invalid request")
                    .userMessage("Invalid request");
        }


    }

    @Override
    public CreditBatchResponse getCreditBatchResponse(UUID publicId) {
        CreditLimitBatches batch = validateCreditBatch(publicId);

        return toCreditBatchResponse(batch);
    }

    @Override
    public List<BNPLPaymentResponse> getBatchItems(UUID publicId) {
        CreditLimitBatches batch = validateCreditBatch(publicId);

        Set<CreditLimitItem> items = batch.getCreditLimitItems();

        return toBNPLPaymentResponseList(items);
    }

    @Override
    public BatchResponsePageable fetchCreditBatches(String search, List<BatchStatus> statuses, UUID parentCustomerPublicId, LocalDate startDate, LocalDate endDate, Pageable pageable) {

        Specification<CreditLimitBatches> spec = Specification
                .where(CreditBatchSpecification.hasSearchParam(search))
                .and(CreditBatchSpecification.isNotDeleted())
                .and(CreditBatchSpecification.hasFromToDates(startDate, endDate))
                .and(CreditBatchSpecification.hasStatuses(statuses))
                .and(CreditBatchSpecification.hasParentCustomer(parentCustomerPublicId));
        Page<CreditLimitBatches> batchesPage = creditLimitBatchRepository.findAll(spec, pageable);

        return toBatchResponsePageable(batchesPage);
    }

    private Set<CreditLimitItem> validateCreditLimitItems(CreditBatchRequest creditBatchRequest, Customers parentCustomer) {
        return validateCreditLimitItems(creditBatchRequest.getCreditItemPublicIds(), parentCustomer);
    }

    private Set<CreditLimitItem> validateCreditLimitItems(List<UUID> creditItemPublicIds, Customers parentCustomer) {
        Set<CreditLimitItem> creditLimitItems = new HashSet<>();

        creditItemPublicIds.forEach(publicId -> {
            Optional<CreditLimitItem> creditLimitItemOptional = creditLimitItemRepository.findByPublicIdAndDeletedIsFalse(publicId);
            if (creditLimitItemOptional.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Credit Limit Item Not Found")
                        .userMessage("Credit Limit Item Not Found");
            }
            creditLimitItems.add(creditLimitItemOptional.get());

        });

        List<String> creditThatDoesMeet = new ArrayList<>();

        creditLimitItems.forEach(creditLimitItem -> {
            if (!isEligibleForBatching(creditLimitItem)) {
                creditThatDoesMeet.add(creditLimitItem.getOrderNumber());
            }
        });

        if (!creditThatDoesMeet.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage(String.format("Credit items doesn't meet the required statuses %s", String.join(", ", creditThatDoesMeet)))
                    .userMessage(String.format("Sorry! Credit items doesn't meet the required statuses  %s", String.join(", ", creditThatDoesMeet)));
        }

        List<CreditLimitItem> batchedItems = creditLimitItems.stream()
                .filter(creditLimitItem -> creditLimitItem.getBatched() == Batched.BATCHED)
                .toList();

        if (!batchedItems.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Credit Limits batched items found")
                    .userMessage(String.format("Sorry! Following credit items already batched %s", batchedItems.stream().map(CreditLimitItem::getOrderNumber).collect(Collectors.joining(","))));
        }

        String parentCustomerPublicId = parentCustomer.getPublicId().toString();
        List<String> differentParentItems = creditLimitItems.stream()
                .filter(item -> {
                    String itemParentCustomerPublicId = item.getParentCustomerPublicId();
                    if (itemParentCustomerPublicId == null
                            && item.getWallet() != null
                            && item.getWallet().getCustomer() != null
                            && item.getWallet().getCustomer().getPublicId() != null) {
                        itemParentCustomerPublicId = item.getWallet().getCustomer().getPublicId().toString();
                    }
                    return itemParentCustomerPublicId == null
                            || !itemParentCustomerPublicId.equalsIgnoreCase(parentCustomerPublicId);
                })
                .map(CreditLimitItem::getOrderNumber)
                .toList();

        if (differentParentItems.size() > 2) {
            throw new BadRequestRestApiException()
                    .developerMessage("Credit limit items are from different parent customers")
                    .userMessage(String.format("Sorry! Credit items are from different parent customers %s", String.join(", ", differentParentItems)));
        }

        return creditLimitItems;
    }

    private CreditLimitBatches validateCreditBatch(UUID publicId) {
        return creditLimitBatchRepository.findCreditLimitBatchesByPublicIdAndDeletedIsFalse(publicId).orElseThrow(
                () -> new BadRequestRestApiException()
                        .developerMessage("Credit Limit Item Not Found")
                        .userMessage("Credit Limit Item Not Found")
        );

    }

    private Customers validateParentCustomer(UUID parentCustomerPublicId) {
        return customersRepository.findByPublicIdAndDeletedIsFalse(parentCustomerPublicId).orElseThrow(
                () -> new BadRequestRestApiException()
                        .developerMessage("Customer with id " + parentCustomerPublicId + " does not exist")
                        .userMessage("Sorry!. Parent Customer not found.")
        );
    }

    private Person validateLoggedInUser(String name) {
        return personRepository.findByEmailAndDeletedIsFalse(name).orElseThrow(
                () -> new BadRequestRestApiException()
                        .developerMessage("user not found")
                        .userMessage("Sorry!.User not found")
        );
    }

    private BigDecimal getPaidAmount(Set<CreditLimitItem> creditLimitItems) {
        return creditLimitItems.stream().map(CreditLimitItem::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getTotalAmount(Set<CreditLimitItem> creditLimitItems) {
        BigDecimal orderAmount = creditLimitItems.stream().map(CreditLimitItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal interest = creditLimitItems.stream().map(CreditLimitItem::getInterestAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return orderAmount.add(interest);
    }

    private void updateCreditItems(UUID publicId, Set<CreditLimitItem> creditLimitItems) {
        creditLimitItems.forEach(creditLimitItem -> {
            creditLimitItem.setBatched(Batched.BATCHED);
            creditLimitItem.setBatchPublicId(publicId.toString());
            creditLimitItem.setUpdateDate(LocalDateTime.now());

            creditLimitItemRepository.save(creditLimitItem);
        });
    }

    private CreditBatchResponse toCreditBatchResponse(CreditLimitBatches saved) {

        CreditBatchResponse response = new CreditBatchResponse();
        response.setPublicId(saved.getPublicId());
        response.setCustomer(saved.getParentCustomer().getFullName());
        response.setCustomerPublicId(String.valueOf(saved.getParentCustomer().getPublicId()));
        response.setBatchNumber(saved.getBatchNumber());
        response.setAmount(saved.getTotalAmount());
        response.setBalanceAmount(saved.getBalanceAmount());
        response.setPaidAmount(saved.getPaidAmount());
        response.setBalanceAmount(saved.getBalanceAmount());
        response.setStatus(saved.getBatchStatus());
        response.setCreatedBy(saved.getCreatedBy().getFullName());
        response.setCreatedDate(saved.getCreationTime().toLocalDate());
        response.setPaymentDate(saved.getPaidDate());
        response.setInvoiceNumber(saved.getInvoiceNumber());
        response.setInvoiceGeneratedDate(saved.getInvoiceGeneratedDate());
        response.setPaymentReference(saved.getPaymentReference());

        return response;
    }

    private List<BNPLPaymentResponse> toBNPLPaymentResponseList(Set<CreditLimitItem> items) {

        List<BNPLPaymentResponse> paymentResponses = new ArrayList<>();
        items.forEach(creditLimitItem -> {
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
            dto.setCreditLimit(creditLimitItem.getWallet().getCustomer().getCreditLimit());
            dto.setCreditBalance(creditLimitItem.getWallet().getCreditLimitBalance());
            dto.setDateTaken(creditLimitItem.getDateTaken());
            dto.setDueDate(creditLimitItem.getDueDate());
            dto.setOrderNumber(creditLimitItem.getOrderNumber());
            dto.setOrderDelivered(creditLimitItem.isOrderDelivered());
            dto.setAmountPaidFromWallet(creditLimitItem.getAmountPaidFromWallet());
            dto.setWalletBalance(creditLimitItem.getWallet().getWalletBalance());
            dto.setEnterpriseCredit(creditLimitItem.getWallet().getCustomer().isHasOtherStores() || creditLimitItem.getWallet().getCustomer().isHasParentCustomer());
            dto.setWalletPublicId(creditLimitItem.getWallet().getPublicId().toString());
            paymentResponses.add(dto);
        });
        return paymentResponses;
    }

    private BatchResponsePageable toBatchResponsePageable(Page<CreditLimitBatches> batchesPage) {

        List<CreditBatchResponse> creditBatchResponses = new ArrayList<>();

        if (batchesPage.getTotalElements() > 0) {
            batchesPage.getContent().forEach(creditLimitBatch -> {
                creditBatchResponses.add(toCreditBatchResponse(creditLimitBatch));
            });
        }

        BatchResponsePageable batchResponsePageable = new BatchResponsePageable();
        batchResponsePageable.setTotalElements(batchesPage.getTotalElements());
        batchResponsePageable.setPageSize(batchesPage.getSize());
        batchResponsePageable.setTotalPages(batchesPage.getTotalPages());
        batchResponsePageable.setLast(batchesPage.isLast());
        batchResponsePageable.setFirst(batchesPage.isFirst());
        batchResponsePageable.setPageNumber(batchesPage.getNumber());
        batchResponsePageable.setContent(creditBatchResponses);

        return batchResponsePageable;

    }

    private void requireStatus(CreditLimitBatches batch, BatchStatus expectedStatus, String message) {
        if (batch.getBatchStatus() != expectedStatus) {
            throw new BadRequestRestApiException()
                    .developerMessage(message)
                    .userMessage(message);
        }
    }

    private boolean isEligibleForBatching(CreditLimitItem creditLimitItem) {
        return creditLimitItem.getPaymentStatus() == PaymentStatusEnum.PENDING
                || creditLimitItem.getPaymentStatus() == PaymentStatusEnum.NOT_PAID
                || creditLimitItem.getPaymentStatus() == PaymentStatusEnum.PARTIALLY_PAID;
    }

    private void markItemUnbatched(CreditLimitItem item) {
        item.setBatched(Batched.NOT_BATCHED);
        item.setBatchPublicId(null);
        item.setUpdateDate(LocalDateTime.now());
        creditLimitItemRepository.save(item);
    }

    private String generateInvoiceNumber(CreditLimitBatches batch) {
        if (batch.getInvoiceNumber() != null && !batch.getInvoiceNumber().isBlank()) {
            return batch.getInvoiceNumber();
        }
        return String.format("INV-%s", batch.getBatchNumber());
    }

    private void allocateAndReconcile(CreditLimitBatches batch, CreditBatchAction creditBatchAction, Person loggedUser) {
        if (batch.getBatchStatus() == BatchStatus.PENDING) {
            throw new BadRequestRestApiException()
                    .developerMessage("Batch must be approved before allocation")
                    .userMessage("Batch must be approved before allocation");
        }
        if (!(batch.getBatchStatus() == BatchStatus.INVOICE_GENERATED || batch.getBatchStatus() == BatchStatus.PARTIALLY_PAID)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Batch must be in INVOICE_GENERATED or PARTIALLY_PAID status for allocation")
                    .userMessage("Batch must be in INVOICE_GENERATED or PARTIALLY_PAID status for allocation");
        }

        if (Validators.anyEqualNull(creditBatchAction.getAmountPaid(), creditBatchAction.getReferenceNumber())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Amount paid or reference number is required")
                    .userMessage("Amount paid or reference number is required");
        }

        if (creditBatchAction.getAmountPaid().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestRestApiException()
                    .developerMessage("Amount paid must be greater than zero")
                    .userMessage("Amount paid must be greater than zero");
        }

        BigDecimal existingPaidAmount = normalize(batch.getPaidAmount());
        BigDecimal totalAmount = normalize(batch.getTotalAmount());
        BigDecimal currentBalance = totalAmount.subtract(existingPaidAmount);

        if (currentBalance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestRestApiException()
                    .developerMessage("Batch is already fully paid")
                    .userMessage("Batch is already fully paid");
        }

        BigDecimal allocationAmount = creditBatchAction.getAmountPaid().min(currentBalance);
        allocateToCreditItems(batch, allocationAmount, creditBatchAction.getReferenceNumber());

        BigDecimal paidAmount = existingPaidAmount.add(allocationAmount);
        BigDecimal balanceAmount = totalAmount.subtract(paidAmount);

        batch.setPaidAmount(paidAmount);
        batch.setBalanceAmount(balanceAmount);
        batch.setPaymentReference(creditBatchAction.getReferenceNumber());
        batch.setPaidDate(LocalDate.now());
        batch.setBatchStatus(balanceAmount.compareTo(BigDecimal.ZERO) <= 0 ? BatchStatus.PAID : BatchStatus.PARTIALLY_PAID);
        batch.setUpdatedBy(loggedUser);
        creditLimitBatchRepository.save(batch);

        eventsRepository.save(new Events(batch.getPublicId().toString(), "BATCH ALLOCATED", LocalDateTime.now(), "Batch allocated and reconciled", "ALLOCATE_RECONCILE", loggedUser));
    }

    private void allocateToCreditItems(CreditLimitBatches batch, BigDecimal amountPaid, String referenceNumber) {
        BigDecimal remaining = amountPaid;
        List<CreditLimitItem> items = batch.getCreditLimitItems().stream()
                .sorted(Comparator.comparing(CreditLimitItem::getCreationTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        for (CreditLimitItem item : items) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal itemBalance = normalize(item.getBalanceAmount());
            if (itemBalance.compareTo(BigDecimal.ZERO) <= 0) {
                item.setPaymentStatus(PaymentStatusEnum.PAID);
                item.setFullyPaid(true);
                item.setReconciled(true);
                creditLimitItemRepository.save(item);
                continue;
            }

            BigDecimal appliedAmount = remaining.min(itemBalance);
            BigDecimal newPaidAmount = normalize(item.getPaidAmount()).add(appliedAmount);
            BigDecimal newBalance = itemBalance.subtract(appliedAmount);

            item.setPaidAmount(newPaidAmount);
            item.setPaymentReference(referenceNumber);
            if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
                item.setPaymentStatus(PaymentStatusEnum.PAID);
                item.setFullyPaid(true);
                item.setReconciled(true);
            } else {
                item.setPaymentStatus(PaymentStatusEnum.PARTIALLY_PAID);
                item.setFullyPaid(false);
                item.setReconciled(false);
            }
            creditLimitItemRepository.save(item);
            remaining = remaining.subtract(appliedAmount);
        }
    }

    private BigDecimal normalize(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }
}
