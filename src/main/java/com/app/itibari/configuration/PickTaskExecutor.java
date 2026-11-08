package com.app.itibari.configuration;

import com.app.itibari.services.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class PickTaskExecutor {

    private final TaskExecutor taskExecutor;
    private final ProductService productService;
    private final SalesOrdersService salesOrdersService;
    private final HelpDeskService helpDeskService;
    private final CustomerExperienceService customerExperienceService;
    private final DeliveryAgentsService deliveryAgentsService;
    private ScheduledExecutorService scheduler;

    public PickTaskExecutor(@Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor,
                            ProductService productService,
                            SalesOrdersService salesOrdersService,
                            HelpDeskService helpDeskService,
                            CustomerExperienceService customerExperienceService,
                            DeliveryAgentsService deliveryAgentsService) {
        this.taskExecutor = taskExecutor;
        this.productService = productService;
        this.salesOrdersService = salesOrdersService;
        this.helpDeskService = helpDeskService;
        this.customerExperienceService = customerExperienceService;
        this.deliveryAgentsService = deliveryAgentsService;
    }

    @PostConstruct
    public void init() {
        scheduler = Executors.newScheduledThreadPool(3);
        // 03:30 — heaviest job; spaced 90 min after the 02:00 AOV recalculation
        scheduleDailyTask(this::runMidnightTask, 3, 30, "Stock rollover task (3:30 AM)");
        scheduleDaytimeTask(); // runs every 10 minutes between 6 AM and 8 PM
        scheduleDailyTask(this::run830PMTask, 20, 30, "8:30 PM task");
    }

    @PreDestroy
    public void shutdown() {
        if (scheduler != null) {
            log.info("Shutting down scheduler...");
            scheduler.shutdown();
        }
    }

    // ---------------- Daily Task Scheduler ----------------
    private void scheduleDailyTask(Runnable task, int hour, int minute, String label) {
        ZoneId zone = ZoneId.of("Africa/Nairobi");
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime nextRun = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0);

        if (now.isAfter(nextRun)) {
            // Missed today's run → execute immediately once
            scheduler.execute(() -> {
                log.info("Catch-up run: {} at {}", label, ZonedDateTime.now(zone));
                try {
                    task.run();
                } catch (Exception e) {
                    log.error("Catch-up run failed for {}", label, e);
                }
            });
            // Schedule for tomorrow
            nextRun = nextRun.plusDays(1);
        }

        long initialDelay = Duration.between(now, nextRun).toMillis();
        long period = TimeUnit.DAYS.toMillis(1);

        scheduler.scheduleAtFixedRate(() -> {
            try {
                taskExecutor.execute(task);
            } catch (Exception e) {
                log.error("{} failed", label, e);
            }
        }, initialDelay, period, TimeUnit.MILLISECONDS);

        log.info("{} scheduled. First run in {} ms, repeating every 24h.", label, initialDelay);
    }

    // ---------------- Daytime Task Scheduler ----------------
    private void scheduleDaytimeTask() {
        Runnable daytimeWrapper = () -> {
            try {
                ZoneId zone = ZoneId.of("Africa/Nairobi");
                LocalTime now = LocalTime.now(zone);

                // Only run between 6:00 AM and 8:00 PM
                if (!now.isBefore(LocalTime.of(6, 0)) && !now.isAfter(LocalTime.of(20, 0))) {
                    taskExecutor.execute(this::runDaytimeTask);
                }
            } catch (Exception e) {
                log.error("Scheduled daytime task failed", e);
            }
        };

        long period = TimeUnit.MINUTES.toMillis(10); // every 10 minutes
        scheduler.scheduleAtFixedRate(daytimeWrapper, 0, period, TimeUnit.MILLISECONDS);
        log.info("Daytime scheduler initialized. Runs every 10 minutes between 6 AM and 8 PM Nairobi.");
    }

    // ---------------- Task Implementations ----------------
    private void runMidnightTask() {
        log.info("--- Running midnight task ---");
        LocalDate today = LocalDate.now(ZoneId.of("Africa/Nairobi"));
        try {
            productService.updateProductStock(today);
        } catch (Exception e) {
            log.error("Live stock opening rollover failed", e);
        }
        try {
            productService.runProductStockByNextDay(null);
        } catch (Exception e) {
            log.error("Product stock next-day snapshot failed", e);
        }
        try {
            helpDeskService.runNotifyFollowUpGuy();
        } catch (Exception e) {
            log.error("Help desk follow-up notify failed", e);
        }
        log.info("--- Finished midnight task ---");
    }

    private void runDaytimeTask() {
        log.info("--- Running daytime task ---");
        try {
            salesOrdersService.notifyCustomerWithSmsOrderInPlanningAndTransit();
        } catch (Exception e) {
            log.error("Daytime task error", e);
        }
        log.info("--- Finished daytime task ---");
    }

    private void run830PMTask() {
        log.info("--- Running 8:30 PM task ---");
        try {
            customerExperienceService.sendCustomerNotesToDepartmentOfficers();
            deliveryAgentsService.sendAmendmentToDepartmentOfficers();
        } catch (Exception e) {
            log.error("8:30 PM task error", e);
        }
        log.info("--- Finished 8:30 PM task ---");
    }
}