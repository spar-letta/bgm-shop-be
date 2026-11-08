//package com.app.itibari.runner;
//
//import com.app.itibari.dataTypes.CustomerType;
//import com.app.itibari.repository.*;
//import com.app.itibari.services.CustomersService;
//import com.app.itibari.services.helper.BnplHelper;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//
//import java.math.BigDecimal;
//
//@Component
//@Slf4j
////@Order(1)
//public class BnplRunner implements CommandLineRunner {
//    private final CustomersService customersService;
//    private final BnplHelper bnplHelper;
//    private final WalletRepository walletRepository;
//    private final CustomersRepository customersRepository;
//    private final SalesOrdersRepository salesOrdersRepository;
//    private final CreditLimitItemRepository creditLimitItemRepository;
//    private final BnplLeadRepository bnplLeadRepository;
//
//    public BnplRunner(CustomersService customersService, BnplHelper bnplHelper, WalletRepository walletRepository, CustomersRepository customersRepository, SalesOrdersRepository salesOrdersRepository, CreditLimitItemRepository creditLimitItemRepository, BnplLeadRepository bnplLeadRepository) {
//        this.customersService = customersService;
//        this.bnplHelper = bnplHelper;
//        this.walletRepository = walletRepository;
//        this.customersRepository = customersRepository;
//        this.salesOrdersRepository = salesOrdersRepository;
//        this.creditLimitItemRepository = creditLimitItemRepository;
//        this.bnplLeadRepository = bnplLeadRepository;
//    }
//
//    @Override
//    public void run(String... args) throws Exception {
//        log.info("BNPL Runner executed");
//
//        bnplLeadRepository.findAllByPotentialCreditLimitIsLessThan(new BigDecimal("1.00")).forEach(bnplLead -> {
//            if (bnplLead.getCustomer() != null) {
//                BigDecimal aov = bnplLead.getCustomer().getPreliminaryValue();
//                if (aov != null) {
//                    CustomerType customerType = bnplLead.getCustomer().getCustomerType() != null ? bnplLead.getCustomer().getCustomerType() : null;
//                    BigDecimal creditLimitForCustomer = bnplHelper.getCreditLimitForCustomer(customerType, bnplLead.getCustomer().getPreliminaryValue());
//
//                    bnplLead.setPotentialCreditLimit(creditLimitForCustomer);
//                    bnplLeadRepository.save(bnplLead);
//                    log.info("BNPL Lead Saved Successfully for {}", bnplLead.getCustomer().getPhoneNumber());
//                }
//            }
//        });
//
//        log.info("Customers AOV recalculation ended");
//
//    }
//}
