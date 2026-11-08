package com.app.itibari.services;

import com.app.itibari.dataTypes.*;
import com.app.itibari.dataTypes.Module;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.PurchaseOrderApprovalRequest;
import com.app.itibari.models.request.RemittanceRequest;
import com.app.itibari.models.request.TaxMappingRequest;
import com.app.itibari.repository.*;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.helpers.Validators;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class FinancialServiceImpl implements FinancialService {

    private final TaxMappingRepository taxMappingRepository;

    private final PersonRepository personRepository;

    private final PurchaseOrderRepository purchaseOrderRepository;

    private final EventsRepository eventsRepository;

    private final CounterService counterService;

    private final RemittanceService remittanceService;

    private final RemittanceRepository remittanceRepository;

    private final CreditNoteRepository creditNoteRepository;

    private final FileService fileService;

    private final RejectionCodeRepository rejectionCodeRepository;

    public FinancialServiceImpl(TaxMappingRepository taxMappingRepository,
                                PersonRepository personRepository,
                                PurchaseOrderRepository purchaseOrderRepository,
                                EventsRepository eventsRepository,
                                CounterService counterService,
                                RemittanceService remittanceService, RemittanceRepository remittanceRepository, CreditNoteRepository creditNoteRepository, FileService fileService, RejectionCodeRepository rejectionCodeRepository) {

        this.taxMappingRepository = taxMappingRepository;
        this.personRepository = personRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.eventsRepository = eventsRepository;
        this.counterService = counterService;
        this.remittanceService = remittanceService;
        this.remittanceRepository = remittanceRepository;
        this.creditNoteRepository = creditNoteRepository;
        this.fileService = fileService;
        this.rejectionCodeRepository = rejectionCodeRepository;
    }

    @Override
    public TaxMapping getTaxMappingByTaxId(UUID publicId) {

        return taxMappingRepository.findByPublicIdAndDeletedIsFalse(publicId)
                .orElse(null);
    }

    @Override
    public TaxMapping createTaxMapping(TaxMappingRequest taxMappingRequest, String name) {

        if (Validators.anyEqualNull(taxMappingRequest.getTaxType(), taxMappingRequest.getTaxRate(), taxMappingRequest.getTaxName())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Tax mapping request is not valid")
                    .userMessage("Sorry, tax mapping request is not valid");
        }

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        Optional<TaxMapping> optionalTaxMapping = taxMappingRepository.findByTaxNameIgnoreCaseAndDeletedIsFalse(taxMappingRequest.getTaxName());

        if (optionalTaxMapping.isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Tax mapping already exists")
                    .userMessage("Sorry, tax mapping already exists");
        }

        TaxMapping taxMapping = new TaxMapping();
        taxMapping.setTaxName(taxMappingRequest.getTaxName());
        taxMapping.setTaxRate(taxMappingRequest.getTaxRate());
        taxMapping.setCreatedBy(optionalPerson.orElse(null));
        taxMapping.setTaxType(taxMappingRequest.getTaxType());
        taxMapping.setTaxCode(taxMappingRequest.getTaxCode());
        taxMapping.setCreationTime(LocalDateTime.now());

        return taxMappingRepository.save(taxMapping);
    }

    @Override
    public void updateTaxMapping(UUID taxMappingPublicId, TaxMappingRequest taxMappingRequest, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        Optional<TaxMapping> optionalTaxMapping = taxMappingRepository.findByPublicIdAndDeletedIsFalse(taxMappingPublicId);

        if (optionalTaxMapping.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Tax mapping not found")
                    .userMessage("Sorry, tax mapping not found");
        }

        TaxMapping taxMapping = optionalTaxMapping.get();
        taxMapping.setTaxName(taxMappingRequest.getTaxName());
        taxMapping.setTaxRate(taxMappingRequest.getTaxRate());
        taxMapping.setUpdatedBy(optionalPerson.orElse(null));
        taxMapping.setUpdateDate(LocalDateTime.now());
        taxMapping.setTaxType(taxMappingRequest.getTaxType());
        taxMapping.setTaxCode(taxMappingRequest.getTaxCode());

        taxMappingRepository.save(taxMapping);
    }

    @Override
    public Page<TaxMapping> getTaxMappings(String taxName, String taxRate, String taxType, Pageable pageable) {

        return taxMappingRepository.findAllByDeletedIsFalse(pageable);
    }

    @Override
    public void deleteTaxMapping(UUID taxMappingPublicId, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        Optional<TaxMapping> optionalTaxMapping = taxMappingRepository.findByPublicIdAndDeletedIsFalse(taxMappingPublicId);

        if (optionalTaxMapping.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Tax mapping not found")
                    .userMessage("Sorry, tax mapping not found");
        }

        TaxMapping taxMapping = optionalTaxMapping.get();
        taxMapping.setDeleted(true);
        taxMapping.setUpdatedBy(optionalPerson.orElse(null));
        taxMapping.setUpdateDate(LocalDateTime.now());

        taxMappingRepository.save(taxMapping);
    }

    @Override
    public void accountantUpdatePurchaseOrderPayment(UUID purchaseOrderPublicId, PurchaseOrderApprovalRequest purchaseOrderApprovalRequest, String name) {
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        Optional<PurchaseOrder> optionalPurchaseOrder = purchaseOrderRepository.findByPublicIdAndDeletedIsFalse(purchaseOrderPublicId);

        if (optionalPurchaseOrder.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Purchase order not found")
                    .userMessage("Sorry, purchase order not found");
        }

        PurchaseOrder purchaseOrder = optionalPurchaseOrder.get();

        if (!purchaseOrder.getStatus().equals(PurchaseOrderStatus.ACCOUNTANT_APPROVED)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Purchase order has to have status accountant approved")
                    .userMessage("Sorry, Purchase order has to have status accountant approved");
        }

        if (purchaseOrderApprovalRequest.getPaymentMethod() == PaymentMethod.Rebate && purchaseOrderApprovalRequest.getAmountPaid() == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("On rebate amount required")
                    .userMessage("Sorry. Rebate amount required!");
        }

        if (purchaseOrder.isRebate() && purchaseOrderApprovalRequest.getPaymentMethod() != PaymentMethod.Rebate) {
            throw new BadRequestRestApiException()
                    .developerMessage("On rebate payment method must be rebate")
                    .userMessage("Sorry. On rebate payment method must be rebate!");
        }

        String description = purchaseOrderApprovalRequest.getComment();
        optionalPerson.ifPresent(purchaseOrder::setUpdatedBy);
        purchaseOrder.setUpdatedBy(optionalPerson.orElse(null));
        purchaseOrder.setPaymentReference(purchaseOrderApprovalRequest.getPaymentReference());

        eventsRepository.save(new Events(purchaseOrder.getPublicId().toString(),
                "Purchase Order Accounts Updated",
                LocalDateTime.now(),
                description,
                "ACCOUNTANT UPDATED",
                optionalPerson.orElse(null)));

        if (!purchaseOrder.isReplacementPurchaseOrder()) {
            updateRemittanceRequest(purchaseOrder, purchaseOrderApprovalRequest, name);
        }

        purchaseOrderRepository.save(purchaseOrder);
    }

    @Override
    public void accountantApprovePurchaseOrder(UUID purchaseOrderPublicId,
                                               PurchaseOrderApprovalRequest purchaseOrderApprovalRequest,
                                               String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person not found")
                    .userMessage("Sorry, person not found");
        }
        Person person = optionalPerson.get();

        Optional<PurchaseOrder> optionalPurchaseOrder = purchaseOrderRepository.findByPublicIdAndDeletedIsFalse(purchaseOrderPublicId);

        if (optionalPurchaseOrder.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Purchase order not found")
                    .userMessage("Sorry, purchase order not found");
        }

        PurchaseOrder purchaseOrder = optionalPurchaseOrder.get();

        if (!purchaseOrder.getStatus().equals(PurchaseOrderStatus.AMIN_APPROVED)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Purchase order not in the right state")
                    .userMessage("Sorry, operation not allowed.");
        }

        if (purchaseOrderApprovalRequest.getPaymentMethod() == PaymentMethod.Rebate && purchaseOrderApprovalRequest.getAmountPaid() == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("On rebate amount required")
                    .userMessage("Sorry. Rebate amount required!");
        }

        if (purchaseOrder.isRebate() && purchaseOrderApprovalRequest.getPaymentMethod() != PaymentMethod.Rebate) {
            throw new BadRequestRestApiException()
                    .developerMessage("On rebate payment method must be rebate")
                    .userMessage("Sorry. On rebate payment method must be rebate!");
        }

        if (purchaseOrderApprovalRequest.getPaymentMethod() != PaymentMethod.Rebate && !purchaseOrder.isReplacementPurchaseOrder()) {
//            if (purchaseOrderApprovalRequest.getAmountPaid() != null && purchaseOrderApprovalRequest.getAmountPaid().compareTo(purchaseOrder.getTotalAmount()) > 0) {
//                throw new BadRequestRestApiException()
//                        .developerMessage("Amount paid cannot be greater than total amount")
//                        .userMessage("Sorry, amount paid cannot be greater than total amount");
//            }
        }

        String description = purchaseOrderApprovalRequest.getComment();
        purchaseOrder.setStatus(PurchaseOrderStatus.ACCOUNTANT_APPROVED);
        optionalPerson.ifPresent(purchaseOrder::setUpdatedBy);
        purchaseOrder.setUpdatedBy(optionalPerson.orElse(null));
        purchaseOrder.setPaymentReference(purchaseOrderApprovalRequest.getPaymentReference());

        if (!purchaseOrderApprovalRequest.getCreditNotePublicIds().isEmpty()) {
            this.attachCreditNotes(purchaseOrder, purchaseOrderApprovalRequest.getCreditNotePublicIds(), purchaseOrderApprovalRequest.getAmountPaid(), person);
        }

        eventsRepository.save(new Events(purchaseOrder.getPublicId().toString(),
                "Purchase Order Accounts Approved",
                LocalDateTime.now(),
                description,
                "ACCOUNTANT APPROVAL",
                optionalPerson.orElse(null)));

        if (!purchaseOrder.isReplacementPurchaseOrder()) {
            createRemittanceRequest(purchaseOrder, purchaseOrderApprovalRequest, name);
        }

        purchaseOrderRepository.save(purchaseOrder);
    }

    @Override
    public void processPurchaseOrderRefund(UUID purchaseOrderPublicId, RefundAction action, UUID rejectionCodePublicId, String name) {
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person not found")
                    .userMessage("Sorry, person not found");
        }
        Person person = optionalPerson.get();

        Optional<PurchaseOrder> optionalPurchaseOrder = purchaseOrderRepository.findByPublicIdAndDeletedIsFalse(purchaseOrderPublicId);

        if (optionalPurchaseOrder.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Purchase order not found")
                    .userMessage("Sorry, purchase order not found");
        }

        PurchaseOrder purchaseOrder = optionalPurchaseOrder.get();

        if (purchaseOrder.getStatus() != PurchaseOrderStatus.REFUND_REQUESTED) {
            throw new BadRequestRestApiException()
                    .developerMessage("Purchase order not in the right state")
                    .userMessage("Sorry, operation not allowed.");
        }

        if (rejectionCodePublicId == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("Purchase order rejection code not found")
                    .userMessage("Sorry, purchase order rejection code not found");
        }

        RejectionsCode rejectionsCode = rejectionCodeRepository.findRejectionsCodeByPublicIdAndDeletedIsFalse(rejectionCodePublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Purchase order rejection code not found")
                        .userMessage("Sorry, purchase order rejection code not found"));

        switch (action) {
            case APPROVE -> {

                if (rejectionsCode.getModule() != Module.ApprovePoRefund) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Approve not allowed")
                            .userMessage("Sorry, approve refund not allowed with the provided code");
                }
                purchaseOrder.setStatus(PurchaseOrderStatus.REFUND_APPROVED);
                fileService.approveRefundFile(purchaseOrder.getPublicId(), person);

                eventsRepository.save(new Events(purchaseOrder.getPublicId().toString(),
                        "Purchase Order Refund Approved",
                        LocalDateTime.now(),
                        "Refund approved for purchase order " + purchaseOrder.getPurchaseOrderNumber() + " with reason: " + rejectionsCode.getDescription(),
                        "REFUND APPROVED",
                        person));
            }
            case REJECT -> {

                if (rejectionsCode.getModule() != Module.RejectPoRefund) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Purchase order rejection code not found")
                            .userMessage("Sorry, purchase order rejection code not found");
                }
                purchaseOrder.setStatus(purchaseOrder.getPreviousStatus());
                eventsRepository.save(new Events(purchaseOrder.getPublicId().toString(),
                        "Purchase Order Refund Rejected",
                        LocalDateTime.now(),
                        "Refund rejected for purchase order " + purchaseOrder.getPurchaseOrderNumber() + " with reason: " + rejectionsCode.getDescription(),
                        "REFUND REJECTED",
                        person));
            }
        }

        purchaseOrder.setUpdatedBy(person);
        purchaseOrder.setUpdateDate(LocalDateTime.now());
        purchaseOrderRepository.save(purchaseOrder);
    }

    private void attachCreditNotes(PurchaseOrder purchaseOrder, List<UUID> creditNotePublicIds, BigDecimal amountPaid, Person person) {
        List<CreditNote> creditNotes = creditNotePublicIds.stream()
                .map(creditNotePublicId -> {
                    CreditNote creditNote = creditNoteRepository
                            .findByPublicIdAndDeletedIsFalse(creditNotePublicId)
                            .orElseThrow(() -> new BadRequestRestApiException()
                                    .developerMessage("Credit note not found")
                                    .userMessage("Sorry, credit note not found"));

                    if (!Objects.equals(creditNote.getSupplier().getPublicId(),
                            purchaseOrder.getSupplier().getPublicId())) {
                        throw new BadRequestRestApiException()
                                .developerMessage("Credit note does not belong to the same supplier as the purchase order")
                                .userMessage("Sorry, credit note does not belong to the same supplier as the purchase order");
                    }

                    if (creditNote.getStatus() != CreditNoteStatus.APPROVED) {
                        throw new BadRequestRestApiException()
                                .developerMessage("Credit note status must be APPROVED")
                                .userMessage("Sorry, credit note status must be APPROVED!");
                    }

                    return creditNote;
                })
                .toList();

        BigDecimal creditNoteAmount = creditNotes.stream()
                .map(CreditNote::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal purchaseOrderAmount = purchaseOrder.getPayableAmount();

        // get excess

        BigDecimal excessAmount = creditNoteAmount.add(amountPaid).subtract(purchaseOrderAmount);

        creditNotes.forEach(creditNote -> {
            creditNote.setTagged(true);
            creditNote.setTaggedPurchaseOrderPublicId(String.valueOf(purchaseOrder.getPublicId()));
            creditNoteRepository.save(creditNote);
        });

        if (excessAmount.compareTo(purchaseOrderAmount) > 0) {
            createCreditNote(purchaseOrder, excessAmount, person);
            purchaseOrder.setPaidAmount(purchaseOrderAmount);
        } else {
            purchaseOrder.setPaidAmount(creditNoteAmount.add(amountPaid));
        }
    }

    private void createCreditNote(PurchaseOrder purchaseOrder, BigDecimal excessAmount, Person person) {

        CreditNote creditNote = new CreditNote();
        creditNote.setAmount(excessAmount);
        creditNote.setNote("Excess amount from credit notes attached to purchase order " + purchaseOrder.getPurchaseOrderNumber());
        creditNote.setActionedBy(person.getFullName());
        creditNote.setSupplier(purchaseOrder.getSupplier());
        creditNote.setCreditNoteNumber("CN-" + counterService.getNextCounter(CounterType.CREDIT_NOTE));
        creditNote.setStatus(CreditNoteStatus.PENDING);
        creditNote.setPurchaseOrderPublicId(String.valueOf(purchaseOrder.getPublicId()));
        creditNote.setActionApplied(CreditNoteOrderAction.SAVE_PURCHASE_ORDER);
        creditNote.setCreatedBy(person);
        creditNote.setCreditNoteDate(LocalDate.now());


        CreditNote savedCreditNote = creditNoteRepository.save(creditNote);

        eventsRepository.save(new Events(savedCreditNote.getPublicId().toString(),
                "Credit Note Raised",
                LocalDateTime.now(),
                "Credit note raised with amount " + creditNote.getAmount() + " for purchase order " + purchaseOrder.getPurchaseOrderNumber(),
                "CREDIT NOTE RAISED",
                person));


        eventsRepository.save(new Events(purchaseOrder.getPublicId().toString(),
                "Excess Amount Credited",
                LocalDateTime.now(),
                "Excess amount of " + excessAmount + " credited to supplier account with credit note number " + savedCreditNote.getCreditNoteNumber(),
                "EXCESS AMOUNT CREDITED",
                person));
    }

    private void createRemittanceRequest(PurchaseOrder purchaseOrder,
                                         PurchaseOrderApprovalRequest purchaseOrderApprovalRequest,
                                         String name) {

        RemittanceRequest remittanceRequest = new RemittanceRequest();
        remittanceRequest.setSupplierPublicId(purchaseOrder.getSupplier().getPublicId());
        remittanceRequest.setPurchaseOrderPublicId(purchaseOrder.getPublicId());
        remittanceRequest.setInvoiceNumber("INV-" + counterService.getNextCounter(CounterType.REMITTANCE));
        remittanceRequest.setPaymentMethod(purchaseOrderApprovalRequest.getPaymentMethod());
        remittanceRequest.setAmountPaid(purchaseOrderApprovalRequest.getAmountPaid());
        remittanceRequest.setPaymentDate(LocalDate.now());
        remittanceRequest.setRebate(true);

        remittanceService.createRemittance(remittanceRequest, name);
    }

    private void updateRemittanceRequest(PurchaseOrder purchaseOrder,
                                         PurchaseOrderApprovalRequest purchaseOrderApprovalRequest,
                                         String name) {
        Remittances remittance =
                remittanceRepository.findByPurchaseOrderNumberAndDeletedFalse(purchaseOrder.getPurchaseOrderNumber()).orElse(null);

        RemittanceRequest remittanceRequest = new RemittanceRequest();
        remittanceRequest.setPurchaseOrderPublicId(purchaseOrder.getPublicId());
        remittanceRequest.setPaymentMethod(purchaseOrderApprovalRequest.getPaymentMethod());
        remittanceRequest.setAmountPaid(purchaseOrderApprovalRequest.getAmountPaid());
        remittanceRequest.setPaymentDate(LocalDate.now());

        remittanceService.updateRemittance(remittance.getPublicId(), remittanceRequest, name);
    }
}
