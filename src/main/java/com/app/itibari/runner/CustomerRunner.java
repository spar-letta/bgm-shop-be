//package com.app.itibari.runner;
//
//import com.app.itibari.models.entities.Customers;
//import com.app.itibari.repository.CustomersRepository;
//import com.app.itibari.services.CustomersService;
//import com.app.itibari.utils.helpers.PhoneNumberHelper;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Sort;
//import org.springframework.stereotype.Component;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Objects;
//
//@Component
//@Slf4j
//public class CustomerRunner implements CommandLineRunner {
//
//    private final CustomersService customersService;
//    private final CustomersRepository customersRepository;
//
//    public CustomerRunner(CustomersService customersService, CustomersRepository customersRepository) {
//        this.customersService = customersService;
//        this.customersRepository = customersRepository;
//    }
//
//    @Override
//    public void run(String... args) throws Exception {
//        log.info("CustomerRunner start");
//        PageRequest pageRequest = PageRequest.of(0, 500, Sort.by("id"));
//        Page<Customers> page;
//        int updatedCustomers = 0;
//        do {
//            page = customersRepository.findByDeletedIsFalse(pageRequest);
//            List<Customers> changedCustomers = new ArrayList<>();
//            for (Customers customer : page.getContent()) {
//                String phoneNumber = normalizePhoneNumber(customer.getPhoneNumber());
//                String ownerNumber = normalizePhoneNumber(customer.getOwnerNumber());
//                String shopKeeperNumber = normalizePhoneNumber(customer.getShopKeeperNumber());
//                if (!Objects.equals(phoneNumber, customer.getPhoneNumber())
//                        || !Objects.equals(ownerNumber, customer.getOwnerNumber())
//                        || !Objects.equals(shopKeeperNumber, customer.getShopKeeperNumber())) {
//                    customer.setPhoneNumber(phoneNumber);
//                    customer.setOwnerNumber(ownerNumber);
//                    customer.setShopKeeperNumber(shopKeeperNumber);
//                    changedCustomers.add(customer);
//                }
//            }
//            if (!changedCustomers.isEmpty()) {
//                customersRepository.saveAll(changedCustomers);
//                updatedCustomers += changedCustomers.size();
//            }
//            pageRequest = pageRequest.next();
//        } while (page.hasNext());
//        log.info("CustomerRunner end: normalized phone numbers for {} customers", updatedCustomers);
//    }
//
//    private String normalizePhoneNumber(String phoneNumber) {
//        return phoneNumber == null || phoneNumber.isBlank()
//                ? phoneNumber
//                : PhoneNumberHelper.toInternationalFormat(phoneNumber);
//    }
//}
