//package com.app.itibari.runner;
//
//import com.app.itibari.repository.SupplierRepository;
//import com.app.itibari.services.JobService;
//import com.app.itibari.services.ProductService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//
//@Order(2)
//@Component
//@Slf4j
//public class BatchRunner implements CommandLineRunner {
//    private final SupplierRepository supplierRepository;
//    private final JobService jobService;
//    private final ProductService productService;
//
//    public BatchRunner(SupplierRepository supplierRepository, JobService jobService, ProductService productService) {
//        this.supplierRepository = supplierRepository;
//        this.jobService = jobService;
//        this.productService = productService;
//    }
//
//    @Override
//    public void run(String... args) throws Exception {
//        if (supplierRepository.count() <= 12) {
//            jobService.runSupplierJob();
//
//            log.info("Supplier job started");
//        }
//
//
//        try {
//            productService.updateProductStock();
//
//        }catch (Exception e){
//            log.error("Error in batch runner {}",e.getMessage());
//        }
//    }
//}
