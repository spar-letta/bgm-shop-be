package com.app.itibari.scheduler;

import com.app.itibari.dataTypes.CustomerActiveStatus;
import com.app.itibari.repository.CustomersRepository;
import com.app.itibari.services.*;
import com.app.itibari.services.helper.BnplHelper;
import com.app.itibari.services.helper.DailyStockRecordChecker;
import com.app.itibari.services.helper.StockDriftDetector;
import com.app.itibari.services.helper.StockHealthNotifier;
import com.app.itibari.services.helper.StockHealthReport;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@AllArgsConstructor
@Slf4j
public class Scheduler {

    private final SalesOrdersService salesOrdersService;
    private final ProductService productService;
    private final CustomersService customersService;
    private final DeliveryAgentsService deliveryAgentsService;
    private final BnplHelper bnplHelper;
    private final BnplCreditLimitAdjustmentService bnplCreditLimitAdjustmentService;
    private final CustomersRepository customersRepository;
    private final BnplCrmService bnplCrmService;
    private final SupplierPortalCustomerLocationsSnapshotService supplierPortalCustomerLocationsSnapshotService;
    private final StockDriftDetector stockDriftDetector;
    private final StockHealthNotifier stockHealthNotifier;
    private final DailyStockRecordChecker dailyStockRecordChecker;
    private final CustomerAppDownloadStatService customerAppDownloadStatService;


    @Scheduled(cron = "0 */5 * * * *", zone = "Africa/Nairobi")
    public void runOrderPlanning() {
        log.info("Order planning started");
        salesOrdersService.moveOrdersToPlanning();
    }

    // run after every day at 00:00
    @Scheduled(cron = "0 0 0 * * *", zone = "Africa/Nairobi")
    public void customersActiveStatusUpdate() {
        log.info("Customers active status update started");   // and aov recalculation

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LocalDateTime thirtyOneDayAgo = LocalDateTime.now().minusDays(31);
        customersRepository.updateAllCustomersActiveStatus(thirtyDaysAgo, thirtyOneDayAgo, CustomerActiveStatus.Active.name(), CustomerActiveStatus.Inactive.name(), CustomerActiveStatus.New.name());
    }

//    @Scheduled(cron = "0 5 0 * * *", zone = "Africa/Nairobi")
//    @Transactional
//    public void productStockUpdate() {
//        ZoneId nairobiZone = ZoneId.of("Africa/Nairobi");
//
//        Instant nowUtc = Instant.now();
//        LocalDate nairobiDate = LocalDate.now(nairobiZone);
//
//        log.info("---------Current UTC time: {}", nowUtc);
//        log.info("------Product stock update started at 0:00 AM Nairobi time ({}).", nairobiDate);
//
////        productService.updateProductStock(nairobiDate);
//    }

//    @Scheduled(cron = "0 1 0 * * *", zone = "Africa/Nairobi")
//    @Transactional
//    public void productStockNew() {
//        ZoneId nairobiZone = ZoneId.of("Africa/Nairobi");
//
//        Instant nowUtc = Instant.now();
//        LocalDate nairobiDate = LocalDate.now(nairobiZone);
//
//        log.info("---------Current UTC time: {}", nowUtc);
//        log.info("------Product stock new update started at 0:00 AM Nairobi time ({}).", nairobiDate);
//

    ////        productService.runProductStockByNextDay(null);
//    }
    @Scheduled(cron = "0 0 2 * * *", zone = "Africa/Nairobi")
    // @Scheduled(cron = "0 */5 * * * *")
    public void customersAovRecalculation() {
        log.info("Customers AOV recalculation started");
        customersService.recalculateAov();
    }


    // 05:00 — after the 03:30 stock rollover window
    @Scheduled(cron = "0 0 5 * * *", zone = "Africa/Nairobi")
    public void bnplFineRecalculation() {
        log.info("BNPL fine recalculation started");
        bnplHelper.recalculateBnplFine();
        log.info("BNPL fine recalculation finished=========");

    }

    @Scheduled(cron = "0 30 5 * * *", zone = "Africa/Nairobi")
    public void bnplCreditLimitAdjustment() {
        log.info("BNPL credit limit adjustment started");
        int adjusted = bnplCreditLimitAdjustmentService.adjustCreditLimits();
        log.info("BNPL credit limit adjustment finished. Adjusted {} wallet(s)", adjusted);
    }

    @Scheduled(cron = "0 0 10 * * *", zone = "Africa/Nairobi")
    public void sendBnplPaymentReminders() {
        log.info("BNPL payment reminder SMS job started");
        bnplHelper.sendScheduledPaymentReminders();
        log.info("BNPL payment reminder SMS job finished=========");
    }


    // 06:15 — after AOV (leads depend on preliminaryValue) and clear of the 05:00 BNPL slot
    @Scheduled(cron = "0 15 6 * * *", zone = "Africa/Nairobi")
    public void leadUpdates() {
        log.info("Lead updates started");
        bnplCrmService.createLeads();
        log.info("Lead updates finished=========");
    }

    // 04:30 — read-only stock health check (layer drift + low stock), after the 03:30 stock
    // rollover and before 05:00 BNPL fines. Findings go to the logs and the stock G-Chat space.
    @Scheduled(cron = "0 30 4 * * *", zone = "Africa/Nairobi")
    public void stockHealthReport() {
        try {
            StockHealthReport report = stockDriftDetector.runStockHealthCheck(LocalDate.now(ZoneId.of("Africa/Nairobi")));
            stockHealthNotifier.send(report);
        } catch (Exception e) {
            log.error("Stock health report failed", e);
        }
    }

    // 04:40 — write missing today's product_stocks / live_stock_value rows for active products.
    // Copies yesterday when present; otherwise creates from inventory closing stock.
    @Scheduled(cron = "0 40 4 * * *", zone = "Africa/Nairobi")
    public void ensureDailyStockRecords() {
        log.info("Daily stock record checker started");
        try {
            dailyStockRecordChecker.ensureTodayRecords(LocalDate.now(ZoneId.of("Africa/Nairobi")));
            log.info("Daily stock record checker finished=========");
        } catch (Exception e) {
            log.error("Daily stock record checker failed", e);
        }
    }

    // 01:00 — moved off the 02:00 slot so it no longer runs concurrently with AOV recalculation
    @Scheduled(cron = "0 0 1 * * *", zone = "Africa/Nairobi")
    public void refreshSupplierPortalCustomerLocationSnapshot() {
        log.info("Supplier portal customer location snapshot refresh started");
        try {
            supplierPortalCustomerLocationsSnapshotService.refreshSnapshot();
            log.info("Supplier portal customer location snapshot refresh finished=========");
        } catch (RuntimeException ex) {
            log.error("Supplier portal customer location snapshot refresh failed; stale snapshot retained", ex);
        }
    }

    @Scheduled(cron = "0 0 6 8 * *", zone = "Africa/Nairobi")
    public void importCustomerAppDownloadStats() {
        log.info("Customer app download stats import started");
        try {
            customerAppDownloadStatService.importMissingCustomerAppDownloads();
            log.info("Customer app download stats import finished");
        } catch (RuntimeException exception) {
            log.error("Customer app download stats import failed", exception);
        }
    }

}
