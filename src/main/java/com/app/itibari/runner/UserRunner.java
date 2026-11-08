//package com.app.itibari.runner;
//
//import com.app.itibari.models.entities.Permission;
//import com.app.itibari.models.entities.PermissionGroup;
//import com.app.itibari.models.entities.Person;
//import com.app.itibari.models.entities.Role;
//import com.app.itibari.repository.*;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.core.annotation.Order;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//
//import java.util.Optional;
//
//@Transactional
//@Component
//@Slf4j
//@Order(1)
//@RequiredArgsConstructor
//public class UserRunner implements CommandLineRunner {
//    private final RoleRepository roleRepository;
//    private final PersonRepository personRepository;
//    private final PasswordEncoder passwordEncoder;
//    private final PermissionRepository permissionRepository;
//    private final PermissionGroupRepository permissionGroupRepository;
//    private final DeliveryAgentsRepository deliveryAgentsRepository;
//    private final VehicleRepository vehicleRepository;
//
//
//    @Override
//    public void run(String... args) throws Exception {
//        if (personRepository.count() < 100) {
//
//            // warehouse permissions
//            PermissionGroup warehouseGroup = createPermissionGroup("Warehouse");
//
//            Permission loadProducts = createPermission("LOAD_PRODUCTS", warehouseGroup);
//            Permission viewRecons = createPermission("VIEW_PRODUCT_RECONCILIATIONS", warehouseGroup);
//            Permission updateRecons = createPermission("UPDATE_PRODUCT_RECONCILIATIONS", warehouseGroup);
//            Permission getWarehouse = createPermission("GET_WAREHOUSE", warehouseGroup);
//            Permission deleteWarehouse = createPermission("DELETE_WAREHOUSE", warehouseGroup);
//            Permission createWarehouse = createPermission("CREATE_WAREHOUSE", warehouseGroup);
//
//            //finance permissions
//
//            PermissionGroup financeGroup = createPermissionGroup("Finance");
//            Permission viewFinance = createPermission("VIEW_FINANCE", financeGroup);
//            Permission updateFinance = createPermission("UPDATE_FINANCE", financeGroup);
//            Permission deleteFinance = createPermission("DELETE_FINANCE", financeGroup);
//            Permission createFinance = createPermission("CREATE_FINANCE", financeGroup);
//            Permission financeUpdateReconciliation = createPermission("FINANCE_UPDATE_RECONCILIATION", financeGroup);
//            Permission financeGetReconciliation = createPermission("FINANCE_GET_RECONCILIATION", financeGroup);
//            Permission financeApprovePurchaseOrder = createPermission("FINANCE_APPROVE_PURCHASE_ORDER", financeGroup);
//
//
//            // Reports
//
//            PermissionGroup reportsGroup = createPermissionGroup("Reports");
//            Permission viewPurchaseOrderReport = createPermission("VIEW_PURCHASE_ORDER_REPORT", reportsGroup);
//            Permission viewSalesReport = createPermission("VIEW_SALES_ORDERS_REPORT", reportsGroup);
//            Permission viewCustomerReport = createPermission("VIEW_CUSTOMERS_REPORT", reportsGroup);
//            // create rejection codes
//
//            PermissionGroup rejectionCodeGroup = createPermissionGroup("Rejection Codes");
//            Permission createRejectionCode = createPermission("CREATE_REJECTION_CODE", rejectionCodeGroup);
//            Permission updateRejectionCode = createPermission("UPDATE_REJECTION_CODE", rejectionCodeGroup);
//            Permission deleteRejectionCode = createPermission("DELETE_REJECTION_CODE", rejectionCodeGroup);
//            Permission viewRejectionCode = createPermission("VIEW_REJECTION_CODE", rejectionCodeGroup);
//
//            // Product group
//
//            PermissionGroup productGroup = createPermissionGroup("Product");
//            Permission createProduct = createPermission("CREATE_PRODUCT", productGroup);
//            Permission updateProduct = createPermission("UPDATE_PRODUCT", productGroup);
//            Permission deleteProduct = createPermission("DELETE_PRODUCT", productGroup);
//            Permission viewProduct = createPermission("VIEW_PRODUCT", productGroup);
//            Permission createProductAdjustment = createPermission("CREATE_PRODUCT_ADJUSTMENT", productGroup);
//            Permission updateProductAdjustment = createPermission("UPDATE_PRODUCT_ADJUSTMENT", productGroup);
//            Permission deleteProductAdjustment = createPermission("DELETE_PRODUCT_ADJUSTMENT", productGroup);
//            Permission viewProductAdjustment = createPermission("VIEW_PRODUCT_ADJUSTMENT", productGroup);
//
//            // Product category
//            PermissionGroup productCategoryGroup = createPermissionGroup("Product Category");
//            Permission createProductCategory = createPermission("CREATE_PRODUCT_CATEGORY", productCategoryGroup);
//            Permission updateProductCategory = createPermission("UPDATE_PRODUCT_CATEGORY", productCategoryGroup);
//            Permission deleteProductCategory = createPermission("DELETE_PRODUCT_CATEGORY", productCategoryGroup);
//            Permission viewProductCategory = createPermission("VIEW_PRODUCT_CATEGORY", productCategoryGroup);
//
//            // Product class and subclass
//
//            PermissionGroup productClassGroup = createPermissionGroup("Product Class");
//            Permission createProductClass = createPermission("CREATE_PRODUCT_CLASS", productClassGroup);
//            Permission updateProductClass = createPermission("UPDATE_PRODUCT_CLASS", productClassGroup);
//            Permission deleteProductClass = createPermission("DELETE_PRODUCT_CLASS", productClassGroup);
//            Permission viewProductClass = createPermission("VIEW_PRODUCT_CLASS", productClassGroup);
//
//
//            // permission groups
//            PermissionGroup userGroup = createPermissionGroup("User");
//            Permission permissions = createPermission("PERMISSIONS", userGroup);
//            Permission viewPermissions = createPermission("VIEW_PERMISSIONS", userGroup);
//            Permission updatePermissions = createPermission("UPDATE_PERMISSIONS", userGroup);
//            Permission deletePermissions = createPermission("DELETE_PERMISSIONS", userGroup);
//            Permission createPermissions = createPermission("CREATE_PERMISSIONS", userGroup);
//            Permission viewPermissionGroup = createPermission("VIEW_PERMISSION_GROUP", userGroup);
//            Permission updatePermissionGroup = createPermission("UPDATE_PERMISSION_GROUP", userGroup);
//            Permission deletePermissionGroup = createPermission("DELETE_PERMISSION_GROUP", userGroup);
//            Permission createPermissionGroup = createPermission("CREATE_PERMISSION_GROUP", userGroup);
//            Permission createRole = createPermission("CREATE_ROLE", userGroup);
//            Permission updateRole = createPermission("UPDATE_ROLE", userGroup);
//            Permission deleteRole = createPermission("DELETE_ROLE", userGroup);
//            Permission viewRole = createPermission("VIEW_ROLE", userGroup);
//            Permission createUser = createPermission("CREATE_PERSON", userGroup);
//            Permission updateUser = createPermission("UPDATE_PERSON", userGroup);
//            Permission deleteUser = createPermission("DELETE_PERSON", userGroup);
//            Permission viewUser = createPermission("VIEW_PERSON", userGroup);
//
//
//            // procurement
//
//            PermissionGroup procurementGroup = createPermissionGroup("Procurement");
//            Permission createPurchaseOrder = createPermission("CREATE_PURCHASE_ORDER", procurementGroup);
//            Permission updatePurchaseOrder = createPermission("UPDATE_PURCHASE_ORDER", procurementGroup);
//            Permission deletePurchaseOrder = createPermission("DELETE_PURCHASE_ORDER", procurementGroup);
//            Permission viewPurchaseOrder = createPermission("VIEW_PURCHASE_ORDER", procurementGroup);
//            Permission createPurchaseOrderItem = createPermission("CREATE_PURCHASE_ORDER_ITEM", procurementGroup);
//            Permission updatePurchaseOrderItem = createPermission("UPDATE_PURCHASE_ORDER_ITEM", procurementGroup);
//            Permission deletePurchaseOrderItem = createPermission("DELETE_PURCHASE_ORDER_ITEM", procurementGroup);
//            Permission viewPurchaseOrderItem = createPermission("VIEW_PURCHASE_ORDER_ITEM", procurementGroup);
//            Permission createSupplier = createPermission("CREATE_SUPPLIER", procurementGroup);
//            Permission updateSupplier = createPermission("UPDATE_SUPPLIER", procurementGroup);
//            Permission deleteSupplier = createPermission("DELETE_SUPPLIER", procurementGroup);
//            Permission viewSupplier = createPermission("VIEW_SUPPLIER", procurementGroup);
//            Permission receiveGoods = createPermission("RECEIVE_GOODS", procurementGroup);
//            Permission approveGoods = createPermission("APPROVE_PURCHASE_ORDER", procurementGroup);
//
//
//            //price management
//
//            PermissionGroup priceManagementGroup = createPermissionGroup("Price Management");
//            Permission createPriceManagement = createPermission("CREATE_PRICE_MANAGEMENT", priceManagementGroup);
//            Permission updatePriceManagement = createPermission("UPDATE_PRICE_MANAGEMENT", priceManagementGroup);
//            Permission deletePriceManagement = createPermission("DELETE_PRICE_MANAGEMENT", priceManagementGroup);
//            Permission viewPriceManagement = createPermission("VIEW_PRICE_MANAGEMENT", priceManagementGroup);
//            Permission uploadPriceManagement = createPermission("UPLOAD_PRICE_MANAGEMENT", priceManagementGroup);
//
//
//// goods return note
//
//            PermissionGroup goodsReturnNoteGroup = createPermissionGroup("Goods Return Note");
//            Permission createGoodsReturnNote = createPermission("CREATE_GOODS_RETURN_NOTE", goodsReturnNoteGroup);
//            Permission updateGoodsReturnNote = createPermission("UPDATE_GOODS_RETURN_NOTE", goodsReturnNoteGroup);
//            Permission deleteGoodsReturnNote = createPermission("DELETE_GOODS_RETURN_NOTE", goodsReturnNoteGroup);
//            Permission viewGoodsReturnNote = createPermission("VIEW_GOODS_RETURN_NOTE", goodsReturnNoteGroup);
//
//            // view inventory
//
//            PermissionGroup inventoryGroup = createPermissionGroup("Inventory");
//            Permission viewInventory = createPermission("VIEW_INVENTORY", inventoryGroup);
//
//            // Logistics,
//            PermissionGroup logisticsGroup = createPermissionGroup("Logistics");
//            Permission createVehicle = createPermission("CREATE_VEHICLE", logisticsGroup);
//            Permission updateVehicle = createPermission("UPDATE_VEHICLE", logisticsGroup);
//            Permission deleteVehicle = createPermission("DELETE_VEHICLE", logisticsGroup);
//            Permission viewVehicle = createPermission("VIEW_VEHICLE", logisticsGroup);
//            Permission createCutOffTime = createPermission("CREATE_ORDER_CUT_OFF_TIME", logisticsGroup);
//            Permission updateCutOffTime = createPermission("UPDATE_ORDER_CUT_OFF_TIME", logisticsGroup);
//            Permission deleteCutOffTime = createPermission("DELETE_ORDER_CUT_OFF_TIME", logisticsGroup);
//            Permission viewCutOffTime = createPermission("VIEW_ORDER_CUT_OFF_TIME", logisticsGroup);
//            Permission createRoutePlanning = createPermission("CREATE_ROUTE_PLANNING", logisticsGroup);
//            Permission updateRoutePlanning = createPermission("UPDATE_ROUTE_PLANNING", logisticsGroup);
//            Permission deleteRoutePlanning = createPermission("DELETE_ROUTE_PLANNING", logisticsGroup);
//            Permission viewRoutePlanning = createPermission("VIEW_ROUTE_PLANNING", logisticsGroup);
//            Permission viewRouteOrder = createPermission("VIEW_ROUTE_ORDER", logisticsGroup);
//            Permission viewReturnNoteReport = createPermission("VIEW_RETURN_NOTE_REPORT", logisticsGroup);
//
//
//            // Delivery Agent
//
//            PermissionGroup deliveryAgentGroup = createPermissionGroup("Delivery Agent");
//            Permission createDeliveryAgent = createPermission("CREATE_DELIVERY_AGENT", deliveryAgentGroup);
//            Permission updateDeliveryAgent = createPermission("UPDATE_DELIVERY_AGENT", deliveryAgentGroup);
//            Permission deleteDeliveryAgent = createPermission("DELETE_DELIVERY_AGENT", deliveryAgentGroup);
//            Permission viewDeliveryAgent = createPermission("VIEW_DELIVERY_AGENT", deliveryAgentGroup);
//            Permission approveAmendmentOrder = createPermission("APPROVE_DELIVERY_AGENT", deliveryAgentGroup);
//
//            // van sales
//
//            PermissionGroup vanSalesGroup = createPermissionGroup("Van Sales");
//            Permission createVanSale = createPermission("CREATE_VANSALE", vanSalesGroup);
//            Permission updateVanSale = createPermission("UPDATE_VANSALE", vanSalesGroup);
//            Permission deleteVanSale = createPermission("DELETE_VANSALE", vanSalesGroup);
//            Permission viewVanSale = createPermission("VIEW_VANSALE", vanSalesGroup);
//
//
//            //views
//            PermissionGroup viewsGroup = createPermissionGroup("Views");
//            Permission viewSalesModule = createPermission("VIEW_SALES_MODULE", viewsGroup);
//            Permission viewMarketingModule = createPermission("VIEW_MARKETING_MODULE", viewsGroup);
//            Permission viewCommercialsModule = createPermission("VIEW_COMMERCIALS_MODULE", viewsGroup);
//            Permission viewProcurementModule = createPermission("VIEW_PROCUREMENT_MODULE", viewsGroup);
//            Permission viewWarehouseModule = createPermission("VIEW_WAREHOUSE_MODULE", viewsGroup);
//            Permission viewAccountingModule = createPermission("VIEW_ACCOUNTING_MODULE", viewsGroup);
//            Permission viewLogisticsModule = createPermission("VIEW_LOGISTICS_MODULE", viewsGroup);
//            Permission viewAdministrationModule = createPermission("VIEW_ADMINISTRATION_MODULE", viewsGroup);
//
//
//            PermissionGroup quarantineGroup = createPermissionGroup("Quarantine");
//            Permission createQuarantine = createPermission("CREATE_QUARANTINE", quarantineGroup);
//            Permission updateQuarantine = createPermission("UPDATE_QUARANTINE", quarantineGroup);
//            Permission deleteQuarantine = createPermission("DELETE_QUARANTINE", quarantineGroup);
//            Permission viewQuarantine = createPermission("GET_QUARANTINE", quarantineGroup);
//            Permission approveQuarantine = createPermission("APPROVE_QUARANTINE", quarantineGroup);
//            Permission returnQuarantineProduct = createPermission("RETURN_QUARANTINE_PRODUCT", quarantineGroup);
//
//            // bnpl
//
//            PermissionGroup bnplGroup = createPermissionGroup("BNPL");
//            Permission createDocumentRule = createPermission("CREATE_DOCUMENT_RULE", bnplGroup);
//            Permission updateDocumentRule = createPermission("UPDATE_DOCUMENT_RULE", bnplGroup);
//            Permission deleteDocumentRule = createPermission("DELETE_DOCUMENT_RULE", bnplGroup);
//            Permission viewDocumentRule = createPermission("READ_DOCUMENT_RULE", bnplGroup);
//
//            Permission createCreditRule = createPermission("CREATE_CREDIT_RULE", bnplGroup);
//            Permission deleteCreditRule = createPermission("DELETE_CREDIT_RULE", bnplGroup);
//            Permission viewCreditRule = createPermission("READ_CREDIT_RULE", bnplGroup);
//
//            Permission bnplViewDashboard = createPermission("BNPL_VIEW_DASHBOARD", bnplGroup);
//            Permission bnplViewCustomer = createPermission("BNPL_VIEW_CUSTOMER", bnplGroup);
//            Permission bnplViewViewCustomerList = createPermission("BNPL_VIEW_CUSTOMER_LIST", bnplGroup);
//            Permission bnplViewCustomerOrders = createPermission("BNPL_VIEW_CUSTOMER_ORDERS", bnplGroup);
//            Permission bnplViewElligibleCustomers = createPermission("BNPL_VIEW_ELLIGIBLE_CUSTOMERS", bnplGroup);
//            Permission bnplViewCustomerFeedback = createPermission("BNPL_VIEW_CUSTOMER_FEEDBACK", bnplGroup);
//            Permission bnplViewReasonCodes = createPermission("BNPL_VIEW_REASON_CODES", bnplGroup);
//            Permission bnplViewLeadList = createPermission("BNPL_VIEW_LEAD_LIST", bnplGroup);
//            Permission bnplViewChurnedCustomers = createPermission("BNPL_VIEW_CHURNED_CUSTOMERS", bnplGroup);
//
//            Permission bnplViewCreditManagement = createPermission("BNPL_VIEW_CREDIT_MANAGEMENT", bnplGroup);
//            Permission bnplViewCreditStatus = createPermission("BNPL_VIEW_CREDIT_STATUS", bnplGroup);
//            Permission bnplViewCreditSettings = createPermission("BNPL_VIEW_CREDIT_SETTINGS", bnplGroup);
//            Permission bnplViewKycDocumentSettings = createPermission("BNPL_VIEW_KYC_DOCUMENT_SETTINGS", bnplGroup);
//            Permission bnplViewLoanManagement = createPermission("BNPL_VIEW_LOAN_MANAGEMENT", bnplGroup);
//            Permission bnplViewWallet = createPermission("BNPL_VIEW_WALLET", bnplGroup);
//            Permission bnplViewPayments = createPermission("BNPL_VIEW_PAYMENTS", bnplGroup);
//            Permission bnplViewSystemRoles = createPermission("BNPL_VIEW_SYSTEM_ROLES", bnplGroup);
//            Permission bnplViewSystemPermissionGroup = createPermission("BNPL_VIEW_SYSTEM_PERMISSION_GROUP", bnplGroup);
//            Permission bnplViewSystemUsers = createPermission("BNPL_VIEW_SYSTEM_USERS", bnplGroup);
//            Permission bnplViewSystemCountries = createPermission("BNPL_VIEW_SYSTEM_COUNTRIES", bnplGroup);
//            Permission bnplViewSystemRegions = createPermission("BNPL_VIEW_SYSTEM_REGIONS", bnplGroup);
//            Permission bnplViewSystemDistricts = createPermission("BNPL_VIEW_SYSTEM_DISTRICTS", bnplGroup);
//            Permission bnplViewSystemWards = createPermission("BNPL_VIEW_SYSTEM_WARDS", bnplGroup);
//            Permission bnplViewSystemDepartments = createPermission("BNPL_VIEW_SYSTEM_DEPARTMENTS", bnplGroup);
//            Permission bnplViewSystemReasonCodes = createPermission("BNPL_VIEW_SYSTEM_REASON_CODES", bnplGroup);
//
//
//            // sub-modules
//            PermissionGroup subModulesGroup = createPermissionGroup("Sub Modules");
//            Permission viewSalesOrderSubModule = createPermission("VIEW_SALES_ORDER_SUB_MODULE", subModulesGroup);
//            Permission viewCustomerSubModule = createPermission("VIEW_CUSTOMER_SUB_MODULE", subModulesGroup);
//            Permission viewSalesAgentSubModule = createPermission("VIEW_SALES_AGENT_SUB_MODULE", subModulesGroup);
//            Permission viewVanSalesSubModule = createPermission("VIEW_VAN_SALES_SUB_MODULE", subModulesGroup);
//            Permission viewCommunicationSubModule = createPermission("VIEW_COMMUNICATION_SUB_MODULE", subModulesGroup);
//            Permission viewProductSubModule = createPermission("VIEW_PRODUCT_SUB_MODULE", subModulesGroup);
//            Permission viewSupplierSubModule = createPermission("VIEW_SUPPLIER_SUB_MODULE", subModulesGroup);
//            Permission viewCategorySubModule = createPermission("VIEW_CATEGORY_SUB_MODULE", subModulesGroup);
//            Permission viewClassSubModule = createPermission("VIEW_CLASS_SUB_MODULE", subModulesGroup);
//            Permission viewPriceManagementSubModule = createPermission("VIEW_PRICE_MANAGEMENT_SUB_MODULE", subModulesGroup);
//            Permission viewPurchaseOrderSubModule = createPermission("VIEW_PURCHASE_ORDER_SUB_MODULE", subModulesGroup);
//            Permission viewGoodsReturnNoteSubModule = createPermission("VIEW_GOODS_RETURN_NOTE_SUB_MODULE", subModulesGroup);
//            Permission viewWarehouseSubModule = createPermission("VIEW_WAREHOUSE_SUB_MODULE", subModulesGroup);
//            Permission viewGoodsReceiveNoteSubModule = createPermission("VIEW_GOODS_RECEIVE_NOTE_SUB_MODULE", subModulesGroup);
//            Permission viewProductReconciliationSubModule = createPermission("VIEW_PRODUCT_RECONCILIATION_SUB_MODULE", subModulesGroup);
//            Permission viewIssueManagementSubModule = createPermission("VIEW_ISSUE_MANAGEMENT_SUB_MODULE", subModulesGroup);
//            Permission viewInventorySubModule = createPermission("VIEW_INVENTORY_SUB_MODULE", subModulesGroup);
//            Permission viewRemittanceSubModule = createPermission("VIEW_REMITTANCE_SUB_MODULE", subModulesGroup);
//            Permission viewTaxManagementSubModule = createPermission("VIEW_TAX_MANAGEMENT_SUB_MODULE", subModulesGroup);
//            Permission viewFinancePurchaseOrderSubModule = createPermission("VIEW_FINANCE_PURCHASE_ORDER_SUB_MODULE", subModulesGroup);
//            Permission viewFinanceCashReconciliationSubModule = createPermission("VIEW_FINANCE_CASH_RECONCILIATION_SUB_MODULE", subModulesGroup);
//            Permission viewLogisticOrderSubModule = createPermission("VIEW_LOGISTIC_ORDER_SUB_MODULE", subModulesGroup);
//            Permission viewLogisticRoutePlanningSubModule = createPermission("VIEW_LOGISTIC_ROUTE_PLANNING_SUB_MODULE", subModulesGroup);
//            Permission viewLogisticDeliveryAgentSubModule = createPermission("VIEW_LOGISTIC_DELIVERY_AGENT_SUB_MODULE", subModulesGroup);
//            Permission viewLogisticVehicleSubModule = createPermission("VIEW_LOGISTIC_VEHICLE_SUB_MODULE", subModulesGroup);
//            Permission viewLogisticCutOffTimeSubModule = createPermission("VIEW_LOGISTIC_CUT_OFF_TIME_SUB_MODULE", subModulesGroup);
//            Permission viewBNPLModule = createPermission("VIEW_BNPL", subModulesGroup);
//            Permission viewSystemModule = createPermission("VIEW_SYSTEM_MODULE", subModulesGroup);
//            Permission viewImsDashboard = createPermission("VIEW_IMS_DASHBOARD", subModulesGroup);
//
//
//            PermissionGroup supplierPortalDashboardGroup = createPermissionGroup("Supplier Portal Dashboard");
//            Permission viewDashboard = createPermission("VIEW_DASHBOARD", supplierPortalDashboardGroup);
//            Permission viewSpDashboard = createPermission("VIEW_SP_DASHBOARD", supplierPortalDashboardGroup);
//            Permission selectRegion = createPermission("SELECT_REGION", supplierPortalDashboardGroup);
//            Permission selectDate = createPermission("SELECT_DATE", supplierPortalDashboardGroup);
//            Permission productSalesDistribution = createPermission("PRODUCT_SALES_DISTRIBUTION", supplierPortalDashboardGroup);
//            Permission quantitySoldPerRegion = createPermission("QUANTITY_SOLD_PER_REGION", supplierPortalDashboardGroup);
//            Permission salesRevenueTrend = createPermission("SALES_REVENUE_TREND", supplierPortalDashboardGroup);
//
//
//            PermissionGroup supplierPortalProductsGroup = createPermissionGroup("Supplier Portal Products");
//            Permission selectProduct = createPermission("SELECT_PRODUCT", supplierPortalProductsGroup);
//            Permission viewSpProduct = createPermission("VIEW_SP_PRODUCTS", supplierPortalProductsGroup);
//            Permission selectProductDate = createPermission("SELECT_PRODUCT_DATE", supplierPortalProductsGroup);
//            Permission selectProductRegion = createPermission("SELECT_PRODUCT_REGION", supplierPortalProductsGroup);
//            Permission exportProductsXls = createPermission("EXPORT_PRODUCTS_XLS", supplierPortalProductsGroup);
//            Permission productPerformance = createPermission("PRODUCT_PERFORMANCE", supplierPortalProductsGroup);
//            Permission productZoneBreakdown = createPermission("PRODUCT_ZONE_BREAKDOWN", supplierPortalProductsGroup);
//            Permission orderValuePerZone = createPermission("ORDER_VALUE_PER_ZONE", supplierPortalProductsGroup);
//
//
//            PermissionGroup supplierPortalSalesGroup = createPermissionGroup("Supplier Portal Sales");
//            Permission viewSpSales = createPermission("VIEW_SP_SALES", supplierPortalSalesGroup);
//            Permission exportSalesXls = createPermission("EXPORT_SALES_XLS", supplierPortalSalesGroup);
//            Permission printSales = createPermission("PRINT_SALES", supplierPortalSalesGroup);
//            Permission downloadSales = createPermission("DOWNLOAD_SALES", supplierPortalSalesGroup);
//            Permission selectSalesDate = createPermission("SELECT_SALES_DATE", supplierPortalSalesGroup);
//            Permission selectSalesRegion = createPermission("SELECT_SALES_REGION", supplierPortalSalesGroup);
//            Permission searchSales = createPermission("SEARCH_SALES", supplierPortalSalesGroup);
//
//
//            PermissionGroup supplierPortalCustomerDistributionGroup = createPermissionGroup("Supplier Portal Customer Distribution");
//            Permission viewSpCustomerDistribution = createPermission("VIEW_SP_CUSTOMER_LIST", supplierPortalCustomerDistributionGroup);
//            Permission searchCustomerDistribution = createPermission("SEARCH_CUSTOMER_DISTRIBUTION", supplierPortalCustomerDistributionGroup);
//            Permission selectCustomerDistributionDate = createPermission("SELECT_CUSTOMER_DISTRIBUTION_DATE", supplierPortalCustomerDistributionGroup);
//            Permission selectCustomerDistributionRegion = createPermission("SELECT_CUSTOMER_DISTRIBUTION_REGION", supplierPortalCustomerDistributionGroup);
//            Permission customerList = createPermission("CUSTOMER_LIST", supplierPortalCustomerDistributionGroup);
//            Permission mapView = createPermission("MAP_VIEW", supplierPortalCustomerDistributionGroup);
//            Permission exportCustomerDistributionXls = createPermission("EXPORT_CUSTOMER_DISTRIBUTION_XLS", supplierPortalCustomerDistributionGroup);
//
//            PermissionGroup crmGroup = createPermissionGroup("CRM");
//            Permission viewCrmTickets = createPermission("VIEW_CRM_TICKETS", crmGroup);
//            Permission viewCrmCommunications = createPermission("VIEW_CRM_COMMUNICATIONS", crmGroup);
//            Permission viewCrmSettings = createPermission("VIEW_CRM_SETTINGS", crmGroup);
//            Permission viewCrmOrderAmendments = createPermission("VIEW_CRM_ORDER_AMENDMENTS", crmGroup);
//
//            PermissionGroup marketingGroup = createPermissionGroup("MARKETING");
//            Permission viewMarketing = createPermission("VIEW_MARKETING", marketingGroup);
//            Permission viewMarketingOrder = createPermission("VIEW_MARKETING_ORDER", marketingGroup);
//            Permission viewMarketingCommunication = createPermission("VIEW_MARKETING_COMMUNICATION", marketingGroup);
//            Permission viewMarketingBanners = createPermission("VIEW_MARKETING_BANNERS", marketingGroup);
//            Permission viewMarketingInboundSupport = createPermission("VIEW_MARKETING_INBOUND_SUPPORT", marketingGroup);
//            Permission viewMarketingDiscounts = createPermission("VIEW_MARKETING_DISCOUNTS", marketingGroup);
//            Permission viewMarketingDiscountApprovals = createPermission("VIEW_MARKETING_DISCOUNT_APPROVALS", marketingGroup);
//            Permission viewMarketingChurnedCustomers = createPermission("VIEW_MARKETING_CHURNED_CUSTOMERS", marketingGroup);
//
//
//            // Create or get roles
////            Role adminRole = createRoleWithPermissions("ROLE_ADMIN", loadProducts, viewRecons,
////                    updateRecons, getWarehouse, deleteWarehouse, createWarehouse,
////                    viewFinance, updateFinance, deleteFinance, createFinance, financeUpdateReconciliation, financeGetReconciliation,
////                    viewPurchaseOrderReport, createRejectionCode, updateRejectionCode, deleteRejectionCode, viewRejectionCode,
////                    createProduct, updateProduct, deleteProduct, viewProduct,
////                    createProductCategory, updateProductCategory, deleteProductCategory, viewProductCategory,
////                    createProductClass, updateProductClass, deleteProductClass, viewProductClass,
////                    createPurchaseOrder, updatePurchaseOrder, deletePurchaseOrder, viewPurchaseOrder,
////                    createPurchaseOrderItem, updatePurchaseOrderItem, deletePurchaseOrderItem, viewPurchaseOrderItem,
////                    createSupplier, updateSupplier, deleteSupplier, viewSupplier,
////                    createUser, updateUser, deleteUser, viewUser,
////                    createRole, updateRole, deleteRole, viewRole,
////                    createPermissionGroup, updatePermissionGroup, deletePermissionGroup, viewPermissionGroup,
////                    createPermissions, updatePermissions, deletePermissions, viewPermissions,
////                    createPurchaseOrder, updatePurchaseOrder, deletePurchaseOrder, viewPurchaseOrder,
////                    createPurchaseOrderItem, updatePurchaseOrderItem, deletePurchaseOrderItem, viewPurchaseOrderItem,
////                    receiveGoods, approveGoods, permissions, financeApprovePurchaseOrder,
////                    createPriceManagement, updatePriceManagement, deletePriceManagement, viewPriceManagement, uploadPriceManagement,
////                    createGoodsReturnNote, updateGoodsReturnNote, deleteGoodsReturnNote, viewGoodsReturnNote,
////                    viewInventory,
////                    createVehicle, updateVehicle, deleteVehicle, viewVehicle,
////                    createCutOffTime, updateCutOffTime, deleteCutOffTime, viewCutOffTime,
////                    createRoutePlanning, updateRoutePlanning, deleteRoutePlanning, viewRoutePlanning,
////                    viewRouteOrder, createDeliveryAgent, updateDeliveryAgent, deleteDeliveryAgent, viewDeliveryAgent,
////                    approveAmendmentOrder, viewReturnNoteReport, createVanSale, updateVanSale, deleteVanSale, viewVanSale,
////                    createProductAdjustment, updateProductAdjustment, deleteProductAdjustment, viewProductAdjustment,
////                    viewSalesModule, viewMarketingModule, viewCommercialsModule, viewProcurementModule, viewWarehouseModule,
////                    viewAccountingModule, viewLogisticsModule, viewAdministrationModule,
////                    viewSalesOrderSubModule, viewCustomerSubModule, viewSalesAgentSubModule, viewVanSalesSubModule,
////                    viewCommunicationSubModule, viewProductSubModule, viewSupplierSubModule, viewCategorySubModule,
////                    viewClassSubModule, viewPriceManagementSubModule, viewPurchaseOrderSubModule, viewGoodsReturnNoteSubModule,
////                    viewWarehouseSubModule, viewGoodsReceiveNoteSubModule, viewProductReconciliationSubModule,
////                    viewIssueManagementSubModule, viewInventorySubModule, viewRemittanceSubModule, viewTaxManagementSubModule,
////                    viewFinancePurchaseOrderSubModule, viewFinanceCashReconciliationSubModule, viewLogisticOrderSubModule,
////                    viewLogisticRoutePlanningSubModule, viewLogisticDeliveryAgentSubModule, viewLogisticVehicleSubModule,
////                    viewLogisticCutOffTimeSubModule, viewCustomerReport, viewSalesReport, createQuarantine, updateQuarantine,
////                    deleteQuarantine, viewQuarantine, approveQuarantine, returnQuarantineProduct,
////                    createDocumentRule, updateDocumentRule, deleteDocumentRule, viewDocumentRule,
////                    createCreditRule, deleteCreditRule, viewCreditRule, bnplViewCreditManagement,
////                    bnplViewCreditStatus, bnplViewCreditSettings, bnplViewKycDocumentSettings,
////                    bnplViewLoanManagement, bnplViewWallet, bnplViewPayments,
////                    bnplViewSystemRoles, bnplViewSystemPermissionGroup, bnplViewSystemUsers,
////                    bnplViewSystemCountries, bnplViewSystemRegions, bnplViewSystemDistricts,
////                    bnplViewSystemWards, bnplViewSystemDepartments, bnplViewSystemReasonCodes,
////                    viewSystemModule,viewImsDashboard
////            );
//
////            createUserIfNotExists("o.chris@itibari.io", "Christopher", "Oigo",
////                    "+254714394472", "montella", adminRole);
////
////            createUserIfNotExists("i.cynthia@itibari.io", "Cynthia", "Isutsa",
////                    "+254727035487", "cynthia", adminRole);
////
////            createUserIfNotExists("e.massawe@itibari.io", "Erick", "Massawe",
////                    "+255764840059", "massawe", adminRole);
////
////            createUserIfNotExists("e.wangila@itibari.io", "Enock", "Wangila",
////                    "+254708761565", "wangila", adminRole);
////
////            createUserIfNotExists("r.gicheha@itibari.io", "Ruth", "Gicheha",
////                    "+254723297812", "gicheha", adminRole);
////
////            createUserIfNotExists("d.chirchir@itibari.io", "Dennis", "Chirchir",
////                    "+254705515476", "dennis", adminRole);
////
////            createUserIfNotExists("system@itibari.io", "Fabian", "Kimambo",
////                    "+255762385818", "kimambo", adminRole);
////
////            createUserIfNotExists("a.munyao@itibari.io", "Muathime", "Muathime",
////                    "+254706434259", "munyao", adminRole);
////
////            createUserIfNotExists("n.wamarite@itibari.io", "Njogu", "Wamarite",
////                    "+254706434259", "wamarite", adminRole);
////
////            createUserIfNotExists("itibari@gmail.com", "Itibari", "Itibari",
////                    "+234567890", "changed", adminRole);
//
////            createUserIfNotExists("c.thiane@itibari.io", "Chris", "Thiane",
////                    "+254703196270", "Thiane", adminRole);
//
//
////            Role mobileCustomerRole = createRoleWithPermissions("ROLE_CUSTOMER", viewProduct, viewProductCategory, viewProductClass,
////                    viewSupplier, viewVehicle);
////
////
////            Role purchaseOrderRole = createRoleWithPermissions("ROLE_PURCHASE_ORDER", viewProduct, viewProductCategory, viewProductClass,
////                    viewSupplier, viewVehicle, createPurchaseOrder, updatePurchaseOrder, deletePurchaseOrder, viewPurchaseOrder,
////                    createPurchaseOrderItem, updatePurchaseOrderItem, deletePurchaseOrderItem, viewPurchaseOrderItem,
////                    receiveGoods, approveGoods, createRejectionCode, updateRejectionCode, deleteRejectionCode, viewRejectionCode,
////                    createGoodsReturnNote, updateGoodsReturnNote, deleteGoodsReturnNote, viewGoodsReturnNote,
////                    createPriceManagement, updatePriceManagement, deletePriceManagement, viewPriceManagement, uploadPriceManagement,
////                    createVehicle, updateVehicle, deleteVehicle, viewVehicle,
////                    createCutOffTime, updateCutOffTime, deleteCutOffTime, viewCutOffTime,
////                    createRoutePlanning, updateRoutePlanning, deleteRoutePlanning, viewRoutePlanning,
////                    viewRouteOrder, viewInventory, approveAmendmentOrder, viewReturnNoteReport,viewSystemModule, createVanSale, updateVanSale, deleteVanSale, viewVanSale);
////
//
////            Role financeRole = createRoleWithPermissions("ROLE_FINANCE", viewFinance, updateFinance, deleteFinance,
////                    createFinance, financeUpdateReconciliation, financeGetReconciliation, financeApprovePurchaseOrder);
////
////
////            Role bnplRole = createRoleWithPermissions("ROLE_BNPL", createDocumentRule, updateDocumentRule,
////                    deleteDocumentRule, viewDocumentRule, createCreditRule,viewSystemModule, deleteCreditRule, viewCreditRule,viewBNPLModule);
////
////            Role deliveryAgentRole = createRoleWithPermissions("ROLE_DELIVERY_AGENT", viewDeliveryAgent, updateDeliveryAgent,
////                    deleteDeliveryAgent, viewDeliveryAgent, viewRouteOrder, viewVehicle, viewRoutePlanning, viewRejectionCode, viewVanSale);
////
////            Role salesAgentRole = createRoleWithPermissions("ROLE_SALES_AGENT", viewProduct, viewProductCategory, viewProductClass,
////                    viewSupplier, viewVehicle, createVanSale, updateVanSale, deleteVanSale, viewVanSale);
////
////            Role userRole = createRole("ROLE_USER");
//
////            log.info("Admin Role: {}", adminRole.getRoleName());
////            log.info("User Role: {}", userRole.getRoleName());
////
//////            // Create admin user
////            createUserIfNotExists("itibari@gmail.com", "Itibari", "Itibari",
////                    "+234567890", "password", adminRole);
////
////            // Create regular user
////            createUserIfNotExists("itibari.user@gmail.com", "Aisha", "Arilee",
////                    "+23988343", "password", userRole);
////
////
////            // Create mobile customer user
////
////            createUserIfNotExists("mobile.user@gmail.com", "Mobile", "User",
////                    "+23456780090", "password", mobileCustomerRole);
////
////            // Create purchase order user
////
////            createUserIfNotExists("purchase.user@gmail.com", "Purchase", "User",
////                    "+23456782290", "password", purchaseOrderRole);
////
////
////            // Create finance user
////
////            createUserIfNotExists("finance.itibari@gmail.com", "Finance", "User",
////                    "+2345337890", "password", financeRole);
//
//            // create bnpl user
//
////            createUserIfNotExists("bnpl.itibari@gmail.com", "BNPL", "User",
////                    "+2345337800", "password", bnplRole);
//
////            createUserIfNotExists("fs@itibari.io", "fs", "bnpl",
////                              "+2345336800", "password", bnplRole);
//
////            createUserIfNotExists("v.nyaki@itibari.io", "Victor", "Nyaki",
////                    "+255746765132", "bnplpassword", bnplRole);
////
////            createUserIfNotExists("admin@itibari.io", "Admin", "Itibari",
////                    "+2345336800", "bnplpassword", bnplRole);
////
////
////            createUserIfNotExists("frankmtegwa@itibari.io", "Frank", "Mtegwa",
////                    "+255713347613", "bnplpassword", bnplRole);
//
//            //john@itibari.io
//            //l.mrema@itibari.io
//            //y.dumba@itibari.io
////
////            createUserIfNotExists("john@bnpl.itibari.io", "John", "Shio",
////                    "+255746562276", "password", bnplRole);
////
////                createUserIfNotExists("l.mrema@bnpl.itibari.io", "Laura", "Mrema",
////                    "+255765881329", "password", bnplRole);
////
////                createUserIfNotExists("y.dumba@bnpl.itibari.io", "Yahaya", "Dumba",
////                    "+255653732826", "password", bnplRole);
//        }
//
//        // log.info("reconciliation job started");
//        //reconRunner();
//
//    }
//
//    private PermissionGroup createPermissionGroup(String warehouse) {
//        Optional<PermissionGroup> optionalPermissionGroup = permissionGroupRepository.findByGroupNameIgnoreCaseAndDeletedIsFalse(warehouse);
//
//        if (optionalPermissionGroup.isEmpty()) {
//            PermissionGroup permissionGroup = new PermissionGroup();
//            permissionGroup.setGroupName(warehouse);
//            return permissionGroupRepository.save(permissionGroup);
//        }
//        return optionalPermissionGroup.get();
//    }
//
//    private Permission createPermission(String permissionName, PermissionGroup permissionGroup) {
//        Optional<Permission> optionalPermission = permissionRepository.findByNameIgnoreCaseAndDeletedIsFalse(permissionName);
//
//        if (optionalPermission.isEmpty()) {
//            Permission permission = new Permission();
//            permission.setName(permissionName);
//            permission.setPermissionGroup(permissionGroup);
//            Permission p = permissionRepository.save(permission);
//
//            permissionGroup.addPermission(permission);
//
//            permissionGroupRepository.save(permissionGroup);
//            log.info("+===================================== + {} permission created +=====================================", permissionName);
//            return p;
//        }
//        return optionalPermission.get();
//    }
//
//    private Role createRole(String roleName) {
//        Optional<Role> roleOptional = roleRepository.findByRoleNameAndDeletedIsFalse(roleName);
//
//        if (roleOptional.isEmpty()) {
//            Role role = new Role();
//            role.setRoleName(roleName);
//            return roleRepository.save(role);
//        }
//        return roleOptional.get();
//    }
//
//    private Role createRoleWithPermissions(String roleName, Permission... permissions) {
//        Role role = createRole(roleName);
//
//        for (Permission permission : permissions) {
//            if (!role.getPermissions().contains(permission)) {
//                role.addPermission(permission);
//            }
//        }
//        return roleRepository.save(role);
//    }
//
//    private void createUserIfNotExists(String email, String firstName, String lastName,
//                                       String phoneNumber, String password, Role... roles) {
//        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(email);
//
//        Person person;
//
//        if (personOptional.isEmpty()) {
//            person = new Person();
//            person.setEmail(email);
//            person.setFirstName(firstName);
//            person.setLastName(lastName);
//            person.setPhoneNumber(phoneNumber);
//            person.setPassword(passwordEncoder.encode(password));
//
//            for (Role role : roles) {
//                person.getRoles().add(role);
//            }
//        } else {
//            person = personOptional.get();
//            person.setPassword(passwordEncoder.encode(password));
//        }
//
//        for (Role role : roles) {
//            person.addRole(role);
//        }
//        Person savedPerson = personRepository.save(person);
//
//        log.info("=====================================");
//        log.info("Created user: {}", savedPerson.getEmail());
//        log.info("=====================================");
//    }
//
//    private void freeAgents() {
//        deliveryAgentsRepository.findAll().forEach(deliveryAgent -> {
//            deliveryAgent.setAvailable(true);
//            deliveryAgentsRepository.save(deliveryAgent);
//        });
//    }
//
//    private void freeVehicles() {
//        vehicleRepository.findAll().forEach(vehicle -> {
//            vehicle.setAvailable(true);
//            vehicleRepository.save(vehicle);
//        });
//    }
//}
