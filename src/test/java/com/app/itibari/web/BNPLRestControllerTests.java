package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import com.app.itibari.dataTypes.*;
import com.app.itibari.models.entities.CreditLimitItem;
import com.app.itibari.models.entities.Customers;
import com.app.itibari.models.entities.SalesOrders;
import com.app.itibari.models.entities.Wallet;
import com.app.itibari.models.request.*;
import com.app.itibari.repository.CreditLimitItemRepository;
import com.app.itibari.repository.CustomersRepository;
import com.app.itibari.repository.SalesOrdersRepository;
import com.app.itibari.repository.WalletRepository;
import com.app.itibari.scheduler.Scheduler;
import com.app.itibari.services.BnplCrmService;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Slf4j
public class BNPLRestControllerTests extends ItibariApplicationTests {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private CreditLimitItemRepository creditLimitItemRepository;

    @Autowired
    private SalesOrdersRepository salesOrdersRepository;

    @Autowired
    private BnplCrmService bnplCrmService;

    @Autowired
    private CustomersRepository customersRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Test
    public void bnplCustomersTests() {

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/customers")
                .then().log().all()
                .statusCode(200);

    }

    @Test
    public void testTopCreditCustomers() {

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/customers/topCredit")
                .then().log().all()
                .statusCode(200);

    }

    // eligible customers for BNPL
    @Test
    public void testEligibleBNPLCustomers() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/eligibleCustomers")
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void testQualifiedBNPLCrmLeads() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/crm/leads")
                .then().log().all()
                .statusCode(200);
    }

    // churned BNPL customers

    @Test
    public void testChurnedBNPLCustomers() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/churnedCustomers")
                .then().log().all()
                .statusCode(200);


// as SA now
        given()
                .contentType("application/json")
                .auth().oauth2(SALES_AGENT_TOKEN)
                .get("/bnpl/churnedCustomers")
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void testChurnedCreditCustomers() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/churnedCustomers")
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(SALES_AGENT_TOKEN)
                .get("/bnpl/churnedCustomers")
                .then().log().all()
                .statusCode(200);
    }

    // credit sales report

    @Test
    public void testCreditSalesReport() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/creditSales")
                .then().log().all()
                .statusCode(200);
    }


    @Test
    public void testBNPLCustomerPurchaseTrend() {
        UUID customerPublicId = UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("year", 2024)
                .get("/bnpl/customerPurchaseTrend/{customerId}", customerPublicId)
                .then().log().all()
                .statusCode(200);
    }


    @Ignore
    @Test
    public void testBNPLCreditStatusDashboard() {

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/creditStatusDashboard")
                .then().log().all()
                .statusCode(200)
                .body("customersOnCredit", notNullValue())
                .body("totalCreditOrders", notNullValue())
                .body("healthyCreditCustomers", notNullValue())
                .body("churnedCreditCustomers", notNullValue())
                .body("defaulterCreditCustomers", notNullValue())
                .body("blacklistedCreditCustomers", notNullValue());
    }

    // onboard BNPL customer

    @Test
    @Ignore // works alone but fails when run with other tests
    public void testOnboardBNPLCustomer() {
        UUID customerPublicId2 = UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d");
        // UUID customerPublicId2 = UUID.fromString("6701ef3b-6e22-496e-9739-2e6fdc61cb74");


        // update customer type

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("customerType", CustomerType.REGULAR)
                .patch("/customers/{customerPublicId}/changeCustomerType", customerPublicId2)
                .then().log().all()
                .statusCode(200);

        BNPLOnboarding request = new BNPLOnboarding();
        request.setCustomerPublicId(customerPublicId2);
        request.setCustomerID(faker.idNumber().valid());
        request.setTinNumber(faker.numerify(8 + "########"));
        request.setGuarantorName(faker.name().fullName());
        request.setGuarantorPhoneNumber(faker.phoneNumber().cellPhone());
        request.setBusinessLicenseNumber(faker.business().creditCardNumber());

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .log().all()
                .post("/bnpl/onboardCustomer")
                .then()
                .statusCode(200);


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/customers")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));

        String walletPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/wallets")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");


        // get wallet payments

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/wallets/{walletId}/payments", UUID.fromString(walletPublicId))
                .then().log().all()
                .statusCode(200);


        // get wallet credits

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/wallets/{walletId}/creditLimitItems", UUID.fromString(walletPublicId))
                .then().log().all()
                .statusCode(200);


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/paymentSummary")
                .then().log().all()
                .statusCode(200);


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/creditPayments")
                .then().log().all()
                .statusCode(200);

        // create a child customer, then check if wallet is created

    }


    @Ignore
    @Test
    public void testBNPLOrderPurchases() throws IOException {
        UUID customerPublicIdNew = UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d");
        UUID customerPublicId = UUID.fromString("c1793a8b-1fb8-4c16-b04b-d3c087c00c6f");
        UUID productPublicId = UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af");


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("customerType", CustomerType.REGULAR)
                .patch("/customers/{customerPublicId}/changeCustomerType", customerPublicId)
                .then()
                .statusCode(200);


        CreateCreditRuleRequest creditRuleRequest = new CreateCreditRuleRequest();
        creditRuleRequest.setCustomerType(CustomerType.REGULAR);
        creditRuleRequest.setIsCreditLimit(true);
        creditRuleRequest.setCreditLimitPercentage(new BigDecimal("35.5"));
        creditRuleRequest.setIsRepaymentTerm(false);
        creditRuleRequest.setRuleName("Regular Credit Limit Rule");
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(creditRuleRequest)
                .post("/settings/creditRule")
                .then()
                .statusCode(200);


        creditRuleRequest = new CreateCreditRuleRequest();
        creditRuleRequest.setCustomerType(CustomerType.REGULAR);
        creditRuleRequest.setIsRepaymentTerm(true);
        creditRuleRequest.setRepaymentTermDays(60);
        creditRuleRequest.setRuleName("Regular Repayment Term Rule");
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(creditRuleRequest)
                .post("/settings/creditRule")
                .then()
                .statusCode(200);


        creditRuleRequest = new CreateCreditRuleRequest();
        creditRuleRequest.setCustomerType(CustomerType.REGULAR);
        creditRuleRequest.setIsInterestRate(true);
        creditRuleRequest.setInterestRatePercentage(new BigDecimal("5.5"));
        creditRuleRequest.setRuleName("Regular Interest Rate Rule");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(creditRuleRequest)
                .post("/settings/creditRule")
                .then()
                .statusCode(200);


        BNPLOnboarding request = new BNPLOnboarding();
        request.setCustomerPublicId(customerPublicId);
        request.setCustomerID(faker.idNumber().valid());
        request.setTinNumber(faker.numerify(8 + "########"));
        request.setGuarantorName(faker.name().fullName());
        request.setGuarantorPhoneNumber(faker.phoneNumber().cellPhone());
        request.setBusinessLicenseNumber(faker.business().creditCardNumber());

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .log().all()
                .post("/bnpl/onboardCustomer")
                .then()
                .statusCode(200);


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/customers")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));

        String walletPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/wallets")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");


        SalesOrderItemsRequest orderItem = SalesOrderItemsRequest
                .builder()
                .productId(productPublicId)
                .quantity(4)
                .build();
        SalesOrdersRequest salesRequest = SalesOrdersRequest.builder()
                .customerId(customerPublicId)
                .bookedBy("Itibari Test")
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .salesOrderItems(Collections.singleton(orderItem))
                .buyOnCredit(true)
                .build();

        String orderPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(salesRequest)
                .post("/salesOrders")
                .then().log().all()
                .statusCode(200)
                .extract().path("publicId");


        // get wallet payments
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .get("/bnpl/wallets/{walletId}/payments", UUID.fromString(walletPublicId))
//                .then()
//                .statusCode(200);
//
//
//        // get wallet credits
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .get("/bnpl/wallets/{walletId}/creditLimitItems", UUID.fromString(walletPublicId))
//                .then()
//                .statusCode(200);
//
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .get("/bnpl/customerPurchaseTrend/{customerId}", customerPublicId)
//                .then()
//                .statusCode(200);

        // plan order,deliver, pay etc. can be added here later

        String orderItemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}/items", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");


        scheduler.runOrderPlanning();


        String orderNumber = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.PLANNING.name()))
                .extract().path("salesOrderNumber");


        RoutePlanningRequest routePlanningRequest = new RoutePlanningRequest();
        routePlanningRequest.setLocationPublicId(UUID.fromString("90ad01f1-4a13-41f7-b825-c428b41f35d0"));
        routePlanningRequest.setRegionPublicId(UUID.fromString("8c5d4db0-f9a1-47ed-a160-56780e4e4d14"));
        routePlanningRequest.setVehiclePublicId(UUID.fromString("fa4bf2ee-6665-493a-8dbd-5d56dc8a7d93"));
        routePlanningRequest.setDeliveryAgentPublicId(UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65"));
        routePlanningRequest.setWarehousePublicId(UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56"));
        routePlanningRequest.setSalesOrdersPublicIds(Collections.singletonList(UUID.fromString(orderPublicId)));

        String routePublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(routePlanningRequest)
                .post("/logistics/routePlanning")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        String deliveryId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{routePlanningPublicId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .extract().path("routeId");

        String productItemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{routePlanningPublicId}/loadingProducts", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].productItemPublicId");


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


        given()
                .contentType("application/json")
                .auth().oauth2(DELIVERY_AGENT_TOKEN)
                .header("X-App-Version", "1.0.0")
                .queryParam("acceptType", AcceptTypeEnum.AcceptRoute)
                .patch("/deliveryAgents/accept/{routeId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200);


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


        // get wallet credits

        Response creditItemBeforeDelivery = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/wallets/{walletId}/creditLimitItems", UUID.fromString(walletPublicId))
                .then()
                .statusCode(200)
                .extract().response();

        BigDecimal initialCreditAmount = BigDecimal.valueOf(((Number) creditItemBeforeDelivery.path("content[0].amount")).doubleValue());
        BigDecimal initialInterestAmount = BigDecimal.valueOf(((Number) creditItemBeforeDelivery.path("content[0].interestAmount")).doubleValue());


        // partial deliver products

        DeliverItemRequest deliverItemRequest = new DeliverItemRequest();
        deliverItemRequest.setSalesOrderPublicId(UUID.fromString(orderPublicId));
        deliverItemRequest.setRoutePublicId(UUID.fromString(routePublicId));

        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setProductItemPublicId(UUID.fromString(orderItemPublicId));
        itemRequest.setProductReconItemPublicId(UUID.fromString(orderItemPublicId));
        itemRequest.setQuantitySold(2);
        deliverItemRequest.setItems(Collections.singletonList(itemRequest));

        given()
                .contentType("application/json")
                .auth().oauth2(DELIVERY_AGENT_TOKEN)
                .header("X-App-Version", "1.0.0")
                .body(deliverItemRequest)
                .patch("/deliveryAgents/markOrderItemAsDelivered")
                .then()
                .statusCode(200);

        // mark order as delivered

        given()
                .contentType("application/json")
                .auth().oauth2(DELIVERY_AGENT_TOKEN)
                .header("X-App-Version", "1.0.0")
                .patch("/deliveryAgents/markOrderAsDelivered/{orderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200);


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.DELIVERED.name()));

        Response creditItemAfterDelivery = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/wallets/{walletId}/creditLimitItems", UUID.fromString(walletPublicId))
                .then()
                .statusCode(200)
                .extract().response();

        BigDecimal updatedCreditAmount = BigDecimal.valueOf(((Number) creditItemAfterDelivery.path("content[0].amount")).doubleValue());
        BigDecimal updatedInterestAmount = BigDecimal.valueOf(((Number) creditItemAfterDelivery.path("content[0].interestAmount")).doubleValue());

        BigDecimal expectedInterestAmount = updatedCreditAmount.multiply(new BigDecimal("5.5"))
                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);

        assertTrue("Expected recalculated credit amount after partial delivery to reduce",
                updatedCreditAmount.compareTo(initialCreditAmount) < 0);
        assertTrue("Expected recalculated interest amount after partial delivery to reduce",
                updatedInterestAmount.compareTo(initialInterestAmount) < 0);
        assertTrue("Expected recalculated interest amount to match recalculated credit amount",
                updatedInterestAmount.subtract(expectedInterestAmount).abs().compareTo(new BigDecimal("0.05")) <= 0);

        Response orderAfterDelivery = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .extract().response();

        String orderTypeAfterDelivery = orderAfterDelivery.path("orderType");
        BigDecimal orderCreditAmount = BigDecimal.valueOf(((Number) orderAfterDelivery.path("creditAmount")).doubleValue());
        BigDecimal orderInterestAmount = BigDecimal.valueOf(((Number) orderAfterDelivery.path("interestAmount")).doubleValue());

        assertTrue("Expected order type to remain a credit flow order type after recalculation",
                OrderType.CREDIT.name().equals(orderTypeAfterDelivery) || OrderType.COMBO.name().equals(orderTypeAfterDelivery));
        assertTrue("Expected order credit amount to match recalculated credit item amount",
                orderCreditAmount.subtract(updatedCreditAmount).abs().compareTo(new BigDecimal("0.05")) <= 0);
        assertTrue("Expected order interest amount to match recalculated credit item interest amount",
                orderInterestAmount.subtract(updatedInterestAmount).abs().compareTo(new BigDecimal("0.05")) <= 0);

        given()
                .contentType("application/json")
                .auth().oauth2(DELIVERY_AGENT_TOKEN)
                .header("X-App-Version", "1.0.0")
                .queryParam("routeId", UUID.fromString(routePublicId))
                .queryParam("action", RouteAction.end)
                .patch("/deliveryAgents/routeActions")
                .then()
                .statusCode(200);


        ProductItemRequest productItemRequest = new ProductItemRequest();
        productItemRequest.setProductItemPublicId(UUID.fromString(productReconItemId));
        productItemRequest.setQuantitySold(2);

        ProductReconAction productReconAction = new ProductReconAction();
        productReconAction.setProductItems(Collections.singletonList(productItemRequest));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(productReconAction)
                .patch("/warehouse/productRecons/{publicId}", UUID.fromString(productReconId))
                .then()
                .statusCode(200);

        // reconcile product recon

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .patch("/warehouse/productRecons/{publicId}/reconcile", UUID.fromString(productReconId))
                .then()
                .statusCode(200);


        String reconId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/reconciliation/creditRecons")
                .then().log().all()
                .statusCode(200)
                .extract().path("content[0].publicId");

        // get wallet credits
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("comment", "Reconciled from test")
                .patch("/reconciliation/creditRecons/{publicId}/reconcile", UUID.fromString(reconId))
                .then().log().all()
                .statusCode(200)
                .extract().path("content[0].publicId");


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/wallets/{walletId}/creditLimitItems", UUID.fromString(walletPublicId))
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].reconciliationComment", equalTo("Reconciled from test"));

        // pay

