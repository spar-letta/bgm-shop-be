package com.app.itibari.services.helper;

import com.app.itibari.dataTypes.AmountType;
import com.app.itibari.dataTypes.DiscountStatus;
import com.app.itibari.dataTypes.DiscountType;
import com.app.itibari.dataTypes.LoadingStatusEnum;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.CartDiscountRequest;
import com.app.itibari.models.request.CartRequestItem;
import com.app.itibari.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Component
@Slf4j
public class DiscountHelper {

    private final DiscountRepository discountRepository;

    private final InvoiceRepository invoiceRepository;

    private final SalesOrdersRepository salesOrdersRepository;

    private final InvoiceItemRepository invoiceItemRepository;

    private final SalesOrderItemsRepository salesOrderItemsRepository;

    public void specialDiscount(SalesOrders savedOrder, Customers customer) {

        Optional<Discounts> optionalDiscounts = discountRepository.findDiscountsByCustomerPublicIdAndDiscountTypeAndStatusAndDeletedIsFalseAndOrderPublicIdIsNull(
                customer.getPublicId(),
                DiscountType.SpecialDiscount,
                DiscountStatus.Active);

        if (optionalDiscounts.isPresent()) {
            Discounts discounts = optionalDiscounts.get();
            discounts.setOrderPublicId(savedOrder.getPublicId().toString());
            Discounts savedDiscounts = discountRepository.save(discounts);

            log.info("[DISCOUNT-TRACE] specialDiscount: order={} customer={} discountPublicId={} amountType={} amount={} hasSpecialItems={}",
                    savedOrder.getSalesOrderNumber(), customer.getPublicId(), savedDiscounts.getPublicId(),
                    savedDiscounts.getAmountType(), savedDiscounts.getAmount(),
                    savedDiscounts.getSpecialItems() != null && !savedDiscounts.getSpecialItems().isEmpty());

            if (savedDiscounts.getSpecialItems() == null || savedDiscounts.getSpecialItems().isEmpty()) {
                createInvoiceWithoutInvoiceItems(savedOrder, savedDiscounts);
            } else {
                createInvoiceWithInvoiceItems(savedOrder, savedDiscounts);
            }
        } else {
            log.info("[DISCOUNT-TRACE] specialDiscount: no active unconsumed special discount for customer={} order={}",
                    customer.getPublicId(), savedOrder.getSalesOrderNumber());
        }
    }

