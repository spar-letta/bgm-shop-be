package com.app.itibari.services;

import com.app.itibari.dataTypes.CounterType;
import com.app.itibari.dataTypes.PaymentMethod;
import com.app.itibari.dataTypes.RemittanceStatus;
import com.app.itibari.models.entities.Events;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.PurchaseOrder;
import com.app.itibari.models.entities.Remittances;
import com.app.itibari.models.entities.Suppliers;
import com.app.itibari.models.request.RemittanceRequest;
import com.app.itibari.repository.EventsRepository;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.repository.PurchaseOrderRepository;
import com.app.itibari.repository.RemittanceRepository;
import com.app.itibari.repository.SupplierRepository;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.specification.RemittanceSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class RemittanceServiceImpl implements RemittanceService {

    private final RemittanceRepository remittanceRepository;

    private final PersonRepository personRepository;

    private final PurchaseOrderRepository purchaseOrderRepository;

    private final SupplierRepository supplierRepository;

    private final CustomersService customersService;

    private final CounterService counterService;

    private final EventsRepository eventsRepository;

    public RemittanceServiceImpl(RemittanceRepository remittanceRepository,
                                 PersonRepository personRepository,
                                 PurchaseOrderRepository purchaseOrderRepository,
                                 SupplierRepository supplierRepository,
                                 CustomersService customersService,
                                 CounterService counterService,
                                 EventsRepository eventsRepository) {

        this.remittanceRepository = remittanceRepository;
        this.personRepository = personRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.supplierRepository = supplierRepository;
        this.customersService = customersService;
        this.counterService = counterService;
        this.eventsRepository = eventsRepository;
    }

    @Override
    public Remittances createRemittance(RemittanceRequest remittanceRequest, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        Optional<PurchaseOrder> optionalPurchaseOrder = purchaseOrderRepository.findByPublicIdAndDeletedIsFalse(remittanceRequest.getPurchaseOrderPublicId());
        Optional<Suppliers> optionalSuppliers = supplierRepository.findByPublicIdAndDeletedIsFalse(remittanceRequest.getSupplierPublicId());
        Optional<Remittances> optionalRemittances = remittanceRepository.findRemittancesByInvoiceNumberIgnoreCaseAndDeletedIsFalse(
                remittanceRequest.getInvoiceNumber());

        if (optionalPurchaseOrder.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Remittance could not be created, Purchase Order not found")
                    .userMessage("Remittance could not be created, Purchase Order not found");
        }

        if (optionalSuppliers.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Remittance could not be created, Supplier not found")
                    .userMessage("Remittance could not be created, Supplier not found");
        }

        if (optionalRemittances.isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Remittance could not be created, Invoice Number already exists")
                    .userMessage("Remittance could not be created, Invoice Number already exists");
        }

        Suppliers suppliers = optionalSuppliers.get();
        PurchaseOrder purchaseOrder = optionalPurchaseOrder.get();

        // check if the supplier is the same as the purchase order supplier

        if (!suppliers.equals(purchaseOrder.getSupplier())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Remittance could not be created, Supplier does not match Purchase Order Supplier")
                    .userMessage("Remittance could not be created, Supplier does not match Purchase Order Supplier");
        }

        if (remittanceRequest.getAmountPaid().compareTo(purchaseOrder.getBalance()) > 0) {
            throw new BadRequestRestApiException()
                    .developerMessage("Remittance could not be created, Amount Paid is greater than Amount Due")
                    .userMessage("Remittance could not be created, Amount Paid is greater than Amount Due");
        }

        Remittances remittances = new Remittances();
        remittances.setSupplier(suppliers);
        remittances.setPurchaseOrderNumber(purchaseOrder.getPurchaseOrderNumber());
        remittances.setInvoiceNumber(remittanceRequest.getInvoiceNumber());
        remittances.setAmountDue(purchaseOrder.getBalance());
        remittances.setAmountPaid(remittanceRequest.getAmountPaid());
        remittances.setPaidDate(remittanceRequest.getPaymentDate());
        remittances.setPaymentMethod(remittanceRequest.getPaymentMethod());
        remittances.setCreationTime(LocalDateTime.now());
        optionalPerson.ifPresent(remittances::setCreatedBy);
        remittances.setRebate(remittances.isRebate());
        remittances.setRemittanceNumber("REM-" + counterService.getNextCounter(CounterType.REMITTANCE));

        if (!(remittanceRequest.getPaymentMethod() == PaymentMethod.Credit || remittanceRequest.getPaymentMethod() == PaymentMethod.Rebate)) {
            if (remittanceRequest.getAmountPaid().compareTo(purchaseOrder.getBalance()) == 0) {
                remittances.setStatus(RemittanceStatus.Paid);
            } else if (remittanceRequest.getAmountPaid().compareTo(purchaseOrder.getBalance()) < 0) {
                remittances.setStatus(RemittanceStatus.PartiallyPaid);
            }
        } else if (remittanceRequest.getPaymentMethod() == PaymentMethod.Rebate) {
            remittances.setStatus(RemittanceStatus.Rebate);
        } else {
            remittances.setStatus(RemittanceStatus.OnCredit);
        }

        Remittances savedRemittances = remittanceRepository.save(remittances);

        purchaseOrder.setPaidAmount(purchaseOrder.getPaidAmount().add(remittanceRequest.getAmountPaid()));

        String description = "Remittance created for Purchase Order " + purchaseOrder.getPurchaseOrderNumber();

        eventsRepository.save(new Events(purchaseOrder.getPublicId().toString(),
                                         "Purchase Order Paid",
                                         LocalDateTime.now(),
                                         description,
                                         "PAID",
                                         optionalPerson.orElse(null)));

        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);

        savedRemittances.setAmountDue(saved.getBalance());

        return remittanceRepository.save(savedRemittances);
    }

    @Override
    public void updateRemittance(UUID remittancePublicId, RemittanceRequest remittanceRequest, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        Optional<Remittances> optionalRemittances = remittanceRepository.findByPublicIdAndDeletedIsFalse(remittancePublicId);

        if (optionalRemittances.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Remittance could not be updated, Remittance not found")
                    .userMessage("Remittance could not be updated, Remittance not found");
        }

        Remittances remittances = optionalRemittances.get();

        if (remittances.isRebate()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Remittance could not be updated, Remittance is a rebate")
                    .userMessage("Remittance could not be updated, Remittance is a rebate");
        }

        BigDecimal initialAmountPaid = remittances.getAmountPaid();
        BigDecimal balance = initialAmountPaid.add(remittanceRequest.getAmountPaid());

        if (remittanceRequest.getAmountPaid().compareTo(balance) > 0) {
            throw new BadRequestRestApiException()
                    .developerMessage("Remittance could not be updated, Amount Paid is greater than Amount Due")
                    .userMessage("Remittance could not be updated, Amount Paid is greater than Amount Due");
        }

        remittances.setAmountPaid(remittanceRequest.getAmountPaid());
        remittances.setPaidDate(remittanceRequest.getPaymentDate());
        remittances.setPaymentMethod(remittanceRequest.getPaymentMethod());

        if (remittanceRequest.getAmountPaid().compareTo(remittances.getAmountDue()) == 0) {
            remittances.setStatus(RemittanceStatus.Paid);
        } else if (remittanceRequest.getAmountPaid().compareTo(remittances.getAmountDue()) < 0) {
            remittances.setStatus(RemittanceStatus.PartiallyPaid);
        }

        Remittances savedRemittances = remittanceRepository.save(remittances);

        PurchaseOrder purchaseOrder = purchaseOrderRepository
                .findByPurchaseOrderNumberAndDeletedIsFalse(remittances.getPurchaseOrderNumber())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Remittance could not be updated, Purchase Order not found")
                        .userMessage("Remittance could not be updated, Purchase Order not found"));

        purchaseOrder.setPaidAmount(purchaseOrder.getPaidAmount().add(remittanceRequest.getAmountPaid().subtract(initialAmountPaid)));

        String description = "Remittance updated for Purchase Order " + purchaseOrder.getPurchaseOrderNumber();

        eventsRepository.save(new Events(purchaseOrder.getPublicId().toString(),
                                         "Purchase Order Paid",
                                         LocalDateTime.now(),
                                         description,
                                         "PAID",
                                         optionalPerson.orElse(null)));

        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);

        savedRemittances.setAmountDue(saved.getBalance());

        remittanceRepository.save(savedRemittances);
    }

    @Override
    public Remittances getRemittanceByPublicId(UUID remittancePublicId) {

        return remittanceRepository.findByPublicIdAndDeletedIsFalse(remittancePublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Remittance not found")
                        .userMessage("Remittance not found"));
    }

    @Override
    public Page<Remittances> getRemittances(String invoiceNumber,
                                            String purchaseOrderNumber,
                                            UUID supplierId,
                                            RemittanceStatus status,
                                            String paymentMethod,
                                            String to,
                                            String from,
                                            Pageable pageable) {

        return remittanceRepository.findAll(RemittanceSpecification.isNotDeleted()
                                                    .and(RemittanceSpecification.hasInvoiceNumber(invoiceNumber))
                                                    .and(RemittanceSpecification.hasOrderNumber(purchaseOrderNumber))
                                                    .and(RemittanceSpecification.hasSupplier(supplierId))
                                                    .and(RemittanceSpecification.hasStatus(status))
                                                    .and(RemittanceSpecification.hasPaymentMethod(paymentMethod))
                                                    .and(RemittanceSpecification.hasFromToDates(from, to)), pageable);
    }

    @Override
    public void deleteRemittance(UUID remittancePublicId, String name) {

        Optional<Remittances> optionalRemittances = remittanceRepository.findByPublicIdAndDeletedIsFalse(remittancePublicId);
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalRemittances.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Remittance not found")
                    .userMessage("Remittance not found");
        }

        Remittances remittances = optionalRemittances.get();

        if (remittances.isRebate()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Remittance could not be deleted, Remittance is a rebate")
                    .userMessage("Remittance could not be deleted, Remittance is a rebate");
        }

        remittances.setDeleted(true);
        optionalPerson.ifPresent(remittances::setUpdatedBy);
        remittances.setUpdateDate(LocalDateTime.now());

        PurchaseOrder purchaseOrder = purchaseOrderRepository
                .findByPurchaseOrderNumberAndDeletedIsFalse(remittances.getPurchaseOrderNumber())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Remittance could not be deleted, Purchase Order not found")
                        .userMessage("Remittance could not be deleted, Purchase Order not found"));

        purchaseOrder.setPaidAmount(purchaseOrder.getPaidAmount().subtract(remittances.getAmountPaid()));

        String description = "Remittance deleted for Purchase Order " + purchaseOrder.getPurchaseOrderNumber();

        eventsRepository.save(new Events(purchaseOrder.getPublicId().toString(),
                                         "Purchase Order Remittance Reversed",
                                         LocalDateTime.now(),
                                         description,
                                         "Reversed",
                                         optionalPerson.orElse(null)));

        purchaseOrderRepository.save(purchaseOrder);

        remittanceRepository.save(remittances);
    }
}
