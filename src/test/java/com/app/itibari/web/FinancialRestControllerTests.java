package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import com.app.itibari.dataTypes.ActionType;
import com.app.itibari.dataTypes.PaymentMethod;
import com.app.itibari.dataTypes.PurchaseOrderAction;
import com.app.itibari.dataTypes.RefundAction;
import com.app.itibari.models.request.PurchaseOrderApprovalRequest;
import com.app.itibari.models.request.PurchaseOrderItemRequest;
import com.app.itibari.models.request.PurchaseOrderRequest;
import com.app.itibari.models.request.PurchaseOrderUpdateRequest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;

@Slf4j
public class FinancialRestControllerTests extends ItibariApplicationTests {

    @Test
    public void test_po_invoiceWorks() throws IOException {
        String payload = productPayload("String description", UUID.fromString("a6f320b2-ad75-42a7-859e-253505242736"));
        String productPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(payload)
                .post("/products")
                .then().log().all()
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
        purchaseOrderItemRequest.setUnitPrice(new BigDecimal(100000));

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
                .then().log().all()
                .statusCode(200)
                .body("status", equalToIgnoringCase("ACCOUNTANT_APPROVED"));

        purchaseOrderApprovalRequest = new PurchaseOrderApprovalRequest();
        purchaseOrderApprovalRequest.setAmountPaid(new BigDecimal(46));
        purchaseOrderApprovalRequest.setPaymentMethod(PaymentMethod.PrePaid);
        purchaseOrderApprovalRequest.setPaymentReference("UUUID22");
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderApprovalRequest).log().all()
                .patch("/financial/purchaseOrder/{purchaseOrderPublicId}/update", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then().log().all()
                .statusCode(200)
                .body("status", equalToIgnoringCase("ACCOUNTANT_APPROVED"));

        Response response = given()
                .auth().oauth2(ADMIN_TOKEN)
                .get("/reports/purchaseOrder/{purchaseOrderId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .extract().response();

        saveExport(response, "purchase_order_invoice_2.pdf");
    }

    @Test
    public void test_update_po_Works() throws IOException {
        String payload = supplierProductPayload("String description", UUID.randomUUID(), "Bibi Tamu Sukuri");
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
                .body("purchaseOrderNumber", equalToIgnoringCase("PO-100000"))
                .body("notes", equalToIgnoringCase("This is a test purchase order"))
                .body("supplier.supplierName", equalToIgnoringCase("Africa Harmony Industry and Trade Limited_1"))
                .body("purchaseOrderItems[0].quantity", equalTo(purchaseOrderItemRequest.getQuantity()))
                .body("purchaseOrderItems[0].product.productName", equalToIgnoringCase("Bibi Tamu Sukuri"))
                .body("warehouseName", equalToIgnoringCase("Dar North"))
                .body("purchaseOrderDate", equalToIgnoringCase(LocalDate.now().toString()));

        payload = supplierProductPayload("String description update", UUID.randomUUID(), "Mr Sugar");
        productPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(payload)
                .post("/products")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        PurchaseOrderRequest updatePurchaseOrderRequest = new PurchaseOrderRequest();
        updatePurchaseOrderRequest.setOrderDate(LocalDate.now().plusMonths(3));
        updatePurchaseOrderRequest.setSupplierId(UUID.fromString("32cd5a2d-7d67-4725-9ab8-a1e3b682bda6"));
        updatePurchaseOrderRequest.setWarehouseId(UUID.fromString("6bd6caf4-f11b-4eba-bc9d-419d4668d182"));
        updatePurchaseOrderRequest.setProformaInvoiceNumber("10009");
        updatePurchaseOrderRequest.setRebate(true);
        updatePurchaseOrderRequest.setReasonCodePublicId(UUID.fromString("d88a0e04-23b7-4145-93ba-48b31efacf8d"));

        PurchaseOrderItemRequest updatePurchaseOrderItemRequest = new PurchaseOrderItemRequest();
        updatePurchaseOrderItemRequest.setProductPublicId(UUID.fromString(productPublicId));
        updatePurchaseOrderItemRequest.setQuantity(24);
        updatePurchaseOrderItemRequest.setUnitPrice(new BigDecimal(100));

        updatePurchaseOrderRequest.setPurchaseOrderItems(List.of(updatePurchaseOrderItemRequest));
        updatePurchaseOrderRequest.setComment("update purchase order");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(updatePurchaseOrderRequest).log().all()
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then().log().all()
                .statusCode(200)
                .body("status", equalToIgnoringCase("PENDING"))
                .body("purchaseOrderNumber", equalToIgnoringCase("PO-100000"))
                .body("notes", equalToIgnoringCase("update purchase order"))
                .body("supplier.supplierName", equalToIgnoringCase("Africa Harmony Industry and Trade Limited"))
                .body("purchaseOrderItems[0].quantity", equalTo(updatePurchaseOrderItemRequest.getQuantity()))
                .body("purchaseOrderItems[0].product.productName", equalToIgnoringCase("Mr Sugar"))
                .body("warehouseName", equalToIgnoringCase("Arusha Warehouse"));
//                .body("purchaseOrderDate", equalToIgnoringCase(LocalDate.now()..toString()));
    }

    @Test
    @Ignore
    public void testApproveRefundRequest_Works() throws IOException {
        String POPublicId = createAndDispatchPurchaseOrder("APPROVE-REFUND-TEST");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/refund", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("REFUND_REQUESTED"));

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("action", RefundAction.APPROVE)
                .patch("/financial/purchaseOrder/{purchaseOrderPublicId}/refundApprove", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("REFUND_APPROVED"));
    }

    @Test
    @Ignore
    public void testRejectRefundRequest_Works() throws IOException {
        String POPublicId = createAndDispatchPurchaseOrder("REJECT-REFUND-TEST");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/refund", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("REFUND_REQUESTED"));

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("action", RefundAction.REJECT)
                .patch("/financial/purchaseOrder/{purchaseOrderPublicId}/refundApprove", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("REFUND_REJECTED"));
    }

    private String createAndDispatchPurchaseOrder(String proformaInvoiceNumber) throws IOException {
        String payload = productPayload("Refund test product", UUID.randomUUID());
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
        purchaseOrderRequest.setProformaInvoiceNumber(proformaInvoiceNumber);
        purchaseOrderRequest.setRebate(false);

        PurchaseOrderItemRequest itemRequest = new PurchaseOrderItemRequest();
        itemRequest.setProductPublicId(UUID.fromString(productPublicId));
        itemRequest.setQuantity(10);
        itemRequest.setUnitPrice(new BigDecimal(500));
        purchaseOrderRequest.setPurchaseOrderItems(List.of(itemRequest));
        purchaseOrderRequest.setComment("Refund flow test purchase order");

        String POPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(purchaseOrderRequest)
                .post("/procurement/purchaseOrder")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        PurchaseOrderUpdateRequest updateRequest = new PurchaseOrderUpdateRequest();
        updateRequest.setPurchaseOrderAction(PurchaseOrderAction.PROCESS);
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(updateRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/actions", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        PurchaseOrderApprovalRequest adminApproval = new PurchaseOrderApprovalRequest();
        adminApproval.setActionType(ActionType.APPROVE);
        adminApproval.setComment("Admin approval");
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(adminApproval)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/adminApprove", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        PurchaseOrderApprovalRequest financeApproval = new PurchaseOrderApprovalRequest();
        financeApproval.setActionType(ActionType.APPROVE);
        financeApproval.setAmountPaid(new BigDecimal(5000));
        financeApproval.setPaymentMethod(PaymentMethod.PrePaid);
        financeApproval.setComment("Finance approval");
        financeApproval.setPaymentReference("REF-" + proformaInvoiceNumber);
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(financeApproval)
                .patch("/financial/purchaseOrder/{purchaseOrderPublicId}/approve", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        updateRequest = new PurchaseOrderUpdateRequest();
        updateRequest.setPurchaseOrderAction(PurchaseOrderAction.DISPATCH);
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(updateRequest)
                .patch("/procurement/purchaseOrder/{purchaseOrderPublicId}/actions", UUID.fromString(POPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/procurement/purchaseOrder/{purchaseOrderPublicId}", UUID.fromString(POPublicId))
                .then()
                .statusCode(200)
                .body("status", equalToIgnoringCase("DISPATCHED"));

        return POPublicId;
    }
}
