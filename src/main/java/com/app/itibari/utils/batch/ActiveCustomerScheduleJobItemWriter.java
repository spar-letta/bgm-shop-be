package com.app.itibari.utils.batch;

import com.app.itibari.models.entities.Customers;
import com.app.itibari.services.HelpDeskService;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class ActiveCustomerScheduleJobItemWriter implements ItemWriter<Customers>, InitializingBean {

    @Autowired
    private HelpDeskService helpDeskService;

    private String message;
    private Set<String> variables;
    private String campaignName;
    private boolean sms;
    private boolean whatsapp;

    @BeforeStep
    public void beforeStep(final StepExecution stepExecution) {
        JobParameters parameters = stepExecution.getJobExecution().getJobParameters();
        message = parameters.getString("message");
        String joinedVariables = parameters.getString("variables");
        variables = joinedVariables == null || joinedVariables.isBlank()
                ? new HashSet<>()
                : new HashSet<>(Arrays.asList(joinedVariables.split("\\s*,\\s*")));
        campaignName = parameters.getString("campaignName");
        sms = !"false".equals(parameters.getString("sms"));
        whatsapp = "true".equals(parameters.getString("whatsapp"));
    }


    @Override
    public void write(@NonNull Chunk<? extends Customers> chunk) throws Exception {
        for (Customers customer : chunk) {
            if (sms) {
                log.info("---Sending SMS to customer: {},  with message {}, variables {}",
                        customer.getName(), message, variables);
                helpDeskService.sendSmsToActiveCustomers(customer, message, variables, campaignName);
            }
            if (whatsapp) {
                log.info("---Sending WhatsApp to customer: {},  with message {}, variables {}",
                        customer.getName(), message, variables);
                helpDeskService.sendWhatsAppToActiveCustomers(customer, message, variables);
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }
}
