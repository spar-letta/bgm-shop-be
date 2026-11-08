//package com.app.itibari.runner;
//
//import com.app.itibari.models.entities.*;
//import com.app.itibari.repository.*;
//import jakarta.transaction.Transactional;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.core.annotation.Order;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.jdbc.support.GeneratedKeyHolder;
//import org.springframework.jdbc.support.KeyHolder;
//import org.springframework.stereotype.Component;
//
//import java.math.BigDecimal;
//import java.sql.PreparedStatement;
//import java.sql.Statement;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Order(6)
//@Component
//public class GoodsReceivedRunner implements CommandLineRunner {
//
//    @Autowired
//    private GoodsReturnRepository goodsReturnRepository;
//
//    @Autowired
//    private ProductItemRepository productItemRepository;
//
//    @Autowired
//    private PurchaseOrderRepository purchaseOrderRepository;
//
//    @Autowired
//    private GoodsReturnDeliveryNoteRepository goodsReturnDeliveryNoteRepository;
//
//    @Autowired
//    private ReceivedProductItemRepository receivedProductItemRepository;
//
//    @Autowired
//    private JdbcTemplate jdbcTemplate;
//
//    @Autowired
//    private GoodsReceivedNoteRepository goodsReceivedNoteRepository;
//    @Autowired
//    private PurchaseOrderItemRepository purchaseOrderItemRepository;
//
//    @Transactional
//    @Override
//    public void run(String... args) throws Exception {
////        int currentYear = Year.now().minusYears(1).getValue();
////        LocalDateTime startDate = LocalDate.of(currentYear, Month.DECEMBER, 1).atStartOfDay();
////        LocalDateTime endDate = LocalDate.of(currentYear, Month.DECEMBER, 14).atTime(LocalTime.MAX);
//
////        LocalDateTime startDate = LocalDate.of(currentYear, Month.AUGUST, 1).atStartOfDay();
////        LocalDateTime endDate = LocalDate.of(currentYear, Month.NOVEMBER, 30).atTime(LocalTime.MAX);
//
//        // August 1st to hio Nov 15th  the date we started from
//
////        goodsReturnRepository
////                .findByGoodsNoteTypeAndDeletedFalseAndCreationTimeBetween(
////                        GoodsNoteType.RECEIVED_GOODS, startDate, endDate)
////                .forEach(goodsReturn -> {
////                    log.info("-------------Goods Return ID: {}", goodsReturn);
////                    Long id = createQueryAndInsert(goodsReturn);
////                    log.info("saved id  ID: {}", id);
////                    goodsReturn.getProductItems().forEach(productItem -> {
////                                log.info("-------------ItemGoodsReturn ID: {}", productItem);
////                                saveProductItem(productItem, id);
////
////                            }
////                    );
////                });
////        runReceivedProductStock();
//    }
//
//    private void runReceivedProductStock() {
//        log.info("--------start updating received product items------------");
//        List<PurchaseOrder> purchaseOrders = purchaseOrderRepository
//                .findAllByReplacementPurchaseOrderIsTrue();
//
//        for (PurchaseOrder purchaseOrder : purchaseOrders) {
//            GoodsReturnDeliveryNote goodsReturnDeliveryNote = goodsReturnDeliveryNoteRepository
//                    .findByGoodsReturnNoteNumberAndDeletedIsFalse(purchaseOrder.getGoodsReturnNoteNumberReference())
//                    .orElse(null);
//
//            if (goodsReturnDeliveryNote == null) {
//                continue;
//            }
//
//            PurchaseOrder linkedPurchaseOrder = purchaseOrderRepository
//                    .findByPurchaseOrderNumberAndDeletedIsFalse(goodsReturnDeliveryNote.getPurchaseOrderNumber())
//                    .orElse(null);
//
//            if (linkedPurchaseOrder == null) {
//                continue;
//            }
//
//            updateReceivedProductItems(purchaseOrder, linkedPurchaseOrder.getPurchaseOrderItems());
//        }
//    }
//
//    private void updateReceivedProductItems(PurchaseOrder purchaseOrder, List<PurchaseOrderItem> purchaseOrderItems) {
//
//        // Build a lookup map for faster matching
//        Map<String, BigDecimal> skuToPriceMap = purchaseOrderItems.stream()
//                .collect(Collectors.toMap(
//                        item -> item.getProduct().getSku(),
//                        PurchaseOrderItem::getUnitPrice
//                ));
//
//        List<PurchaseOrderItem> updatedItems = new ArrayList<>();
//        for (PurchaseOrderItem purchaseOrderItem : purchaseOrder.getPurchaseOrderItems()) {
//            BigDecimal newPrice = skuToPriceMap.get(purchaseOrderItem.getProduct().getSku());
//            if (newPrice != null && !newPrice.equals(purchaseOrderItem.getUnitPrice())) {
//                purchaseOrderItem.setUnitPrice(newPrice);
//                updatedItems.add(purchaseOrderItem);
//            }
//        }
//
//        if (!updatedItems.isEmpty()) {
//            purchaseOrderItemRepository.saveAll(updatedItems);
//        }
//        log.info("------end updating received product items------------");
//    }
//
//
//
//    private Long createQueryAndInsert(GoodsReturnNote goodsReturn) {
//        String checkSql = "SELECT id FROM itibari.goods_received_notes WHERE public_id = ?";
//        List<Long> existing = jdbcTemplate.query(checkSql,
//                new Object[]{goodsReturn.getPublicId()},
//                (rs, rowNum) -> rs.getLong("id"));
//        if (!existing.isEmpty()) {
//            return existing.get(0);
//        }
//
//        String sql = "INSERT INTO itibari.goods_received_notes " +
//                "(note_date, public_id, note_number, supplier_id_fk, status, " +
//                "purchase_order_number, delivery_note_number, purchase_order_status) " +
//                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
//
//        KeyHolder keyHolder = new GeneratedKeyHolder();
//
//        jdbcTemplate.update(connection -> {
//            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"}); // 👈 restrict to id
//            ps.setDate(1, java.sql.Date.valueOf(goodsReturn.getGoodsReturnNoteDate()));
//            ps.setObject(2, goodsReturn.getPublicId(), java.sql.Types.OTHER);
//            ps.setString(3, goodsReturn.getGoodsReturnNoteNumber());
//            ps.setLong(4, goodsReturn.getSupplier().getId());
//            ps.setString(5, goodsReturn.getStatus().name());
//            ps.setString(6, goodsReturn.getPurchaseOrderNumber());
//            ps.setString(7, goodsReturn.getDeliveryNoteNumber());
//            ps.setString(8, goodsReturn.getPurchaseOrderStatus().name());
//            return ps;
//        }, keyHolder);
//
//        return keyHolder.getKey().longValue();
//    }
//
//    private void saveProductItem(ProductItem productItem, Long id) {
//        String checkSql = "SELECT id FROM itibari.received_product_items WHERE public_id = ?";
//        List<Long> existing = jdbcTemplate.query(checkSql, new Object[]{productItem.getPublicId()}, (rs, rowNum) -> rs.getLong("id"));
//        if (existing.isEmpty()) {
//
//            String sql = "INSERT INTO itibari.received_product_items " +
//                    "(public_id, creation_time, product_name, product_code, price, " +
//                    "product_public_id, quantity_received, quantity_requested, goods_received_note_id_fk, " +
//                    "order_quantity, van_sale_quantity, quantity_picked, quantity_sold, total_units, quantity_missing, quantity_returned) " +
//                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
//            KeyHolder keyHolder = new GeneratedKeyHolder();
//
//            jdbcTemplate.update(connection -> {
//                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
//                ps.setObject(1, productItem.getPublicId(), java.sql.Types.OTHER);
//                ps.setObject(2, productItem.getCreationTime());
//                ps.setObject(3, productItem.getProductName());
//                ps.setObject(4, productItem.getProductCode());
//                ps.setBigDecimal(5, productItem.getProductSellingPrice());
//                ps.setObject(6, productItem.getProductPublicId(), java.sql.Types.OTHER);
//                ps.setLong(7, productItem.getQuantityReceived());
//                ps.setLong(8, productItem.getQuantityRequested());
//                ps.setLong(9, id);
//                ps.setLong(10, productItem.getOrderQuantity());
//                ps.setLong(11, productItem.getVanSaleQuantity());
//                ps.setLong(12, productItem.getQuantityPicked());
//                ps.setLong(13, productItem.getQuantitySold());
//                ps.setLong(14, productItem.getTotalUnits());
//                ps.setLong(15, productItem.getQuantityMissing());
//                ps.setLong(16, productItem.getQuantityReturned());
//                return ps;
//            }, keyHolder);
//        }
//    }
//}
