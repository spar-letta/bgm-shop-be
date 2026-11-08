package com.app.itibari.utils.helpers;

import com.app.itibari.dataTypes.CreditStatus;
import com.app.itibari.dataTypes.PaymentStatusEnum;
import com.app.itibari.dataTypes.WalletItemStatus;
import com.app.itibari.models.entities.CreditLimitItem;
import com.app.itibari.models.entities.Customers;
import com.app.itibari.models.entities.Wallet;
import com.app.itibari.repository.CreditLimitItemRepository;
import com.app.itibari.repository.CustomersRepository;
import com.app.itibari.repository.SalesOrdersRepository;
import com.app.itibari.repository.WalletRepository;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Component
public class CustomerCheckerHelper {
    private final SalesOrdersRepository salesOrdersRepository;
    private final CustomersRepository customersRepository;
    private final WalletRepository walletRepository;
    private final CreditLimitItemRepository creditLimitItemRepository;

    public CustomerCheckerHelper(SalesOrdersRepository salesOrdersRepository, CustomersRepository customersRepository, WalletRepository walletRepository, CreditLimitItemRepository creditLimitItemRepository) {
        this.salesOrdersRepository = salesOrdersRepository;
        this.customersRepository = customersRepository;
        this.walletRepository = walletRepository;
        this.creditLimitItemRepository = creditLimitItemRepository;
    }

//    public void checkIfCustomerHasActiveOrder(UUID customerId) {
//        Customers customer = customersRepository.findByPublicIdAndDeletedIsFalse(customerId)
//                .orElseThrow(() -> new BadRequestRestApiException()
//                        .userMessage("Sorry, the customer does not exist.")
//                        .developerMessage("Customer with publicId " + customerId + " does not exist.")
//                );
//
//        List<LoadingStatusEnum> activeStatuses = List.of(
//                LoadingStatusEnum.ORDER_PLACED
//        );
//
//        LocalDateTime endOfToday = LocalDate.of(2026, 1, 14).atTime(LocalTime.MAX);
//
//        List<SalesOrders> activeOrders = salesOrdersRepository
//                .findByCustomerAndSalesOrderStatusInAndDeletedIsFalse(customer, activeStatuses);
//
//        long pastOrTodayCount = activeOrders.stream()
//                .filter(order -> !order.getCreationTime().isAfter(endOfToday))
//                .count();
//
//        long futureCount = activeOrders.stream()
//                .filter(order -> order.getCreationTime().isAfter(endOfToday))
//                .count();
//
//        if (pastOrTodayCount <= 3) {
//            //Allow placing a new order
//            return;
//        } else {
//            // Only allow one future order
//            if (futureCount >= 1) {
//                throw new BadRequestRestApiException()
//                        .userMessage("This customer already has more than 3 active orders up to today. Only one future order is allowed.")
//                        .developerMessage("Customer [" + customer.getName() + "] currently has "
//                                + pastOrTodayCount + " active orders up to today and "
//                                + futureCount + " future orders.");
//            }
//        }
//    }


//    public void checkIfCustomerHasActiveOrder(UUID customerId) {
//
//        Optional<Customers> customerOptional = customersRepository.findByPublicIdAndDeletedIsFalse(customerId);
//
//        if (customerOptional.isEmpty()) {
//            throw new BadRequestRestApiException()
//                    .userMessage("Sorry, the customer does not exist.")
//                    .developerMessage("Customer with publicId " + customerId + " does not exist.");
//        }
//
//        Customers customer = customerOptional.get();
//
//        List<LoadingStatusEnum> activeStatuses = List.of(
//                LoadingStatusEnum.ORDER_PLACED
//        );
//
//        List<SalesOrders> ordersList = salesOrdersRepository.findByCustomerAndSalesOrderStatusInAndDeletedIsFalse(
//                customer,
//                activeStatuses
//        );
//
//        int MAX_ACTIVE_ORDERS = 3;
//
//        if (ordersList != null) {
//            if (ordersList.size() >= MAX_ACTIVE_ORDERS) {
//                throw new BadRequestRestApiException()
//                        .userMessage("This customer already has " + MAX_ACTIVE_ORDERS + " active orders. No more can be created.")
//                        .developerMessage("Customer [" + customer.getName() + "] currently has "
//                                + ordersList.size() + " active orders.");
//            }
//        }
//    }