//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .queryParam("amount", new BigDecimal("500000"))
//                .patch("/bnpl/{customerPublicId}/pay", customerPublicId)
//                .then()
//                .statusCode(200);
//
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .get("/reconciliation/creditRecons")
//                .then().log().all()
//                .statusCode(200);
//        // get wallet credits
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .get("/bnpl/wallets/{walletId}/creditLimitItems", UUID.fromString(walletPublicId))
//                .then().log().all()
//                .statusCode(200);
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .get("/bnpl/wallets/{walletId}/transactionItems", UUID.fromString(walletPublicId))
//                .then().log().all()
//                .statusCode(200);
//
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .get("/bnpl/wallets/{walletId}/payments", UUID.fromString(walletPublicId))
//                .then().log().all()
//                .statusCode(200);


//
//
//        // fetch wallet payments
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .get("/bnpl/wallets/{walletId}/payments", UUID.fromString(walletPublicId))
//                .then().log().all()
//                .statusCode(200);
//
//
//        // fetch wallet credits
//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .get("/bnpl/wallets/{walletId}/creditLimitItems", UUID.fromString(walletPublicId))
//                .then().log().all()
//                .statusCode(200);
//
//
//        // fetch feedback
//
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .get("/bnpl/customerFeedbackTicket")
//                .then().log().all()
//                .statusCode(200);
//
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .get("/bnpl/creditPayments")
//                .then().log().all()
//                .statusCode(200);
//
//
//        String requestBody = """
//                {
//                    "code": "CUST_REJ_001",
//                    "description": "Customer Blacklist Reason 001",
//                    "module": "BnplBlacklistReason"
//                }
//                """;
//
//        String reasonPublicId = given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .body(requestBody)
//                .post("/rejectionCodes")
//                .then()
//                .statusCode(200)
//                .extract().path("publicId");
//
//
//        // blacklist customer
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .queryParam("action", WalletAction.Blacklist)
//                .queryParam("reasonPublicId", UUID.fromString(reasonPublicId))
//                .patch("/bnpl/{customerPublicId}/actions", customerPublicId)
//                .then().log().all()
//                .statusCode(200);
//
//
//        // fetch orders on credit
//
//        given()
//                .contentType("application/json")
//                .auth().oauth2(ADMIN_TOKEN)
//                .queryParam("isCredit", true)
//                .get("/salesOrders")
//                .then()
//                .statusCode(200)
//                .body("content.size()", greaterThanOrEqualTo(1));
//
//
//        Response walletExport = given()
//                .auth().oauth2(ADMIN_TOKEN)
//                .queryParam("walletId", UUID.fromString(walletPublicId))
//                .queryParam("exportType", StatementExportType.EXCEL)
//                .get("/reports/customerCreditItemsReportExport")
//                .then()
//                .statusCode(200)
//                .extract().response();
//
//        // saveExport(walletExport, "customerReportExcel.xlsx");
//
//        Response walletExportPdf = given()
//                .auth().oauth2(ADMIN_TOKEN)
//                .queryParam("walletId", UUID.fromString(walletPublicId))
//                .queryParam("exportType", StatementExportType.PDF)
//                .get("/reports/customerCreditItemsReportExport")
//                .then()
//                .statusCode(200)
//                .extract().response();
//
//        saveExport(walletExportPdf, "customerReportExcel.pdf");
//
//        Response rece = given()
//                .auth().oauth2(ADMIN_TOKEN)
//                .queryParam("orderType", OrderType.CREDIT)
//                .get("/reports/{salesOrderPublicId}/customerInvoiceReceipt", UUID.fromString(orderPublicId))
//                .then()
//                .statusCode(200)
//                .extract().response();
//
//        //  saveExport(rece, "recr.pdf");

    }


    // tests for BNPLs


    @Test
    public void fetchVerifiedCustomerTest() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/customers/{customerPublicId}", UUID.fromString("11264d01-098e-407a-978d-2296b12425b6"))
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void testFetchBNPLWallets() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/wallets")
                .then().log().all()
                .statusCode(200);
    }


    @Test
    public void testFetchBNPLCustomers() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/customers")
                .then().log().all()
                .statusCode(200);
    }

    @Ignore
    @Test
    public void testWalletActions() {
        UUID customerPublicId = UUID.fromString("11264d01-098e-407a-978d-2296b12425b6");
        String tinNumber = "123456789";

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("action", WalletAction.RecalculateLimit)
                .queryParam("amount", new BigDecimal("15000"))
                .patch("/bnpl/{customerPublicId}/actions", customerPublicId)
                .then().log().all()
                .statusCode(200);


        // fetch wallet and check limit

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/wallets")
                .then().log().all()
                .statusCode(200)
                .body("content[0].creditLimit", equalTo(10000f));


        // check customer credit limit

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/customers/{customerPublicId}", UUID.fromString("11264d01-098e-407a-978d-2296b12425b6"))
                .then().log().all()
                .statusCode(200)
                .body("creditLimit", equalTo(15000f));


    }

    @Test
    public void testCustomerCreditPayment() {
        UUID customerPublicId = UUID.fromString("37946b2b-b08d-4117-9927-f8adcf32292f");
        UUID walletPublicId = UUID.fromString("13b5e125-8e65-4bf7-8144-1d35ca2cadd4");
        String orderNumber = "ORD101009";

        // Reset seed wallet/credit item so earlier BNPL journeys in this class cannot poison payment.
        jdbcTemplate.update(
                "update itibari.wallet set wallet_balance = 5000 where public_id = ?",
                walletPublicId.toString());
        jdbcTemplate.update(
                "update itibari.credit_items set payment_status = 'NOT_PAID', fully_paid = false, "
                        + "paid_amount = 0, cancelled = false, status = 'ACTIVE', "
                        + "order_public_id = 'ef2f4703-b478-4ca6-98d1-bc3aa88e1c3f' "
                        + "where order_number = ?",
                orderNumber);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("customerPublicId", UUID.fromString(customerPublicId.toString()))
                .get("/bnpl/wallets")
                .then().log().all()
                .statusCode(200);


        //fetch wallet
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/wallets/{walletId}/creditLimitItems", walletPublicId)
                .then().log().all()
                .statusCode(200)
                .body("content[0].walletBalance", equalTo(5000f));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("referenceId", customerPublicId)
                .queryParam("orderNumber", "SO-000001")
                .queryParam("payViaWallet", true)
                .queryParam("hasFile", false)
                .post("/bnpl/customerCreditPay")
                .then()
                .statusCode(400)
                .body("userMessage", equalTo("Invalid orderNumber format. orderNumber should start with 'ORD'"));

        Optional<CreditLimitItem> optionalCreditLimitItem = creditLimitItemRepository.findByOrderNumberAndDeletedIsFalse(orderNumber);
        assertTrue("Expected credit item to exist before payment", optionalCreditLimitItem.isPresent());
        CreditLimitItem creditLimitItem = optionalCreditLimitItem.get();
        creditLimitItem.setOrderPublicId("invalid-order-public-id");
        creditLimitItemRepository.save(creditLimitItem);


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("referenceId", customerPublicId)
                .queryParam("orderNumber", orderNumber)
                .queryParam("payViaWallet", true)
                .queryParam("hasFile", false)
                .post("/bnpl/customerCreditPay")
                .then()
                .statusCode(200);


        // fetch credit payments and check if payment is reflected


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/wallets/{walletId}/creditLimitItems", walletPublicId)
                .then().log().all()
                .statusCode(200)
                .body("content[0].balanceAmount", equalTo(0f))
                .body("content[0].paymentStatus", equalTo(PaymentStatusEnum.PAID.name()))
                .body("content[0].fullyPaid", equalTo(true))
                .body("content[0].approvedBy", notNullValue())
                .body("content[0].paymentReference", notNullValue())
                .body("content[0].walletBalance", equalTo(2971.69f));

        Optional<SalesOrders> paidOrder = salesOrdersRepository.findBySalesOrderNumberAndDeletedIsFalse(orderNumber);
        assertTrue("Expected order to exist after payment", paidOrder.isPresent());
        assertEquals("Expected payment status to be PAID after full settlement", PaymentStatusEnum.PAID, paidOrder.get().getPaymentStatus());


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("customerPublicId", UUID.fromString(customerPublicId.toString()))
                .get("/bnpl/wallets")
                .then().log().all()
                .statusCode(200);

    }


    @Test
    public void testRecalculateFineSchedular() {
        scheduler.bnplFineRecalculation();
    }

    // ─── BNPL CRM tests ──────────────────────────────────────────────────────

    @Test
    public void testBnplCrmGetLeadsEmpty() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/crm/leads")
                .then().log().all()
                .statusCode(200)
                .body("content", notNullValue());
    }

    @Test
    public void testBnplCrmGetReasonCodesEmpty() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/crm/reason-codes")
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void testBnplCrmLeadNotFound() {
        UUID unknownId = UUID.randomUUID();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/crm/leads/{leadPublicId}", unknownId)
                .then().log().all()
                .statusCode(400)
                .body("userMessage", notNullValue());

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .post("/bnpl/crm/leads/{leadPublicId}/calls", unknownId)
                .then()
                .statusCode(400);

        BnplFieldVisitRequest visitReq = new BnplFieldVisitRequest();
        visitReq.setVisitDate(LocalDate.now());
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(visitReq)
                .post("/bnpl/crm/leads/{leadPublicId}/visits", unknownId)
                .then()
                .statusCode(400);
    }

    @Test
    public void testBnplCrmReasonCodeCrud() {
        String uniqueCode = "RC_" + faker.number().digits(6);

        BnplReasonCodeRequest createReq = new BnplReasonCodeRequest();
        createReq.setCode(uniqueCode);
        createReq.setDescription("Customer requested callback at a later time");
        createReq.setIsActive(true);

        String codePublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(createReq)
                .log().all()
                .post("/bnpl/crm/reason-codes")
                .then().log().all()
                .statusCode(200)
                .body("publicId", notNullValue())
                .body("code", equalTo(uniqueCode))
                .body("isActive", equalTo(true))
                .extract().path("publicId");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/crm/reason-codes")
                .then().log().all()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1));

        BnplReasonCodeRequest updateReq = new BnplReasonCodeRequest();
        updateReq.setDescription("Updated - do not use");
        updateReq.setIsActive(false);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(updateReq)
                .patch("/bnpl/crm/reason-codes/{codePublicId}", UUID.fromString(codePublicId))
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/crm/reason-codes")
                .then()
                .statusCode(200)
                .body("findAll { it.publicId == '" + codePublicId + "' }.size()", equalTo(0));
    }

    @Test
    @Ignore("works alone")
    public void testBnplCrmFullFlow() {
        bnplCrmService.createLeads();

        String leadPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/crm/leads")
                .then().log().all()
                .statusCode(200)
                .body("content", not(empty()))
                .extract().path("content[0].publicId");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/crm/leads/{leadPublicId}", UUID.fromString(leadPublicId))
                .then().log().all()
                .statusCode(200)
                .body("publicId", equalTo(leadPublicId))
                .body("customerName", notNullValue())
                .body("totalCallAttempts", notNullValue())
                .body("funnelStage", equalTo(BnplFunnelStage.PROSPECT.name()));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("funnelStage", BnplFunnelStage.PROSPECT.name())
                .get("/bnpl/crm/leads")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("search", "a")
                .get("/bnpl/crm/leads")
                .then()
                .statusCode(200);

        String uniqueCode = "INTERESTED_" + faker.number().digits(5);
        BnplReasonCodeRequest rcReq = new BnplReasonCodeRequest();
        rcReq.setCode(uniqueCode);
        rcReq.setDescription("Customer expressed clear interest");
        rcReq.setIsActive(true);

        String reasonCodeId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(rcReq)
                .post("/bnpl/crm/reason-codes")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        String callPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .post("/bnpl/crm/leads/{leadPublicId}/calls", UUID.fromString(leadPublicId))
                .then().log().all()
                .statusCode(200)
                .body("publicId", notNullValue())
                .body("attemptNumber", equalTo(1))
                .body("leadPublicId", equalTo(leadPublicId))
                .body("startTime", notNullValue())
                .extract().path("publicId");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/crm/leads/{leadPublicId}", UUID.fromString(leadPublicId))
                .then()
                .statusCode(200)
                .body("totalCallAttempts", equalTo(1))
                .body("lastCalledAt", notNullValue());

        BnplCallOutcomeRequest answeredReq = new BnplCallOutcomeRequest();
        answeredReq.setCallStatus(BnplCallStatus.ANSWERED);
        answeredReq.setLeadStatus(BnplLeadStatus.GREEN);
        answeredReq.setNotes("Customer is interested and ready for a credit officer visit.");
        answeredReq.setReasonCodePublicIds(List.of(UUID.fromString(reasonCodeId)));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(answeredReq)
                .patch("/bnpl/crm/calls/{callPublicId}/outcome", UUID.fromString(callPublicId))
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/crm/leads/{leadPublicId}", UUID.fromString(leadPublicId))
                .then()
                .statusCode(200)
                .body("leadStatus", equalTo(BnplLeadStatus.GREEN.name()))
                .body("lastReason", notNullValue());

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/crm/leads/{leadPublicId}/calls", UUID.fromString(leadPublicId))
                .then().log().all()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].callStatus", equalTo(BnplCallStatus.ANSWERED.name()))
                .body("content[0].notes", notNullValue())
                .body("content[0].reasonCodes.size()", equalTo(1));

        String callPublicId2 = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .post("/bnpl/crm/leads/{leadPublicId}/calls", UUID.fromString(leadPublicId))
                .then()
                .statusCode(200)
                .body("attemptNumber", equalTo(2))
                .extract().path("publicId");

        BnplCallOutcomeRequest noAnswerReq = new BnplCallOutcomeRequest();
        noAnswerReq.setCallStatus(BnplCallStatus.NO_ANSWER);
        noAnswerReq.setNotes("Phone rang with no response.");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(noAnswerReq)
                .patch("/bnpl/crm/calls/{callPublicId}/outcome", UUID.fromString(callPublicId2))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/crm/leads/{leadPublicId}/calls", UUID.fromString(leadPublicId))
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(2))
                .body("content[0].callStatus", equalTo(BnplCallStatus.NO_ANSWER.name()));

        BnplLeadStatusRequest yellowReq = new BnplLeadStatusRequest();
        yellowReq.setLeadStatus(BnplLeadStatus.YELLOW);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(yellowReq)
                .patch("/bnpl/crm/leads/{leadPublicId}/status", UUID.fromString(leadPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("leadStatus", BnplLeadStatus.YELLOW.name())
                .get("/bnpl/crm/leads")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));

        BnplCallbackFlagRequest callbackReq = new BnplCallbackFlagRequest();
        callbackReq.setCallbackDate(LocalDate.now().plusDays(3));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(callbackReq)
                .patch("/bnpl/crm/leads/{leadPublicId}/callback", UUID.fromString(leadPublicId))
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/crm/leads/{leadPublicId}", UUID.fromString(leadPublicId))
                .then()
                .statusCode(200)
                .body("callbackFlagged", equalTo(true))
                .body("callbackDate", notNullValue());

        BnplLeadStatusRequest greenReq = new BnplLeadStatusRequest();
        greenReq.setLeadStatus(BnplLeadStatus.GREEN);
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(greenReq)
                .patch("/bnpl/crm/leads/{leadPublicId}/status", UUID.fromString(leadPublicId))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(callbackReq)
                .patch("/bnpl/crm/leads/{leadPublicId}/callback", UUID.fromString(leadPublicId))
                .then()
                .statusCode(400)
                .body("userMessage", containsString("Yellow"));

        BnplFieldVisitRequest visitReq = new BnplFieldVisitRequest();
        visitReq.setVisitDate(LocalDate.now());
        visitReq.setNextVisitDate(LocalDate.now().plusDays(7));
        visitReq.setOutcome("Customer was present and agreed to proceed with BNPL.");
        visitReq.setNotes("Collected NID copy and took photos of the shop.");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(visitReq)
                .post("/bnpl/crm/leads/{leadPublicId}/visits", UUID.fromString(leadPublicId))
                .then().log().all()
                .statusCode(200)
                .body("publicId", notNullValue())
                .body("visitDate", notNullValue())
                .body("outcome", notNullValue());

        log.info("--------notes");
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/crm/leads/{leadPublicId}", UUID.fromString(leadPublicId))
                .then().log().all()
                .statusCode(200)
                .body("lastFieldVisitDate", notNullValue());

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/crm/leads/{leadPublicId}/visits", UUID.fromString(leadPublicId))
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].visitDate", notNullValue())
                .body("content[0].outcome", notNullValue());
    }

    @Test
    @Ignore("wprks alone")
    public void testBnplCrmCallbackRejectedOnNonYellowLead() {
        bnplCrmService.createLeads();

        String leadPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/crm/leads")
                .then()
                .statusCode(200)
                .body("content", not(empty()))
                .extract().path("content[0].publicId");

        BnplLeadStatusRequest redReq = new BnplLeadStatusRequest();
        redReq.setLeadStatus(BnplLeadStatus.RED);
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(redReq)
                .patch("/bnpl/crm/leads/{leadPublicId}/status", UUID.fromString(leadPublicId))
                .then()
                .statusCode(200);

        BnplCallbackFlagRequest callbackReq = new BnplCallbackFlagRequest();
        callbackReq.setCallbackDate(LocalDate.now().plusDays(2));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(callbackReq)
                .patch("/bnpl/crm/leads/{leadPublicId}/callback", UUID.fromString(leadPublicId))
                .then().log().all()
                .statusCode(400)
                .body("userMessage", containsString("Yellow"));
    }


    @Test
    public void testCorporateJourney() {
        UUID customerPublicId = UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("customerType", CustomerType.CORPORATE)
                .patch("/customers/{customerPublicId}/changeCustomerType", customerPublicId)
                .then()
                .statusCode(200);


        // get requests
        String approvalId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/corporateApprovalRequests")
                .then().log().all()
                .statusCode(200)
                .extract().path("content[0].publicId");

        // approve

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("action", ApprovalAction.APPROVE)
                .patch("/bnpl/corporateApprovalRequests/{publicId}", UUID.fromString(approvalId))
                .then()
                .statusCode(200);


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/corporateApprovalRequests")
                .then().log().all()
                .statusCode(200)
                .body("content[0].actionedBy", notNullValue())
                .body("content[0].actionedOn", notNullValue())
                .body("content[0].approvalStatus", equalTo(CorporateApprovalStatus.APPROVED.name()));


        // fetch wallet

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("customerPublicId", customerPublicId)
                .get("/bnpl/wallets")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThan(0));


    }

    // ─── Credit Batch tests ───────────────────────────────────────────────────

    @Test
    public void testCreateCreditBatchValidationErrors() {
        // null parentCustomerPublicId and creditItemPublicIds → 400
        CreditBatchRequest emptyRequest = new CreditBatchRequest();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(emptyRequest)
                .post("/bnpl/creditBatch")
                .then().log().all()
                .statusCode(400)
                .body("userMessage", notNullValue());

        // empty credit item list → 400
        CreditBatchRequest noItems = new CreditBatchRequest();
        noItems.setParentCustomerPublicId(UUID.fromString("ef2f4703-b478-4ca6-98d1-bc3aa88e1c3f"));
        noItems.setCreditItemPublicIds(Collections.emptyList());

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(noItems)
                .post("/bnpl/creditBatch")
                .then().log().all()
                .statusCode(400)
                .body("userMessage", notNullValue());

        // unknown credit item uuid → 400
        CreditBatchRequest unknownItem = new CreditBatchRequest();
        unknownItem.setParentCustomerPublicId(UUID.fromString("ef2f4703-b478-4ca6-98d1-bc3aa88e1c3f"));
        unknownItem.setCreditItemPublicIds(List.of(UUID.randomUUID()));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(unknownItem)
                .post("/bnpl/creditBatch")
                .then().log().all()
                .statusCode(400)
                .body("userMessage", notNullValue());
    }

    @Test
    public void testCreditBatchNotFound() {
        UUID unknownId = UUID.randomUUID();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/creditBatch/{publicId}", unknownId)
                .then().log().all()
                .statusCode(400)
                .body("userMessage", notNullValue());

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/creditBatch/{publicId}/creditItems", unknownId)
                .then().log().all()
                .statusCode(400);

        CreditBatchAction anyAction = new CreditBatchAction();
        anyAction.setBatchAction(BatchAction.APPROVE_BATCH);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(anyAction)
                .patch("/bnpl/creditBatch/{publicId}/actions", unknownId)
                .then().log().all()
                .statusCode(400);
    }

    @Test
    public void testCreditBatchCreateListGetAndItemsEndpoints() {
        UUID creditItemPublicId = UUID.fromString("b4a1c3e2-1111-2222-3333-000000000122");
        String batchPublicId = createCreditBatch(List.of(creditItemPublicId));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("parentCustomerPublicId", UUID.fromString("ef2f4703-b478-4ca6-98d1-bc3aa88e1c3f"))
                .queryParam("statuses", BatchStatus.PENDING.name())
                .get("/bnpl/creditBatch")
                .then().log().all()
                .statusCode(200)
                .body("content.publicId", hasItem(batchPublicId));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/creditBatch/{publicId}", UUID.fromString(batchPublicId))
                .then().log().all()
                .statusCode(200)
                .body("publicId", equalTo(batchPublicId))
                .body("status", equalTo(BatchStatus.PENDING.name()))
                .body("amount", equalTo(1050.0f));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/creditBatch/{publicId}/creditItems", UUID.fromString(batchPublicId))
                .then().log().all()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("creditLimitPublicId", hasItem(creditItemPublicId.toString()));
    }

    @Test
    public void testCreditBatchAddRemoveAndDeleteActions() {
        UUID firstItemPublicId = UUID.fromString("b4a1c3e2-1111-2222-3333-000000000123");
        UUID secondItemPublicId = UUID.fromString("b4a1c3e2-1111-2222-3333-000000000124");
        String batchPublicId = createCreditBatch(List.of(firstItemPublicId));

        CreditBatchAction addAction = new CreditBatchAction();
        addAction.setBatchAction(BatchAction.ADD_CREDIT_ITEMS);
        addAction.setCreditItemPublicIds(List.of(secondItemPublicId));
        performCreditBatchAction(batchPublicId, addAction, 200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/creditBatch/{publicId}/creditItems", UUID.fromString(batchPublicId))
                .then().log().all()
                .statusCode(200)
                .body("size()", equalTo(2));

        CreditBatchAction removeAction = new CreditBatchAction();
        removeAction.setBatchAction(BatchAction.REMOVE_CREDIT_ITEMS);
        removeAction.setCreditItemPublicIds(List.of(secondItemPublicId));
        performCreditBatchAction(batchPublicId, removeAction, 200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/creditBatch/{publicId}/creditItems", UUID.fromString(batchPublicId))
                .then().log().all()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("creditLimitPublicId", not(hasItem(secondItemPublicId.toString())));

        CreditBatchAction deleteAction = new CreditBatchAction();
        deleteAction.setBatchAction(BatchAction.DELETE_BATCH);
        performCreditBatchAction(batchPublicId, deleteAction, 200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/creditBatch/{publicId}", UUID.fromString(batchPublicId))
                .then().log().all()
                .statusCode(400);
    }

    @Test
    public void testCreditBatchApproveGenerateInvoiceAndAllocatePaidFlow() {
        UUID creditItemPublicId = UUID.fromString("b4a1c3e2-1111-2222-3333-000000000125");
        String batchPublicId = createCreditBatch(List.of(creditItemPublicId));

        CreditBatchAction allocateBeforeApproval = allocateAction(new BigDecimal("1260.00"), "REF-BATCH-PAID-001");
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(allocateBeforeApproval)
                .patch("/bnpl/creditBatch/{publicId}/actions", UUID.fromString(batchPublicId))
                .then()
                .statusCode(400)
                .body("userMessage", containsString("approved"));

        CreditBatchAction approveAction = new CreditBatchAction();
        approveAction.setBatchAction(BatchAction.APPROVE_BATCH);
        performCreditBatchAction(batchPublicId, approveAction, 200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/creditBatch/{publicId}", UUID.fromString(batchPublicId))
                .then()
                .statusCode(200)
                .body("status", equalTo(BatchStatus.APPROVED.name()));

        CreditBatchAction addAfterApproval = new CreditBatchAction();
        addAfterApproval.setBatchAction(BatchAction.ADD_CREDIT_ITEMS);
        addAfterApproval.setCreditItemPublicIds(List.of(UUID.fromString("b4a1c3e2-1111-2222-3333-000000000127")));
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(addAfterApproval)
                .patch("/bnpl/creditBatch/{publicId}/actions", UUID.fromString(batchPublicId))
                .then()
                .statusCode(400)
                .body("userMessage", containsString("PENDING"));

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(allocateBeforeApproval)
                .patch("/bnpl/creditBatch/{publicId}/actions", UUID.fromString(batchPublicId))
                .then()
                .statusCode(400)
                .body("userMessage", containsString("INVOICE"));

        CreditBatchAction invoiceAction = new CreditBatchAction();
        invoiceAction.setBatchAction(BatchAction.GENERATE_INVOICE);
        performCreditBatchAction(batchPublicId, invoiceAction, 200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/creditBatch/{publicId}", UUID.fromString(batchPublicId))
                .then()
                .statusCode(200)
                .body("status", equalTo(BatchStatus.INVOICE_GENERATED.name()))
                .body("invoiceNumber", equalTo("INV-" + getCreditBatchNumber(batchPublicId)))
                .body("invoiceGeneratedDate", notNullValue());

        performCreditBatchAction(batchPublicId, allocateBeforeApproval, 200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/creditBatch/{publicId}", UUID.fromString(batchPublicId))
                .then()
                .statusCode(200)
                .body("status", equalTo(BatchStatus.PAID.name()))
                .body("paidAmount", equalTo(1260.0f))
                .body("balanceAmount", equalTo(0.0f))
                .body("paymentReference", equalTo("REF-BATCH-PAID-001"));

        CreditLimitItem item = creditLimitItemRepository.findByPublicIdAndDeletedIsFalse(creditItemPublicId).orElseThrow();
        assertEquals(PaymentStatusEnum.PAID, item.getPaymentStatus());
        assertTrue(item.isFullyPaid());
    }

    @Test
    public void testCreditBatchAllocatePartiallyPaidFlow() {
        UUID creditItemPublicId = UUID.fromString("b4a1c3e2-1111-2222-3333-000000000126");
        String batchPublicId = createCreditBatch(List.of(creditItemPublicId));

        CreditBatchAction approveAction = new CreditBatchAction();
        approveAction.setBatchAction(BatchAction.APPROVE_BATCH);
        performCreditBatchAction(batchPublicId, approveAction, 200);

        CreditBatchAction invoiceAction = new CreditBatchAction();
        invoiceAction.setBatchAction(BatchAction.GENERATE_INVOICE);
        performCreditBatchAction(batchPublicId, invoiceAction, 200);

        performCreditBatchAction(batchPublicId, allocateAction(new BigDecimal("300.00"), "REF-BATCH-PARTIAL-001"), 200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/creditBatch/{publicId}", UUID.fromString(batchPublicId))
                .then()
                .statusCode(200)
                .body("status", equalTo(BatchStatus.PARTIALLY_PAID.name()))
                .body("paidAmount", equalTo(300.0f))
                .body("balanceAmount", equalTo(435.0f));

        CreditLimitItem item = creditLimitItemRepository.findByPublicIdAndDeletedIsFalse(creditItemPublicId).orElseThrow();
        assertEquals(PaymentStatusEnum.PARTIALLY_PAID, item.getPaymentStatus());
    }

    private String createCreditBatch(List<UUID> creditItemPublicIds) {
        CreditBatchRequest batchRequest = new CreditBatchRequest();
        batchRequest.setParentCustomerPublicId(UUID.fromString("ef2f4703-b478-4ca6-98d1-bc3aa88e1c3f"));
        batchRequest.setCreditItemPublicIds(creditItemPublicIds);

        return given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(batchRequest)
                .log().all()
                .post("/bnpl/creditBatch")
                .then().log().all()
                .statusCode(200)
                .body("publicId", notNullValue())
                .body("batchNumber", notNullValue())
                .body("status", equalTo(BatchStatus.PENDING.name()))
                .extract().path("publicId");
    }

    private void performCreditBatchAction(String batchPublicId, CreditBatchAction action, int expectedStatusCode) {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(action)
                .patch("/bnpl/creditBatch/{publicId}/actions", UUID.fromString(batchPublicId))
                .then().log().all()
                .statusCode(expectedStatusCode);
    }

    private CreditBatchAction allocateAction(BigDecimal amountPaid, String referenceNumber) {
        CreditBatchAction action = new CreditBatchAction();
        action.setBatchAction(BatchAction.ALLOCATE_RECONCILE);
        action.setAmountPaid(amountPaid);
        action.setReferenceNumber(referenceNumber);
        return action;
    }

    private String getCreditBatchNumber(String batchPublicId) {
        return given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/creditBatch/{publicId}", UUID.fromString(batchPublicId))
                .then()
                .statusCode(200)
                .extract().path("batchNumber");
    }

    @Ignore
    @Test
    public void testCreateCreditOrdersFullJourney() {

        String customerPublicId = "ef2f4703-b478-4ca6-98d1-bc3aa88e1c3f";
        String walletPublicId = "ea0052e0-1d46-4f82-bdfa-05c235730547";

        CreateCreditRuleRequest creditRuleRequest = new CreateCreditRuleRequest();
        creditRuleRequest.setCustomerType(CustomerType.REGULAR);
        creditRuleRequest.setIsCreditLimit(true);
        creditRuleRequest.setCreditLimitPercentage(new BigDecimal("35.0"));
        creditRuleRequest.setIsRepaymentTerm(false);
        creditRuleRequest.setRuleName("Regular Credit Limit Rule");
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(creditRuleRequest)
                .post("/settings/creditRule")
                .then()
                .statusCode(200);


        creditRuleRequest = new CreateCreditRuleRequest();
        creditRuleRequest.setCustomerType(CustomerType.REGULAR);
        creditRuleRequest.setIsRepaymentTerm(true);
        creditRuleRequest.setRepaymentTermDays(60);
        creditRuleRequest.setRuleName("Regular Repayment Term Rule");
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(creditRuleRequest)
                .post("/settings/creditRule")
                .then()
                .statusCode(200);


        creditRuleRequest = new CreateCreditRuleRequest();
        creditRuleRequest.setCustomerType(CustomerType.REGULAR);
        creditRuleRequest.setIsInterestRate(true);
        creditRuleRequest.setInterestRatePercentage(new BigDecimal("5.0"));
        creditRuleRequest.setRuleName("Regular Interest Rate Rule");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(creditRuleRequest)
                .post("/settings/creditRule")
                .then()
                .statusCode(200);


        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(UUID.fromString("08ebb084-2633-428f-82b9-e8d98a22ec53"))
                .quantity(2)
                .build();

        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(UUID.fromString("ef2f4703-b478-4ca6-98d1-bc3aa88e1c3f"))
                .bookedBy("Itibari Test")
                .buyOnCredit(Boolean.TRUE)
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .salesOrderItems(Collections.singleton(orderItems))
                .build();

        String publicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/salesOrders")
                .then().log().all()
                .statusCode(200)
                .extract().path("publicId");


        // transition to planning

        scheduler.runOrderPlanning();

        String orderId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(publicId))
                .then()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.PLANNING.name()))
                .extract().path("publicId");


        // fetch credit item and check if credit amount and interest amount are calculated correctly

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("customerPublicId", UUID.fromString(customerPublicId))
                .get("/bnpl/wallets")
                .then().log().all()
                .statusCode(200);


        //fetch wallet
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/wallets/{walletId}/creditLimitItems", UUID.fromString(walletPublicId))
                .then().log().all()
                .statusCode(200);

        // further steps like deliver, pay etc. can be added here later

        runAnOrderViaSA(UUID.fromString(orderId), true);  // this fully delivers the order and marks it as delivered


        String creditItemId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/wallets/{walletId}/creditLimitItems", UUID.fromString(walletPublicId))
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(publicId))
                .then().log().all()
                .statusCode(200);


        runCreditItemPayments(customerPublicId, "5000", "RT-009");


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/wallets/{walletId}/creditLimitItems", UUID.fromString(walletPublicId))
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(publicId))
                .then().log().all()
                .statusCode(200);

        runCreditItemPayments(customerPublicId, "50000", "RT-008");


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/wallets/{walletId}/creditLimitItems", UUID.fromString(walletPublicId))
                .then().log().all()
                .statusCode(200)
                .extract().path("content[0].publicId");


        runCreditItemPayments(customerPublicId, "50000", null);


        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/wallets/{walletId}/creditLimitItems", UUID.fromString(walletPublicId))
                .then().log().all()
                .statusCode(200)
                .extract().path("content[0].publicId");
    }


    @Test
    @Ignore
    public void testCreateAndAssignSalesAgent() {
        bnplCrmService.createLeads();

        String leadPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/crm/leads")
                .then().log().all()
                .statusCode(200)
                .body("content", not(empty()))
                .extract().path("content[0].publicId");

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("scheduleDate", LocalDate.now().format(DateTimeFormatter.ofPattern("yyy-MM-dd")))
                .patch("/bnpl/crm/leads/{leadPublicId}/assign/{assignedSalesAgentPublicId}", UUID.fromString(leadPublicId), UUID.fromString("a3397780-f434-4fa2-8041-32ade6ecb425"))
                .then().log().all()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/crm/leads/assignedCustomers")
                .then().log().all()
                .statusCode(200);
    }


    // get credit utilization

    @Test
    public void testGetCreditUtilization() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/creditLimitUtilization/{parentCustomerPublicId}", UUID.fromString("ef2f4703-b478-4ca6-98d1-bc3aa88e1c3f"))
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void testGetEnterpriseCustomers() {
        UUID parentCustomerPublicId = UUID.fromString("ef2f4703-b478-4ca6-98d1-bc3aa88e1c3f");
        UUID childCustomerPublicId = UUID.fromString("37946b2b-b08d-4117-9927-f8adcf32292f");
        UUID childCreditItemPublicId = UUID.fromString("0da20626-3498-41b7-84e6-3b6bbebcfc02");

        Customers parentCustomer = customersRepository.findByPublicIdAndDeletedIsFalse(parentCustomerPublicId).orElseThrow();
        Customers childCustomer = customersRepository.findByPublicIdAndDeletedIsFalse(childCustomerPublicId).orElseThrow();
        CreditLimitItem childCreditItem = creditLimitItemRepository.findByPublicIdAndDeletedIsFalse(childCreditItemPublicId).orElseThrow();
        Customers originalParent = childCustomer.getParentCustomer();
        PaymentStatusEnum originalPaymentStatus = childCreditItem.getPaymentStatus();

        try {
            childCustomer.setParentCustomer(parentCustomer);
            customersRepository.save(childCustomer);
            childCreditItem.setPaymentStatus(PaymentStatusEnum.NOT_PAID);
            creditLimitItemRepository.save(childCreditItem);

            given()
                    .contentType("application/json")
                    .auth().oauth2(ADMIN_TOKEN)
                    .queryParam("size", 100)
                    .get("/bnpl/enterpriseCustomers")
                    .then().log().all()
                    .statusCode(200)
                    .body("totalElements", greaterThanOrEqualTo(1))
                    .body("pageSize", equalTo(100))
                    .body("pageNumber", equalTo(0))
                    .body("content", not(empty()))
                    .body("content.find { it.publicId == '" + parentCustomerPublicId + "' }.customerName", equalTo(parentCustomer.getName()))
                    .body("content.find { it.publicId == '" + parentCustomerPublicId + "' }.creditLimit", notNullValue())
                    .body("content.find { it.publicId == '" + parentCustomerPublicId + "' }.creditStatus", notNullValue())
                    .body("content.find { it.publicId == '" + parentCustomerPublicId + "' }.walletBalance", notNullValue())
                    .body("content.find { it.publicId == '" + parentCustomerPublicId + "' }.isParent", equalTo(true))
                    .body("content.find { it.publicId == '" + parentCustomerPublicId + "' }.parentCustomerName", nullValue())
                    .body("content.find { it.publicId == '" + parentCustomerPublicId + "' }.parentCustomerPublicId", nullValue());
        } finally {
            childCreditItem.setPaymentStatus(originalPaymentStatus);
            creditLimitItemRepository.save(childCreditItem);
            childCustomer.setParentCustomer(originalParent);
            customersRepository.save(childCustomer);
        }
    }

    @Test
    public void testGetEnterpriseCreditItemsIncludesParentAndChildItems() {
        UUID parentCustomerPublicId = UUID.fromString("ef2f4703-b478-4ca6-98d1-bc3aa88e1c3f");
        UUID childCustomerPublicId = UUID.fromString("37946b2b-b08d-4117-9927-f8adcf32292f");
        UUID parentCreditItemPublicId = UUID.fromString("b4a1c3e2-1111-2222-3333-000000000099");
        UUID childCreditItemPublicId = UUID.fromString("0da20626-3498-41b7-84e6-3b6bbebcfc02");

        Customers parentCustomer = customersRepository.findByPublicIdAndDeletedIsFalse(parentCustomerPublicId).orElseThrow();
        Customers childCustomer = customersRepository.findByPublicIdAndDeletedIsFalse(childCustomerPublicId).orElseThrow();
        CreditLimitItem parentCreditItem = creditLimitItemRepository.findByPublicIdAndDeletedIsFalse(parentCreditItemPublicId).orElseThrow();
        CreditLimitItem childCreditItem = creditLimitItemRepository.findByPublicIdAndDeletedIsFalse(childCreditItemPublicId).orElseThrow();
        Customers originalParent = childCustomer.getParentCustomer();
        Batched originalParentBatchStatus = parentCreditItem.getBatched();
        Batched originalChildBatchStatus = childCreditItem.getBatched();

        try {
            childCustomer.setParentCustomer(parentCustomer);
            customersRepository.save(childCustomer);
            parentCreditItem.setBatched(Batched.NOT_BATCHED);
            childCreditItem.setBatched(Batched.NOT_BATCHED);
            creditLimitItemRepository.saveAll(List.of(parentCreditItem, childCreditItem));

            given()
                    .contentType("application/json")
                    .auth().oauth2(ADMIN_TOKEN)
                    .queryParam("size", 100)
                    .get("/bnpl/enterpriseCreditItems/{parentCustomerPublicId}", parentCustomerPublicId)
                    .then().log().all()
                    .statusCode(200)
                    .body("totalElements", greaterThanOrEqualTo(2))
                    .body("pageSize", equalTo(100))
                    .body("pageNumber", equalTo(0))
                    .body("content", not(empty()))
                    .body("content.creditLimitPublicId", hasItems(
                            parentCreditItemPublicId.toString(),
                            childCreditItemPublicId.toString()))
                    .body("content.find { it.creditLimitPublicId == '" + parentCreditItemPublicId + "' }.publicId", equalTo(parentCustomerPublicId.toString()))
                    .body("content.find { it.creditLimitPublicId == '" + childCreditItemPublicId + "' }.publicId", equalTo(childCustomerPublicId.toString()))
                    .body("content.find { it.creditLimitPublicId == '" + childCreditItemPublicId + "' }.customerName", equalTo(childCustomer.getName()))
                    .body("content.find { it.creditLimitPublicId == '" + childCreditItemPublicId + "' }.paymentStatus", notNullValue())
                    .body("content.find { it.creditLimitPublicId == '" + childCreditItemPublicId + "' }.enterpriseCredit", equalTo(true))
                    .body("content.batched", everyItem(equalTo(Batched.NOT_BATCHED.name())));
        } finally {
            parentCreditItem.setBatched(originalParentBatchStatus);
            childCreditItem.setBatched(originalChildBatchStatus);
            creditLimitItemRepository.saveAll(List.of(parentCreditItem, childCreditItem));
            childCustomer.setParentCustomer(originalParent);
            customersRepository.save(childCustomer);
        }
    }

    @Test
    public void testParentWalletCreditLimitItemsIncludesAndIdentifiesChildItems() {
        UUID parentCustomerPublicId = UUID.fromString("ef2f4703-b478-4ca6-98d1-bc3aa88e1c3f");
        UUID parentWalletPublicId = UUID.fromString("ea0052e0-1d46-4f82-bdfa-05c235730547");
        UUID childCustomerPublicId = UUID.fromString("37946b2b-b08d-4117-9927-f8adcf32292f");
        UUID parentCreditItemPublicId = UUID.fromString("b4a1c3e2-1111-2222-3333-000000000099");
        UUID childCreditItemPublicId = UUID.fromString("0da20626-3498-41b7-84e6-3b6bbebcfc02");

        Customers parentCustomer = customersRepository.findByPublicIdAndDeletedIsFalse(parentCustomerPublicId).orElseThrow();
        Customers childCustomer = customersRepository.findByPublicIdAndDeletedIsFalse(childCustomerPublicId).orElseThrow();
        Customers originalParent = childCustomer.getParentCustomer();

        try {
            childCustomer.setParentCustomer(parentCustomer);
            customersRepository.save(childCustomer);

            given()
                    .contentType("application/json")
                    .auth().oauth2(ADMIN_TOKEN)
                    .queryParam("size", 100)
                    .get("/bnpl/wallets/{walletId}/creditLimitItems", parentWalletPublicId)
                    .then().log().all()
                    .statusCode(200)
                    .body("content.publicId", hasItems(
                            parentCreditItemPublicId.toString(),
                            childCreditItemPublicId.toString()))
                    .body("content.find { it.publicId == '" + parentCreditItemPublicId + "' }.childCreditLimitItem", equalTo(false))
                    .body("content.find { it.publicId == '" + childCreditItemPublicId + "' }.childCreditLimitItem", equalTo(true));
        } finally {
            childCustomer.setParentCustomer(originalParent);
            customersRepository.save(childCustomer);
        }
    }

    @Ignore
    @Test
    public void testChildCustomerCreditLimitReturnsParentCreditLimit() {
        UUID childCustomerPublicId = UUID.fromString("37946b2b-b08d-4117-9927-f8adcf32292f");
        UUID parentCustomerPublicId = UUID.fromString("ef2f4703-b478-4ca6-98d1-bc3aa88e1c3f");

        Customers childCustomer = customersRepository.findByPublicIdAndDeletedIsFalse(childCustomerPublicId).orElseThrow();
        Customers parentCustomer = customersRepository.findByPublicIdAndDeletedIsFalse(parentCustomerPublicId).orElseThrow();

        Customers originalParent = childCustomer.getParentCustomer();
        childCustomer.setParentCustomer(parentCustomer);
        customersRepository.save(childCustomer);

        try {
            given()
                    .contentType("application/json")
                    .auth().oauth2(ADMIN_TOKEN)
                    .get("/bnpl/customer/{customerId}/creditLimit", childCustomerPublicId)
                    .then().log().all()
                    .statusCode(200)
                    .body("creditLimit", equalTo(200000.0f));
        } finally {
            childCustomer.setParentCustomer(originalParent);
            customersRepository.save(childCustomer);
        }
    }

    @Test
    public void testLoanManagementParentCustomerFilterValidation() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("parentCustomerPublicId", UUID.randomUUID())
                .get("/bnpl/loanManagement")
                .then().log().all()
                .statusCode(400)
                .body("userMessage", containsString("Parent customer not found"));
    }

    @Test
    public void testCreditPaymentsParentCustomerFilterValidation() {
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("parentCustomerPublicId", UUID.randomUUID())
                .get("/bnpl/creditPayments")
                .then().log().all()
                .statusCode(400)
                .body("userMessage", containsString("Parent customer not found"));
    }

    @Test
    @Ignore
    public void zzTestChildCustomerCreditOrderEndToEndUsesParentWalletForUpdateAndPayment() {
        UUID childCustomerPublicId = UUID.fromString("37946b2b-b08d-4117-9927-f8adcf32292f");
        UUID parentCustomerPublicId = UUID.fromString("ef2f4703-b478-4ca6-98d1-bc3aa88e1c3f");
        UUID childWalletPublicId = UUID.fromString("13b5e125-8e65-4bf7-8144-1d35ca2cadd4");
        UUID parentWalletPublicId = UUID.fromString("ea0052e0-1d46-4f82-bdfa-05c235730547");
        UUID productPublicId = UUID.fromString("08ebb084-2633-428f-82b9-e8d98a22ec53");

        Customers childCustomer = customersRepository.findByPublicIdAndDeletedIsFalse(childCustomerPublicId).orElseThrow();
        Customers parentCustomer = customersRepository.findByPublicIdAndDeletedIsFalse(parentCustomerPublicId).orElseThrow();
        Wallet childWallet = walletRepository.findByPublicIdAndDeletedIsFalse(childWalletPublicId).orElseThrow();
        Wallet parentWallet = walletRepository.findByPublicIdAndDeletedIsFalse(parentWalletPublicId).orElseThrow();

        Customers originalParentCustomer = childCustomer.getParentCustomer();
        Wallet originalParentWallet = childWallet.getParentWallet();
        BigDecimal originalChildWalletBalance = childWallet.getWalletBalance();
        BigDecimal originalChildCreditLimitBalance = childWallet.getCreditLimitBalance();
        BigDecimal originalParentWalletBalance = parentWallet.getWalletBalance();
        BigDecimal originalParentCreditLimitBalance = parentWallet.getCreditLimitBalance();

        childCustomer.setParentCustomer(parentCustomer);
        customersRepository.save(childCustomer);

        childWallet.setParentWallet(parentWallet);
        walletRepository.save(childWallet);

        parentWallet.setWalletBalance(new BigDecimal("5000.00"));
        walletRepository.save(parentWallet);

        try {
            SalesOrderItemsRequest initialOrderItem = SalesOrderItemsRequest.builder()
                    .productId(productPublicId)
                    .quantity(1)
                    .build();

            SalesOrdersRequest createRequest = SalesOrdersRequest.builder()
                    .customerId(childCustomerPublicId)
                    .bookedBy("Itibari Test")
                    .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                    .salesOrderItems(Collections.singleton(initialOrderItem))
                    .buyOnCredit(true)
                    .build();

            Response createOrderResponse = given()
                    .contentType("application/json")
                    .auth().oauth2(ADMIN_TOKEN)
                    .body(createRequest)
                    .post("/salesOrders")
                    .then().log().all()
                    .statusCode(200)
                    .body("creditApplied", equalTo(true))
                    .body("orderType", equalTo(OrderType.CREDIT.name()))
                    .extract().response();

            String orderPublicId = createOrderResponse.path("publicId");
            String orderNumber = createOrderResponse.path("salesOrderNumber");

            Wallet parentWalletAfterCreate = walletRepository.findByPublicIdAndDeletedIsFalse(parentWalletPublicId).orElseThrow();
            Wallet childWalletAfterCreate = walletRepository.findByPublicIdAndDeletedIsFalse(childWalletPublicId).orElseThrow();

            assertTrue("Expected parent wallet credit limit balance to be deducted after child credit order creation",
                    parentWalletAfterCreate.getCreditLimitBalance().compareTo(originalParentCreditLimitBalance) < 0);
            assertEquals("Expected child wallet credit limit balance to remain unchanged after parent-funded child order creation",
                    0, childWalletAfterCreate.getCreditLimitBalance().compareTo(originalChildCreditLimitBalance));

            SalesOrderItemsRequest updatedOrderItem = SalesOrderItemsRequest.builder()
                    .productId(productPublicId)
                    .quantity(2)
                    .build();

            SalesOrdersRequest updateRequest = SalesOrdersRequest.builder()
                    .customerId(childCustomerPublicId)
                    .bookedBy("Itibari Test")
                    .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                    .salesOrderItems(Collections.singleton(updatedOrderItem))
                    .buyOnCredit(true)
                    .build();

            given()
                    .contentType("application/json")
                    .auth().oauth2(ADMIN_TOKEN)
                    .body(updateRequest)
                    .patch("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                    .then().log().all()
                    .statusCode(200);

            Wallet parentWalletAfterUpdate = walletRepository.findByPublicIdAndDeletedIsFalse(parentWalletPublicId).orElseThrow();
            Wallet childWalletAfterUpdate = walletRepository.findByPublicIdAndDeletedIsFalse(childWalletPublicId).orElseThrow();

            assertTrue("Expected parent wallet credit limit balance to be deducted again after child credit order update",
                    parentWalletAfterUpdate.getCreditLimitBalance().compareTo(parentWalletAfterCreate.getCreditLimitBalance()) < 0);
            assertEquals("Expected child wallet credit limit balance to remain unchanged after child order update",
                    0, childWalletAfterUpdate.getCreditLimitBalance().compareTo(originalChildCreditLimitBalance));

            scheduler.runOrderPlanning();

            given()
                    .contentType("application/json")
                    .auth().oauth2(ADMIN_TOKEN)
                    .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                    .then().log().all()
                    .statusCode(200)
                    .body("salesOrderStatus", equalTo(LoadingStatusEnum.PLANNING.name()));

            runAnOrderViaSA(UUID.fromString(orderPublicId), false);

            given()
                    .contentType("application/json")
                    .auth().oauth2(ADMIN_TOKEN)
                    .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                    .then().log().all()
                    .statusCode(200)
                    .body("salesOrderStatus", equalTo(LoadingStatusEnum.DELIVERED.name()));

            CreditLimitItem creditLimitItemBeforePayment = creditLimitItemRepository.findByOrderNumberAndDeletedIsFalse(orderNumber).orElseThrow();
            assertEquals("Expected credit item to remain attached to the child wallet",
                    childWallet.getId(), creditLimitItemBeforePayment.getWallet().getId());
            assertEquals("Expected credit item reference to remain the child customer public ID",
                    childCustomerPublicId.toString(), creditLimitItemBeforePayment.getReferenceId());
            assertEquals("Expected credit item to carry parent customer public ID for scope tracking",
                    parentCustomerPublicId.toString(), creditLimitItemBeforePayment.getParentCustomerPublicId());

            Wallet parentWalletBeforePayment = walletRepository.findByPublicIdAndDeletedIsFalse(parentWalletPublicId).orElseThrow();
            assertTrue("Expected parent wallet to hold wallet funds before child credit payment",
                    parentWalletBeforePayment.getWalletBalance().compareTo(BigDecimal.ZERO) > 0);

            given()
                    .contentType("application/json")
                    .auth().oauth2(ADMIN_TOKEN)
                    .queryParam("referenceId", childCustomerPublicId)
                    .queryParam("orderNumber", orderNumber)
                    .queryParam("payViaWallet", true)
                    .queryParam("hasFile", false)
                    .post("/bnpl/customerCreditPay")
                    .then().log().all()
                    .statusCode(200);

            CreditLimitItem creditLimitItemAfterPayment = creditLimitItemRepository.findByOrderNumberAndDeletedIsFalse(orderNumber).orElseThrow();
            Wallet parentWalletAfterPayment = walletRepository.findByPublicIdAndDeletedIsFalse(parentWalletPublicId).orElseThrow();
            Wallet childWalletAfterPayment = walletRepository.findByPublicIdAndDeletedIsFalse(childWalletPublicId).orElseThrow();

            assertEquals("Expected child credit payment to be partially settled from the parent wallet",
                    PaymentStatusEnum.PARTIALLY_PAID, creditLimitItemAfterPayment.getPaymentStatus());
            assertEquals("Expected the amount paid from wallet to equal the parent wallet funding amount",
                    0, creditLimitItemAfterPayment.getAmountPaidFromWallet().compareTo(new BigDecimal("5000.00")));
            assertEquals("Expected parent wallet balance to be consumed by child payment",
                    0, parentWalletAfterPayment.getWalletBalance().compareTo(BigDecimal.ZERO));
            assertEquals("Expected child wallet balance to remain unchanged because parent wallet funded the payment",
                    0, childWalletAfterPayment.getWalletBalance().compareTo(originalChildWalletBalance));

            given()
                    .contentType("application/json")
                    .auth().oauth2(ADMIN_TOKEN)
                    .queryParam("parentCustomerPublicId", parentCustomerPublicId)
                    .get("/bnpl/creditPayments")
                    .then().log().all()
                    .statusCode(200)
                    .body("content.find { it.orderNumber == '" + orderNumber + "' }.paymentStatus", equalTo(PaymentStatusEnum.PARTIALLY_PAID.name()))
                    .body("content.find { it.orderNumber == '" + orderNumber + "' }.amountPaidFromWallet", equalTo(5000.0f))
                    .body("content.find { it.orderNumber == '" + orderNumber + "' }.walletBalance", equalTo(0.0f));
        } finally {
            childCustomer.setParentCustomer(originalParentCustomer);
            customersRepository.save(childCustomer);

            childWallet.setParentWallet(originalParentWallet);
            childWallet.setWalletBalance(originalChildWalletBalance);
            childWallet.setCreditLimitBalance(originalChildCreditLimitBalance);
            walletRepository.save(childWallet);

            parentWallet.setWalletBalance(originalParentWalletBalance);
            parentWallet.setCreditLimitBalance(originalParentCreditLimitBalance);
            walletRepository.save(parentWallet);
        }
    }


    @Test
    public void endToEndTestForEnterpriseCustomerCreditOrderJourney() {

        UUID parentCustomerPublicId = UUID.fromString("ef2f4703-b478-4ca6-98d1-bc3aa88e1c3f");
        UUID parentCustomerWalletPublicId = UUID.fromString("ea0052e0-1d46-4f82-bdfa-05c235730547");

        jdbcTemplate.update(
                "update itibari.wallet set wallet_balance = 200000, credit_limit_balance = 200000 "
                        + "where public_id = ?",
                parentCustomerWalletPublicId.toString());
        jdbcTemplate.update(
                "update itibari.customers set credit_limit = 200000, bnpl_account_created = true "
                        + "where public_id = ?",
                parentCustomerPublicId.toString());
        // Seed/batch credit items under this parent can be overdue relative to "today" and block new credit.
        jdbcTemplate.update(
                "update itibari.credit_items set due_date = ? "
                        + "where reference_id = ? and payment_status in ('NOT_PAID','PARTIALLY_PAID') "
                        + "and cancelled = false and deleted = false",
                java.sql.Date.valueOf(LocalDate.now().plusDays(30)),
                parentCustomerPublicId.toString());
        // Ensure planning prerequisites after earlier BNPL delivery journeys.
        resetSeededDeliveryAgentAvailability();
        jdbcTemplate.update(
                "update itibari.products set available_stock = 50, reserved_stock = 0, quantity = 50 "
                        + "where public_id = '08ebb084-2633-428f-82b9-e8d98a22ec53'");

        // get parent balance
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("customerPublicId", parentCustomerPublicId)
                .get("/bnpl/wallets")
                .then().log().all()
                .statusCode(200)
                .body("content.size()",equalTo(1))
                .body("content[0].publicId", notNullValue())
                .body("content[0].customerName", not(isEmptyOrNullString()))
                .body("content[0].customerType", notNullValue())
                .body("content[0].creditStatus", notNullValue())
                .body("content[0].creditLimit", equalTo(200000.0f))
                .body("content[0].availableBalance", equalTo(200000.0f));

        String childCustomerPublicId = createChildCustomer(parentCustomerPublicId);


        // check child wallet is created

      String childWalletPublicId =   given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("customerPublicId", UUID.fromString(childCustomerPublicId))
                .get("/bnpl/wallets")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .extract().path("content[0].publicId");



        String orderPublicId = createOrder_createRoute_load_daAccept_deliver(UUID.fromString(childCustomerPublicId));


       String creditLimitItemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/bnpl/wallets/{walletId}/creditLimitItems", UUID.fromString(childWalletPublicId))
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");



        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("customerPublicId", parentCustomerPublicId)
                .get("/bnpl/wallets")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content.size()",equalTo(1))
                .body("content[0].publicId", notNullValue())
                .body("content[0].customerName", not(isEmptyOrNullString()))
                .body("content[0].customerType", notNullValue())
                .body("content[0].creditStatus", notNullValue())
                .body("content[0].creditLimit", equalTo(200000.0f))
                .body("content[0].availableBalance", equalTo(147200.0f))
                .body("content[0].unpaidAmount", equalTo(8415.0f))
                .body("content[0].totalCreditTaken", equalTo(8100.0f));


        // batch and pay later...


    }

}
