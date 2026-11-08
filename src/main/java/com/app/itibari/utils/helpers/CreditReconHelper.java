package com.app.itibari.utils.helpers;

import com.app.itibari.dataTypes.LoadingStatusEnum;
import com.app.itibari.models.entities.*;
import com.app.itibari.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreditReconHelper {
    private final RoutePlanningRepository routePlanningRepository;
    private final CreditReconRepository creditReconRepository;
    private final SalesAgentsRepository salesAgentsRepository;
    private final CreditReconItemRepository creditReconItemRepository;
    @PersistenceContext
    private final EntityManager entityManager;
    private final SalesOrderItemsRepository salesOrderItemsRepository;
    private final EventsRepository eventsRepository;
    private final CreditLimitItemRepository creditLimitItemRepository;
    private final ReconciliationRepository reconciliationRepository;


    public void processCreditForReconciliation(ProductRecon saved) {

        Optional<RoutePlanning> optionalRoutePlanning = routePlanningRepository.findByRouteIdAndDeletedIsFalse(saved.getDeliveryId());

        if (optionalRoutePlanning.isPresent()) {
            RoutePlanning routePlanning = optionalRoutePlanning.get();

            List<SalesOrders> creditSalesOrders = routePlanning.getSalesOrders().stream()
                    .filter(salesOrders -> salesOrders.getSalesOrderStatus() == LoadingStatusEnum.DELIVERED)
                    .filter(SalesOrders::isCreditApplied)
                    .filter(salesOrder -> salesOrder.getCreditAmount() != null && salesOrder.getCreditAmount().compareTo(BigDecimal.ZERO) > 0)
                    .toList();

            if (!creditSalesOrders.isEmpty()) {
                log.info("=====================credit sales orders for delivery {}: {}=================", saved.getDeliveryId(), creditSalesOrders.stream().map(SalesOrders::getId).toList());

                log.info("Processing {} credit sales orders for delivery {}",
                        creditSalesOrders.size(), saved.getDeliveryId());

                for (SalesOrders so : creditSalesOrders) {
                    try {
                        this.createCreditRecon(saved, so);
                        log.info("Success: credit recon created for SO {}", so.getId());
                    } catch (Throwable t) {
                        log.error("Failed for SO {}: {}", so.getId(), t.toString(), t);
                    }
                }

//                if (creditSalesOrders != null || !creditSalesOrders.isEmpty()) {
//                    createCreditReconV2(saved, creditSalesOrders);
//                }
            }
        }
    }

    private void createCreditReconV2(ProductRecon saved, List<SalesOrders> creditSalesOrders) {

        BigDecimal creditAmount = BigDecimal.ZERO;
        BigDecimal interestAmount = BigDecimal.ZERO;
        BigDecimal orderTotalAmount = BigDecimal.ZERO;

        for (SalesOrders order : creditSalesOrders) {
            creditAmount = creditAmount.add(order.getCreditAmount());
            interestAmount = interestAmount.add(order.getInterestAmount());
            orderTotalAmount = orderTotalAmount.add(order.getTotalAmount());
        }

        CreditRecon creditRecon = creditReconRepository
                .findCreditReconByOrderPublicIdAndDeletedIsFalse(saved.getPublicId().toString())
                .orElseGet(CreditRecon::new);

        SalesOrders firstOrder = creditSalesOrders.get(0);
        Pair<String, String> agentInfo = resolveSalesAgent(firstOrder);

//        creditRecon.setCustomerPublicId(firstOrder.getCustomer().getPublicId().toString());
//        creditRecon.setCustomerName(firstOrder.getCustomer().getName());
//        creditRecon.setCustomerPhone(firstOrder.getCustomer().getPhoneNumber());

        creditRecon.setSalesAgentPublicId(agentInfo.getLeft());
        creditRecon.setSalesAgentName(agentInfo.getRight());
        creditRecon.setDeliveryAgentPublicId(saved.getDeliveryAgentPublicId());
        creditRecon.setDeliveryAgentName(saved.getDeliveryAgent());
        creditRecon.setCreditAmount(creditAmount);
        creditRecon.setOrderTotalAmount(orderTotalAmount);
        creditRecon.setRouteId(saved.getDeliveryId());
        creditRecon.setInterestRate(firstOrder.getInterestRate());
        creditRecon.setInterestAmount(interestAmount);

        if (creditRecon.getCreationTime() == null) {
            creditRecon.setCreationTime(LocalDateTime.now());
        }

        CreditRecon savedCreditRecon = creditReconRepository.save(creditRecon);

        createCreditReconItems(savedCreditRecon, creditSalesOrders);
    }

    private Pair<String, String> resolveSalesAgent(SalesOrders salesOrder) {
        if (salesOrder.getSalesAgentPublicId() != null) {
            String id = salesOrder.getSalesAgentPublicId();
            String name = salesAgentsRepository.findByPublicId(UUID.fromString(id)).getName();
            return Pair.of(id, name);
        } else if (salesOrder.getCustomer() != null && salesOrder.getCustomer().getServedBy() != null) {
            return Pair.of(
                    salesOrder.getCustomer().getServedBy().getPublicId().toString(),
                    salesOrder.getCustomer().getServedBy().getName()
            );
        }
        return Pair.of(null, null);
    }

    private void createCreditReconItems(CreditRecon savedCreditRecon, List<SalesOrders> creditSalesOrders) {
        List<CreditReconItem> creditReconItems = new ArrayList<>();

        for (SalesOrders salesOrder : creditSalesOrders) {
            List<SalesOrderItems> salesOrderItems =
                    salesOrderItemsRepository.findAllBySalesOrderAndDeletedIsFalse(salesOrder);

            salesOrderItems.stream()
                    .filter(item -> item.getQuantityDelivered() > 0)
                    .forEach(item -> {
                        CreditReconItem creditReconItem = new CreditReconItem();
                        creditReconItem.setCreditRecon(savedCreditRecon);
                        creditReconItem.setProductPublicId(item.getProduct().getPublicId().toString());
                        creditReconItem.setProductName(item.getProduct().getProductName());
                        creditReconItem.setSalesOrderItemPublicId(item.getPublicId().toString());
                        creditReconItem.setQuantity(item.getQuantityDelivered());
                        creditReconItem.setUnitPrice(item.getPricePerUnit());
                        creditReconItem.setDiscountAmount(item.getDiscount());
                        creditReconItem.setTotalPrice(item.getTotalAmount());
                        if (item.getSalesOrder() != null && item.getSalesOrder().getCustomer() != null) {
                            creditReconItem.setZone(item.getSalesOrder().getCustomer().getOrganizationLocation());
                        }
                        creditReconItems.add(creditReconItem);
                    });
        }

        if (!creditReconItems.isEmpty()) {
            creditReconItemRepository.saveAll(creditReconItems);
            savedCreditRecon.setCreditReconItems(creditReconItems);
            creditReconRepository.save(savedCreditRecon);
        }
    }


    private void createCreditRecon(ProductRecon saved, SalesOrders salesOrder) {
        CreditRecon creditRecon = creditReconRepository.findCreditReconByOrderPublicIdAndDeletedIsFalse(salesOrder.getPublicId().toString())
                .orElseGet(CreditRecon::new);

        String salesAgentPublicId;
        String salesAgentName;
        if (salesOrder.getSalesAgentPublicId() != null) {
            salesAgentPublicId = salesOrder.getSalesAgentPublicId();
            salesAgentName = salesAgentsRepository.findByPublicId(UUID.fromString(salesOrder.getSalesAgentPublicId())).getName();
        } else if (salesOrder.getCustomer().getServedBy() != null) {
            salesAgentPublicId = salesOrder.getCustomer().getServedBy().getPublicId().toString();
            salesAgentName = salesOrder.getCustomer().getServedBy().getName();
        } else {
            salesAgentPublicId = null;
            salesAgentName = null;
        }

        Optional<CreditLimitItem> optionalCreditLimitItem = creditLimitItemRepository.findByOrderNumberAndDeletedIsFalse(salesOrder.getSalesOrderNumber());
        if (optionalCreditLimitItem.isEmpty()) {
            log.warn("[DISCOUNT-TRACE] createCreditRecon: SKIPPED — no CreditLimitItem for order={} route={} (credit recon row not created)",
                    salesOrder.getSalesOrderNumber(), saved.getDeliveryId());
            return;
        }
        CreditLimitItem creditLimitItem = optionalCreditLimitItem.get();

        creditRecon.setOrderPublicId(salesOrder.getPublicId().toString());
        creditRecon.setOrderNumber(salesOrder.getSalesOrderNumber());
        creditRecon.setOrderCreationDateTime(salesOrder.getCreationTime());
        creditRecon.setCustomerPublicId(salesOrder.getCustomer().getPublicId().toString());
        creditRecon.setCustomerName(salesOrder.getCustomer().getName());
        creditRecon.setCustomerPhone(salesOrder.getCustomer().getPhoneNumber());
        creditRecon.setSalesAgentPublicId(salesAgentPublicId);
        creditRecon.setSalesAgentName(salesAgentName);
        creditRecon.setDeliveryAgentPublicId(saved.getDeliveryAgentPublicId());
        creditRecon.setDeliveryAgentName(saved.getDeliveryAgent());
        creditRecon.setCreditAmount(creditLimitItem.getAmount());
        creditRecon.setOrderTotalAmount(salesOrder.getTotalAmount());
        creditRecon.setRouteId(saved.getDeliveryId());
        creditRecon.setOtpCode(salesOrder.getCreditOneTimePassword());
        creditRecon.setInterestRate(creditLimitItem.getInterestRate());
        creditRecon.setInterestAmount(creditLimitItem.getInterestAmount());
        creditRecon.setCreationTime(LocalDateTime.now());
        creditRecon.setCreditStatus(creditLimitItem.getCreditStatus());
        creditRecon.setPaymentStatus(creditLimitItem.getPaymentStatus());

        Reconciliation reconciliation = reconciliationRepository.findByDeliveryIdIgnoreCase(saved.getDeliveryId());
        creditRecon.setReconciliation(reconciliation);
        CreditRecon savedCreditRecon = creditReconRepository.save(creditRecon);

        reconciliation.addCreditRecon(savedCreditRecon);
        reconciliationRepository.save(reconciliation);
        List<CreditReconItem> creditReconItems = new ArrayList<>();

        List<SalesOrderItems> salesOrderItems = salesOrderItemsRepository.findAllBySalesOrderAndDeletedIsFalse(salesOrder);

        salesOrderItems.forEach(salesOrderItem -> {
            if (salesOrderItem.getQuantityDelivered() > 0) {
                CreditReconItem creditReconItem = new CreditReconItem();
                creditReconItem.setCreditRecon(savedCreditRecon);
                creditReconItem.setProductPublicId(salesOrderItem.getProduct().getPublicId().toString());
                creditReconItem.setProductName(salesOrderItem.getProduct().getProductName());
                creditReconItem.setSalesOrderItemPublicId(salesOrderItem.getPublicId().toString());
                creditReconItem.setQuantity(salesOrderItem.getQuantityDelivered());
                creditReconItem.setUnitPrice(salesOrderItem.getPricePerUnit());
                creditReconItem.setDiscountAmount(salesOrderItem.getDiscount());
                creditReconItem.setTotalPrice(salesOrderItem.getTotalAmount());
                creditReconItems.add(creditReconItemRepository.save(creditReconItem));
            }
        });

        if (!creditReconItems.isEmpty()) {
            savedCreditRecon.setCreditReconItems(creditReconItems);
            creditReconRepository.save(savedCreditRecon);

            BigDecimal itemDiscountTotal = creditReconItems.stream()
                    .map(CreditReconItem::getDiscountAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            log.info("[DISCOUNT-TRACE] createCreditRecon: order={} creditAmount(net)={} itemDiscountTotal={} items={}",
                    salesOrder.getSalesOrderNumber(), creditLimitItem.getAmount(), itemDiscountTotal, creditReconItems.size());

            eventsRepository.save(new Events(savedCreditRecon.getPublicId().toString(), "Created", LocalDateTime.now(), "CREATED", "CREATED", saved.getCreatedBy()));
        } else {
            log.warn("[DISCOUNT-TRACE] createCreditRecon: no delivered items for order={} — credit recon row deleted", salesOrder.getSalesOrderNumber());
            creditReconRepository.delete(savedCreditRecon);
        }

    }

    public Page<CreditRecon> getAllCreditRecons(String searchParam,
                                                String deliveryAgentPublicId,
                                                String salesAgentPublicId,
                                                String customerPublicId,
                                                String orderNumber,
                                                LocalDate from,
                                                LocalDate to,
                                                Pageable pageable) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Build main query
        CriteriaQuery<CreditRecon> cq = cb.createQuery(CreditRecon.class);
        Root<CreditRecon> root = cq.from(CreditRecon.class);

        List<Predicate> predicates = buildPredicates(cb, root,
                searchParam, customerPublicId, salesAgentPublicId,
                deliveryAgentPublicId, orderNumber, from, to);

        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        cq.orderBy(cb.desc(root.get("id")));

        TypedQuery<CreditRecon> query = entityManager.createQuery(cq)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize());

        List<CreditRecon> results = query.getResultList();

        // Build count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<CreditRecon> countRoot = countQuery.from(CreditRecon.class);

        List<Predicate> countPredicates = buildPredicates(cb, countRoot,
                searchParam, customerPublicId, salesAgentPublicId,
                deliveryAgentPublicId, orderNumber, from, to);

        countQuery.select(cb.countDistinct(countRoot));
        countQuery.where(cb.and(countPredicates.toArray(new Predicate[0])));

        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }


    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<CreditRecon> root, String searchParam, String customerPublicId, String salesAgentPublicId, String deliveryAgentPublicId, String orderNumber, LocalDate from, LocalDate to) {
        List<Predicate> predicates = new ArrayList<>();

        if (searchParam != null && !searchParam.isEmpty()) {
            String likePattern = "%" + searchParam.toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("customerName")), likePattern),
                    cb.like(cb.lower(root.get("customerPhone")), likePattern),
                    cb.like(cb.lower(root.get("salesAgentName")), likePattern),
                    cb.like(cb.lower(root.get("deliveryAgentName")), likePattern),
                    cb.like(cb.lower(root.get("routeId")), likePattern),
                    cb.like(cb.lower(root.get("otpCode")), likePattern),
                    cb.like(cb.lower(root.get("orderNumber")), likePattern)
            ));
        }

        if (customerPublicId != null && !customerPublicId.isEmpty()) {
            predicates.add(cb.equal(root.get("customerPublicId"), customerPublicId));
        }

        if (salesAgentPublicId != null && !salesAgentPublicId.isEmpty()) {
            predicates.add(cb.equal(root.get("salesAgentPublicId"), salesAgentPublicId));
        }

        if (deliveryAgentPublicId != null && !deliveryAgentPublicId.isEmpty()) {
            predicates.add(cb.equal(root.get("deliveryAgentPublicId"), deliveryAgentPublicId));
        }

        if (orderNumber != null && !orderNumber.isEmpty()) {
            predicates.add(cb.equal(root.get("orderNumber"), orderNumber));
        }

        if (from != null && to != null) {
            predicates.add(cb.between(root.get("creationTime"), from.atStartOfDay(), to.plusDays(1).atStartOfDay()));
        } else if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("creationTime"), from.atStartOfDay()));
        } else if (to != null) {
            predicates.add(cb.lessThan(root.get("creationTime"), to.plusDays(1).atStartOfDay()));
        }

        return predicates;
    }

    public Page<CreditReconItem> getCreditReconItems(CreditRecon creditRecon, String searchParam, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Main query
        CriteriaQuery<CreditReconItem> cq = cb.createQuery(CreditReconItem.class);
        Root<CreditReconItem> root = cq.from(CreditReconItem.class);
        cq.distinct(true);
        cq.where(cb.and(buildProductItemPredicates(cb, root, creditRecon, searchParam).toArray(new Predicate[0])));
        cq.orderBy(cb.desc(root.get("id")));

        TypedQuery<CreditReconItem> query = entityManager.createQuery(cq)
                .setMaxResults(pageable.getPageSize())
                .setFirstResult((int) pageable.getOffset());

        List<CreditReconItem> queryResultList = query.getResultList();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<CreditReconItem> countRoot = countQuery.from(CreditReconItem.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(cb.and(buildProductItemPredicates(cb, countRoot, creditRecon, searchParam).toArray(new Predicate[0])));

        Long count = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(queryResultList, pageable, count);
    }

    private List<Predicate> buildProductItemPredicates(CriteriaBuilder cb, Root<CreditReconItem> countRoot, CreditRecon creditRecon, String searchParam) {
        List<Predicate> predicates = new ArrayList<>();


        predicates.add(cb.equal(countRoot.get("creditRecon").get("publicId"), creditRecon.getPublicId()));

        if (searchParam != null && !searchParam.isEmpty()) {
            String likePattern = "%" + searchParam.toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(countRoot.get("productName")), likePattern)
            ));
        }

        return predicates;
    }

    public void updateReconAmount(CreditLimitItem item, String orderNumber) {
        Optional<CreditRecon> optionalCreditRecon = creditReconRepository.findByOrderNumberAndDeletedIsFalse(orderNumber);
        if (optionalCreditRecon.isPresent()) {
            CreditRecon creditRecon = optionalCreditRecon.get();
            creditRecon.setPaidAmount(item.getPaidAmount().subtract(item.getExcessAmountPaid()));
            creditRecon.setDepositAmount(item.getPaidAmount().subtract(item.getExcessAmountPaid()));
            creditRecon.setBalanceAmount(item.getBalanceAmount());
            creditRecon.setCreditStatus(item.getCreditStatus());
            creditRecon.setPaidDateTime(LocalDateTime.now());
            creditRecon.setPaymentStatus(item.getPaymentStatus());
            creditRecon.setPaymentReference(item.getPaymentReference());
            creditReconRepository.save(creditRecon);
        }
    }
}
