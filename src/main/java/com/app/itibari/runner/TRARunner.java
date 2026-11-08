//package com.app.itibari.runner;
//
//import com.app.itibari.repository.SalesOrderItemsRepository;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//import java.math.BigDecimal;
//
//
//@Component
//@Slf4j
//public class TRARunner implements CommandLineRunner {
//    private final SalesOrderItemsRepository salesOrderItemsRepository;
//
//    public TRARunner(SalesOrderItemsRepository salesOrderItemsRepository) {
//        this.salesOrderItemsRepository = salesOrderItemsRepository;
//    }
//
//
//    @Override
//    public void run(String... args) throws Exception {
//
//        log.info("==================Starting TRARunner...===============");
//
//        salesOrderItemsRepository.findAll().forEach(orderItem -> {
//
//            orderItem.setProductBeforePriceAfterTax(orderItem.getProduct().getSellingPriceBeforeTax());
//            BigDecimal taxPercentage = orderItem.getProduct().getTaxCode()!=null? orderItem.getProduct().getTaxCode().getCodeValue(): BigDecimal.ZERO;
//            orderItem.setTaxPercentageApplied(taxPercentage);
//            orderItem.setTaxApplied(taxPercentage.compareTo(BigDecimal.ZERO)>0);
//
//            salesOrderItemsRepository.save(orderItem);
//
//        });
//
//        log.info("Finished TRARunner...");
//
//    }
//}
