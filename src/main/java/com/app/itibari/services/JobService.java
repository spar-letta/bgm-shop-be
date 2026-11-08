package com.app.itibari.services;

import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;

import java.time.LocalDateTime;
import java.util.Set;

public interface JobService {

    void runSupplierJob() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException,
                                 JobRestartException, InterruptedException;

    void runDeliveryAgentJob() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException,
                                      JobParametersInvalidException, JobRestartException, InterruptedException;

    void runProductJob() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException,
                                JobRestartException, InterruptedException;

    void runProductSkuJob() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException,
                                   JobRestartException, InterruptedException;

    void runActiveCustomerJob(String message, Set<String> variables, String campaignName, boolean sms, boolean whatsapp) throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException,
                                   JobRestartException, InterruptedException;

    void runBnplEligibleCustomerJob(String message, Set<String> variables, String campaignName, boolean sms, boolean whatsapp) throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException,
                                   JobRestartException, InterruptedException;
}
