package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import com.app.itibari.dataTypes.*;
import com.app.itibari.models.request.*;
import com.app.itibari.scheduler.Scheduler;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Slf4j
public class GoodsReturnRestControllerTests extends ItibariApplicationTests {

//    @Autowired
//    private GoodsReceivedRunner goodsReceivedRunner;

//    @Autowired
//    private GoodsReturnedRunner goodsReturnedRunner;

    @Autowired
    private Scheduler scheduler;

//    @Ignore
    @Test
    public void fetchReturnGoodsTestWorks() throws IOException {
//        scheduler.productStockUpdate(LocalDate.now());
        String payload = productPayload("String description", UUID.randomUUID());
        String productPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(payload)
                .post("/products")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        PurchaseOrderRequest purchaseOrderRequest = new PurchaseOrderRequest();
        purchaseOrderRequest.setOrderDate(LocalDate.now());
        purchaseOrderRequest.setSupplierId(UUID.fromString("0bacd072-df86-4d05-bf86-27a1d38cacfb"));
        purchaseOrderRequest.setWarehouseId(UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56"));
        purchaseOrderRequest.setProformaInvoiceNumber("23458");
        purchaseOrderRequest.setRebate(false);

        PurchaseOrderItemRequest purchaseOrderItemRequest = new PurchaseOrderItemRequest();
        purchaseOrderItemRequest.setProductPublicId(UUID.fromString(productPublicId));
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
                .then().log().all()
                .statusCode(200)
                .body("status", equalToIgnoringCase("PENDING"))
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
                .then().log().all()
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

        log.info("-----text---1----");
        String shipmentReceivedNotePublicId = given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/goodsReturnNote/shipmentReceived")
                .then().log().all()
                .statusCode(200)
                .body("content[0].noteDate", notNullValue())
                .body("content[0].noteNumber", notNullValue())
                .body("content[0].supplier.supplierName", notNullValue())
                .body("content[0].status", equalToIgnoringCase("APPROVED"))
                .body("content[0].purchaseOrderNumber", equalToIgnoringCase(purchaseOrderNumber))
                .body("content[0].deliveryNoteNumber", notNullValue())
                .body("content[0].purchaseOrderStatus", equalToIgnoringCase("RECEIVED"))
                .extract().path("content[0].publicId");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/productStock")
                .then()
                .statusCode(200);

        log.info("-----text----2---");
        String itemReceivedPublicId = given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/goodsReturnNote/shipmentReceived/{goodsReceivedNotePublicId}/items", UUID.fromString(shipmentReceivedNotePublicId))
                .then().log().all()
                .statusCode(200)
//                .body("content[0].productName", equalToIgnoringCase("PRO_NAME"))
//                .body("content[0].productCode", equalToIgnoringCase("SKU100000"))
//                .body("content[0].productSellingPrice", equalTo(10.0F))
//                .body("content[0].productPublicId", equalTo(productPublicId))
//                .body("content[0].quantityReceived", equalTo(4))
//                .body("content[0].quantityRequested", equalTo(4))
                .extract().path("productItemList[0].publicId");

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
                .body("content[0].goodsReturnNoteNumber", equalToIgnoringCase("GRTN100000"))
                .body("content[0].replacementRaised", equalTo(true))
                .body("content[0].replacementPurchaseOrder", notNullValue())
                .extract().path("content[0].publicId");

        log.info("-----text-------");
        given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/goodsReturnNote/returnedGoodsNotes/{goodsReturnedNotePublicId}/items", UUID.fromString(returnedNotePublicId))
                .then().log().all()
                .statusCode(200)
                .body("content[0].quantityReturned", equalTo(2))
                .body("content[0].rejectionCode", equalToIgnoringCase("Shop Closed"));

        Response response = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("exportType", StatementExportType.EXCEL)
                .get("/reports/goodsReturnNote/{publicId}", UUID.fromString(returnedNotePublicId))
                .then()
                .statusCode(200)
                .extract().response();

        saveExport(response, "grtn_items_report.xlsx");

        String publicId = given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/goodsReturnNote/returnedGoodsNotes")
                .then()
                .statusCode(200)
                .extract().path("content[0].replacementPurchaseOrder");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .post("/procurement/purchaseOrder/{goodReturnNotePublicId}/replacement", UUID.fromString(returnedNotePublicId))
                .then().log().all()
                .statusCode(400)
                .body("userMessage", equalToIgnoringCase("Replacement purchase order already raised for this goods return note"));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(publicId))
                .then().log().all()
                .statusCode(200)
                .body("replacementPurchaseOrder", equalTo(true))
                .body("status", equalToIgnoringCase("DISPATCHED"));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}/items", UUID.fromString(publicId))
                .then().log().all()
                .statusCode(200)
                .body("content.size()", equalTo(1));

