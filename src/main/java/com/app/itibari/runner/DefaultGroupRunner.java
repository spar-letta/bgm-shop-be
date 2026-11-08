package com.app.itibari.runner;

import com.app.itibari.models.entities.SmsGroup;
import com.app.itibari.repository.SmsGroupRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

@Order(6)
@Component
@Slf4j
public class DefaultGroupRunner implements CommandLineRunner {

    @Autowired
    private SmsGroupRepository smsGroupRepository;

    @Override
    public void run(String... args) throws Exception {

        Map<String, String> defaultNames = Map.of("Cooporate", "COOPERATE",
                "Regular Credit", "REGULAR_CREDIT",
                "Supplier", "SUPPLIER",
                "Delivery Agent", "DELIVERY_AGENT",
                "Sales Agent", "SALES_AGENT",
                "All Customers", "ALL_CUSTOMERS",
                "Buy Now Pay Later", "BNPL"
        );

        defaultNames.forEach((key, value) -> {
            SmsGroup smsGroup = smsGroupRepository.findByNameIgnoreCaseAndDeletedIsFalse(key).orElse(null);
            if (smsGroup == null) {
                SmsGroup smsGroup1 = new SmsGroup();
                smsGroup1.setName(key);
                smsGroup1.setDefaultGroup(Boolean.TRUE);
                smsGroup1.setIsActive(Boolean.TRUE);
                smsGroup1.setDefaultName(value);
                smsGroupRepository.save(smsGroup1);
            }
        });
    }
}
