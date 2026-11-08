//package com.app.itibari.runner;
//
//import com.app.itibari.dataTypes.LoadingStatusEnum;
//import com.app.itibari.models.entities.ProductItem;
//import com.app.itibari.models.entities.RoutePlanning;
//import com.app.itibari.repository.*;
//import com.app.itibari.services.JobService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//import java.util.Optional;
//
//@Order(4)
//@Component
//@Slf4j
//public class ProductRunner implements CommandLineRunner {
//    private final ProductRepository productRepository;
//    private final JobService jobService;
//    private final SalesOrdersRepository salesOrdersRepository;
//    private final ProductReconRepository productReconRepository;
//    private final RoutePlanningRepository routePlanningRepository;
//    private final ProductItemRepository productItemRepository;
//
//    public ProductRunner(ProductRepository productRepository, JobService jobService, SalesOrdersRepository salesOrdersRepository, ProductReconRepository productReconRepository, RoutePlanningRepository routePlanningRepository, ProductItemRepository productItemRepository) {
//        this.productRepository = productRepository;
//        this.jobService = jobService;
//        this.salesOrdersRepository = salesOrdersRepository;
//        this.productReconRepository = productReconRepository;
//        this.routePlanningRepository = routePlanningRepository;
//        this.productItemRepository = productItemRepository;
//    }
//
//
//    @Override
//    public void run(String... args) throws Exception {
////        if (productRepository.count() <= 46) {
////            jobService.runProductJob();
////
////            log.info("Product job started");
////        }
//
//        productReconRepository.findAll().forEach(productRecon -> {
//            Optional<RoutePlanning> routePlanningOptional = routePlanningRepository.findByRouteIdAndDeliveryAgentIdIgnoreCaseAndDeletedIsFalse(
//                    productRecon.getDeliveryId(),
//                    productRecon.getDeliveryAgentPublicId()
//            );
//            routePlanningOptional.ifPresent(routePlanning -> routePlanning.getSalesOrders().forEach(salesOrders -> {
//                salesOrders.getSalesOrderItems().forEach(salesOrderItems -> {
//                    List<ProductItem> lists = productItemRepository.findByProductReconAndProductPublicIdAndHasVanSaleIsTrue(
//                            productRecon,
//                            salesOrderItems.getProduct().getPublicId()
//                    );
//                    if (!lists.isEmpty()) {
//                        log.info("Found {} product items for productRecon: {} and productPublicId: {}",
//                                lists.size(),
//                                productRecon.getPublicId(),
//                                salesOrderItems.getProduct().getPublicId());
//                        lists.forEach(productItem -> {
//                            productItem.setSalesOrderItemPublicId(salesOrderItems.getPublicId().toString());
//                            if(productItem.getOrderQuantity()> 0){
//                                // get abs value
//                                long vanQuantity = Math.abs(productItem.getQuantitySold() - productItem.getOrderQuantity()) - productItem.getQuantityReturned();
//                                if (vanQuantity> 0){
//                                    productItem.setVanSaleItemSold((int) vanQuantity);
//                                }else {
//                                    productItem.setVanSaleItemSold(0);
//                                }
//                            }
//                            productItemRepository.save(productItem);
//                            log.info("Updated product item: {}", productItem.getPublicId());
//                        });
//                    }
//                });
//            }));
//        });
//    }
//}
