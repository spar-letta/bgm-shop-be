package com.app.itibari.services;

import com.app.itibari.dataTypes.*;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.*;
import com.app.itibari.models.response.ProductItemDTO;
import com.app.itibari.models.response.ReconRouteSummary;
import com.app.itibari.repository.ProductStockRapperResponse;
import com.itextpdf.io.source.ByteArrayOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface WarehouseService {

    Warehouse createWarehouse(@Valid WarehouseRequest warehouseRequest, String name);

    void updateWarehouse(UUID warehousePublicId, @Valid WarehouseRequest warehouseRequest, String name);

    Warehouse getWarehouse(UUID warehousePublicId);

    Page<Warehouse> getAllWarehouses(String searchParam, WarehouseType warehouseType, Pageable pageable);

    void deleteWarehouse(UUID warehousePublicId, String name);

    Page<Warehouse> getMobileWarehousePerMain(UUID warehousePublicId, WarehouseType warehouseType, Pageable pageable);

    void addRemoveMobileWarehouse(UUID warehousePublicId, @Valid WarehouseRequest warehouseRequest, ActionType action, String name);

    void loadWarehouseProducts(UUID agentPublicId, @Valid LoadingRequest loadingRequest, String name);

    Page<ProductRecon> getAllProductRecons(String searchParam,
                                           String deliveryAgentPublicId,
                                           String deliveryId,
                                           String vehicleNumber,
                                           Boolean reconciled,
                                           String warehousePublicId,
                                           String from,
                                           String to,
                                           ReconStatus status,
                                           UUID createdBy,
                                           Boolean isWalkIn,
                                           Pageable pageable);

    Page<ProductItem> getAllProductItemsFromRecon(UUID publicId, String searchParam, Pageable pageable);

    void reconcileProductRecon(UUID publicId, @Valid ProductReconAction productReconAction, String name);

    void reconcileProductReconAction(UUID publicId, String name);

    Page<ProductItem> getAllProductItemsFromLoaded(UUID agentPublicId, String searchParam, String deliveryId, Pageable pageable);

    void loadVanSalesProducts(@Valid LoadVanSaleRequest loadVanSaleRequest, String name);

    ProductRecon reLoadProductRecon(DeliveryAgents deliveryAgent, RoutePlanning routePlanning);

    void createReconciliation(ProductRecon productRecon, String routePublicId, boolean update);

    Warehouse createQuarantineWarehouse(UUID warehousePublicId, String name);

    void transferProductsToQuarantineWarehouse(UUID warehousePublicId,
                                               TransferType transferType,
                                               @Valid TransferProductsToQuarantineWarehouseRequest transferProductsToQuarantineWarehouseRequest,
                                               String name);

    void updateProductReconSoldProduct(UUID publicId,
                                       @Valid UpdateProductReconSoldProductRequest updateProductReconSoldProductRequest,
                                       String name);

    void updateReconDiscount(ProductRecon savedProductRecon, String routePublicId);

    void stageReturns(UUID productReconPublicId, @Valid CreateStageRequest stageReturnsRequest, String name);

    Page<ReturnStaging> getAllReturnStaging(String searchParam, Pageable pageable);

    void approveStagedReturn(UUID returnStagePublicId, String name);

    Page<ReconciliationStaging> getReconciliationStagingItems(String searchParam, LocalDate startDate,
                                                              LocalDate endDate, Pageable pageable);

    void stagingActions(UUID publicId, @Valid StagingActionRequest stagingActionRequest, String name, UUID itemPublicId);

    Page<ReconciliationStagingItem> getStagingItems(UUID publicId, Pageable pageable);

    ByteArrayOutputStream generateStagingReportExport(LocalDate startDate, LocalDate endDate, String searchParam, StatementExportType statementExportType, String name, Pageable pageable);

    Page<StockMovementItem> getAllStockMovements(String searchParam, UUID warehousePublicId, StockMovementTransactionType transactionType, List<StockMovementTransactionType> transactionTypes, LocalDate startDate, LocalDate endDate, Pageable pageable);

    Page<ProductStock> getAllProductStocks(String searchParam, UUID warehousePublicId, LocalDate startDate, LocalDate endDate, ActiveType activeType, Pageable pageable);

    Page<ProductStockRapperResponse> getProductStockRapper(String searchParam, UUID warehousePublicId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    void downloadProductStockTemplate(UUID warehousePublicId, HttpServletResponse response, String prefix, Principal principal);

    void uploadProductStockExcel(InputStream inputStream, Principal loggedInUserDetails) throws IOException;

    void revertReconcileProductRecon(UUID publicId, UUID reasonPublicId, String name);

    void createUpdateStock(UUID wareHouseId, List<ProductItemDTO> loadedProducts);

     void createProductReconciliation(RoutePlanning delivery, List<ProductItemDTO> productItemDTOS,UUID productReconPublicId);

    ReconRouteSummary getAllProductReconSummary(String routeId);
}
