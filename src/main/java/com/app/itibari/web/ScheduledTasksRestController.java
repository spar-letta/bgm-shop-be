package com.app.itibari.web;

import com.app.itibari.dataTypes.CustomerActiveStatus;
import com.app.itibari.repository.CustomersRepository;
import com.app.itibari.services.BnplCreditLimitAdjustmentService;
import com.app.itibari.services.BnplCrmService;
import com.app.itibari.services.CustomerAppDownloadStatService;
import com.app.itibari.services.CustomersService;
import com.app.itibari.services.SalesOrdersService;
import com.app.itibari.services.SupplierPortalCustomerLocationsSnapshotService;
import com.app.itibari.services.helper.BnplHelper;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Slf4j
@Hidden
@SecurityRequirements
@RequestMapping("/scheduledTasks")
@RestController
public class ScheduledTasksRestController {

    private final SalesOrdersService salesOrdersService;
    private final CustomersRepository customersRepository;
    private final CustomersService customersService;
    private final BnplHelper bnplHelper;
    private final BnplCreditLimitAdjustmentService bnplCreditLimitAdjustmentService;
    private final BnplCrmService bnplCrmService;
    private final SupplierPortalCustomerLocationsSnapshotService supplierPortalCustomerLocationsSnapshotService;
    private final CustomerAppDownloadStatService customerAppDownloadStatService;

    public ScheduledTasksRestController(SalesOrdersService salesOrdersService, CustomersRepository customersRepository, CustomersService customersService, BnplHelper bnplHelper, BnplCreditLimitAdjustmentService bnplCreditLimitAdjustmentService, BnplCrmService bnplCrmService, SupplierPortalCustomerLocationsSnapshotService supplierPortalCustomerLocationsSnapshotService, CustomerAppDownloadStatService customerAppDownloadStatService) {
        this.salesOrdersService = salesOrdersService;
        this.customersRepository = customersRepository;
        this.customersService = customersService;
        this.bnplHelper = bnplHelper;
        this.bnplCreditLimitAdjustmentService = bnplCreditLimitAdjustmentService;
        this.bnplCrmService = bnplCrmService;
        this.supplierPortalCustomerLocationsSnapshotService = supplierPortalCustomerLocationsSnapshotService;
        this.customerAppDownloadStatService = customerAppDownloadStatService;
    }

    @PostMapping("/inventoryTask")
    public ResponseEntity<?> createInventoryTask() {
        log.info("=========Received request to create inventory task");
        //
        return ResponseEntity.ok().build();
    }

    @PostMapping("/updateOrdersPlannings")
    public ResponseEntity<?> updateOrdersPlannings() {
        salesOrdersService.moveOrdersToPlanning();
        return ResponseEntity.ok().build();
    }

    // customer update task

    @PostMapping("/updateCustomersActiveStatus")
    public ResponseEntity<?> updateCustomersActiveStatus() {
        log.info("=========Received request to update customers active status");

        log.info("Customers active status update started");   // and aov recalculation

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LocalDateTime thirtyOneDayAgo = LocalDateTime.now().minusDays(31);
        customersRepository.updateAllCustomersActiveStatus(thirtyDaysAgo,thirtyOneDayAgo, CustomerActiveStatus.Active.name(),CustomerActiveStatus.Inactive.name(),CustomerActiveStatus.New.name());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/customerAovCalculation")
    public ResponseEntity<?> customerAovCalculation() {
        customersService.recalculateAov();
        return ResponseEntity.ok().build();
    }

    // bnl fine

    @PostMapping("/bnplFineCalculation")
    public ResponseEntity<?> bnplFineCalculation() {
        log.info("=========Received request to calculate BNPL fines");
        bnplHelper.recalculateBnplFine();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bnplCreditLimitAdjustment")
    public ResponseEntity<?> bnplCreditLimitAdjustment() {
        log.info("=========Received request to adjust BNPL credit limits");
        int adjusted = bnplCreditLimitAdjustmentService.adjustCreditLimits();
        return ResponseEntity.ok(adjusted);
    }

//    @PostMapping("/notifyCustomerWithSmsOrderInPlanningAndTransit")
//    public ResponseEntity<?> notifyCustomerWithSmsOrderInPlanningAndTransit() throws IOException, InterruptedException {
//        salesOrdersService.notifyCustomerWithSmsOrderInPlanningAndTransit();
//        return ResponseEntity.ok().build();
//    }


    @PostMapping("/updatesLeads")
    public ResponseEntity<?> updateLeads() {
        log.info("=========Received request to update leads");
        bnplCrmService.createLeads();

        log.info("Leads updated");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refreshSupplierPortalCustomerLocationSnapshot")
    public ResponseEntity<?> refreshSupplierPortalCustomerLocationSnapshot() {
        log.info("=========Received request to refresh supplier portal customer location snapshot");
        supplierPortalCustomerLocationsSnapshotService.refreshSnapshot();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/importCustomerAppDownloadStats")
    public ResponseEntity<?> importCustomerAppDownloadStats() {
        log.info("=========Received request to import customer app download stats");
        customerAppDownloadStatService.importMissingCustomerAppDownloads();
        return ResponseEntity.ok().build();
    }
}