        // fetch stock movement records

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/productStock")
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("from", LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_DATE))
                .queryParam("to", LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_DATE))
                .queryParam("transactionType", StockMovementTransactionType.GRN)
                .queryParam("warehousePublicId", UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56"))
                .get("/warehouse/stockMovements")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }

    @Ignore
    @Test
    public void fetchPromoGoodsTestWorks() throws IOException {
        UUID productPublicId = UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("productPublicId", productPublicId)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content[0].availableStock", equalTo(5));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].availableStock", greaterThanOrEqualTo(0));

        PurchaseOrderRequest purchaseOrderRequest = new PurchaseOrderRequest();
        purchaseOrderRequest.setOrderDate(LocalDate.now());
        purchaseOrderRequest.setSupplierId(UUID.fromString("0bacd072-df86-4d05-bf86-27a1d38cacfb"));
        purchaseOrderRequest.setWarehouseId(UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56"));
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
                .then().log().all()
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
                .then().log().all()
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

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("productPublicId", productPublicId)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content[0].availableStock", equalTo(9));

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

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("productPublicId", productPublicId)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content[0].availableStock", equalTo(19));
    }

    @Ignore
    @Test
    public void fetchReturnGoodsAndExportWorks() throws Exception {
//        goodsReceivedRunner.run();

        String publicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/goodsReturnNote/shipmentReceived")
                .then().log().all()
                .statusCode(200)
                .extract().body().path("content[0].publicId");

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/goodsReturnNote/shipmentReceived/{goodsReceivedNotePublicId}/items", UUID.fromString(publicId))
                .then().log().all()
                .statusCode(200);
    }

//    @Ignore
    @Test
    public void fetchPromoGoodsTestWorks2() throws IOException {
        UUID productPublicId = UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("productPublicId", productPublicId)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content[0].availableStock", greaterThanOrEqualTo(0));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].availableStock", greaterThanOrEqualTo(0));

        PurchaseOrderRequest purchaseOrderRequest = new PurchaseOrderRequest();
        purchaseOrderRequest.setOrderDate(LocalDate.now());
        purchaseOrderRequest.setSupplierId(UUID.fromString("0bacd072-df86-4d05-bf86-27a1d38cacfb"));
        purchaseOrderRequest.setWarehouseId(UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56"));
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
                .then().log().all()
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
                .then().log().all()
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
                .then().log().all()
                .statusCode(200)
                .body("content[0].noteDate", notNullValue())
                .body("content[0].noteNumber", notNullValue())
                .body("content[0].supplier.supplierName", notNullValue())
                .body("content[0].status", equalToIgnoringCase("APPROVED"))
                .body("content[0].purchaseOrderNumber", equalToIgnoringCase(purchaseOrderNumber))
                .body("content[0].deliveryNoteNumber", notNullValue())
                .body("content[0].purchaseOrderStatus", equalToIgnoringCase("RECEIVED"))
                .extract().path("content[0].publicId");

        Response response = given()
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("exportType", StatementExportType.PDF)
                .get("/reports/goodsReceiveNote/{publicId}", UUID.fromString(shipmentReceivedNotePublicId))
                .then()
                .statusCode(200)
                .extract().response();

        saveExport(response, "grn_items_report.pdf");

//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .queryParam("productPublicId", productPublicId)
//                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
//                .then()
//                .statusCode(200)
//                .body("content[0].availableStock", equalTo(13));
//
//        PromoRequestNoteRequest promoNoteRequest = new PromoRequestNoteRequest();
//        promoNoteRequest.setReturnNotePublicId(UUID.fromString(shipmentReceivedNotePublicId));
//        promoNoteRequest.setPurchaseOrderNumber(purchaseOrderNumber);
//
//        PromoItemRequest promoItemRequest = new PromoItemRequest();
//        promoItemRequest.setProductPublicId(productPublicId);
//        promoItemRequest.setQuantityReceived(10);
//
//        promoNoteRequest.setItems(Arrays.asList(promoItemRequest));
//
//        given()
//                .auth().oauth2(ADMIN_TOKEN)
//                .contentType(ContentType.JSON)
//                .body(promoNoteRequest)
//                .patch("/goodsReturnNote/promo/receive")
//                .then()
//                .statusCode(200);
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .queryParam("productPublicId", productPublicId)
//                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
//                .then()
//                .statusCode(200)
//                .body("content[0].availableStock", equalTo(23));
    }

