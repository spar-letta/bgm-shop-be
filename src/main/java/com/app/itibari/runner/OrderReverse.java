//package com.app.itibari.runner;
//
//import com.app.itibari.dataTypes.LoadingStatusEnum;
//import com.app.itibari.models.entities.SalesOrders;
//import com.app.itibari.repository.SalesOrdersRepository;
//import com.app.itibari.services.SalesOrdersService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDateTime;
//import java.util.Arrays;
//import java.util.List;
//
//@Order(6)
//@Component
//@Slf4j
//public class OrderReverse implements CommandLineRunner {
//
//    @Autowired
//    private SalesOrdersRepository salesOrdersRepository;
//
//    @Autowired
//    private SalesOrdersService salesOrdersService;
//
//    @Override
//    public void run(String... args) throws Exception {
//        List<LoadingStatusEnum> status = Arrays.asList(
//                LoadingStatusEnum.PENDING_LOADING,
//                LoadingStatusEnum.LOADED,
//                LoadingStatusEnum.IN_TRANSIT,
//                LoadingStatusEnum.PLANNING    //
//        );
//        log.info("---------salesOrders started");
//
//        LocalDateTime startDate = LocalDateTime.now().minusMonths(6);
//        LocalDateTime endDate = LocalDateTime.now().minusDays(1);
//
//        List<SalesOrders> salesOrdersList =
//                salesOrdersRepository.findAllBySalesOrderStatusInAndDeletedIsFalseAndCreationTimeBetween(
//                        status,
//                        startDate,
//                        endDate
//                );
//
//        log.info("---------salesOrdersList size {}", salesOrdersList.size());
//
//        for (SalesOrders salesOrders : salesOrdersList) {
//            log.info("---------salesOrders number {}", salesOrders.getSalesOrderNumber());
//            salesOrders.setSalesOrderStatus(LoadingStatusEnum.ORDER_PLACED);
//            SalesOrders savedSalesOrder = salesOrdersRepository.save(salesOrders);
//
//            salesOrdersService.cancelSalesOrderV2(savedSalesOrder.getPublicId());
//        }
//    }
//}
