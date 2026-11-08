//package com.app.itibari.runner;
//
//import com.app.itibari.dataTypes.LoadingStatusEnum;
//import com.app.itibari.dataTypes.RouteStatus;
//import com.app.itibari.models.entities.Product;
//import com.app.itibari.models.entities.ProductItem;
//import com.app.itibari.models.entities.ProductRecon;
//import com.app.itibari.models.entities.RoutePlanning;
//import com.app.itibari.models.entities.SalesOrderItems;
//import com.app.itibari.models.entities.SalesOrders;
//import com.app.itibari.repository.*;
//import com.app.itibari.services.LogisticService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Component;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class Planning implements CommandLineRunner {
//
//    private final LogisticService logisticService;
//
//    private final ProductReconRepository productReconRepository;
//
//    private final ProductItemRepository productItemRepository;
//
//    private final ProductRepository productRepository;
//
//    private final SalesOrderItemsRepository salesOrderItemsRepository;
//
//    private final SalesOrdersRepository salesOrdersRepository;
//    private final ReconciliationRepository reconciliationRepository;
//    private final RoutePlanningRepository routePlanningRepository;
//
//    @Override
//    public void run(String... args) throws Exception {
//
//       String route = "RT-101027";
//
//       routePlanningRepository.findByRouteIdAndDeletedIsFalse(route).ifPresent(routePlanning -> {
//           List<SalesOrders> salesOrders = salesOrdersRepository.findAllByRoutePublicIdAndDeletedIsFalse(routePlanning.getPublicId().toString());
//
//           for (SalesOrders order: salesOrders){
//               routePlanning.addSalesOrder(order);
//           }
//
//           routePlanningRepository.save(routePlanning);
//       });
//
//    }
//}
