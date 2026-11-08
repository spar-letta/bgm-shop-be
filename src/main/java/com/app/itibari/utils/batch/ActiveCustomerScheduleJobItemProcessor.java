package com.app.itibari.utils.batch;

import com.app.itibari.models.entities.Customers;
import org.springframework.batch.item.ItemProcessor;

public class ActiveCustomerScheduleJobItemProcessor implements ItemProcessor<Customers, Customers> {

    @Override
    public Customers process(Customers customers) throws Exception {

        return customers;
    }
}
