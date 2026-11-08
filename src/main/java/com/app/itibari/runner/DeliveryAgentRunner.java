//package com.app.itibari.runner;
//
//import com.app.itibari.dataTypes.LoadingStatusEnum;
//import com.app.itibari.dataTypes.StockType;
//import com.app.itibari.dataTypes.UpdateType;
//import com.app.itibari.models.entities.RegionWarehouseMapping;
//import com.app.itibari.models.entities.Warehouse;
//import com.app.itibari.repository.*;
//import com.app.itibari.services.JobService;
//import com.app.itibari.services.helper.InventoryHelper;
//import com.app.itibari.services.helper.WarehouseGetterHelper;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.core.annotation.Order;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDateTime;
//import java.util.Optional;
//import java.util.UUID;
//
//@Order(3)
//@Component
//@Slf4j
//@RequiredArgsConstructor
//public class DeliveryAgentRunner implements CommandLineRunner {
//    private final DeliveryAgentsRepository deliveryAgentsRepository;
//    private final JobService jobService;
//    private final GoodsReturnRepository goodsReturnRepository;
//    private final ProductRepository productRepository;
//    private final ProductItemRepository productItemRepository;
//    private final PurchaseOrderRepository purchaseOrderRepository;
//    private final PersonRepository personRepository;
//    private final PasswordEncoder passwordEncoder;
//    private final SalesOrdersRepository salesOrdersRepository;
//    private final CustomersRepository customersRepository;
//    private final RegionWarehouseMappingRepository regionWarehouseMappingRepository;
//    private final WarehouseRepository warehouseRepository;
//    private final OrganizationRegionsRepository organizationRegionsRepository;
//    private final WarehouseGetterHelper warehouseGetterHelper;
//    private final InventoryHelper inventoryHelper;
//
//
//    @Override
//    public void run(String... args) throws Exception {
//
////        String warehousePublicId = "7371cd7e-da28-4325-b331-e5787e211b14";
////
////        regionWarehouseMappingRepository.deleteAll();
////
////        Optional<Warehouse> warehouseOptional = warehouseRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(warehousePublicId));
////
////        if (warehouseOptional.isPresent()) {
////            Warehouse warehouse = warehouseOptional.get();
////            RegionWarehouseMapping regionWarehouseMapping;
////
////            Optional<RegionWarehouseMapping> optionalRegionWarehouseMapping = regionWarehouseMappingRepository.findByWarehouseAndDeletedIsFalse(warehouse);
////
////            if (optionalRegionWarehouseMapping.isPresent()) {
////                regionWarehouseMapping = optionalRegionWarehouseMapping.get();
////            } else {
////                regionWarehouseMapping = new RegionWarehouseMapping();
////                regionWarehouseMapping.setWarehouse(warehouse);
////                regionWarehouseMapping.setCreationTime(LocalDateTime.now());
////
////            }
////
////            RegionWarehouseMapping saved = regionWarehouseMappingRepository.save(regionWarehouseMapping);
////
////            organizationRegionsRepository.findAll()
////                    .forEach(organizationRegion -> {
////                        saved.addRegion(organizationRegion);
////                        organizationRegion.setRegionWarehouseMapping(saved);
////                        organizationRegionsRepository.save(organizationRegion);
////                    });
////
////            log.info("Region Warehouse Mapping created for warehouse: {}", warehouse.getWarehouseName());
////
////            RegionWarehouseMapping ss = regionWarehouseMappingRepository.save(saved);
////
////            if (ss.getMappedRegions().isEmpty()) {
////
////                log.info("No regions mapped to warehouse: {}", warehouse.getWarehouseName());
////                //delete
////                regionWarehouseMappingRepository.delete(ss);
////            }
////
////        }
//
//
//        //  fetch sales order in planning and placed status, update them and inventory
//
//
//        salesOrdersRepository.findAll().forEach(salesOrders ->{
//
//            if(salesOrders.getSalesOrderStatus() == LoadingStatusEnum.ORDER_PLACED ||
//                    salesOrders.getSalesOrderStatus() == LoadingStatusEnum.PLANNING) {
//
//                Warehouse warehouse = salesOrders.getWarehouse();
//                if(warehouse != null) {
//                    log.info("Processing Sales Order: {} for Warehouse: {}", salesOrders.getSalesOrderNumber(), warehouse.getWarehouseName());
//                    salesOrders.setWarehouse(warehouse);
//
//                    salesOrders.getSalesOrderItems().forEach(salesOrderItem -> {
//                        //
//                        inventoryHelper.updateInventory(salesOrderItem.getProduct(),warehouse.getPublicId(), (long) salesOrderItem.getQuantity(), StockType.ReservedStock, UpdateType.Increment,false);
//                        log.info("Updated Inventory for Product: {} in Warehouse: {} with Quantity: {}", salesOrderItem.getProduct().getProductName(), warehouse.getWarehouseName(), salesOrderItem.getQuantity());
//                    });
//
//                }
//                log.info("Updated Sales Order: {}", salesOrders.getSalesOrderNumber());
//            }
//
//        });
//
//
//
//    }
//
//
//}
