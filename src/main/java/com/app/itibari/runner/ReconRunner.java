//package com.app.itibari.runner;
//
//import com.app.itibari.dataTypes.PaymentStatusEnum;
//import com.app.itibari.models.entities.CreditRecon;
//import com.app.itibari.repository.CreditLimitItemRepository;
//import com.app.itibari.repository.CreditReconRepository;
//import com.app.itibari.repository.ReceiptItemRepository;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
//@Order(1)
//@Component
//@Slf4j
//public class ReconRunner implements CommandLineRunner {
//
//    private final ReceiptItemRepository receiptItemRepository;
//    private final CreditLimitItemRepository creditLimitItemRepository;
//    private final CreditReconRepository creditReconRepository;
//
//    public ReconRunner(ReceiptItemRepository receiptItemRepository, CreditLimitItemRepository creditLimitItemRepository, CreditReconRepository creditReconRepository) {
//        this.receiptItemRepository = receiptItemRepository;
//        this.creditLimitItemRepository = creditLimitItemRepository;
//        this.creditReconRepository = creditReconRepository;
//    }
//
//
//    @Override
//    public void run(String... args) throws Exception {
//
//        log.info("Running ReconRunner");
//
////        receiptItemRepository.findAll().forEach(receiptItem -> {
////            if(receiptItem.getDiscountPercentage() == null) {
////                receiptItem.setDiscountPercentage("0");
////            }else {
////                try {
////                    BigDecimal percentage = new BigDecimal(receiptItem.getDiscountPercentage());
////
////                    receiptItem.setDiscountPercentage(String.valueOf(percentage.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)));
////
////                    log.info("Discount percentage is {}  {}", receiptItem.getDiscountPercentage(), percentage);
////                }catch (NumberFormatException e) {
////                    log.error("Invalid discount percentage for receipt item with id {}: {}", receiptItem.getId(), receiptItem.getDiscountPercentage());
////                    receiptItem.setDiscountPercentage("0");
////                }
////            }
////            receiptItemRepository.save(receiptItem);
////        });
////
////
////        log.info("Finished ReconRunner===============");
////
////
////    }
////}