//    @Ignore
    @Test
    public void testCreateAndFetchProductStock() throws IOException {
        String accessCode = SUPER_ADMIN_TOKEN;

        UUID productPublicId = UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af");

        given()
                .contentType("application/json")
                .auth().oauth2(accessCode)
                .queryParam("productPublicId", productPublicId)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content[0].availableStock", greaterThanOrEqualTo(0));

        given()
                .contentType("application/json")
                .auth().oauth2(accessCode)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].availableStock", greaterThanOrEqualTo(0));

        PurchaseOrderRequest purchaseOrderRequest = new PurchaseOrderRequest();
        purchaseOrderRequest.setOrderDate(LocalDate.now());
        purchaseOrderRequest.setSupplierId(UUID.fromString("0bacd072-df86-4d05-bf86-27a1d38cacfb"));
        purchaseOrderRequest.setWarehouseId(UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56"));
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
                .auth().oauth2(accessCode)
                .body(purchaseOrderRequest)
                .post("/procurement/purchaseOrder")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessCode)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then().log().all()
                .statusCode(200)
                .body("purchaseOrderItems[0].quantity", equalTo(purchaseOrderItemRequest.getQuantity()));


        PurchaseOrderUpdateRequest purchaseOrderUpdateRequest = new PurchaseOrderUpdateRequest();
        purchaseOrderUpdateRequest.setPurchaseOrderAction(PurchaseOrderAction.PROCESS);
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessCode)
                .body(purchaseOrderUpdateRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/actions", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessCode)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("SUBMITTED"));

        PurchaseOrderApprovalRequest purchaseOrderApprovalRequest = new PurchaseOrderApprovalRequest();
        purchaseOrderApprovalRequest.setActionType(ActionType.APPROVE);
        purchaseOrderApprovalRequest.setComment("This is a test purchase order");
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessCode)
                .body(purchaseOrderApprovalRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/adminApprove", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessCode)
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
                .auth().oauth2(accessCode)
                .body(purchaseOrderApprovalRequest)
                .patch("/financial/purchaseOrder/{purchaseOrderPublicId}/approve", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessCode)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("ACCOUNTANT_APPROVED"));

        purchaseOrderUpdateRequest = new PurchaseOrderUpdateRequest();
        purchaseOrderUpdateRequest.setPurchaseOrderAction(PurchaseOrderAction.DISPATCH);
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessCode)
                .body(purchaseOrderUpdateRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/actions", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        String purchaseOrderNumber = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessCode)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("DISPATCHED"))
                .extract().path("purchaseOrderNumber");

        String purchaseOrderItemPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessCode)
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
                .auth().oauth2(accessCode)
                .contentType(ContentType.JSON)
                .body(purchaseOrderReceiveRequest)
                .patch("/procurement/{purchaseOrderPublicId}/receiveShipment", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        String shipmentReceivedNotePublicId = given()
                .auth().oauth2(accessCode)
                .get("/goodsReturnNote/shipmentReceived")
                .then()
                .statusCode(200)
//                .body("content[0].createdBy", notNullValue())
//                .body("content[0].noteDate", notNullValue())
//                .body("content[0].noteNumber", notNullValue())
//                .body("content[0].supplier.supplierName", notNullValue())
//                .body("content[0].status", equalToIgnoringCase("APPROVED"))
//                .body("content[0].purchaseOrderNumber", equalToIgnoringCase(purchaseOrderNumber))
//                .body("content[0].deliveryNoteNumber", notNullValue())
//                .body("content[0].purchaseOrderStatus", equalToIgnoringCase("RECEIVED"))
                .extract().path("content[0].publicId");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessCode)
                .get("/warehouse/productStock")
                .then().log().all()
                .statusCode(200);
    }

    @Ignore("GoodsReceivedRunner/GoodsReturnedRunner are commented out in production")
    @Test
    public void testRunGRTN_Migration() throws Exception {
//        goodsReceivedRunner.run();


        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/goodsReturnNote")
                .then().log().all()
                .statusCode(200);

        log.info("------------rrr");
        String publicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/goodsReturnNote/shipmentReceived")
                .then().log().all()
                .statusCode(200)
                .extract().body().path("content[0].publicId");

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/goodsReturnNote/shipmentReceived/{goodsReceivedNotePublicId}/items", UUID.fromString(publicId))
                .then().log().all()
                .statusCode(200);

//        goodsReturnedRunner.run();

        log.info("-----------GRTNPublicId -----------");
        String GRTNPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/goodsReturnNote/returnedGoodsNotes")
                .then().log().all()
                .statusCode(200)
                .extract().body().path("content[0].publicId");

        given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/goodsReturnNote/returnedGoodsNotes/{goodsReturnedNotePublicId}/items", UUID.fromString(GRTNPublicId))
                .then().log().all()
                .statusCode(200);
    }

    @Ignore
    @Test
    public void fetchPromoGoodsTestWorks22() throws IOException {
        UUID productPublicId = UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("productPublicId", productPublicId)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content[0].availableStock", greaterThanOrEqualTo(0));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].availableStock", greaterThanOrEqualTo(0));

        PurchaseOrderRequest purchaseOrderRequest = new PurchaseOrderRequest();
        purchaseOrderRequest.setOrderDate(LocalDate.now());
        purchaseOrderRequest.setSupplierId(UUID.fromString("0bacd072-df86-4d05-bf86-27a1d38cacfb"));
        purchaseOrderRequest.setWarehouseId(UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56"));
        purchaseOrderRequest.setProformaInvoiceNumber("23458");
        purchaseOrderRequest.setRebate(false);

        PurchaseOrderItemRequest purchaseOrderItemRequest = new PurchaseOrderItemRequest();
        purchaseOrderItemRequest.setProductPublicId(productPublicId);
        purchaseOrderItemRequest.setQuantity(4);
        purchaseOrderItemRequest.setUnitPrice(new BigDecimal(140));

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
                .then().log().all()
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
                .then().log().all()
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

        log.info("------------chipment---");
        String shipmentReceivedNotePublicId = given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/goodsReturnNote/shipmentReceived")
                .then().log().all()
                .statusCode(200)
                .body("content[0].purchaseOrderStatus", equalToIgnoringCase("RECEIVED"))
                .extract().path("content[0].publicId");

        log.info("------------chipment--items-");
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("productPublicId", productPublicId)
                .get("goodsReturnNote/shipmentReceived/{id}/items", shipmentReceivedNotePublicId)
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("productPublicId", productPublicId)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content[0].availableStock", equalTo(54));

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
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("productPublicId", productPublicId)
                .get("goodsReturnNote/shipmentReceived/{id}/items", shipmentReceivedNotePublicId)
                .then().log().all()
                .statusCode(200);

//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .queryParam("productPublicId", productPublicId)
//                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
//                .then().log().all()
//                .statusCode(200)
//                .body("content[0].availableStock", equalTo(64));


    }

    @Test
    public void fetchWarehouse_Products_Works() throws IOException {
        String accessCode = SUPER_ADMIN_TOKEN;

        UUID productPublicId = UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af");

        given()
                .contentType("application/json")
                .auth().oauth2(accessCode)
                .queryParam("productPublicId", productPublicId)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content[0].availableStock", greaterThanOrEqualTo(0));

        given()
                .contentType("application/json")
                .auth().oauth2(accessCode)
                .get("/inventory/{inventoryPublicId}/products", UUID.fromString("ef834e71-e55a-4ead-ac45-7617b7ee1386"))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].availableStock", greaterThanOrEqualTo(0));

        PurchaseOrderRequest purchaseOrderRequest = new PurchaseOrderRequest();
        purchaseOrderRequest.setOrderDate(LocalDate.now());
        purchaseOrderRequest.setSupplierId(UUID.fromString("0bacd072-df86-4d05-bf86-27a1d38cacfb"));
        purchaseOrderRequest.setWarehouseId(UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56"));
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
                .auth().oauth2(accessCode)
                .body(purchaseOrderRequest)
                .post("/procurement/purchaseOrder")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessCode)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then().log().all()
                .statusCode(200)
                .body("purchaseOrderItems[0].quantity", equalTo(purchaseOrderItemRequest.getQuantity()));


        PurchaseOrderUpdateRequest purchaseOrderUpdateRequest = new PurchaseOrderUpdateRequest();
        purchaseOrderUpdateRequest.setPurchaseOrderAction(PurchaseOrderAction.PROCESS);
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessCode)
                .body(purchaseOrderUpdateRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/actions", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessCode)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("SUBMITTED"));

        PurchaseOrderApprovalRequest purchaseOrderApprovalRequest = new PurchaseOrderApprovalRequest();
        purchaseOrderApprovalRequest.setActionType(ActionType.APPROVE);
        purchaseOrderApprovalRequest.setComment("This is a test purchase order");
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessCode)
                .body(purchaseOrderApprovalRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/adminApprove", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessCode)
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
                .auth().oauth2(accessCode)
                .body(purchaseOrderApprovalRequest)
                .patch("/financial/purchaseOrder/{purchaseOrderPublicId}/approve", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessCode)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("ACCOUNTANT_APPROVED"));

        purchaseOrderUpdateRequest = new PurchaseOrderUpdateRequest();
        purchaseOrderUpdateRequest.setPurchaseOrderAction(PurchaseOrderAction.DISPATCH);
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessCode)
                .body(purchaseOrderUpdateRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/actions", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        String purchaseOrderNumber = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessCode)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("DISPATCHED"))
                .extract().path("purchaseOrderNumber");

        String purchaseOrderItemPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessCode)
                .queryParam("searchParam", purchaseOrderNumber)
                .queryParam("statuses", Arrays.asList(PurchaseOrderStatus.PARTIAL_RECEIVED, PurchaseOrderStatus.DISPATCHED))
                .get("/procurement/purchaseOrders")
                .then()
                .statusCode(200)
                .extract().path("content[0].purchaseOrderItems[0].publicId");

//        PurchaseOrderReceiveRequest purchaseOrderReceiveRequest = new PurchaseOrderReceiveRequest();
//        purchaseOrderReceiveRequest.setDeliveryNoteNumber("12345");
//
//        PurchaseOrderItemReceiveRequest purchaseOrderItemReceiveRequest = new PurchaseOrderItemReceiveRequest();
//        purchaseOrderItemReceiveRequest.setOrderItemPublicId(UUID.fromString(purchaseOrderItemPublicId));
//        purchaseOrderItemReceiveRequest.setQuantityReceived(4);
//
//        purchaseOrderReceiveRequest.setPurchaseOrderItems(Arrays.asList(purchaseOrderItemReceiveRequest));
//        given()
//                .auth().oauth2(accessCode)
//                .contentType(ContentType.JSON)
//                .body(purchaseOrderReceiveRequest)
//                .patch("/procurement/{purchaseOrderPublicId}/receiveShipment", UUID.fromString(POPublicId))
//                .then()
//                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessCode)
                .get("/warehouse/productStock")
                .then().log().all()
                .statusCode(200);
    }

}
