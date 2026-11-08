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
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//import java.util.function.Function;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Order(6)
//@Component
//public class GoodsReturnedRunner implements CommandLineRunner {
//
//    @Autowired
//    private GoodsReturnRepository goodsReturnRepository;
//
//    @Autowired
//    private GoodsReceivedNoteRepository goodsReceivedNoteRepository;
//
//    @Autowired
//    private ProductItemRepository productItemRepository;
//
//    @Autowired
//    private GoodsReturnDeliveryNoteRepository goodsReturnDeliveryNoteRepository;
//
//    @Autowired
//    private ReturnedProductItemRepository returnedProductItemRepository;
//
//    @Autowired
//    private JdbcTemplate jdbcTemplate;
//    @Autowired
//    private PurchaseOrderRepository purchaseOrderRepository;
//
//    @Transactional
//    @Override
//    public void run(String... args) throws Exception {
////        int currentYear = Year.now().minusYears(1).getValue();
//////        LocalDateTime startDate = LocalDate.of(currentYear, Month.DECEMBER, 1).atStartOfDay();
//////        LocalDateTime endDate = LocalDate.of(currentYear, Month.DECEMBER, 14).atTime(LocalTime.MAX);
////
////        LocalDateTime startDate = LocalDate.of(currentYear, Month.AUGUST, 1).atStartOfDay();
////        LocalDateTime endDate = LocalDate.of(currentYear, Month.NOVEMBER, 30).atTime(LocalTime.MAX);
////
////        List<GoodsReturnNote> goodsReturnNote = goodsReturnRepository
////                .findByGoodsNoteTypeAndDeletedFalseAndCreationTimeBetween(
////                        GoodsNoteType.RETURN_GOODS, startDate, endDate);
////
////        log.info("---size to process {}", goodsReturnNote.size());
////
////        goodsReturnNote.forEach(goodsReturn -> {
////                    GoodsReceivedNote goodsReceivedNote =
////                            goodsReceivedNoteRepository.findByNoteNumber(goodsReturn.getGoodsReceiveNoteNumber()).orElse(null);
////                    log.info("-------------Goods Return ID: {}", goodsReturn);
////                    if (goodsReceivedNote != null){
////                        Long id = createQueryAndInsert(goodsReturn, goodsReceivedNote.getId());
////                        log.info("saved Return id  ID: {}", id);
////                        goodsReturn.getProductItems().forEach(productItem -> {
////                                    log.info("-------------Item Goods Return item ID: {}", productItem);
////                                    saveProductItem(productItem, id);
////                                }
////                        );
////                    }
////                });
//        runUpdateCostPrice();
//    }
//
//    private void runUpdateCostPrice() {
//        log.info("---- update cost price ----");
//        List<GoodsReturnDeliveryNote> notes = goodsReturnDeliveryNoteRepository
//                .findAllByDeletedIsFalse();
//
//        for (GoodsReturnDeliveryNote note : notes) {
//            for (ReturnedProductItem returnedProductItem : note.getReturnedProductItems()) {
//                if (returnedProductItem.getCostPrice() == null
//                        || returnedProductItem.getCostPrice().compareTo(BigDecimal.ZERO) == 0) {
//
//                    Optional<PurchaseOrder> originalPo = purchaseOrderRepository
//                            .findByPurchaseOrderNumberAndDeletedIsFalse(
//                                    note.getGoodsReceivedNote().getPurchaseOrderNumber());
//
//                    if (originalPo.isPresent()) {
//                        for (PurchaseOrderItem purchaseOrderItem : originalPo.get().getPurchaseOrderItems()) {
//                            if (purchaseOrderItem.getProduct().getPublicId()
//                                    .equals(returnedProductItem.getProductPublicId())) {
//
//                                BigDecimal unitPrice = purchaseOrderItem.getUnitPrice();
//                                returnedProductItem.setCostPrice(unitPrice);
//                                returnedProductItem.setTotalValue(
//                                        calculateTotalValue(unitPrice, returnedProductItem.getQuantityReturned())
//                                );
//                                returnedProductItemRepository.save(returnedProductItem);
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        log.info("--- end update cost price ----");
//    }
//
//
//
//    private Long createQueryAndInsert(GoodsReturnNote goodsReturn, Long goodsReceivedNoteId) {
//
//        String checkSql = "SELECT id FROM itibari.goods_return_delivery_notes WHERE public_id = ?";
//        List<Long> existing = jdbcTemplate.query(checkSql,
//                new Object[]{goodsReturn.getPublicId()},
//                (rs, rowNum) -> rs.getLong("id"));
//        if (!existing.isEmpty()) {
//            return existing.get(0);
//        }
//
//
//        String sql = "INSERT INTO itibari.goods_return_delivery_notes " +
//                "(creation_time, public_id, goods_return_note_number, goods_return_note_date, goods_return_note_status, " +
//                "purchase_order_number, supplier_id_fk, replacement_raised, goods_received_note_fk, replacement_purchase_order) " +
//                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
//
//        KeyHolder keyHolder = new GeneratedKeyHolder();
//
//        jdbcTemplate.update(connection -> {
//            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"}); // 👈 restrict to id
//            ps.setDate(1, java.sql.Date.valueOf(goodsReturn.getCreationTime().toLocalDate()));
//            ps.setObject(2, goodsReturn.getPublicId(), java.sql.Types.OTHER);
//            ps.setString(3, goodsReturn.getGoodsReturnNoteNumber());
//            ps.setDate(4, java.sql.Date.valueOf(goodsReturn.getGoodsReturnNoteDate()));
//            ps.setString(5, goodsReturn.getStatus().name());
//            ps.setString(6, goodsReturn.getPurchaseOrderNumber());
//            ps.setLong(7, goodsReturn.getSupplier().getId());
//            ps.setBoolean(8, goodsReturn.isReplacementRaised());
//            ps.setLong(9, goodsReceivedNoteId);
//            ps.setString(10, goodsReturn.getReplacementPurchaseOrder());
//            return ps;
//        }, keyHolder);
//
//        return keyHolder.getKey().longValue();
//    }
//
//    private void saveProductItem(ProductItem productItem, Long id) {
//        String checkSql = "SELECT id FROM itibari.returned_product_items WHERE public_id = ?";
//        List<Long> existing = jdbcTemplate.query(checkSql, new Object[]{productItem.getPublicId()}, (rs, rowNum) -> rs.getLong("id"));
//        if (existing.isEmpty()) {
//
//            String sql = "INSERT INTO itibari.returned_product_items " +
//                    "(public_id, creation_time, product_name, product_code, price, " +
//                    "product_public_id, quantity_requested, " +
//                    " sales_item_public_id, goods_return_delivery_note_fk, quantity_returned) " +
//                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
//            KeyHolder keyHolder = new GeneratedKeyHolder();
//
//            jdbcTemplate.update(connection -> {
//                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
//                ps.setObject(1, productItem.getPublicId(), java.sql.Types.OTHER);
//                ps.setDate(2, java.sql.Date.valueOf(productItem.getCreationTime().toLocalDate()));
//                ps.setObject(3, productItem.getProductName());
//                ps.setObject(4, productItem.getProductCode());
//                ps.setBigDecimal(5, productItem.getProductSellingPrice());
//                ps.setObject(6, productItem.getProductPublicId(), java.sql.Types.OTHER);
//                ps.setLong(7, productItem.getQuantityRequested());
//                ps.setObject(8, productItem.getSalesOrderItemPublicId());
//                ps.setLong(9, id);
//                ps.setLong(10, productItem.getQuantityReturned());
//                return ps;
//            }, keyHolder);
//        }
//    }
//
//    private BigDecimal calculateTotalValue(BigDecimal purchasePrice, Long quantity) {
//        if (purchasePrice == null || quantity == null) {
//            return BigDecimal.ZERO;
//        }
//        return purchasePrice.multiply(BigDecimal.valueOf(quantity));
//    }
//}
