package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import com.app.itibari.dataTypes.*;
import com.app.itibari.models.request.*;
import com.app.itibari.scheduler.Scheduler;
import com.app.itibari.services.ProductService;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Slf4j
public class StaticStockTests extends ItibariApplicationTests {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    ProductService productService;

    @Test
    @Ignore // works alone
    public void fetchReturnGoodsTestWorks() throws IOException {

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("runDate", LocalDate.now().format(DateTimeFormatter.ISO_DATE))
                .patch("/warehouse/runStaticStock")
                .then()
                .statusCode(200);

//        scheduler.productStockUpdate(LocalDate.now());
        UUID productPublicId = UUID.fromString("581c5c73-afc0-4b89-bafb-e160011f2715");
        UUID inventoryProductPublicId = UUID.fromString("94a50a32-7e62-4aad-84d9-8383cc78cd1b");
        UUID inventoryPublicId = UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386");
        UUID wareHousePublicId = UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56");

        PurchaseOrderRequest purchaseOrderRequest = new PurchaseOrderRequest();
        purchaseOrderRequest.setOrderDate(LocalDate.now());
        purchaseOrderRequest.setSupplierId(UUID.fromString("0bacd072-df86-4d05-bf86-27a1d38cacfb"));
        purchaseOrderRequest.setWarehouseId(wareHousePublicId);
        purchaseOrderRequest.setProformaInvoiceNumber("23458");
        purchaseOrderRequest.setRebate(false);

        PurchaseOrderItemRequest purchaseOrderItemRequest = new PurchaseOrderItemRequest();
        purchaseOrderItemRequest.setProductPublicId(productPublicId);
        purchaseOrderItemRequest.setQuantity(4);
        purchaseOrderItemRequest.setUnitPrice(new BigDecimal(10));

        purchaseOrderRequest.setPurchaseOrderItems(List.of(purchaseOrderItemRequest));
        purchaseOrderRequest.setComment("This is a test purchase order");


        String POPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderRequest)
                .post("/procurement/purchaseOrder")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("purchaseOrderItems[0].quantity", equalTo(purchaseOrderItemRequest.getQuantity()));

        PurchaseOrderUpdateRequest purchaseOrderUpdateRequest = new PurchaseOrderUpdateRequest();
        purchaseOrderUpdateRequest.setPurchaseOrderAction(PurchaseOrderAction.PROCESS);
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderUpdateRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/actions", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("SUBMITTED"));

        PurchaseOrderApprovalRequest purchaseOrderApprovalRequest = new PurchaseOrderApprovalRequest();
        purchaseOrderApprovalRequest.setActionType(ActionType.APPROVE);
        purchaseOrderApprovalRequest.setComment("This is a test purchase order");
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderApprovalRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/adminApprove", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("AMIN_APPROVED"));