    public void checkIfCustomerHasActiveOrder(UUID customerId) {
//        Customers customer = customersRepository.findByPublicIdAndDeletedIsFalse(customerId)
//                .orElseThrow(() -> new BadRequestRestApiException()
//                        .userMessage("Sorry, the customer does not exist.")
//                        .developerMessage("Customer with publicId " + customerId + " does not exist.")
//                );
//
//        List<LoadingStatusEnum> activeStatuses = List.of(
//                LoadingStatusEnum.ORDER_PLACED
//        );
//
//        List<SalesOrders> activeOrders = salesOrdersRepository
//                .findByCustomerAndSalesOrderStatusInAndDeletedIsFalse(customer, activeStatuses);

//        if (!activeOrders.isEmpty()) {
//            throw new BadRequestRestApiException()
//                    .userMessage("This customer already has an active order and cannot place another one.")
//                    .developerMessage("Customer [" + customer.getName() + "] already has "
//                            + activeOrders.size() + " active order(s) in status ORDER PLACED.");
//        }
    }

    public BigDecimal checkIfCustomerIsAllowedToBuyOnCredit(UUID customerId, boolean bookedByParent) {

        Optional<Customers> customerOptional = customersRepository.findByPublicIdAndDeletedIsFalse(customerId);

        if (customerOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, customer does not exist.")
                    .developerMessage("Customer with publicId " + customerId + " does not exist.");
        }

        Customers customer = customerOptional.get();
        Customers creditOwner = resolveCreditLimitOwner(customer);

        if (!creditOwner.isBnplAccountCreated()) {
            throw new BadRequestRestApiException()
                    .userMessage("This customer is not allowed to buy on credit.")
                    .developerMessage("Customer [" + creditOwner.getName() + "] is not allowed to buy on credit.");
        }

        // check credit limit and outstanding amount
        Wallet wallet = getCustomerWallet(creditOwner);

        if (wallet == null) {
            throw new BadRequestRestApiException()
                    .userMessage("This customer does not have a wallet.")
                    .developerMessage("Customer [" + creditOwner.getName() + "] does not have a wallet.");
        }

        List<String> referenceIds = resolveCreditScopeReferenceIds(creditOwner);
        List<CreditLimitItem> creditLimitItems = creditLimitItemRepository.findAllByReferenceIdInAndDeletedIsFalse(referenceIds);

        BigDecimal utilizedCredit = creditLimitItems.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getStatus() == WalletItemStatus.ACTIVE)
                .filter(item -> !item.isCancelled())
                .filter(item -> item.getPaymentStatus() == PaymentStatusEnum.NOT_PAID || item.getPaymentStatus() == PaymentStatusEnum.PARTIALLY_PAID)
                .map(CreditLimitItem::getBalanceAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCreditLimit = Optional.ofNullable(creditOwner.getCreditLimit()).orElse(BigDecimal.ZERO);
        // creditOwner is already the parent when the shopper is a child; use that wallet's balance
        // for parent-booked orders (parentWallet is null on the parent's own wallet).
        BigDecimal availableCredit = bookedByParent
                ? Optional.ofNullable(wallet.getWalletBalance()).orElse(BigDecimal.ZERO)
                : totalCreditLimit.subtract(utilizedCredit);

        if (availableCredit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestRestApiException()
                    .userMessage("This customer has exceeded their credit limit.")
                    .developerMessage("Customer [" + creditOwner.getName() + "] has exceeded their credit limit.");
        }

        if (wallet.getCreditStatus() == CreditStatus.Suspended || wallet.getCreditStatus() == CreditStatus.Defaulted) {
            throw new BadRequestRestApiException()
                    .userMessage("This customer's credit status does not allow them to buy on credit.")
                    .developerMessage("Customer [" + creditOwner.getName() + "] has credit status " + wallet.getCreditStatus() + ".");
        }

        // check if there are overdue payments

        if (!creditLimitItems.isEmpty()) {
            creditLimitItems.forEach(creditLimitItem -> {

                if (creditLimitItem.getStatus() == WalletItemStatus.ACTIVE) {
                    if (creditLimitItem.getPaymentStatus().equals(PaymentStatusEnum.NOT_PAID) || creditLimitItem.getPaymentStatus().equals(PaymentStatusEnum.PARTIALLY_PAID)) {
                        if (creditLimitItem.getDueDate().isBefore(LocalDate.now())) {
                            throw new BadRequestRestApiException()
                                    .userMessage("This customer has overdue payments and is not allowed to buy on credit.")
                                    .developerMessage("Customer [" + customer.getName() + "] has overdue payment for Credit Limit Item with order number " + creditLimitItem.getOrderNumber() + ".");
                        }
                    }
                }
            });

        }

        return availableCredit;
    }