    public void addVolumeDiscount(SalesOrders savedOrder) {

        Invoice invoice = invoiceRepository.findBySalesOrderPublicIdAndDeletedIsFalse(savedOrder.getPublicId().toString()).orElse(new Invoice());

        invoice.setSalesOrder(savedOrder);
        invoice.setHasBeenApplied(true);
        invoice.setDiscountType(DiscountType.VolumeDiscount);
        invoice.setSalesOrderPublicId(savedOrder.getPublicId().toString());

        if (!invoice.getInvoiceItems().isEmpty()) {
            for (InvoiceItem invoiceItem : invoice.getInvoiceItems()) {
                invoiceItem.setDeleted(true);
                invoiceItemRepository.save(invoiceItem);
            }
        }

        Invoice saved = invoiceRepository.save(invoice);
        savedOrder.setInvoice(saved);
        salesOrdersRepository.save(savedOrder);

        List<InvoiceItem> invoiceItems = new ArrayList<>();
        for (SalesOrderItems item : savedOrder.getSalesOrderItems()) {
//            List<Discounts> discount= discountRepository.findDiscountByProductPublicIdAndDiscountTypeAndStatus(item.getProduct().getPublicId(), DiscountType.VolumeDiscount, DiscountStatus.Active);
//            Optional<Discounts> optionalDiscounts = discount.stream().filter(d -> d.getDeleted() == false).findFirst();
            Optional<Discounts> optionalDiscounts = discountRepository.findDiscountByProductPublicIdAndDiscountTypeAndStatusAndDeletedIsFalse(
                    item.getProduct().getPublicId(),
                    DiscountType.VolumeDiscount, DiscountStatus.Active);
            if (optionalDiscounts.isPresent()) {
                log.info("==============Found volume discounts for product: {}", item.getProduct().getPublicId());
                Discounts discounts = optionalDiscounts.get();
                log.info("Min quantity for discount: {}", discounts.getMinQuantity());
                log.info("Quantity for discount: {}", item.getQuantity());
                if (discounts.rangeAmount(item.getQuantity()).compareTo(BigDecimal.ZERO) > 0) {
                    log.info("==========discounted amount========: {}", discounts.rangeAmount(item.getQuantity()));
                    InvoiceItem invoiceItem = invoiceItemRepository.findBySalesOrderItemIdAndDeletedIsFalse(item.getPublicId().toString())
                            .orElse(new InvoiceItem());
                    invoiceItem.setInvoice(saved);
                    invoiceItem.setAmount(discounts.rangeAmount(item.getQuantity()));
                    invoiceItem.setAmountType(AmountType.Fixed);
                    invoiceItem.setHasBeenApplied(true);
                    invoiceItem.setSalesOrderItemId(item.getPublicId().toString());
                    invoiceItem.setSalesOrderId(savedOrder.getPublicId().toString());
                    invoiceItem.setDiscountPublicId(discounts.getPublicId().toString());
                    invoiceItem.setDiscountAmount(discounts
                            .rangeAmount(item.getQuantity())
                            .multiply(BigDecimal.valueOf(item.getQuantity())));
                    BigDecimal percentage = discounts
                            .rangeAmount(item.getQuantity())
                            .divide(item.getTotalAmount(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                    invoiceItem.setPercentage(percentage);

                    //save
                    InvoiceItem savedItem = invoiceItemRepository.save(invoiceItem);

                    log.info("Created invoice item {} for sales order {} with volume discount",
                            savedItem.getPublicId(),
                            savedOrder.getPublicId());
                    log.info(
                            "Invoice item details: Sales Order Item ID: {}, Sales Order ID: {}, Discount Public ID: {}, Amount Type: {}, Amount: {}, Discount Amount: {}, Percentage: {}",
                            savedItem.getSalesOrderItemId(),
                            savedItem.getSalesOrderId(),
                            savedItem.getDiscountPublicId(),
                            savedItem.getAmountType(),
                            savedItem.getAmount(),
                            savedItem.getDiscountAmount(),
                            savedItem.getPercentage());

                    invoiceItems.add(savedItem);

                    item.setHasDiscount(true);
                    item.setDiscount(savedItem.getDiscountAmount());
                    item.setNewDiscountPercentage(percentage);
                }
            } else {
                log.info("No volume discounts found for product: {}", item.getProduct().getPublicId());
            }
            salesOrderItemsRepository.save(item);
        }

        if (!invoiceItems.isEmpty()) {
            saved.setInvoiceItems(invoiceItems);
            Invoice savedInvoice = invoiceRepository.save(saved);
            log.info("Created invoice {} for sales order {} with volume discount", savedInvoice.getPublicId(), savedOrder.getPublicId());
            log.info("[DISCOUNT-TRACE] addVolumeDiscount summary: order={} discountedItems={} totalDiscount={}",
                    savedOrder.getSalesOrderNumber(), invoiceItems.size(),
                    invoiceItems.stream().map(InvoiceItem::getDiscountAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
        } else {
            log.info("[DISCOUNT-TRACE] addVolumeDiscount summary: order={} no discountable items, invoice removed", savedOrder.getSalesOrderNumber());
            // delete the invoice as no items were added
            invoiceRepository.delete(saved);
            savedOrder.setInvoice(null);
            salesOrdersRepository.save(savedOrder);
        }
    }

    public void updateCreateDiscounts(RoutePlanning routePlanning) {

        log.info("[DISCOUNT-TRACE] updateCreateDiscounts: route={} orders={}",
                routePlanning.getPublicId(), routePlanning.getSalesOrders().size());
        List<SalesOrders> salesOrders = routePlanning.getSalesOrders();

        for (SalesOrders orders : salesOrders) {
            // Per-order isolation: one failing order must not abort discount recalc for the rest of the route
            try {
                BigDecimal discountBefore = orders.getTotalDiscount();
                String path;
                if (orders.getInvoice() != null) {
                    if (orders.isHasSpecialDiscount()) {
                        path = "SPECIAL_INVOICE";
                        updateOrderInvoiceDiscount(orders);
                    } else {
                        path = "VOLUME_INVOICE";
                        updateVolumeInvoiceDiscount(orders);
                    }
                } else {
                    // check only for volume discounts
                    path = "VOLUME_ADDON";
                    addVolumeDiscountAddOns(orders);
                }
                log.info("[DISCOUNT-TRACE] updateCreateDiscounts: order={} status={} path={} discountBefore={} discountAfter={}",
                        orders.getSalesOrderNumber(), orders.getSalesOrderStatus(), path, discountBefore, orders.getTotalDiscount());
            } catch (Exception e) {
                log.error("[DISCOUNT-TRACE] updateCreateDiscounts: recalc FAILED for order={} status={} route={} — continuing with remaining orders",
                        orders.getSalesOrderNumber(), orders.getSalesOrderStatus(), routePlanning.getPublicId(), e);
            }
        }
    }

    ///  private methods
    private void createInvoiceWithoutInvoiceItems(SalesOrders savedOrder, Discounts discounts) {

        Invoice invoice = invoiceRepository.findBySalesOrderPublicIdAndDeletedIsFalse(savedOrder.getPublicId().toString()).orElse(new Invoice());
        invoice.setSalesOrder(savedOrder);
        invoice.setDiscountType(DiscountType.SpecialDiscount);
        invoice.setAmountType(discounts.getAmountType());
        invoice.setAmount(discounts.getAmount());
        invoice.setHasBeenApplied(true);
        invoice.setSalesOrderPublicId(savedOrder.getPublicId().toString());
        invoice.setDiscountPublicId(discounts.getPublicId().toString());

        if (discounts.getAmountType() == AmountType.Fixed) {
            invoice.setDiscountAmount(discounts.getAmount());
            BigDecimal percentage = discounts
                    .getAmount()
                    .divide(savedOrder.getTotalAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            invoice.setPercentage(percentage);
        } else if (discounts.getAmountType() == AmountType.Percentage) {
            invoice.setPercentage(discounts.getAmount());
            BigDecimal fixedAmount = savedOrder
                    .getTotalAmount()
                    .multiply(discounts.getAmount())
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            invoice.setDiscountAmount(fixedAmount);
        }

        Invoice saved = invoiceRepository.save(invoice);
        savedOrder.setInvoice(saved);
        log.info("Created invoice {} for sales order {} with special discount", saved.getPublicId(), savedOrder.getPublicId());

        salesOrdersRepository.save(savedOrder);
    }

    private void createInvoiceWithInvoiceItems(SalesOrders savedOrder, Discounts discounts) {

        Invoice invoice = invoiceRepository.findBySalesOrderPublicIdAndDeletedIsFalse(savedOrder.getPublicId().toString()).orElse(new Invoice());
        invoice.setSalesOrder(savedOrder);
        invoice.setHasBeenApplied(true);
        invoice.setDiscountType(DiscountType.SpecialDiscount);
        invoice.setAmountType(discounts.getAmountType());
        invoice.setAmount(discounts.getAmount());
        invoice.setSalesOrderPublicId(savedOrder.getPublicId().toString());
        invoice.setDiscountPublicId(discounts.getPublicId().toString());
        Invoice saved = invoiceRepository.save(invoice);
        savedOrder.setInvoice(saved);
        salesOrdersRepository.save(savedOrder);

        List<InvoiceItem> invoiceItems = new ArrayList<>();
        for (SpecialItem product : discounts.getSpecialItems()) {
            for (SalesOrderItems item : savedOrder.getSalesOrderItems()) {
                if (item.getProduct().getPublicId().equals(product.getProductId())) {  //special item
                    BigDecimal discountAmount = product.getAmountApprovedPerQuantity().multiply(BigDecimal.valueOf(item.getQuantity()));
                    InvoiceItem invoiceItem = new InvoiceItem();
                    invoiceItem.setInvoice(saved);
                    invoiceItem.setAmountType(discounts.getAmountType());
                    invoiceItem.setAmount(product.getAmountApprovedPerQuantity());
                    invoiceItem.setHasBeenApplied(true);
                    invoiceItem.setSalesOrderItemId(item.getPublicId().toString());
                    invoiceItem.setSalesOrderId(savedOrder.getPublicId().toString());
                    invoiceItem.setDiscountPublicId(discounts.getPublicId().toString());
                    invoiceItem.setProductName(item.getProduct().getProductName());
                    invoiceItem.setProductPublicId(item.getProduct().getPublicId().toString());
                    invoiceItem.setQuantity(item.getQuantity());

                    if (discounts.getAmountType() == AmountType.Fixed) {
                        invoiceItem.setDiscountAmount(discountAmount);
                        BigDecimal percentage = discounts
                                .getAmount().divide(item.getTotalAmount(), 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100));
                        invoiceItem.setPercentage(percentage);
                    } else if (discounts.getAmountType() == AmountType.Percentage) {
                        invoiceItem.setPercentage(discounts.getAmount());
                        BigDecimal fixedAmount = item
                                .getTotalAmount()
                                .multiply(discountAmount)
                                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                        invoiceItem.setDiscountAmount(fixedAmount);
                    }

                    InvoiceItem savedItem = invoiceItemRepository.save(invoiceItem);
                    invoiceItems.add(savedItem);
                    log.info("Created invoice item {} for sales order {} with special discount",
                            savedItem.getPublicId(),
                            savedOrder.getPublicId());
                    log.info(
                            "Invoice item details: Sales Order Item ID: {}, Sales Order ID: {}, Discount Public ID: {}, Amount Type: {}, Amount: {}, Discount Amount: {}, Percentage: {}",
                            savedItem.getSalesOrderItemId(),
                            savedItem.getSalesOrderId(),
                            savedItem.getDiscountPublicId(),
                            savedItem.getAmountType(),
                            savedItem.getAmount(),
                            savedItem.getDiscountAmount(),
                            savedItem.getPercentage());

                    item.setHasDiscount(true);
                    item.setDiscount(savedItem.getDiscountAmount());
                    item.setNewDiscountPercentage(invoiceItem.getPercentage());
                }
                salesOrderItemsRepository.save(item);
            }
        }

        if (!invoiceItems.isEmpty()) {
            saved.setInvoiceItems(invoiceItems);
            invoiceRepository.save(saved);
            log.info("Created invoice {} for sales order {} with special discount--", saved.getPublicId(), savedOrder.getPublicId());
        } else {
            log.info("No invoice items to add for sales order-- {}", savedOrder.getPublicId());
            savedOrder.setInvoice(null);
            salesOrdersRepository.save(savedOrder);
            invoiceRepository.delete(saved);
        }
    }

    private void updateOrderInvoiceDiscount(SalesOrders savedOrder) {

        Invoice invoice = savedOrder.getInvoice();

        if (invoice.getInvoiceItems() == null || invoice.getInvoiceItems().isEmpty()) {
            log.info("[DISCOUNT-TRACE] updateOrderInvoiceDiscount (order-level): order={} amountType={} amount={} totalAmountDelivered={}",
                    savedOrder.getSalesOrderNumber(), invoice.getAmountType(), invoice.getAmount(), savedOrder.getTotalAmountDelivered());
            if (invoice.getAmountType() == AmountType.Fixed) {
                invoice.setDiscountAmount(invoice.getAmount());
                BigDecimal percentage = invoice
                        .getAmount()
                        .divide(savedOrder.getTotalAmountDelivered(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                invoice.setPercentage(percentage);
            } else if (invoice.getAmountType() == AmountType.Percentage) {
                invoice.setPercentage(invoice.getAmount());
                BigDecimal fixedAmount = savedOrder
                        .getTotalAmountDelivered()
                        .multiply(invoice.getAmount())
                        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                invoice.setDiscountAmount(fixedAmount);
            }
        } else {
            List<InvoiceItem> updatedItems = new ArrayList<>();
            for (InvoiceItem item : invoice.getInvoiceItems()) {
                for (SalesOrderItems orderItem : savedOrder.getSalesOrderItems()) {
                    if (item.getSalesOrderItemId().equals(orderItem.getPublicId().toString())) {
                        BigDecimal soldAmount = orderItem.getPricePerUnit().multiply(BigDecimal.valueOf(orderItem.getQuantityDelivered()));
                        if (item.getAmountType() == AmountType.Fixed) {
                            item.setDiscountAmount(item.getAmount().multiply(BigDecimal.valueOf(orderItem.getQuantityDelivered())));  // multiply by quantity as requested
                            BigDecimal percentage = item
                                    .getAmount()
                                    .divide(soldAmount, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100));
                            item.setPercentage(percentage);
                        } else if (item.getAmountType() == AmountType.Percentage) {
                            item.setPercentage(item.getAmount());
                            BigDecimal fixedAmount = soldAmount
                                    .multiply(item.getAmount())
                                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                            item.setDiscountAmount(fixedAmount);
                        }
                        InvoiceItem invoiceItem = invoiceItemRepository.save(item);
                        updatedItems.add(invoiceItem);

                        orderItem.setHasDiscount(true);
                        orderItem.setDiscount(invoiceItem.getDiscountAmount());
                        orderItem.setNewDiscountPercentage(invoiceItem.getPercentage());

                        log.info("[DISCOUNT-TRACE] updateOrderInvoiceDiscount (item): order={} orderItem={} qtyDelivered={} amountType={} amount={} itemDiscount={}",
                                savedOrder.getSalesOrderNumber(), orderItem.getPublicId(), orderItem.getQuantityDelivered(),
                                item.getAmountType(), item.getAmount(), invoiceItem.getDiscountAmount());
                    }
                    salesOrderItemsRepository.save(orderItem);
                }
            }
            invoice.setInvoiceItems(updatedItems);
        }

        invoiceRepository.save(invoice);
    }

    private void updateVolumeInvoiceDiscount(SalesOrders orders) {

        Invoice invoice = orders.getInvoice();

        List<InvoiceItem> updatedItems = new ArrayList<>();
        for (InvoiceItem item : invoice.getInvoiceItems()) {
            for (SalesOrderItems orderItem : orders.getSalesOrderItems()) {
                if (item.getSalesOrderItemId().equals(orderItem.getPublicId().toString())) {

                    Optional<Discounts> optionalDiscounts = discountRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(item.getDiscountPublicId()));
                    if (optionalDiscounts.isPresent()) {
                        Discounts discounts = optionalDiscounts.get();
                        if (discounts.rangeAmount(orderItem.getQuantityDelivered()).compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal soldAmount = orderItem
                                    .getPricePerUnit()
                                    .multiply(BigDecimal.valueOf(orderItem.getQuantityDelivered()));
                            BigDecimal amount = discounts.rangeAmount(orderItem.getQuantityDelivered());
                            item.setDiscountAmount(amount.multiply(BigDecimal.valueOf(orderItem.getQuantityDelivered())));
                            BigDecimal percentage = item
                                    .getAmount()
                                    .divide(soldAmount, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100));
                            item.setPercentage(percentage);
                            item.setAmount(discounts.rangeAmount(orderItem.getQuantityDelivered()));
                            item.setQuantity(orderItem.getQuantityDelivered());
                            InvoiceItem invoiceItem = invoiceItemRepository.save(item);
                            updatedItems.add(invoiceItem);
                            orderItem.setHasDiscount(true);
                            orderItem.setDiscount(invoiceItem.getDiscountAmount());
                            orderItem.setNewDiscountPercentage(invoiceItem.getPercentage());

                            log.info("[DISCOUNT-TRACE] updateVolumeInvoiceDiscount (item): order={} orderItem={} qtyDelivered={} perUnitAmount={} itemDiscount={}",
                                    orders.getSalesOrderNumber(), orderItem.getPublicId(), orderItem.getQuantityDelivered(),
                                    invoiceItem.getAmount(), invoiceItem.getDiscountAmount());
                        } else {
                            item.setDiscountAmount(BigDecimal.ZERO);
                            item.setAmount(BigDecimal.ZERO);
                            invoiceItemRepository.save(item);

                            log.info("[DISCOUNT-TRACE] updateVolumeInvoiceDiscount: qtyDelivered={} outside discount range, zeroing discount for order={} orderItem={}",
                                    orderItem.getQuantityDelivered(), orders.getSalesOrderNumber(), item.getSalesOrderItemId());
                            orderItem.setHasDiscount(false);
                            orderItem.setDiscount(BigDecimal.ZERO);
                            orderItem.setNewDiscountPercentage(BigDecimal.ZERO);
                        }
                    } else {
                        invoiceItemRepository.delete(item);
                        log.info("No discount found for discount id: {}", item.getDiscountPublicId());
                    }
                }

                salesOrderItemsRepository.save(orderItem);
            }
        }
        invoice.getInvoiceItems().clear();
        invoice.setInvoiceItems(updatedItems);
        invoiceRepository.save(invoice);
    }

    private void addVolumeDiscountAddOns(SalesOrders savedOrder) {

        Invoice invoice = invoiceRepository.findBySalesOrderPublicIdAndDeletedIsFalse(savedOrder.getPublicId().toString()).orElse(new Invoice());
        invoice.setSalesOrder(savedOrder);
        invoice.setHasBeenApplied(true);
        invoice.setDiscountType(DiscountType.VolumeDiscount);
        invoice.setSalesOrderPublicId(savedOrder.getPublicId().toString());

        Invoice saved = invoiceRepository.save(invoice);
        savedOrder.setInvoice(saved);
        salesOrdersRepository.save(savedOrder);

        List<InvoiceItem> invoiceItems = new ArrayList<>();

        for (SalesOrderItems item : savedOrder.getSalesOrderItems()) {
            Optional<Discounts> optionalDiscounts = discountRepository.findDiscountByProductPublicIdAndDiscountTypeAndStatusAndDeletedIsFalse(
                    item.getProduct().getPublicId(),
                    DiscountType.VolumeDiscount,
                    DiscountStatus.Active);
            if (optionalDiscounts.isPresent()) {
                Discounts discounts = optionalDiscounts.get();

                if (discounts.rangeAmount(item.getQuantityDelivered()).compareTo(BigDecimal.ZERO) > 0) {
                    InvoiceItem invoiceItem = invoiceItemRepository.findBySalesOrderItemIdAndDeletedIsFalse(item.getPublicId().toString())
                            .orElse(new InvoiceItem());

                    invoiceItem.setInvoice(saved);
                    invoiceItem.setAmount(discounts.rangeAmount(item.getQuantityDelivered()));
                    invoiceItem.setAmountType(AmountType.Fixed);
                    invoiceItem.setHasBeenApplied(true);
                    invoiceItem.setSalesOrderItemId(item.getPublicId().toString());
                    invoiceItem.setSalesOrderId(savedOrder.getPublicId().toString());
                    invoiceItem.setDiscountPublicId(discounts.getPublicId().toString());
                    BigDecimal soldAmount = item.getPricePerUnit().multiply(BigDecimal.valueOf(item.getQuantityDelivered()));
                    BigDecimal amount = discounts.rangeAmount(item.getQuantityDelivered());
                    invoiceItem.setDiscountAmount(amount.multiply(BigDecimal.valueOf(item.getQuantityDelivered())));
                    invoiceItem.setProductName(item.getProduct().getProductName());
                    invoiceItem.setProductPublicId(item.getProduct().getPublicId().toString());
                    BigDecimal percentage = discounts
                            .getAmount().divide(soldAmount, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                    invoiceItem.setPercentage(percentage);
                    invoiceItem.setQuantity(item.getQuantityDelivered());

                    InvoiceItem savedItem = invoiceItemRepository.save(invoiceItem);

                    invoiceItems.add(savedItem);

                    item.setHasDiscount(true);
                    item.setDiscount(savedItem.getDiscountAmount());
                    item.setNewDiscountPercentage(percentage);
                }
            } else {
                log.info("No volume discount found for product id: {}", item.getProduct().getPublicId());
            }

            salesOrderItemsRepository.save(item);
        }

        if (!invoiceItems.isEmpty()) {
            saved.setInvoiceItems(invoiceItems);
        } else {
            log.info("No invoice items to add for sales order {}", savedOrder.getPublicId());
        }
    }

    public BigDecimal calculateCartDiscount(CartDiscountRequest cartDiscountRequest, String name) {

        BigDecimal amount = BigDecimal.ZERO;

        if (!cartDiscountRequest.getCartRequestItems().isEmpty()) {
            for (CartRequestItem cartRequestItem : cartDiscountRequest.getCartRequestItems()) {
                Optional<Discounts> optionalDiscount = discountRepository.findDiscountByProductPublicIdAndDiscountTypeAndStatusAndDeletedIsFalse(
                        cartRequestItem.getProductPublicId(),
                        DiscountType.VolumeDiscount,
                        DiscountStatus.Active);

                if (optionalDiscount.isPresent()) {
                    Discounts discount = optionalDiscount.get();

                    BigDecimal rangeAmount = discount.rangeAmount(cartRequestItem.getQuantity());

                    BigDecimal discountAmount = rangeAmount.multiply(BigDecimal.valueOf(cartRequestItem.getQuantity()));

                    amount = amount.add(discountAmount);
                }
            }
        }

        return amount;
    }

    public void recalculateVolumeDiscount(SalesOrders savedOrder) {

        boolean isOrderDelivered =
                savedOrder.getSalesOrderStatus() == LoadingStatusEnum.DELIVERED;

        Invoice invoice;
        if (savedOrder.getInvoice() != null) {
            invoice = savedOrder.getInvoice();
        } else {
            invoice = new Invoice();
        }
        invoice.setSalesOrder(savedOrder);
        invoice.setHasBeenApplied(true);
        invoice.setDiscountType(DiscountType.VolumeDiscount);
        invoice.setSalesOrderPublicId(savedOrder.getPublicId().toString());

        Invoice saved = invoiceRepository.save(invoice);
        savedOrder.setInvoice(saved);
        salesOrdersRepository.save(savedOrder);

        List<InvoiceItem> invoiceItems = new ArrayList<>();
        for (SalesOrderItems item : savedOrder.getSalesOrderItems()) {

            if (invoice.getInvoiceItems() != null) {
                Optional<InvoiceItem> existingInvoiceItem = invoice.getInvoiceItems().stream()
                        .filter(ii -> ii.getSalesOrderItemId().equals(item.getPublicId().toString()))
                        .findFirst();

                int applicableQuantity = isOrderDelivered ? item.getQuantityDelivered() : item.getQuantity();
                if (existingInvoiceItem.isPresent()) {
                    InvoiceItem invoiceItem = existingInvoiceItem.get();
                    Discounts existingDiscount = discountRepository.findByPublicId(
                            UUID.fromString(invoiceItem.getDiscountPublicId()));

                    if (existingDiscount != null) {
                        log.info("Existing discount found for invoice item {}: {}", invoiceItem.getPublicId(), existingDiscount.getPublicId());
                        invoiceItem.setInvoice(saved);
                        invoiceItem.setAmount(existingDiscount.rangeAmount(applicableQuantity));
                        invoiceItem.setAmountType(AmountType.Fixed);
                        invoiceItem.setHasBeenApplied(true);
                        invoiceItem.setSalesOrderItemId(item.getPublicId().toString());
                        invoiceItem.setSalesOrderId(savedOrder.getPublicId().toString());
                        invoiceItem.setDiscountPublicId(existingDiscount.getPublicId().toString());
                        invoiceItem.setProductPublicId(item.getProduct().getPublicId().toString());
                        invoiceItem.setProductName(item.getProduct().getProductName());

                        invoiceItem.setDiscountAmount(existingDiscount
                                .rangeAmount(applicableQuantity)
                                .multiply(BigDecimal.valueOf(applicableQuantity)));

                        BigDecimal percentage = existingDiscount.rangeAmount(applicableQuantity)
                                .divide(item.getTotalAmount(), 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100));

                        invoiceItem.setPercentage(percentage);

                        //save
                        InvoiceItem savedItem = invoiceItemRepository.save(invoiceItem);

                        invoiceItems.add(savedItem);

                        item.setHasDiscount(true);
                        item.setDiscount(savedItem.getDiscountAmount());
                        item.setNewDiscountPercentage(percentage);
                    }
                }
            } else {

                Optional<Discounts> optionalDiscounts = discountRepository.findDiscountByProductPublicIdAndDiscountTypeAndStatusAndDeletedIsFalse(
                        item.getProduct().getPublicId(),
                        DiscountType.VolumeDiscount,
                        DiscountStatus.Active);
                if (optionalDiscounts.isPresent()) {

                    log.info("Founds volume discounts for product: {}", item.getProduct().getPublicId());

                    Discounts discounts = optionalDiscounts.get();

                    log.info("Min quantity  discount: {}", discounts.getMinQuantity());

                    log.info("Quantity for discounts: {}", item.getQuantity());

                    //  boolean discountCreatedBeforeOrder = discounts.getCreationTime().isBefore(savedOrder.getCreationTime());

                    int applicableQuantity = isOrderDelivered ? item.getQuantityDelivered() : item.getQuantity();

                    if (discounts.rangeAmount(applicableQuantity).compareTo(BigDecimal.ZERO) > 0) {

                        log.info("==========discount amount========: {}", discounts.rangeAmount(item.getQuantity()));

                        InvoiceItem invoiceItem;
                        if (!invoice.getInvoiceItems().isEmpty()) {
                            Optional<InvoiceItem> existingInvoiceItem = invoice.getInvoiceItems().stream()
                                    .filter(ii -> ii.getSalesOrderItemId().equals(item.getPublicId().toString()))
                                    .findFirst();
                            invoiceItem = existingInvoiceItem.orElseGet(InvoiceItem::new);

                            Discounts existingDiscount = discountRepository.findByPublicIdAndDeletedIsFalse(
                                            UUID.fromString(invoiceItem.getDiscountPublicId()))
                                    .orElse(null);
                            if (existingDiscount != null && !existingDiscount.getPublicId().equals(discounts.getPublicId())) {
                                log.info("Updating discount for invoice item {} from discount {} to {}",
                                        invoiceItem.getPublicId(),
                                        existingDiscount.getPublicId(),
                                        discounts.getPublicId());
                            }
                        } else {
                            invoiceItem = new InvoiceItem();
                        }

                        invoiceItem.setInvoice(saved);
                        invoiceItem.setAmount(discounts.rangeAmount(applicableQuantity));
                        invoiceItem.setAmountType(AmountType.Fixed);
                        invoiceItem.setHasBeenApplied(true);
                        invoiceItem.setSalesOrderItemId(item.getPublicId().toString());
                        invoiceItem.setSalesOrderId(savedOrder.getPublicId().toString());
                        invoiceItem.setDiscountPublicId(discounts.getPublicId().toString());
                        invoiceItem.setProductPublicId(item.getProduct().getPublicId().toString());
                        invoiceItem.setProductName(item.getProduct().getProductName());

                        invoiceItem.setDiscountAmount(discounts
                                .rangeAmount(applicableQuantity)
                                .multiply(BigDecimal.valueOf(applicableQuantity)));

                        BigDecimal percentage = discounts.rangeAmount(applicableQuantity)
                                .divide(item.getTotalAmount(), 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100));

                        invoiceItem.setPercentage(percentage);

                        //save
                        InvoiceItem savedItem = invoiceItemRepository.save(invoiceItem);

                        invoiceItems.add(savedItem);

                        item.setHasDiscount(true);
                        item.setDiscount(savedItem.getDiscountAmount());
                        item.setNewDiscountPercentage(percentage);
                    }
                } else {
                    log.info("No volume discount found for product: {}", item.getProduct().getPublicId());
                }

                if (!invoice.getInvoiceItems().isEmpty()) {
                    List<InvoiceItem> oldItems = invoice.getInvoiceItems();
                    List<InvoiceItem> itemsToRemove = new ArrayList<>();
                    for (InvoiceItem oldItem : oldItems) {
                        boolean itemExists = invoiceItems.contains(oldItem);

                        if (!itemExists) {
                            itemsToRemove.add(oldItem);
                        }
                    }

                    if (!itemsToRemove.isEmpty()) {
                        for (InvoiceItem itemToRemove : itemsToRemove) {
                            itemToRemove.setDeleted(true);
                            invoiceItemRepository.save(itemToRemove);
                            log.info("Removed invoice item {} as it no longer qualifies for volume discount", itemToRemove.getPublicId());
                        }
                    }
                }

                salesOrderItemsRepository.save(item);
            }

            if (!invoiceItems.isEmpty()) {
                saved.setInvoiceItems(invoiceItems);
                Invoice savedInvoice = invoiceRepository.save(saved);
                log.info("Create invoice {} for sales order {} with volume discount", savedInvoice.getPublicId(), savedOrder.getPublicId());
            } else {
                log.info("No invoices items to add for sales order {}", savedOrder.getPublicId());
                // delete the invoice as no items were added
                invoiceRepository.delete(saved);
                savedOrder.setInvoice(null);
                salesOrdersRepository.save(savedOrder);
            }
        }
    }
}