        purchaseOrderApprovalRequest = new PurchaseOrderApprovalRequest();
        purchaseOrderApprovalRequest.setActionType(ActionType.APPROVE);
        purchaseOrderApprovalRequest.setAmountPaid(new BigDecimal(10));
        purchaseOrderApprovalRequest.setPaymentMethod(PaymentMethod.PrePaid);
        purchaseOrderApprovalRequest.setComment("This is a test purchase order");
        purchaseOrderApprovalRequest.setPaymentReference("UUUID20");
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderApprovalRequest)
                .patch("/financial/purchaseOrder/{purchaseOrderPublicId}/approve", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("ACCOUNTANT_APPROVED"));

        purchaseOrderUpdateRequest = new PurchaseOrderUpdateRequest();
        purchaseOrderUpdateRequest.setPurchaseOrderAction(PurchaseOrderAction.DISPATCH);
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderUpdateRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/actions", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        String purchaseOrderNumber = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("DISPATCHED"))
                .extract().path("purchaseOrderNumber");

        String purchaseOrderItemPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("searchParam", purchaseOrderNumber)
                .queryParam("statuses", Arrays.asList(PurchaseOrderStatus.PARTIAL_RECEIVED, PurchaseOrderStatus.DISPATCHED))
                .get("/procurement/purchaseOrders")
                .then()
                .statusCode(200)
                .extract().path("content[0].purchaseOrderItems[0].publicId");

        PurchaseOrderReceiveRequest purchaseOrderReceiveRequest = new PurchaseOrderReceiveRequest();
        purchaseOrderReceiveRequest.setDeliveryNoteNumber("12345");

        PurchaseOrderItemReceiveRequest purchaseOrderItemReceiveRequest = new PurchaseOrderItemReceiveRequest();
        purchaseOrderItemReceiveRequest.setOrderItemPublicId(UUID.fromString(purchaseOrderItemPublicId));
        purchaseOrderItemReceiveRequest.setQuantityReceived(4);

        purchaseOrderReceiveRequest.setPurchaseOrderItems(Arrays.asList(purchaseOrderItemReceiveRequest));
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .contentType(ContentType.JSON)
                .body(purchaseOrderReceiveRequest)
                .patch("/procurement/{purchaseOrderPublicId}/receiveShipment", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        String shipmentReceivedNotePublicId = given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/goodsReturnNote/shipmentReceived")
                .then()
                .statusCode(200)
                .body("content[0].createdBy", notNullValue())
                .body("content[0].noteDate", notNullValue())
                .body("content[0].noteNumber", notNullValue())
                .body("content[0].supplier.supplierName", notNullValue())
                .body("content[0].status", equalToIgnoringCase("APPROVED"))
                .body("content[0].purchaseOrderNumber", equalToIgnoringCase(purchaseOrderNumber))
                .body("content[0].deliveryNoteNumber", notNullValue())
                .body("content[0].purchaseOrderStatus", equalToIgnoringCase("RECEIVED"))
                .extract().path("content[0].publicId");

        String itemReceivedPublicId = given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/goodsReturnNote/shipmentReceived/{goodsReceivedNotePublicId}/items", UUID.fromString(shipmentReceivedNotePublicId))
                .then().log().all()
                .statusCode(200)
                .body("productItemList[0].productName", equalToIgnoringCase("Biscuite 50G x 200"))
                .body("productItemList[0].productCode", equalToIgnoringCase("SKU100021"))
                .body("productItemList[0].productSellingPrice", equalTo(10.0F))
                .body("productItemList[0].productPublicId", equalToIgnoringCase(String.valueOf(productPublicId)))
                .body("productItemList[0].quantityReceived", equalTo(4))
                .body("productItemList[0].quantityRequested", equalTo(4))
                .extract().path("productItemList[0].publicId");


        PromoRequestNoteRequest promoNoteRequest = new PromoRequestNoteRequest();
        promoNoteRequest.setReturnNotePublicId(UUID.fromString(shipmentReceivedNotePublicId));
        promoNoteRequest.setPurchaseOrderNumber(purchaseOrderNumber);

        PromoItemRequest promoItemRequest = new PromoItemRequest();
        promoItemRequest.setProductPublicId(productPublicId);
        promoItemRequest.setQuantityReceived(10);

        promoNoteRequest.setItems(Arrays.asList(promoItemRequest));

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .contentType(ContentType.JSON)
                .body(promoNoteRequest)
                .patch("/goodsReturnNote/promo/receive")
                .then()
                .statusCode(200);

        GoodsReturnNoteRequest goodsReturnNoteRequest = new GoodsReturnNoteRequest();
        goodsReturnNoteRequest.setGoodsReturnDate(LocalDate.now());
        goodsReturnNoteRequest.setReturnNotePublicId(UUID.fromString(shipmentReceivedNotePublicId));

        GoodsNoteItemRequest goodsNoteItemRequest1 = new GoodsNoteItemRequest();
        goodsNoteItemRequest1.setItemPublicId(UUID.fromString(itemReceivedPublicId));
        goodsNoteItemRequest1.setQuantity(2);
        goodsNoteItemRequest1.setRejectionCodePublicId(UUID.fromString("632b6fda-e23c-4a5f-bec0-a0f9e1f0e293"));

        goodsReturnNoteRequest.setGoodsNoteItemRequests(List.of(goodsNoteItemRequest1));

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .contentType(ContentType.JSON)
                .body(goodsReturnNoteRequest)
                .post("/goodsReturnNote/createGoodsReturnNotes")
                .then()
                .statusCode(200);

        String returnedNotePublicId = given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/goodsReturnNote/returnedGoodsNotes")
                .then()
                .statusCode(200)
                .extract().path("content[0].publicId");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .post("/procurement/purchaseOrder/{goodReturnNotePublicId}/replacement", returnedNotePublicId)
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", POPublicId)
                .then()
                .statusCode(200);

        testReceiveShipmentTestWorks();

//        testCreateOrderWithSAFullJourney(productPublicId, inventoryPublicId, wareHousePublicId, inventoryProductPublicId);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/stockMovements")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }

    public void testCreateOrderWithSAFullJourney(UUID productPublicId, UUID inventoryPublicId, UUID wareHousePublicId
            , UUID inventoryProductPublicId) throws IOException {
        UUID customerPublicId = UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d");

        String orderPublicId;
        String routePublicId;
        String orderItemPublicId;
        String deliveryId;
        String productItemPublicId;

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("productPublicId", productPublicId)
                .get("/inventory/{inventoryPublicId}/products", inventoryPublicId)
                .then()
                .statusCode(200);

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(productPublicId)
                .quantity(5)
                .build();
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(customerPublicId)
                .bookedBy("Itibari Test")
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .salesOrderItems(Collections.singleton(orderItems))
                .build();

        orderPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/salesOrders")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        orderItemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}/items", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");

        checkInventoryNegativeTests();

        scheduler.runOrderPlanning();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200);

        checkInventoryNegativeTests();

        RoutePlanningRequest routePlanningRequest = new RoutePlanningRequest();
        routePlanningRequest.setLocationPublicId(UUID.fromString("90ad01f1-4a13-41f7-b825-c428b41f35d0"));
        routePlanningRequest.setRegionPublicId(UUID.fromString("8c5d4db0-f9a1-47ed-a160-56780e4e4d14"));
        routePlanningRequest.setVehiclePublicId(UUID.fromString("fa4bf2ee-6665-493a-8dbd-5d56dc8a7d93"));
        routePlanningRequest.setDeliveryAgentPublicId(UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65"));
        routePlanningRequest.setWarehousePublicId(wareHousePublicId);
        routePlanningRequest.setSalesOrdersPublicIds(Collections.singletonList(UUID.fromString(orderPublicId)));

        routePublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(routePlanningRequest)
                .post("/logistics/routePlanning")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        deliveryId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .extract().path("routeId");

        productItemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{routePlanningPublicId}/loadingProducts", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].productItemPublicId");

        checkInventoryNegativeTests();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.CREATED.name()));

        LoadingRequest loadingRequest = new LoadingRequest();
        LoadingDTO loadingDTO = new LoadingDTO();
        loadingDTO.setLoadingType(LoadingType.OrderSale);
        loadingDTO.setProductItemPublicId(UUID.fromString(productItemPublicId));
        loadingRequest.setLoadingDTOList(Collections.singletonList(loadingDTO));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(loadingRequest)
                .post("warehouse/agent/{agentPublicId}/loading", UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65"))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.Loaded.name()));

        checkInventoryNegativeTests();

        given()
                .contentType("application/json")
                .auth().oauth2(DELIVERY_AGENT_TOKEN)
                .header("X-App-Version", "1.0.0")
                .queryParam("acceptType", AcceptTypeEnum.AcceptRoute)
                .patch("/deliveryAgents/accept/{routeId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200);

        checkInventoryNegativeTests();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.ReadyToGo.name()));

        given()
                .contentType("application/json")
                .auth().oauth2(DELIVERY_AGENT_TOKEN)
                .header("X-App-Version", "1.0.0")
                .queryParam("routeId", UUID.fromString(routePublicId))
                .queryParam("action", RouteAction.Start)
                .patch("/deliveryAgents/routeActions")
                .then()
                .statusCode(200);

        String productReconId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/productRecons")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");

        String productReconItemId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/productRecons/{publicId}/items", UUID.fromString(productReconId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");

        checkInventoryNegativeTests();

        DeliverItemRequest deliverItemRequest = new DeliverItemRequest();
        deliverItemRequest.setSalesOrderPublicId(UUID.fromString(orderPublicId));
        deliverItemRequest.setRoutePublicId(UUID.fromString(routePublicId));

        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setProductItemPublicId(UUID.fromString(orderItemPublicId));
        itemRequest.setProductReconItemPublicId(UUID.fromString(orderItemPublicId));
        itemRequest.setQuantitySold(3);
        deliverItemRequest.setItems(Collections.singletonList(itemRequest));

        given()
                .contentType("application/json")
                .auth().oauth2(DELIVERY_AGENT_TOKEN)
                .header("X-App-Version", "1.0.0")
                .body(deliverItemRequest)
                .patch("/deliveryAgents/markOrderItemAsDelivered")
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(DELIVERY_AGENT_TOKEN)
                .header("X-App-Version", "1.0.0")
                .patch("/deliveryAgents/markOrderAsDelivered/{orderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200);

        checkInventoryNegativeTests();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.DELIVERED.name()));

        given()
                .contentType("application/json")
                .auth().oauth2(DELIVERY_AGENT_TOKEN)
                .header("X-App-Version", "1.0.0")
                .queryParam("routeId", UUID.fromString(routePublicId))
                .queryParam("action", RouteAction.end)
                .patch("/deliveryAgents/routeActions")
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}/items", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].quantity", equalTo(5))
                .extract().path("content[0].publicId");

        String recoPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("deliveryId", deliveryId)
                .get("/warehouse/productRecons")
                .then()
                .statusCode(200)
                .extract().path("content[0].publicId");

        String recoItemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("deliveryId", deliveryId)
                .get("/warehouse/productRecons/{publicId}/items", UUID.fromString(recoPublicId))
                .then()
                .statusCode(200)
                .extract().path("content[0].publicId");

        ProductReconAction productReconAction = new ProductReconAction();
        productReconAction.setProductReconPublicId(UUID.fromString(recoPublicId));

        ProductItemRequest productItemRequest = new ProductItemRequest();
        productItemRequest.setQuantityReturned(1);
        productItemRequest.setProductItemPublicId(UUID.fromString(recoItemPublicId));
        productItemRequest.setQuantityMissing(1);
        productItemRequest.setMissingRejectionPublicId(UUID.fromString("d88a0e04-23b7-4145-93ba-48b31efacf8d"));
        productReconAction.setProductItems(Collections.singletonList(productItemRequest));

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(productReconAction)
                .patch("/warehouse/productRecons/{publicId}", UUID.fromString(recoPublicId))
                .then()
                .statusCode(200);

        String stagedItemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("searchParam", "Erick")
