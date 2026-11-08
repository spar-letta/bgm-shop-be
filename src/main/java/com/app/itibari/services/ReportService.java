package com.app.itibari.services;

import com.app.itibari.dataTypes.StatementExportType;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.report.*;
import com.app.itibari.models.response.*;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.text.DocumentException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ReportService {

    ByteArrayOutputStream generatePurchaseOrderReport(UUID purchaseOrderId, String name) throws DocumentException, IOException;

    ByteArrayOutputStream generateReturnNoteReport(UUID publicId, String name, StatementExportType exportType) throws DocumentException, IOException;

    ByteArrayOutputStream generateReceiveNoteReport(UUID publicId, String name, StatementExportType exportType) throws DocumentException, IOException;

    ByteArrayOutputStream generateIssuanceReport(UUID routePublicId, StatementExportType exportType, String name) throws DocumentException, IOException;

    ByteArrayOutputStream generateSalesOrdersReport(StatementExportType exportType,
                                                    java.util.List<com.app.itibari.models.entities.SalesOrders> content,
                                                    String name);

    ByteArrayOutputStream generateCustomersReport(StatementExportType exportType, List<Customers> content, String name);

    ByteArrayOutputStream generateProductsReport(List<Product> content, StatementExportType statementExportType, String name);

    SalesAgentReportPegable getSalesReport(LocalDate startDate, LocalDate endDate, LocalDate date, UUID salesAgentPublicId, String name);

    ByteArrayOutputStream generateSalesOrdersReportExport(LocalDate startDate,
                                                          LocalDate endDate,
                                                          LocalDate date,
                                                          UUID salesAgentPublicId,
                                                          StatementExportType statementExportType,
                                                          String name);

    SupplierDashboardAnalysis getSupplierDashboardAnalysis(String sku,
                                                           UUID regionPublicId,
                                                           UUID locationPublicId,
                                                           LocalDate startDate,
                                                           LocalDate endDate,
                                                           String name,
                                                           Pageable pageable);

    ProductDistributionPageableDTO getProductDistributionReport(UUID productId,
                                                                UUID categoryId,
                                                                UUID regionId,
                                                                UUID locationId,
                                                                UUID customerId,
                                                                LocalDate startDate,
                                                                LocalDate endDate,
                                                                String name,
                                                                Pageable pageable);

    Page<CustomerOrderDTORapper> getPurchaseReport(String searchParm, UUID regionId, LocalDate startDate, LocalDate endDate,
                                                   String name,
                                                   Pageable pageable);

    ByteArrayOutputStream generatePurchaseOrderReportExport(List<CustomerOrderDTORapper> content, StatementExportType exportType, String name);

    ResponseEntity<List<StatisticsDTO>> getSupplierStatisticsReport(UUID regionPublicId, LocalDate start, LocalDate end, String name, Pageable pageable);

    Page<ProductSaleDistributionDTO> generateSupplierDistributionReport(UUID regionPublicId, LocalDate start, LocalDate end, String name, Pageable pageable);

    ResponseEntity<List<QuantitySoldPerRegionDTO>> generateSupplierProductDistributionReport(UUID regionPublicId, LocalDate start, LocalDate end, String name, Pageable pageable);

    ResponseEntity<List<SalesRevenueDTO>> generateSupplierRevenueReport(UUID regionPublicId, Boolean monthly, Boolean quarterly, Boolean yearly, String name, Pageable pageable);

    ResponseEntity<List<ProductPerformanceDTO>> getProductPerformanceReport(UUID productId, UUID regionId, List<UUID> zones, LocalDate startDate, LocalDate endDate, String name, Pageable pageable);

    ResponseEntity<List<ProductPerformancePerZone>> getProductPerformancePerZoneReport(UUID productId, UUID regionId, List<UUID> zones, LocalDate startDate, LocalDate endDate, String name, Pageable pageable);

    Page<ProductCustomerPerformanceDTO> getProductCustomerPerformanceReport(UUID productId,
                                                                            UUID regionId, UUID locationId, LocalDate startDate, LocalDate endDate, String name, Pageable pageable);

    Map<String, Integer> generateSupplierTotalOrdersReport(UUID regionPublicId, LocalDate start, LocalDate end, String name);

    Map<String, Integer> getQuantitySoldReport(UUID regionPublicId, LocalDate start, LocalDate end, String name);

    Map<String, Integer> getTotalActiveCustomers(UUID regionPublicId, LocalDate start, LocalDate end, String name);

    Map<String, BigDecimal> runSupplierRevenueReport(UUID regionId, LocalDate startDate, LocalDate endDate, String name, Pageable pageable);

    ByteArrayOutputStream productCustomerPerformanceExport(UUID productId, UUID regionId, UUID locationId,
                                                           LocalDate startDate, LocalDate endDate, String name, StatementExportType statementExportType, String name1, Pageable pageable);

    ByteArrayOutputStream generateLoanManagementReportExport(List<BnplLoanManagement> content, StatementExportType exportType, String email);

    ByteArrayOutputStream generateBNPLCustomerListReportExport(List<BNPLCustomerResponseDTO> content, StatementExportType exportType, String name);

    ByteArrayOutputStream generateBNPLEligibleCustomerListReportExport(List<BNPLEligibleCustomerDTO> content, StatementExportType exportType, String name);

    ByteArrayOutputStream generateCreditReconReportExport(List<CreditRecon> content, StatementExportType exportType, String name);

    ByteArrayOutputStream generateCustomerCreditItemsReportExport(List<CreditLimitItem> content, StatementExportType exportType, String name);

    ByteArrayOutputStream generateCustomerCreditPaymentReportExport(List<BNPLPaymentResponse> content, StatementExportType exportType, String name);

    ByteArrayOutputStream generateBnplSalesOrdersReport(StatementExportType exportType, List<SalesOrders> content, String name);

    ByteArrayOutputStream generateWalletReportExport(List<WalletResponseDTO> content, StatementExportType exportType, String name);

    InventoryDashboardSummaryDTO getInventoryDashboardSummary(UUID regionId, LocalDate startDate, LocalDate endDate);

    List<InventoryQuantityByProductDTO> getInventoryDashboardQuantityByProduct(UUID regionId, LocalDate startDate, LocalDate endDate);

    List<InventoryRegionDistributionDTO> getInventoryDashboardDistributionByRegion(UUID regionId, LocalDate startDate, LocalDate endDate);

    Page<InventoryDashboardDetailDTO> getInventoryDashboardDetails(UUID regionId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    List<BestProductBoughtDto> getCustomerMostBoughtProductReport(UUID customerPublicId, String name);

    ByteArrayOutputStream generateLocationApprovalExport(List<LocationApproval> content, StatementExportType exportType, String name);

    ByteArrayOutputStream getLeadExportReport(List<BnplLeadResponse> content, StatementExportType exportType, String name);

    ByteArrayOutputStream downloadCashCreditReconReport(CashCreditReconReportDTO cashCreditReconReportDTO, StatementExportType exportType, String name);

    ByteArrayOutputStream getAllSalesOrdersForSAExport(SalesAgentOrderPageable salesAgentOrderPageable, StatementExportType exportType);

    ByteArrayOutputStream generateCreditLimitAdjustmentsReportExport(List<BnplCreditLimitAdjustmentResponse> content, StatementExportType exportType, String name);
}
