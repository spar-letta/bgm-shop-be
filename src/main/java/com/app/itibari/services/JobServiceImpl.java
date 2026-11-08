package com.app.itibari.services;

import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class JobServiceImpl implements JobService {

    private final JobLauncher jobLauncher;

    private final ApplicationContext applicationContext;

    public JobServiceImpl(JobLauncher jobLauncher, ApplicationContext applicationContext) {

        this.jobLauncher = jobLauncher;
        this.applicationContext = applicationContext;
    }

    @Override
    public void runSupplierJob() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException,
                                        JobParametersInvalidException, JobRestartException, InterruptedException {

        Job job = (Job) applicationContext.getBean("ImportSupplierJob");

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("JobID", String.valueOf(System.currentTimeMillis()))
                .toJobParameters();

        var jobExecution = jobLauncher.run(job, jobParameters);

        var batchStatus = jobExecution.getStatus();
        while (batchStatus.isRunning()) {
            System.out.println("Still running...");
            Thread.sleep(5000L);
        }
    }

    @Override
    public void runDeliveryAgentJob() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException,
                                             JobParametersInvalidException, JobRestartException, InterruptedException {

        Job job = (Job) applicationContext.getBean("ImportDeliveryAgentsJob");

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("JobID", String.valueOf(System.currentTimeMillis()))
                .toJobParameters();

        var jobExecution = jobLauncher.run(job, jobParameters);

        var batchStatus = jobExecution.getStatus();
        while (batchStatus.isRunning()) {
            System.out.println("Still running...");
            Thread.sleep(5000L);
        }
    }

    @Override
    public void runProductJob() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException,
                                       JobParametersInvalidException, JobRestartException, InterruptedException {

        Job job = (Job) applicationContext.getBean("ImportProductsJob");

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("JobID", String.valueOf(System.currentTimeMillis()))
                .toJobParameters();

        var jobExecution = jobLauncher.run(job, jobParameters);

        var batchStatus = jobExecution.getStatus();
        while (batchStatus.isRunning()) {
            System.out.println("Still running...");
            Thread.sleep(5000L);
        }
    }

    @Override
    public void runProductSkuJob() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException,
                                          JobParametersInvalidException, JobRestartException, InterruptedException {

        Job job = (Job) applicationContext.getBean("ImportSkuJob");

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("JobID", String.valueOf(System.currentTimeMillis()))
                .toJobParameters();

        var jobExecution = jobLauncher.run(job, jobParameters);

        var batchStatus = jobExecution.getStatus();
        while (batchStatus.isRunning()) {
            System.out.println("Still running...");
            Thread.sleep(5000L);
        }
    }

    @Override
    public void runActiveCustomerJob(String message, Set<String> variables, String campaignName, boolean sms, boolean whatsapp) throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException, InterruptedException {
        Job job = (Job) applicationContext.getBean("ActiveCustomerScheduleJob");

        JobParameters jobParameters = buildSmsJobParameters(message, variables, campaignName, sms, whatsapp);

        var jobExecution = jobLauncher.run(job, jobParameters);

        var batchStatus = jobExecution.getStatus();
        while (batchStatus.isRunning()) {
            System.out.println("Still running...");
            Thread.sleep(5000L);
        }
    }

    @Override
    public void runBnplEligibleCustomerJob(String message, Set<String> variables, String campaignName, boolean sms, boolean whatsapp) throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException, InterruptedException {
        Job job = (Job) applicationContext.getBean("BnplEligibleCustomerScheduleJob");

        JobParameters jobParameters = buildSmsJobParameters(message, variables, campaignName, sms, whatsapp);

        var jobExecution = jobLauncher.run(job, jobParameters);

        var batchStatus = jobExecution.getStatus();
        while (batchStatus.isRunning()) {
            System.out.println("Still running...");
            Thread.sleep(5000L);
        }
    }

    private JobParameters buildSmsJobParameters(String message, Set<String> variables, String campaignName, boolean sms, boolean whatsapp) {
        JobParametersBuilder builder = new JobParametersBuilder()
                .addString("JobID", String.valueOf(System.currentTimeMillis()))
                .addString("message", message)
                .addString("sms", String.valueOf(sms))
                .addString("whatsapp", String.valueOf(whatsapp));

        if (variables != null && !variables.isEmpty()) {
            builder.addString("variables", String.join(",", variables));
        }

        if (campaignName != null) {
            builder.addString("campaignName", campaignName);
        }

        return builder.toJobParameters();
    }
}