//                .queryParam("startDate", LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
//                .queryParam("endDate", LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .get("/warehouse/stagedForApproval")
                .then()
                .statusCode(200)
                .extract().path("content[0].publicId");

        Response response = given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/reports/warehouse/stagedForApproval")
                .then()
                .statusCode(200)
                .extract().response();

//        saveExport(response, "Staging_Report.xlsx");

        String itemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/stagedForApproval/{publicId}/items", UUID.fromString(stagedItemPublicId))
                .then()
                .statusCode(200)
                .body("content[0].quantityReturned", equalTo(1))
                .extract().path("content[0].publicId");

        StagingActionRequest stagingActionRequest = StagingActionRequest.builder()
                .actionType(StagingActionRequest.ActionType.Edit_Item)
                .quantity(2)
                .comment("approved")
                .build();
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(stagingActionRequest).log().all()
                .queryParam("itemPublicId", UUID.fromString(itemPublicId))
                .patch("/warehouse/stagedForApproval/{publicId}", UUID.fromString(stagedItemPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/stagedForApproval/{publicId}/items", UUID.fromString(stagedItemPublicId))
                .then()
                .statusCode(200)
                .body("content[0].quantityReturned", equalTo(2));

        stagingActionRequest = StagingActionRequest.builder().actionType(StagingActionRequest.ActionType.Approve).comment("approved").build();
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(stagingActionRequest).log().all()
                .patch("/warehouse/stagedForApproval/{publicId}", UUID.fromString(stagedItemPublicId))
                .then()
                .statusCode(200);


//        testCreateQuarantineProductWorks(productPublicId, inventoryPublicId, wareHousePublicId, inventoryProductPublicId);
    }

    public void testCreateQuarantineProductWorks(UUID productId, UUID inventoryProductId, UUID mainWarehouseId, UUID inventoryProductPublicId) throws IOException {
        UUID quarantineWarehouseId = UUID.fromString("6bd6caf4-f11b-4eba-bc9d-419d4668d182");
        UUID reasonId = UUID.fromString("632b6fda-e23c-4a5f-bec0-a0f9e1f0e293");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("productPublicId", productId)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content[0].availableStock", equalTo(29))
                .body("content[0].reservedStock", equalTo(0))
                .body("content[0].quarantinedStock", equalTo(0))
                .body("content[0].totalStock", equalTo(29));

        QuarantineRequest quarantineRequest = new QuarantineRequest();
        quarantineRequest.setMainWarehousePublicId(mainWarehouseId);
        quarantineRequest.setQuarantineWarehousePublicId(quarantineWarehouseId);

        QuarantineProductRequest quarantineProductRequest = new QuarantineProductRequest();
        quarantineProductRequest.setInventoryProductPublicId(inventoryProductPublicId);
        quarantineProductRequest.setQuantity(4);
        quarantineProductRequest.setReasonPublicId(reasonId);
        quarantineRequest.setQuarantineProducts(Arrays.asList(quarantineProductRequest));

        String quarantinePublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(quarantineRequest)
                .post("/quarantine")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(quarantineRequest)
                .queryParam("action", QuarantineAction.Approve)
                .patch("/quarantine/{publicId}/actions", UUID.fromString(quarantinePublicId))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/quarantine/{publicId}", UUID.fromString(quarantinePublicId))
                .then()
                .statusCode(200)
                .body("transferType", equalToIgnoringCase("TO_QUARANTINE_WAREHOUSE"))
                .body("quarantineStatus", equalToIgnoringCase("Approved"));

        String quarantineProductId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/quarantine/{publicId}/quarantineProducts", UUID.fromString(quarantinePublicId))
                .then()
                .statusCode(200)
                .body("content[0].quantity", equalTo(4))
                .body("content[0].productName", equalToIgnoringCase("Biscuite 50G x 200"))
                .body("content[0].reasonDescription", equalToIgnoringCase("Shop Closed"))
                .extract().path("content[0].publicId");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("productPublicId", productId)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("quantity", 2)
                .pathParam("publicId", UUID.fromString(quarantinePublicId))
                .pathParam("quarantineProductId", quarantineProductId)
                .post("/quarantine/{publicId}/returnProduct/{quarantineProductId}")
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/quarantine/{publicId}/quarantineProducts", UUID.fromString(quarantinePublicId))
                .then()
                .statusCode(200)
                .body("content[0].quantity", equalTo(2));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("productPublicId", productId)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("runDate", LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_DATE))
                .patch("/warehouse/runStaticStock")
                .then()
                .statusCode(200);

        log.info("--------------12388");
        String liveDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("warehouseId", mainWarehouseId)
                .queryParam("liveDate", liveDate)
                .queryParam("searchParam", "biscu")
                .get("/reports/liveStockReport")
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/productStock")
                .then().log().all()
                .statusCode(200);

        String wareHousePublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("warehousePublicId", UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56"))
                .get("/warehouse/productStockRapper")
                .then().log().all()
                .statusCode(200)
                .extract().path("content[0].wareHousePublicId");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("searchParam", "Biscuite")
                .get("/warehouse/productStock")
                .then()
                .statusCode(200);

        Response responseResult = given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/downloadTemplate")
                .andReturn();

        byte[] fileContents = responseResult.getBody().asByteArray();

        saveExport(responseResult, "product_stock_template.xlsx");

        int available_stock = 4;
        int reserved_stock = 5;
        int quarantine_stock = 6;
        int total_stock = 7;
        int closing_stock = 8;

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(fileContents);

        XSSFWorkbook workbook = new XSSFWorkbook(byteArrayInputStream);
        Sheet sheet = workbook.getSheetAt(0);

        Row rowIndex1 = sheet.getRow(1);

        rowIndex1.createCell(available_stock).setCellValue("100");
        rowIndex1.createCell(reserved_stock).setCellValue("20");
        rowIndex1.createCell(quarantine_stock).setCellValue("10");
        rowIndex1.createCell(total_stock).setCellValue("100");
        rowIndex1.createCell(closing_stock).setCellValue("110");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        bos.close();

        try (OutputStream outputStream = Files.newOutputStream(Paths.get("src/test/resources/downloads" +
                "/Updated_Upload_Product_Stock.xlsx"))) {
            bos.writeTo(outputStream);
        }

        Response updatedResponse = given()
                .auth().oauth2(ADMIN_TOKEN)
                .multiPart("file", "product_stock_template.xlsx", bos.toByteArray())
                .post("/warehouse/productStockBulkUpload")
                .andReturn();
//        saveExport(updatedResponse, "Updated_Upload_Product_Stock.xlsx");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/productStock")
                .then().log().all()
                .statusCode(200);

        createNextRecord(mainWarehouseId);

        Response response = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("warehouseId", mainWarehouseId)
                .queryParam("liveDate", liveDate)
                .queryParam("searchParam", "SKU100021")
                .get("/reports/staticReportExport")
                .then()
                .statusCode(200)
                .extract().response();

//        saveExport(response, "static_stock_report.xlsx");

        given()
                .get("/authentication/v2/products")
                .then()
                .statusCode(200);
//                .log().all();
    }

    private void createNextRecord(UUID mainWarehouseId) {
//        scheduler.productStockUpdate(LocalDate.now().plusDays(1));
        String liveDate = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("warehouseId", mainWarehouseId)
                .queryParam("liveDate", liveDate)
                .queryParam("searchParam", "SKU100021")
                .get("/reports/liveStockReport")
                .then()
                .statusCode(200);
    }

    private void checkInventoryNegativeTests() {

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].availableStock", greaterThanOrEqualTo(0));
    }

    //    @Test
    public void testReceiveShipmentTestWorks() throws IOException {

        UUID productPublicId = UUID.fromString("534cf5d4-f8c3-495c-a813-9a42f338c466");
        UUID wareHousePublicId = UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56");

        PurchaseOrderRequest purchaseOrderRequest = new PurchaseOrderRequest();
        purchaseOrderRequest.setOrderDate(LocalDate.now());
        purchaseOrderRequest.setSupplierId(UUID.fromString("0bacd072-df86-4d05-bf86-27a1d38cacfb"));
        purchaseOrderRequest.setWarehouseId(wareHousePublicId);
        purchaseOrderRequest.setProformaInvoiceNumber("23458");
        purchaseOrderRequest.setRebate(false);

        PurchaseOrderItemRequest purchaseOrderItemRequest = new PurchaseOrderItemRequest();
        purchaseOrderItemRequest.setProductPublicId(productPublicId);
        purchaseOrderItemRequest.setQuantity(20);
        purchaseOrderItemRequest.setUnitPrice(new BigDecimal(100));

        purchaseOrderRequest.setPurchaseOrderItems(List.of(purchaseOrderItemRequest));
        purchaseOrderRequest.setComment("This is a test purchase order");


        String POPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderRequest)
                .post("/procurement/purchaseOrder")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("purchaseOrderItems[0].quantity", equalTo(purchaseOrderItemRequest.getQuantity()));

        PurchaseOrderUpdateRequest purchaseOrderUpdateRequest = new PurchaseOrderUpdateRequest();
        purchaseOrderUpdateRequest.setPurchaseOrderAction(PurchaseOrderAction.PROCESS);
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderUpdateRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/actions", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("SUBMITTED"));

        PurchaseOrderApprovalRequest purchaseOrderApprovalRequest = new PurchaseOrderApprovalRequest();
        purchaseOrderApprovalRequest.setActionType(ActionType.APPROVE);
        purchaseOrderApprovalRequest.setComment("This is a test purchase order");
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderApprovalRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/adminApprove", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("AMIN_APPROVED"));

        purchaseOrderApprovalRequest = new PurchaseOrderApprovalRequest();
        purchaseOrderApprovalRequest.setActionType(ActionType.APPROVE);
        purchaseOrderApprovalRequest.setAmountPaid(new BigDecimal(10));
        purchaseOrderApprovalRequest.setPaymentMethod(PaymentMethod.PrePaid);
        purchaseOrderApprovalRequest.setComment("This is a test purchase order");
        purchaseOrderApprovalRequest.setPaymentReference("UUUID20");
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderApprovalRequest)
                .patch("/financial/purchaseOrder/{purchaseOrderPublicId}/approve", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("ACCOUNTANT_APPROVED"));

        purchaseOrderUpdateRequest = new PurchaseOrderUpdateRequest();
        purchaseOrderUpdateRequest.setPurchaseOrderAction(PurchaseOrderAction.DISPATCH);
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderUpdateRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/actions", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        String purchaseOrderNumber = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("DISPATCHED"))
                .extract().path("purchaseOrderNumber");

        String purchaseOrderItemPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("searchParam", purchaseOrderNumber)
                .queryParam("statuses", Arrays.asList(PurchaseOrderStatus.PARTIAL_RECEIVED, PurchaseOrderStatus.DISPATCHED))
                .get("/procurement/purchaseOrders")
                .then()
                .statusCode(200)
                .extract().path("content[0].purchaseOrderItems[0].publicId");

        PurchaseOrderReceiveRequest purchaseOrderReceiveRequest = new PurchaseOrderReceiveRequest();
        purchaseOrderReceiveRequest.setDeliveryNoteNumber("12345");

        PurchaseOrderItemReceiveRequest purchaseOrderItemReceiveRequest = new PurchaseOrderItemReceiveRequest();
        purchaseOrderItemReceiveRequest.setOrderItemPublicId(UUID.fromString(purchaseOrderItemPublicId));
        purchaseOrderItemReceiveRequest.setQuantityReceived(20);

        purchaseOrderReceiveRequest.setPurchaseOrderItems(Arrays.asList(purchaseOrderItemReceiveRequest));
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .contentType(ContentType.JSON)
                .body(purchaseOrderReceiveRequest)
                .patch("/procurement/{purchaseOrderPublicId}/receiveShipment", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        String shipmentReceivedNotePublicId = given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/goodsReturnNote/shipmentReceived")
                .then()
                .statusCode(200)
                .body("content[0].createdBy", notNullValue())
                .body("content[0].noteDate", notNullValue())
                .body("content[0].noteNumber", notNullValue())
                .body("content[0].supplier.supplierName", notNullValue())
                .body("content[0].status", equalToIgnoringCase("APPROVED"))
                .body("content[0].purchaseOrderNumber", equalToIgnoringCase(purchaseOrderNumber))
                .body("content[0].deliveryNoteNumber", notNullValue())
                .body("content[0].purchaseOrderStatus", equalToIgnoringCase("RECEIVED"))
                .extract().path("content[0].publicId");

        String itemReceivedPublicId = given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/goodsReturnNote/shipmentReceived/{goodsReceivedNotePublicId}/items", UUID.fromString(shipmentReceivedNotePublicId))
                .then()
                .statusCode(200)
                .body("productItemList[0].productName", equalToIgnoringCase("Bwana Sukari Brown Sugar 50G x 200"))
                .body("productItemList[0].productCode", equalToIgnoringCase("SKU100021"))
                .body("productItemList[0].productSellingPrice", equalTo(100.0F))
                .body("productItemList[0].productPublicId", equalToIgnoringCase(String.valueOf(productPublicId)))
                .body("productItemList[0].quantityReceived", equalTo(20))
                .body("productItemList[0].quantityRequested", equalTo(20))
                .extract().path("productItemList[0].publicId");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/productStock")
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void testCreateInitialProductStock(){
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .patch("/warehouse/runProductStock")
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/productStock")
                .then().log().all()
                .statusCode(200);
    }

    @Test
    @Ignore // works alone
    public void testGRNandGRTNonProductStockWorks() throws IOException {

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("runDate", LocalDate.now().format(DateTimeFormatter.ISO_DATE))
                .patch("/warehouse/runStaticStock")
                .then()
                .statusCode(200);

        productService.runProductStockByNextDay(null);

        String dateToday = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("from", dateToday)
                .queryParam("to", dateToday)
                .get("/warehouse/productStock")
                .then().log().all()
                .statusCode(200);


        UUID productPublicId = UUID.fromString("581c5c73-afc0-4b89-bafb-e160011f2715");
        UUID inventoryProductPublicId = UUID.fromString("94a50a32-7e62-4aad-84d9-8383cc78cd1b");
        UUID inventoryPublicId = UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386");
        UUID wareHousePublicId = UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56");

        PurchaseOrderRequest purchaseOrderRequest = new PurchaseOrderRequest();
        purchaseOrderRequest.setOrderDate(LocalDate.now());
        purchaseOrderRequest.setSupplierId(UUID.fromString("0bacd072-df86-4d05-bf86-27a1d38cacfb"));
        purchaseOrderRequest.setWarehouseId(wareHousePublicId);
        purchaseOrderRequest.setProformaInvoiceNumber("23458");
        purchaseOrderRequest.setRebate(false);

        PurchaseOrderItemRequest purchaseOrderItemRequest = new PurchaseOrderItemRequest();
        purchaseOrderItemRequest.setProductPublicId(productPublicId);
        purchaseOrderItemRequest.setQuantity(1500);
        purchaseOrderItemRequest.setUnitPrice(new BigDecimal(10));

        purchaseOrderRequest.setPurchaseOrderItems(List.of(purchaseOrderItemRequest));
        purchaseOrderRequest.setComment("This is a test purchase order");


        String POPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderRequest)
                .post("/procurement/purchaseOrder")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("purchaseOrderItems[0].quantity", equalTo(purchaseOrderItemRequest.getQuantity()));

        PurchaseOrderUpdateRequest purchaseOrderUpdateRequest = new PurchaseOrderUpdateRequest();
        purchaseOrderUpdateRequest.setPurchaseOrderAction(PurchaseOrderAction.PROCESS);
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderUpdateRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/actions", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("SUBMITTED"));

        PurchaseOrderApprovalRequest purchaseOrderApprovalRequest = new PurchaseOrderApprovalRequest();
        purchaseOrderApprovalRequest.setActionType(ActionType.APPROVE);
        purchaseOrderApprovalRequest.setComment("This is a test purchase order");
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderApprovalRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/adminApprove", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("AMIN_APPROVED"));

        purchaseOrderApprovalRequest = new PurchaseOrderApprovalRequest();
        purchaseOrderApprovalRequest.setActionType(ActionType.APPROVE);
        purchaseOrderApprovalRequest.setAmountPaid(new BigDecimal(10));
        purchaseOrderApprovalRequest.setPaymentMethod(PaymentMethod.PrePaid);
        purchaseOrderApprovalRequest.setComment("This is a test purchase order");
        purchaseOrderApprovalRequest.setPaymentReference("UUUID20");
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderApprovalRequest)
                .patch("/financial/purchaseOrder/{purchaseOrderPublicId}/approve", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("ACCOUNTANT_APPROVED"));

        purchaseOrderUpdateRequest = new PurchaseOrderUpdateRequest();
        purchaseOrderUpdateRequest.setPurchaseOrderAction(PurchaseOrderAction.DISPATCH);
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderUpdateRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/actions", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        String purchaseOrderNumber = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("DISPATCHED"))
                .extract().path("purchaseOrderNumber");

        String purchaseOrderItemPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("searchParam", purchaseOrderNumber)
                .queryParam("statuses", Arrays.asList(PurchaseOrderStatus.PARTIAL_RECEIVED, PurchaseOrderStatus.DISPATCHED))
                .get("/procurement/purchaseOrders")
                .then()
                .statusCode(200)
                .extract().path("content[0].purchaseOrderItems[0].publicId");

        PurchaseOrderReceiveRequest purchaseOrderReceiveRequest = new PurchaseOrderReceiveRequest();
        purchaseOrderReceiveRequest.setDeliveryNoteNumber("12345");

        PurchaseOrderItemReceiveRequest purchaseOrderItemReceiveRequest = new PurchaseOrderItemReceiveRequest();
        purchaseOrderItemReceiveRequest.setOrderItemPublicId(UUID.fromString(purchaseOrderItemPublicId));
        purchaseOrderItemReceiveRequest.setQuantityReceived(1500);

        purchaseOrderReceiveRequest.setPurchaseOrderItems(Arrays.asList(purchaseOrderItemReceiveRequest));
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .contentType(ContentType.JSON)
                .body(purchaseOrderReceiveRequest)
                .patch("/procurement/{purchaseOrderPublicId}/receiveShipment", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        String shipmentReceivedNotePublicId = given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/goodsReturnNote/shipmentReceived")
                .then().log().all()
                .statusCode(200)
                .body("content[0].createdBy", notNullValue())
                .body("content[0].noteDate", notNullValue())
                .body("content[0].noteNumber", notNullValue())
                .body("content[0].supplier.supplierName", notNullValue())
                .body("content[0].status", equalToIgnoringCase("APPROVED"))
                .body("content[0].purchaseOrderNumber", equalToIgnoringCase(purchaseOrderNumber))
                .body("content[0].deliveryNoteNumber", notNullValue())
                .body("content[0].purchaseOrderStatus", equalToIgnoringCase("RECEIVED"))
                .extract().path("content[0].publicId");

        String itemReceivedPublicId = given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/goodsReturnNote/shipmentReceived/{goodsReceivedNotePublicId}/items", UUID.fromString(shipmentReceivedNotePublicId))
                .then()
                .statusCode(200)
                .body("content[0].productName", equalToIgnoringCase("Biscuite 50G x 200"))
                .body("content[0].productCode", equalToIgnoringCase("SKU100021"))
                .body("content[0].productSellingPrice", equalTo(10.0F))
                .body("content[0].productPublicId", equalToIgnoringCase(String.valueOf(productPublicId)))
                .body("content[0].quantityReceived", equalTo(1500))
                .body("content[0].quantityRequested", equalTo(1500))
                .extract().path("content[0].publicId");

        GoodsReturnNoteRequest goodsReturnNoteRequest = new GoodsReturnNoteRequest();
        goodsReturnNoteRequest.setGoodsReturnDate(LocalDate.now());
        goodsReturnNoteRequest.setReturnNotePublicId(UUID.fromString(shipmentReceivedNotePublicId));

        GoodsNoteItemRequest goodsNoteItemRequest1 = new GoodsNoteItemRequest();
        goodsNoteItemRequest1.setItemPublicId(UUID.fromString(itemReceivedPublicId));
        goodsNoteItemRequest1.setQuantity(1);
        goodsNoteItemRequest1.setRejectionCodePublicId(UUID.fromString("632b6fda-e23c-4a5f-bec0-a0f9e1f0e293"));

        goodsReturnNoteRequest.setGoodsNoteItemRequests(List.of(goodsNoteItemRequest1));

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .contentType(ContentType.JSON)
                .body(goodsReturnNoteRequest)
                .post("/goodsReturnNote/createGoodsReturnNotes")
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("from", dateToday)
                .queryParam("to", dateToday)
                .get("/warehouse/productStock")
                .then().log().all()
                .statusCode(200);
    }
}
