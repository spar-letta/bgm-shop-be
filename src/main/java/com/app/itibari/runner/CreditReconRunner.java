//package com.app.itibari.runner;
//
//import com.app.itibari.models.entities.CreditRecon;
//import com.app.itibari.models.entities.Reconciliation;
//import com.app.itibari.repository.CreditLimitItemRepository;
//import com.app.itibari.repository.CreditReconRepository;
//import com.app.itibari.repository.ReconciliationRepository;
//import com.app.itibari.repository.SalesOrdersRepository;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//
//import java.util.Optional;
//
//@Component
//@Slf4j
////Order(1)
//public class CreditReconRunner implements CommandLineRunner {
//
//    private final CreditLimitItemRepository creditLimitItemRepository;
//    private final SalesOrdersRepository salesOrdersRepository;
//    private final CreditReconRepository creditReconRepository;
//    private final ReconciliationRepository reconciliationRepository;
//
//    public CreditReconRunner(CreditLimitItemRepository creditLimitItemRepository, SalesOrdersRepository salesOrdersRepository, CreditReconRepository creditReconRepository, ReconciliationRepository reconciliationRepository) {
//        this.creditLimitItemRepository = creditLimitItemRepository;
//        this.salesOrdersRepository = salesOrdersRepository;
//        this.creditReconRepository = creditReconRepository;
//        this.reconciliationRepository = reconciliationRepository;
//    }
//
//    @Override
//    public void run(String... args) throws Exception {
//        log.info("==========Starting to update credit recons with reconciliations==========");
//
//        creditReconRepository.findAll().forEach(creditRecon -> {
//       // Optional<CreditRecon> optionalCreditRecon = creditReconRepository.findByOrderNumberAndDeletedIsFalse("ORD166932");
//
//        //if(optionalCreditRecon.isPresent()) {
//         //   CreditRecon creditRecon = optionalCreditRecon.get();
//            log.info("==========Updating credit recon with id: {} and order number: {}", creditRecon.getRouteId(), creditRecon.getOrderNumber());
//            Reconciliation reconciliation = reconciliationRepository.findByDeliveryIdIgnoreCase(creditRecon.getRouteId());
//            if (reconciliation != null) {
//                log.info("==========Found reconciliation with id: {} for credit recon with route: {}", reconciliation.getId(), creditRecon.getRouteId());
//                creditRecon.setReconciliation(reconciliation);
//                CreditRecon savedCreditRecon = creditReconRepository.save(creditRecon);
//
//                reconciliation.addCreditRecon(savedCreditRecon);
//                reconciliationRepository.save(reconciliation);
//            }
//
//            log.info("==========Updated credit recon with id: {} and order number: {}", creditRecon.getRouteId(), creditRecon.getOrderNumber());
//        //}
//       });
//
//        log.info("==========Finished updating credit recons with reconciliations==========");
//    }
//}