    private Customers resolveCreditLimitOwner(Customers customer) {
        if (customer != null && customer.getParentCustomer() != null) {
            return customer.getParentCustomer();
        }
        return customer;
    }

    private List<String> resolveCreditScopeReferenceIds(Customers creditOwner) {
        List<String> referenceIds = new ArrayList<>();
        referenceIds.add(creditOwner.getPublicId().toString());

        List<Customers> childCustomers = customersRepository.findAllByParentCustomerAndDeletedIsFalse(creditOwner);
        referenceIds.addAll(childCustomers.stream()
                .map(Customers::getPublicId)
                .filter(Objects::nonNull)
                .map(UUID::toString)
                .toList());

        return referenceIds;
    }

    public Wallet getCustomerWallet(Customers customer) {
        return walletRepository.findByCustomerAndDeletedIsFalse(customer);
    }

    public boolean validateCustomerTinNumber(Customers customer) {
        log.info("==============Validating TIN for customer: {} with TIN: {}==============", customer.getName(), customer.getTin());
        if (customer.getTin() != null && !customer.getTin().isEmpty()) {


            log.info("==============Customer TIN is not null or empty==============");
            // the tin is only numeric, check that

            if (customer.getTin().matches("\\d+")) {
                log.info("==============Customer TIN is numeric==============");
                if (customer.getTin().length() == 9) {
                    log.info("==============Customer TIN is valid==============");
                    return true;
                }

            }
        }

        return false;
    }

    public void checkIfCustomerHasOverdueCreditMoreTHanSevenDays(UUID customerPublicId) {
        Optional<Customers> customerOptional = customersRepository.findByPublicIdAndDeletedIsFalse(customerPublicId);

        if (customerOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .userMessage("Sorry, customer does not exist.")
                    .developerMessage("Customer with publicId " + customerPublicId + " does not exist.");
        }
        Customers customer = customerOptional.get();

        List<CreditLimitItem> creditLimitItemList = creditLimitItemRepository.findAllByReferenceIdAndDeletedIsFalse(customer.getPublicId().toString());

        creditLimitItemList = creditLimitItemList.stream()
                .filter(Objects::nonNull)
                .filter(creditLimitItem -> creditLimitItem.getStatus() == WalletItemStatus.ACTIVE)
                .filter(creditLimitItem -> creditLimitItem.getPaymentStatus() == PaymentStatusEnum.NOT_PAID || creditLimitItem.getPaymentStatus() == PaymentStatusEnum.PARTIALLY_PAID)
                .filter(creditLimitItem -> creditLimitItem.getDueDate().isBefore(LocalDate.now().minusDays(7)))
                .toList();

        if (!creditLimitItemList.isEmpty()) {
            throw new BadRequestRestApiException()
                    .userMessage("This customer has overdue credit for more than 7 days and is not allowed to place new orders.")
                    .developerMessage("Customer [" + customer.getName() + "] has overdue credit for more than 7 days.");
        }

    }
}
