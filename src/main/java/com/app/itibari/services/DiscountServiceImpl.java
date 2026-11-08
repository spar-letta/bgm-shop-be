package com.app.itibari.services;

import com.app.itibari.dataTypes.AmountType;
import com.app.itibari.dataTypes.DiscountApprovalStatus;
import com.app.itibari.dataTypes.DiscountStatus;
import com.app.itibari.dataTypes.DiscountType;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.ApprovalRequest;
import com.app.itibari.models.request.DiscountCreateRequest;
import com.app.itibari.models.request.VolumeRangeCreateRequest;
import com.app.itibari.repository.*;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.helpers.Validators;
import com.app.itibari.utils.specification.CustomerSpecification;
import com.app.itibari.utils.specification.DiscountApprovalSpecification;
import com.app.itibari.utils.specification.DiscountSpecification;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * created by christopher oigo(oigoc60@gmail.com) on August 2025
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscountServiceImpl implements DiscountService {

    private final DiscountRepository discountRepository;

    private final PersonRepository personRepository;

    private final CustomersRepository customersRepository;

    private final ProductRepository productRepository;

    private final EventsRepository eventsRepository;

    private final DiscountApprovalRepository discountApprovalRepository;

    private final SalesAgentsRepository salesAgentsRepository;

    private final ItibariSmsService itibariSmsService;

    private final RequestItemRepository requestItemRepository;

    private final ObjectMapper objectMapper;

    private final VolumeRangeRepository volumeRangeRepository;
    private final SpecialItemRepository specialItemRepository;

    @Override
    @Transactional
    public void createUpdateDiscount(DiscountCreateRequest discountCreateRequest, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person with name " + name + " not found")
                    .userMessage("Sorry, we could not find your details. Please contact support");
        }

        Person person = optionalPerson.get();

        Discounts discounts = new Discounts();
        discounts.setDiscountType(discountCreateRequest.getDiscountType());
        discounts.setCreatedBy(person);
        discounts.setCreationTime(LocalDateTime.now());
        discounts.setStartDate(discountCreateRequest.getStartDate());
        discounts.setEndDate(discountCreateRequest.getEndDate());
        discounts.setSuspended(true);
        discounts.setApproved(false);

        createDiscount(discounts, discountCreateRequest, false);
    }

    @Override
    public void update(UUID publicId, DiscountCreateRequest discountCreateRequest, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person with name " + name + " not found")
                    .userMessage("Sorry, we could not find your details. Please contact support");
        }

        Person person = optionalPerson.get();

        Optional<Discounts> optionalDiscount = discountRepository.findByPublicIdAndDeletedIsFalse(publicId);

        if (optionalDiscount.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Discount with ID " + publicId + " not found")
                    .userMessage("The discount you are trying to update does not exist. Please check the ID and try again.");
        }

        Discounts discounts = optionalDiscount.get();
        discounts.setDiscountType(discountCreateRequest.getDiscountType());
        discounts.setUpdatedBy(person);
        discounts.setUpdateDate(LocalDateTime.now());

        createDiscount(discounts, discountCreateRequest, true);
    }

    private void createDiscount(Discounts discounts, DiscountCreateRequest discountCreateRequest, boolean update) {

        if (discountCreateRequest.getDiscountType() == DiscountType.SpecialDiscount) {
            if (Validators.anyEqualNull(discountCreateRequest.getCustomerIds().isEmpty(),
                                        discountCreateRequest.getAmount(),
                                        discountCreateRequest.getAmountType())) {
                throw new BadRequestRestApiException()
                        .developerMessage("For SpecialDiscount, customerIds, amount, and amountType must be provided")
                        .userMessage("Please provide customer IDs, amount, and amount type for Special Discount.");
            }

            if (discountCreateRequest.getCustomerIds().size() != 1) {
                throw new BadRequestRestApiException()
                        .developerMessage("For SpecialDiscount, customerIds must be provided with one customer")
                        .userMessage("Sorry!. Only one customer is required for special discount");
            }

            UUID customerPublicId = discountCreateRequest.getCustomerIds().get(0);

            Optional<Customers> optionalCustomer = customersRepository.findByPublicIdAndDeletedIsFalse(customerPublicId);

            if (optionalCustomer.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Customer with ID " + customerPublicId + " not found")
                        .userMessage("One of the specified customers was not found. Please check the customer IDs.");
            }
            Customers customer = optionalCustomer.get();

            List<Discounts> specialDiscounts = discountRepository.findByCustomerPublicIdAndDiscountTypeAndStatusAndDeletedIsFalse(
                    customerPublicId,
                    DiscountType.SpecialDiscount,
                    DiscountStatus.Active);

            if(!specialDiscounts.isEmpty()) {
                specialDiscounts.forEach(specialDiscount -> {
                    if(specialDiscount.getDiscountType().equals(DiscountType.SpecialDiscount) && specialDiscount.getOrderPublicId()== null){
                        throw new BadRequestRestApiException()
                                .developerMessage("Customer with ID " + customerPublicId + " already has active special discount ")
                                .userMessage("The customer has approved discount yet to be utilized!");
                    }
                });
            }

            discounts.setDiscountName(customer.getName() + " Special Discount");
            discounts.setCustomerPublicId(customerPublicId);

            if (!discountCreateRequest.getProductIds().isEmpty()) {
                List<Product> specialProducts = discountCreateRequest.getProductIds().stream()
                        .map(productId -> {
                            Optional<Product> optionalProduct = productRepository.findByPublicIdAndDeletedIsFalse(productId);
                            if (optionalProduct.isEmpty()) {
                                throw new BadRequestRestApiException()
                                        .developerMessage("Product with ID " + productId + " not found")
                                        .userMessage("One of the specified products was not found. Please check the product IDs.");
                            }
                            return optionalProduct.get();
                        })
                        .toList();
                discounts.setProductsList(specialProducts);

                List<SpecialItem> specialItems = new ArrayList<>();
                specialProducts.forEach(p->{
                    SpecialItem specialItem = new SpecialItem();
                    specialItem.setProductId(p.getPublicId());
                    specialItem.setProductName(p.getProductName());
                    specialItem.setCreationTime(LocalDateTime.now());
                    specialItem.setQuantityRequested(0);
                    specialItem.setAmountApprovedPerQuantity(discountCreateRequest.getAmount());

                    SpecialItem savedSpecialItem = specialItemRepository.save(specialItem);
                    specialItems.add(savedSpecialItem);
                });
                discounts.setSpecialItems(specialItems);
            }
            discounts.addCustomer(Collections.singletonList(customer));
            discounts.setAmount(discountCreateRequest.getAmount());
            discounts.setAmountType(discountCreateRequest.getAmountType());
            discounts.setStatus(DiscountStatus.Pending);
        } else if (discountCreateRequest.getDiscountType() == DiscountType.VolumeDiscount) {

            if (discountCreateRequest.getProductIds().isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("For VolumeDiscount, productIds must be provided")
                        .userMessage("Please provide product IDs for Volume Discount.");
            }

            if (discountCreateRequest.getProductIds().size() != 1) {
                throw new BadRequestRestApiException()
                        .developerMessage("For VolumeDiscount, productIds must be provided with one product")
                        .userMessage("Sorry!. Only one product is required for volume discount");
            }

            UUID productPublicId = discountCreateRequest.getProductIds().get(0);

            Optional<Discounts> optionalVolumeProductDiscount = discountRepository.findDiscountByProductPublicIdAndDiscountTypeAndStatusAndDeletedIsFalse(
                    productPublicId,
                    DiscountType.VolumeDiscount,
                    DiscountStatus.Active);

            if (optionalVolumeProductDiscount.isPresent() && !update) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product already has active discount ")
                        .userMessage("Sorry!. Product has active discount ongoing");
            }

            Optional<Product> optionalProduct = productRepository.findByPublicIdAndDeletedIsFalse(productPublicId);
            if (optionalProduct.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product with ID " + productPublicId + " not found")
                        .userMessage("One of the specified products was not found. Please check the product IDs.");
            }

            Product product = optionalProduct.get();

            discounts.addProduct(Collections.singletonList(product));
            discounts.setDiscountName(product.getProductName() + " Volume Discount");
            discounts.setProductPublicId(productPublicId);
            discounts.setAmount(discountCreateRequest.getAmount());
            discounts.setAmountType(discountCreateRequest.getAmountType());
            discounts.setStatus(DiscountStatus.Incomplete);
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("Discount type not provided")
                    .userMessage("Sorry!. Discount type ought to be provided");
        }

        Discounts savedDiscount = discountRepository.save(discounts);

        String description = savedDiscount.getDiscountType() != null ? savedDiscount.getDiscountType().name() : "N/A" + "discount";
        String eventType = update ? "UPDATE" : "CREATE";
        String action = update ? "Discount Update" : "Discount Create";
        Person eventPerson = update ? savedDiscount.getUpdatedBy() : savedDiscount.getCreatedBy();
        eventsRepository.save(new Events(savedDiscount.getPublicId().toString(),
                                         eventType,
                                         LocalDateTime.now(),
                                         description,
                                         action,
                                         eventPerson));
    }

    @Override
    public void suspend(UUID publicId, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person with name " + name + " not found")
                    .userMessage("Sorry, we could not find your details. Please contact support");
        }

        Person person = optionalPerson.get();

        Optional<Discounts> optionalDiscount = discountRepository.findByPublicIdAndDeletedIsFalse(publicId);

        if (optionalDiscount.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Discount with ID " + publicId + " not found")
                    .userMessage("The discount you are trying to suspend does not exist. Please check the ID and try again.");
        }

        Discounts discounts = optionalDiscount.get();
        discounts.setSuspended(true);
        discounts.setApproved(false);
        discounts.setStatus(DiscountStatus.Cancelled);
        discounts.setUpdatedBy(person);
        discounts.setUpdateDate(LocalDateTime.now());

        discountRepository.save(discounts);


        if(discounts.getDiscountType() == DiscountType.VolumeDiscount){
            Optional<Product> optionalProduct = productRepository.findByPublicIdAndDeletedIsFalse(discounts.getProductPublicId());
            if(optionalProduct.isPresent()){
                Product product = optionalProduct.get();
                product.setDiscountMoq(null);
                productRepository.save(product);
            }
        }

        String description = "Suspended discount: " + discounts.getDiscountName() + " of type " + discounts
                .getDiscountType()
                .name() + " with ID: " + discounts.getPublicId();
        String eventType = "SUSPEND";
        String action = "Discount Suspend";
        eventsRepository.save(new Events(discounts.getPublicId().toString(), eventType, LocalDateTime.now(), description, action, person));
    }

    @Override
    public Page<Discounts> getAllDiscounts(DiscountType discountType, String searchParam, LocalDate startDate, LocalDate endDate, DiscountStatus status, Pageable pageable) {

        LocalDateTime startDateLocal = Optional.ofNullable(startDate)
                .map(LocalDate::atStartOfDay)
                .orElse(null);

        LocalDateTime endDateLocal = Optional.ofNullable(endDate)
                .map(d -> d.atTime(LocalTime.MAX))
                .orElse(null);


        Specification<Discounts> spec = Specification
                .where(DiscountSpecification.searchParamMatches(searchParam))
                .and(DiscountSpecification.hasDiscountType(discountType))
                .and(DiscountSpecification.hasStatus(status))
                .and(DiscountSpecification.isNotDeleted())
                .and(DiscountSpecification.hasDateFromTo(startDateLocal, endDateLocal));


        return discountRepository.findAll(spec, pageable);
    }

    @Override
    public void delete(UUID publicId, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person with name " + name + " not found")
                    .userMessage("Sorry, we could not find your details. Please contact support");
        }

        Person person = optionalPerson.get();

        Optional<Discounts> optionalDiscount = discountRepository.findByPublicIdAndDeletedIsFalse(publicId);

        if (optionalDiscount.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Discount with ID " + publicId + " not found")
                    .userMessage("The discount you are trying to delete does not exist. Please check the ID and try again.");
        }

        Discounts discounts = optionalDiscount.get();
        discounts.setDeleted(true);
        discounts.setUpdatedBy(person);
        discounts.setUpdateDate(LocalDateTime.now());

        discountRepository.save(discounts);

        String description = "Deleted discount: " + discounts.getDiscountName() + " of type " + discounts
                .getDiscountType()
                .name() + " with ID: " + discounts.getPublicId();
        String eventType = "DELETE";
        String action = "Discount Delete";
        eventsRepository.save(new Events(discounts.getPublicId().toString(), eventType, LocalDateTime.now(), description, action, person));
    }

    @Override
    public void firstApproval(UUID publicId, String comment, boolean approve, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person with name " + name + " not found")
                    .userMessage("Sorry, we could not find your details. Please contact support");
        }

        Person person = optionalPerson.get();

        Optional<DiscountApproval> optionalApproval = discountApprovalRepository.findDiscountApprovalByPublicIdAndDeletedIsFalse(publicId);
        if (optionalApproval.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Discount approval with ID " + publicId + " not found")
                    .userMessage("The discount approval you are trying to process does not exist. Please check the ID and try again.");
        }
        DiscountApproval discountApproval = optionalApproval.get();
        if (discountApproval.getFirstApproved() != null && discountApproval.getFirstApproved()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Discount approval with ID " + publicId + " has already been first approved")
                    .userMessage("This discount approval has already been first approved.");
        }

        if (discountApproval.getApprovalStatus() == DiscountApprovalStatus.Rejected) {
            throw new BadRequestRestApiException()
                    .developerMessage("Discount approval with ID " + publicId + " has been rejected and cannot be approved")
                    .userMessage("This discount approval has been rejected and cannot be approved.");
        }

        List<Discounts> specialDiscounts = discountRepository.findByCustomerPublicIdAndDiscountTypeAndStatusAndDeletedIsFalse(
                UUID.fromString(discountApproval.getCustomerPublicId()),
                DiscountType.SpecialDiscount,
                DiscountStatus.Active);

        if(!specialDiscounts.isEmpty()) {
            specialDiscounts.forEach(specialDiscount -> {
                if(specialDiscount.getDiscountType().equals(DiscountType.SpecialDiscount) && specialDiscount.getOrderPublicId()== null){
                    throw new BadRequestRestApiException()
                            .developerMessage("Customer with ID " + discountApproval.getCustomerPublicId() + " already has an active special discount")
                            .userMessage("The customer has approved discount yet to be utilezed!");
                }
            });
        }

        // check if customer has active special discount for product requested.

        discountApproval.getRequestItems().forEach(requestItem->{
            Optional<Product> optionalProduct = productRepository.findByPublicIdAndDeletedIsFalse(requestItem.getProductId());
            if (optionalProduct.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product with ID " + requestItem.getProductId() + " not found")
                        .userMessage("One of the products in the discount approval request was not found. Please check the product IDs.");
            }
        });

        if (approve) {
            discountApproval.setFirstApproved(true);
            discountApproval.setApprovalStatus(DiscountApprovalStatus.FirstLevelApproved);
        } else {
            discountApproval.setFirstApproved(false);
            discountApproval.setApprovalStatus(DiscountApprovalStatus.Rejected);
        }
        discountApproval.setUpdatedBy(person);
        discountApproval.setUpdateDate(LocalDateTime.now());
        discountApprovalRepository.save(discountApproval);

        String description = "First level " + (approve ? "approved" : "rejected") + " discount approval by: " + person.getFullName() + " at " + LocalDateTime.now() + (comment != null ? ". Comment: " + comment : "");
        String eventType = approve ? "FIRST_LEVEL_APPROVAL" : "REJECTION";
        String action = approve ? "Discount Approval First Level" : "Discount Approval Rejection";
        eventsRepository.save(new Events(discountApproval.getPublicId().toString(),
                                         eventType,
                                         LocalDateTime.now(),
                                         description,
                                         action,
                                         person));
    }

    @Override
    public void finalApproval(UUID publicId, ApprovalRequest request, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person with name " + name + " not found")
                    .userMessage("Sorry, we could not find your details. Please contact support");
        }

        Person person = optionalPerson.get();

        Optional<DiscountApproval> optionalApproval = discountApprovalRepository.findDiscountApprovalByPublicIdAndDeletedIsFalse(publicId);
        if (optionalApproval.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Discount approval with ID " + publicId + " not found")
                    .userMessage("The discount approval you are trying to process does not exist. Please check the ID and try again.");
        }

        DiscountApproval discountApproval = optionalApproval.get();
        if (discountApproval.getFirstApproved() == null || !discountApproval.getFirstApproved()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Discount approval with ID " + publicId + " has not been first approved yet")
                    .userMessage("This discount approval has not been first approved yet.");
        }
        if (discountApproval.getApprovalStatus() == DiscountApprovalStatus.Approved) {
            throw new BadRequestRestApiException()
                    .developerMessage("Discount approval with ID " + publicId + " has already been final approved")
                    .userMessage("This discount approval has already been final approved.");
        }

        if (discountApproval.getApprovalStatus() == DiscountApprovalStatus.Rejected) {
            throw new BadRequestRestApiException()
                    .developerMessage("Discount approval with ID " + publicId + " has been rejected and cannot be approved")
                    .userMessage("This discount approval has been rejected and cannot be approved.");
        }

        if (request.getAction().equals(ApprovalRequest.ApprovalAction.APPROVE)) {
            discountApproval.setApprovalStatus(DiscountApprovalStatus.Approved);
        } else if (request.getAction().equals(ApprovalRequest.ApprovalAction.REJECT)) {
            discountApproval.setApprovalStatus(DiscountApprovalStatus.Rejected);
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid approval action")
                    .userMessage("The approval action provided is invalid. Please check and try again.");
        }

        // update approved items


        discountApproval.getRequestItems().forEach(item -> {
            if (request.getAction().equals(ApprovalRequest.ApprovalAction.APPROVE)) {
                request.getProductItems().forEach(productItem -> {
                    if (productItem.isApproved() && productItem.getProductId().equalsIgnoreCase(item.getProductId().toString())) {
                        item.setApproved(true);
                        item.setDiscountApproved(productItem.getApprovedAmount());
                        requestItemRepository.save(item);
                    }
                });
            }
        });

        discountApproval.setUpdatedBy(person);
        discountApproval.setUpdateDate(LocalDateTime.now());

        DiscountApproval saved = discountApprovalRepository.save(discountApproval);


        String description = "Final level " + (request
                .getAction()
                .equals(ApprovalRequest.ApprovalAction.APPROVE) ? "approved" : "rejected") + " discount approval by: " + person.getFullName() + " at " + LocalDateTime.now();
        String eventType = request.getAction().equals(ApprovalRequest.ApprovalAction.APPROVE) ? "FINAL_APPROVAL" : "REJECTION";
        String action = request
                .getAction()
                .equals(ApprovalRequest.ApprovalAction.APPROVE) ? "Discount Approval Final Level" : "Discount Approval Rejection";
        eventsRepository.save(new Events(discountApproval.getPublicId().toString(),
                                         eventType,
                                         LocalDateTime.now(),
                                         description,
                                         action,
                                         person));

        if (request.getAction().equals(ApprovalRequest.ApprovalAction.APPROVE)) {

            this.createSpecialDiscount(saved, name);
        }
        // send verification OTP to a sales agent
        if (request.getAction().equals(ApprovalRequest.ApprovalAction.APPROVE)) {

            if(discountApproval.getHasVanSales()== null || !discountApproval.getHasVanSales()) {
                sendVerificationOTPToSalesAgent(discountApproval.getRequestedByPublicId(),
                        discountApproval.getCustomer(),
                        discountApproval.getPublicId());
            }

            Customers customer = customersRepository
                    .findByPublicIdAndDeletedIsFalse(UUID.fromString(discountApproval.getCustomerPublicId()))
                    .orElse(null);
            if (customer != null) {
                customer.setSpecialDiscountApproved(true);
                customersRepository.save(customer);
            }

            if(discountApproval.getHasVanSales()){
                this.createVanSalesOrder(discountApproval);
            }
        }
    }


    private void createSpecialDiscount(DiscountApproval approval, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person with name " + name + " not found")
                    .userMessage("Sorry, we could not find your details. Please contact support");
        }
        Person person = optionalPerson.get();

        Customers customer = customersRepository
                .findByPublicIdAndDeletedIsFalse(UUID.fromString(approval.getCustomerPublicId()))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Customer with ID " + approval.getCustomerPublicId() + " not found")
                        .userMessage("The specified customer was not found. Please check the customer ID."));

        Discounts discounts = new Discounts();
        discounts.setDiscountType(DiscountType.SpecialDiscount);
        discounts.setCreatedBy(person);
        discounts.setCreationTime(LocalDateTime.now());
        discounts.setStartDate(LocalDate.now());
        discounts.setEndDate(LocalDate.now().plusDays(180));
        discounts.setSuspended(false);
        discounts.setApproved(true);
        discounts.setStatus(DiscountStatus.Active);
        discounts.setCustomerPublicId(UUID.fromString(approval.getCustomerPublicId()));


        List<Product> approvedProducts = approval.getRequestItems().stream()
                .filter(RequestItems::isApproved)
                .map(item -> {
                    Optional<Product> optionalProduct = productRepository.findByPublicIdAndDeletedIsFalse(item.getProductId());
                    if (optionalProduct.isEmpty()) {
                        throw new BadRequestRestApiException()
                                .developerMessage("Product with ID " + item.getProductId() + " not found")
                                .userMessage("One of the approved products was not found. Please check the product IDs.");
                    }
                    return optionalProduct.get();
                })
                .toList();

        List<SpecialItem> specialItems = new ArrayList<>();

        approval.getRequestItems().forEach(approvedItem->{
            if(approvedItem.isApproved()){
                SpecialItem specialItem = new SpecialItem();
                specialItem.setProductId(approvedItem.getProductId());
                specialItem.setProductName(approvedItem.getProductName());
                specialItem.setAmountApprovedPerQuantity(approvedItem.getDiscountApproved());
                specialItem.setQuantityRequested(approvedItem.getQuantity());
                specialItem.setCreationTime(LocalDateTime.now());
                specialItem.setCreatedBy(person);

                SpecialItem savedSpecialItem = specialItemRepository.save(specialItem);
                specialItems.add(savedSpecialItem);
            }
        });

        discounts.setSpecialItems(specialItems);
        discounts.setProductsList(approvedProducts);
        discounts.setCustomersList(Collections.singletonList(customer));
        discounts.setAmount(approval.getTotalDiscountApproved());
        discounts.setAmountType(AmountType.Fixed);
        discounts.setDiscountName(customer.getName() + " Special Discount");
        Discounts savedDiscount = discountRepository.save(discounts);

        String description = "Created discount: " + savedDiscount.getDiscountName() + " of type " + savedDiscount.getDiscountType().name();
        String eventType = "CREATE";
        String action = "Discount Create";
        Person eventPerson = savedDiscount.getCreatedBy();
        eventsRepository.save(new Events(savedDiscount.getPublicId().toString(),
                                         eventType,
                                         LocalDateTime.now(),
                                         description,
                                         action,
                                         eventPerson));
    }

    private void sendVerificationOTPToSalesAgent(String requestedByPublicId, String customer, UUID publicId) {

        UUID salesAgentId = UUID.fromString(requestedByPublicId);
        Optional<SalesAgents> optionalSalesAgent = salesAgentsRepository.findByPublicIdAndDeletedIsFalse(salesAgentId);
        if (optionalSalesAgent.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Sales agent with ID " + requestedByPublicId + " not found")
                    .userMessage("The sales agent associated with this request was not found. Please contact support.");
        }
        SalesAgents salesAgent = optionalSalesAgent.get();

        DiscountApproval discountApproval = discountApprovalRepository
                .findDiscountApprovalByPublicIdAndDeletedIsFalse(publicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Discount approval with ID " + publicId + " not found")
                        .userMessage("The discount approval you are trying to process does not exist. Please check the ID and try again."));

        // otp 6 digit
        String otp = RandomStringUtils.randomNumeric(6);
        // save otp to discount approval
        discountApproval.setOtp(otp);
        discountApproval.setExpiryTime(LocalDateTime.now().plusHours(12));

        itibariSmsService.send(salesAgent.getPhoneNumber(),"Discount request for customer " + customer + " has been approved. Your OTP is " + otp + ". It expires in 20 minutes.");
        discountApprovalRepository.save(discountApproval);
    }

    @Override
    public Page<DiscountApproval> getAllDiscountApprovals(String searchParam,
                                                          UUID salesAgentPublicId,
                                                          UUID customerPublicId,
                                                          DiscountApprovalStatus status,
                                                          LocalDate startDate,
                                                          LocalDate endDate,
                                                          Pageable pageable) {

        LocalDateTime startDateLocal = Optional.ofNullable(startDate)
                .map(LocalDate::atStartOfDay)
                .orElse(null);

        LocalDateTime endDateLocal = Optional.ofNullable(endDate)
                .map(d -> d.atTime(LocalTime.MAX))
                .orElse(null);

        Specification<DiscountApproval> spec = Specification
                .where(DiscountApprovalSpecification.isNotDeleted())
                .and(DiscountApprovalSpecification.hasSalesAgent(salesAgentPublicId))
                .and(DiscountApprovalSpecification.hasCustomer(customerPublicId))
                .and(DiscountApprovalSpecification.hasStatus(status))
                .and(DiscountApprovalSpecification.hasDateFromTo(startDateLocal, endDateLocal))
                .and(DiscountApprovalSpecification.searchParamMatches(searchParam));

        return discountApprovalRepository.findAll(spec, pageable);
    }

    @Override
    public void approve(UUID publicId, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person with name " + name + " not found")
                    .userMessage("Sorry, we could not find your details. Please contact support");
        }

        Person person = optionalPerson.get();

        Optional<Discounts> optionalDiscount = discountRepository.findByPublicIdAndDeletedIsFalse(publicId);

        if (optionalDiscount.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Discount with ID " + publicId + " not found")
                    .userMessage("The discount you are trying to suspend does not exist. Please check the ID and try again.");
        }

        Discounts discounts = optionalDiscount.get();
        if (discounts.getStatus() == DiscountStatus.Cancelled) {
            throw new BadRequestRestApiException()
                    .developerMessage("Cannot approve a cancelled discount with ID " + publicId)
                    .userMessage("The discount you are trying to approve has been cancelled and cannot be approved.");
        }

        if (discounts.getStatus() == DiscountStatus.Incomplete) {
            throw new BadRequestRestApiException()
                    .developerMessage("Cannot approve an incomplete discount with ID " + publicId)
                    .userMessage(
                            "The discount you are trying to approve has not been completed and cannot be approved.Add Volume Ranges to complete the discount.");
        }

        discounts.setSuspended(false);
        discounts.setApproved(true);
        discounts.setUpdatedBy(person);
        discounts.setUpdateDate(LocalDateTime.now());
        discounts.setStatus(DiscountStatus.Active);

        discountRepository.save(discounts);

        String description = "Approved discount: " + discounts.getDiscountName() + " of type " + discounts
                .getDiscountType()
                .name() + " with ID: " + discounts.getPublicId();
        String eventType = "APPROVE";
        String action = "Discount Approved";
        eventsRepository.save(new Events(discounts.getPublicId().toString(), eventType, LocalDateTime.now(), description, action, person));

        if (discounts.getDiscountType() == DiscountType.SpecialDiscount) {
            // hold on
            //sendVerificationCodeToCustomer(discounts);
        }

        if (discounts.getDiscountType() == DiscountType.VolumeDiscount) {
            Optional<Product> optionalProduct = productRepository.findByPublicIdAndDeletedIsFalse(discounts.getProductPublicId());
            if (optionalProduct.isPresent()) {
                Product product = optionalProduct.get();
                product.setDiscountMoq(discounts.getLeastVolumeRangeMoq());
                productRepository.save(product);
            }
        }
    }

    private void sendVerificationCodeToCustomer(Discounts discounts) {

        discounts.getCustomersList().forEach(customer -> {

            String code = RandomStringUtils.randomAlphanumeric(6);
            String message = String.format(
                    "Dear %s. You have received a discount of %s for each product you purchase\n.Your confirmation code is %s",
                    customer.getName(),
                    discounts.getAmount(),
                    code);

            itibariSmsService.send(customer.getPhoneNumber(), message);

            customer.setSpecialDiscountApproved(true);

            customersRepository.save(customer);
        });
    }

    @Override
    public void addVolumeRange(UUID discountPublicId, VolumeRangeCreateRequest volumeRangeCreateRequest, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person with name " + name + " not found")
                    .userMessage("Sorry, we could not find your details. Please contact support");
        }

        Person person = optionalPerson.get();

        Optional<Discounts> optionalDiscount = discountRepository.findByPublicIdAndDeletedIsFalse(discountPublicId);

        if (optionalDiscount.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Discount with ID " + discountPublicId + " not found")
                    .userMessage("The discount you are trying to add volume range to does not exist. Please check the ID and try again.");
        }

        Discounts discount = optionalDiscount.get();

        if (discount.getDiscountType() != DiscountType.VolumeDiscount) {
            throw new BadRequestRestApiException()
                    .developerMessage("Cannot add volume range to a non-volume discount with ID " + discountPublicId)
                    .userMessage("The discount you are trying to add volume range to is not a volume discount and cannot be added to.");
        }

        if (discount.getStatus() == DiscountStatus.Cancelled) {
            throw new BadRequestRestApiException()
                    .developerMessage("Cannot add volume range to a cancelled discount with ID " + discountPublicId)
                    .userMessage("The discount you are trying to add volume range to has been cancelled and cannot be approved.");
        }

        VolumeRange volumeRange = new VolumeRange();
        volumeRange.setMinVolume(volumeRangeCreateRequest.getMinVolume());
        volumeRange.setMaxVolume(volumeRangeCreateRequest.getMaxVolume());
        volumeRange.setDiscountedAmount(volumeRangeCreateRequest.getDiscountAmount());
        volumeRange.setCreatedBy(person);
        volumeRange.setCreationTime(LocalDateTime.now());
        volumeRange.setDiscountPublicId(discount.getPublicId().toString());

        VolumeRange saved = volumeRangeRepository.save(volumeRange);

        discount.addVolumeRange(saved);

        if (discount.getStatus() == DiscountStatus.Incomplete) {
            discount.setStatus(DiscountStatus.Pending);
        }
        discount.setUpdatedBy(person);
        discount.setUpdateDate(LocalDateTime.now());
        discountRepository.save(discount);

        String description = "Added volume range to discount: " + discount.getDiscountName() + " of type " + discount
                .getDiscountType()
                .name() + " with ID: " + discount.getPublicId();
        String eventType = "ADD_VOLUME_RANGE";
        String action = "Discount Add Volume Range";
        eventsRepository.save(new Events(discount.getPublicId().toString(), eventType, LocalDateTime.now(), description, action, person));
    }

    @Override
    public List<VolumeRange> getDiscountVolumeRanges(UUID discountPublicId) {

        Optional<Discounts> optionalDiscount = discountRepository.findByPublicIdAndDeletedIsFalse(discountPublicId);

        if (optionalDiscount.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Discount with ID " + discountPublicId + " not found")
                    .userMessage("The discount you are trying to add volume range to does not exist. Please check the ID and try again.");
        }

        Discounts discount = optionalDiscount.get();

        if (discount.getDiscountType() != DiscountType.VolumeDiscount) {
            throw new BadRequestRestApiException()
                    .developerMessage("Cannot add volume range to a non-volume discount with ID " + discountPublicId)
                    .userMessage("The discount you are trying to add volume range to is not a volume discount and cannot be added to.");
        }

        // sort by creation time descending
        return discount.getVolumeRanges()
                .stream()
                .sorted(Comparator.comparing(VolumeRange::getCreationTime).reversed())
                .toList();
    }

    @Override
    public void deleteVolumeRange(UUID discountPublicId, UUID publicId, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person with name " + name + " not found")
                    .userMessage("Sorry, we could not find your details. Please contact support");
        }
        Person person = optionalPerson.get();

        Optional<Discounts> optionalDiscount = discountRepository.findByPublicIdAndDeletedIsFalse(discountPublicId);
        if (optionalDiscount.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Discount with ID " + discountPublicId + " not found")
                    .userMessage("The discount you are trying to add volume range to does not exist. Please check the ID and try again.");
        }

        Discounts discount = optionalDiscount.get();

        if (discount.getDiscountType() != DiscountType.VolumeDiscount) {
            throw new BadRequestRestApiException()
                    .developerMessage("Cannot add volume range to a non-volume discount with ID " + discountPublicId)
                    .userMessage("The discount you are trying to add volume range to is not a volume discount and cannot be added to.");
        }

        Optional<VolumeRange> optionalVolumeRange = volumeRangeRepository.findByPublicIdAndDeletedIsFalse(publicId);

        if (optionalVolumeRange.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Volume range with ID " + publicId + " not found")
                    .userMessage("The volume range you are trying to delete does not exist. Please check the ID and try again.");
        }

        var volumeRange = optionalVolumeRange.get();

        discount.removeVolumeRange(volumeRange);
        discount.setUpdatedBy(person);
        discount.setUpdateDate(LocalDateTime.now());
        discountRepository.save(discount);

        String description = "Deleted volume range with ID " + publicId + " from discount: " + discount.getDiscountName() + " of type " + discount
                .getDiscountType()
                .name() + " with ID: " + discount.getPublicId();
        String eventType = "DELETE_VOLUME_RANGE";
        String action = "Discount Delete Volume Range";
        eventsRepository.save(new Events(discount.getPublicId().toString(), eventType, LocalDateTime.now(), description, action, person));

        volumeRange.setDeleted(true);
        volumeRange.setUpdatedBy(person);
        volumeRange.setUpdateDate(LocalDateTime.now());
        volumeRangeRepository.save(volumeRange);
    }


    private void createVanSalesOrder(DiscountApproval discountApproval) {

        // create order.... add  discount on it




    }
}
