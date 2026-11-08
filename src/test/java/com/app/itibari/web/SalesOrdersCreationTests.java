package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import com.app.itibari.dataTypes.*;
import com.app.itibari.models.entities.DeliveryAgents;
import com.app.itibari.models.request.*;
import com.app.itibari.repository.DeliveryAgentsRepository;
import com.app.itibari.scheduler.Scheduler;
import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * API coverage for all sales-order creation entry points:
 * staff create, customer/SA placeOrder, walk-in, van-sale (existing + new customer).
 */
@Slf4j
public class SalesOrdersCreationTests extends ItibariApplicationTests {

    private static final UUID CUSTOMER_ID = UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d");
    private static final UUID CUSTOMER_TOKEN_OWNER_ID = UUID.fromString("c1793a8b-1fb8-4c16-b04b-d3c087c00c6f");
    private static final UUID PRODUCT_ID = UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af");
    private static final UUID SALES_AGENT_ID = UUID.fromString("e388bdc8-5de4-41b3-91f5-35ef26c51200");
    private static final UUID DA_ID = UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65");
    private static final UUID WAREHOUSE_ID = UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56");
    private static final UUID REGION_ID = UUID.fromString("8c5d4db0-f9a1-47ed-a160-56780e4e4d14");
    private static final UUID LOCATION_ID = UUID.fromString("90ad01f1-4a13-41f7-b825-c428b41f35d0");
    private static final UUID VEHICLE_ID = UUID.fromString("fa4bf2ee-6665-493a-8dbd-5d56dc8a7d93");
    private static final String MOBILE_PASSWORD = "password123";
    private static final float PRODUCT_SELL_PRICE = 500.0F;

    @Autowired
    private Scheduler scheduler;
    @Autowired
    private DeliveryAgentsRepository deliveryAgentsRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String deliveryAgentToken;

    @Before
    public void loginDeliveryAgent() {
        DeliveryAgents deliveryAgent = deliveryAgentsRepository.findDeliveryAgentsByPhoneNumberAndDeletedIsFalse("+254712345678")
                .orElseThrow();
        deliveryAgent.setPassword(passwordEncoder.encode(MOBILE_PASSWORD));
        deliveryAgent.setForcePasswordReset(false);
        deliveryAgentsRepository.save(deliveryAgent);

        MobileLoginRequest login = new MobileLoginRequest();
        login.setPhoneNumber("+254712345678");
        login.setPassword(MOBILE_PASSWORD);
        login.setMobileUserType(MobileUserType.DeliveryAgent);
        login.setDeviceId("creation-tests-device");
        login.setDeviceName("Creation Tests");
        login.setPlatform("TEST");

        deliveryAgentToken = given()
                .contentType(ContentType.JSON)
                .body(login)
                .post("/authentication/mobilePlatform/login")
                .then()
                .statusCode(200)
                .extract().path("token");
    }

    // -------------------------------------------------------------------------
    // Path A — POST /salesOrders (staff / back office)
    // -------------------------------------------------------------------------

    @Test
    public void staffCreate_cashOrder_setsPlacedPendingCashAndGrossTotal() {
        int qty = 2;
        SalesOrdersRequest request = baseStaffRequest(CUSTOMER_ID, qty, SourceChannelEnum.BACK_OFFICE_APP);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/salesOrders")
                .then().log().all()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.ORDER_PLACED.name()))
                .body("paymentStatus", equalTo(PaymentStatusEnum.PENDING.name()))
                .body("creditApplied", equalTo(false))
                .body("walkIn", equalTo(false))
                .body("sourceChannel", equalTo(SourceChannelEnum.BACK_OFFICE_APP.name()))
                .body("totalAmount", equalTo(PRODUCT_SELL_PRICE * qty))
                .body("totalAmountPayable", equalTo(PRODUCT_SELL_PRICE * qty))
                .body("salesOrderNumber", startsWith("ORD"));
    }

    @Test
    public void staffCreate_customerCareChannel_mapsSourceChannel() {
        SalesOrdersRequest request = baseStaffRequest(CUSTOMER_ID, 1, SourceChannelEnum.CUSTOMER_CARE_APP);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/salesOrders")
                .then()
                .statusCode(200)
                .body("sourceChannel", equalTo(SourceChannelEnum.CUSTOMER_CARE_APP.name()))
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.ORDER_PLACED.name()));
    }

    @Test
    public void staffCreate_duplicateProductLines_mergesQuantities() {
        SalesOrderItemsRequest line1 = SalesOrderItemsRequest.builder().productId(PRODUCT_ID).quantity(2).build();
        SalesOrderItemsRequest line2 = SalesOrderItemsRequest.builder().productId(PRODUCT_ID).quantity(3).build();
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(CUSTOMER_ID)
                .bookedBy("Creation Tests")
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .salesOrderItems(new HashSet<>(Arrays.asList(line1, line2)))
                .build();

        String orderPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/salesOrders")
                .then()
                .statusCode(200)
                .body("totalAmount", equalTo(PRODUCT_SELL_PRICE * 5))
                .extract().path("publicId");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{id}/items", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].quantity", equalTo(5));
    }

    @Test
    public void staffCreate_creditWithinLimit_setsCreditApplied() {
        UUID bnplCustomerId = seedBnplReadyCustomer();

        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(bnplCustomerId)
                .bookedBy("Creation Tests")
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .buyOnCredit(true)
                .salesOrderItems(Collections.singleton(
                        SalesOrderItemsRequest.builder().productId(PRODUCT_ID).quantity(1).build()))
                .build();

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/salesOrders")
                .then().log().all()
                .statusCode(200)
                .body("creditApplied", equalTo(true))
                .body("paymentStatus", equalTo(PaymentStatusEnum.PENDING.name()))
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.ORDER_PLACED.name()));
    }

    @Test
    public void staffCreate_creditWithoutBnplAccount_rejected() {
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(CUSTOMER_ID)
                .bookedBy("Creation Tests")
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .buyOnCredit(true)
                .salesOrderItems(Collections.singleton(
                        SalesOrderItemsRequest.builder().productId(PRODUCT_ID).quantity(1).build()))
                .build();

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/salesOrders")
                .then()
                .statusCode(400);
    }

    @Test
    public void staffCreate_emptyBody_rejected() {
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(SalesOrdersRequest.builder().build())
                .post("/salesOrders")
                .then()
                .statusCode(400);
    }

    @Test
    public void staffCreate_unknownCustomer_rejected() {
        SalesOrdersRequest request = baseStaffRequest(UUID.randomUUID(), 1, SourceChannelEnum.BACK_OFFICE_APP);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/salesOrders")
                .then()
                .statusCode(400);
    }

    @Test
    public void staffCreate_zeroQuantity_rejected() {
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(CUSTOMER_ID)
                .bookedBy("Creation Tests")
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .salesOrderItems(Collections.singleton(
                        SalesOrderItemsRequest.builder().productId(PRODUCT_ID).quantity(0).build()))
                .build();

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/salesOrders")
                .then()
                .statusCode(400);
    }

    @Test
    public void staffCreate_unknownProduct_rejected() {
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(CUSTOMER_ID)
                .bookedBy("Creation Tests")
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .salesOrderItems(Collections.singleton(
                        SalesOrderItemsRequest.builder().productId(UUID.randomUUID()).quantity(1).build()))
                .build();

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/salesOrders")
                .then()
                .statusCode(400);
    }

    @Test
    public void staffCreate_bookedBySalesAgentWithoutAgentId_rejected() {
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(CUSTOMER_ID)
                .bookedBy("Creation Tests")
                .sourceChannel(SourceChannelEnum.SALES_AGENTS_APP.name())
                .bookedBySalesAgent(true)
                .salesOrderItems(Collections.singleton(
                        SalesOrderItemsRequest.builder().productId(PRODUCT_ID).quantity(1).build()))
                .build();

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/salesOrders")
                .then()
                .statusCode(400);
    }

    @Test
    public void staffCreate_volumeDiscount_reducesPayableButKeepsGrossTotal() {
        ensureActiveVolumeDiscount(PRODUCT_ID, 2, 100, new BigDecimal("20"));

        int qty = 2;
        SalesOrdersRequest request = baseStaffRequest(CUSTOMER_ID, qty, SourceChannelEnum.BACK_OFFICE_APP);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/salesOrders")
                .then().log().all()
                .statusCode(200)
                .body("totalAmount", equalTo(PRODUCT_SELL_PRICE * qty))
                .body("totalDiscount", greaterThan(0f))
                .body("totalAmountPayable", lessThan(PRODUCT_SELL_PRICE * qty))
                .body("creditApplied", equalTo(false));
    }

    // -------------------------------------------------------------------------
    // Path B — POST /salesOrders/customer/placeOrder
    // -------------------------------------------------------------------------

    @Test
    public void placeOrder_customerApp_setsCustomerCareChannel() {
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(CUSTOMER_TOKEN_OWNER_ID)
                .bookedBy("Customer App")
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name()) // ignored by placeOrder for non-SA
                .salesOrderItems(Collections.singleton(
                        SalesOrderItemsRequest.builder().productId(PRODUCT_ID).quantity(1).build()))
                .build();

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(CUSTOMER_TOKEN)
                .body(request)
                .post("/salesOrders/customer/placeOrder")
                .then().log().all()
                .statusCode(200)
                .body("sourceChannel", equalTo(SourceChannelEnum.CUSTOMER_CARE_APP.name()))
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.ORDER_PLACED.name()))
                .body("paymentStatus", equalTo(PaymentStatusEnum.PENDING.name()))
                .body("bookedBySalesAgent", equalTo(false))
                .body("creditApplied", equalTo(false))
                .body("totalAmount", equalTo(PRODUCT_SELL_PRICE));
    }

    @Test
    public void placeOrder_salesAgent_setsSalesAgentsChannel() {
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(CUSTOMER_ID)
                .bookedBy("SA App")
                .sourceChannel(SourceChannelEnum.SALES_AGENTS_APP.name())
                .bookedBySalesAgent(true)
                .salesAgentPublicId(SALES_AGENT_ID)
                .salesOrderItems(Collections.singleton(
                        SalesOrderItemsRequest.builder().productId(PRODUCT_ID).quantity(1).build()))
                .build();

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(SALES_AGENT_TOKEN)
                .header("X-App-Version", "2.0.0")
                .body(request)
                .post("/salesOrders/customer/placeOrder")
                .then().log().all()
                .statusCode(200)
                .body("sourceChannel", equalTo(SourceChannelEnum.SALES_AGENTS_APP.name()))
                .body("bookedBySalesAgent", equalTo(true))
                .body("salesAgentPublicId", equalTo(SALES_AGENT_ID.toString()))
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.ORDER_PLACED.name()))
                .body("paymentStatus", equalTo(PaymentStatusEnum.PENDING.name()))
                .body("creditApplied", equalTo(false));
    }

    @Test
    public void placeOrder_salesAgentMissingAgentId_rejected() {
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(CUSTOMER_ID)
                .bookedBy("SA App")
                .bookedBySalesAgent(true)
                .salesOrderItems(Collections.singleton(
                        SalesOrderItemsRequest.builder().productId(PRODUCT_ID).quantity(1).build()))
                .build();

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(SALES_AGENT_TOKEN)
                .header("X-App-Version", "2.0.0")
                .body(request)
                .post("/salesOrders/customer/placeOrder")
                .then()
                .statusCode(400);
    }

    // -------------------------------------------------------------------------
    // Path C — POST /salesOrders/walk-in-order
    // -------------------------------------------------------------------------

    @Test
    public void walkInCreate_withDiscounts_isDeliveredPaidNetOfDiscounts() {
        int qty = 2;
        BigDecimal volumeDiscount = BigDecimal.valueOf(20);
        BigDecimal specialDiscount = BigDecimal.valueOf(10);
        float gross = PRODUCT_SELL_PRICE * qty;
        float payable = gross - volumeDiscount.floatValue() - specialDiscount.floatValue();

        WalkInSalesOrdersRequest.OrderItemsRequest item = WalkInSalesOrdersRequest.OrderItemsRequest.builder()
                .productId(PRODUCT_ID)
                .quantity(qty)
                .volumeDiscount(volumeDiscount)
                .specialDiscount(specialDiscount)
                .build();

        WalkInSalesOrdersRequest request = WalkInSalesOrdersRequest.builder()
                .customerId(CUSTOMER_ID)
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .orderItemsRequest(Collections.singleton(item))
                .build();

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/salesOrders/walk-in-order")
                .then().log().all()
                .statusCode(200)
                .body("walkIn", equalTo(true))
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.DELIVERED.name()))
                .body("paymentStatus", equalTo(PaymentStatusEnum.PAID.name()))
                .body("totalAmount", equalTo(payable))
                .body("totalAmountPayable", equalTo(payable));
    }

    @Test
    public void walkInCreate_withoutDiscount_isDeliveredPaidAtGross() {
        int qty = 1;
        WalkInSalesOrdersRequest.OrderItemsRequest item = WalkInSalesOrdersRequest.OrderItemsRequest.builder()
                .productId(PRODUCT_ID)
                .quantity(qty)
                .build();

        WalkInSalesOrdersRequest request = WalkInSalesOrdersRequest.builder()
                .customerId(CUSTOMER_ID)
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .orderItemsRequest(Collections.singleton(item))
                .build();

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/salesOrders/walk-in-order")
                .then()
                .statusCode(200)
                .body("walkIn", equalTo(true))
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.DELIVERED.name()))
                .body("paymentStatus", equalTo(PaymentStatusEnum.PAID.name()))
                .body("totalAmount", equalTo(PRODUCT_SELL_PRICE * qty));
    }

    // -------------------------------------------------------------------------
    // Paths D + E — van sale creates (existing customer + new customer)
    // -------------------------------------------------------------------------

    @Test
    public void vanSale_existingAndNewCustomer_createOrdersWithExpectedStatuses() {
        VanSaleFixture fixture = prepareRouteWithVanStock(3);

        SellVanSaleRequest sellRequest = new SellVanSaleRequest();
        sellRequest.setRoutePublicId(UUID.fromString(fixture.routePublicId));
        sellRequest.setCustomerPublicId(CUSTOMER_ID);
        VanItemRequest vanItem = new VanItemRequest();
        vanItem.setProductItemPublicId(UUID.fromString(fixture.vanReconItemPublicId));
        vanItem.setQuantitySold(1);
        sellRequest.setItems(List.of(vanItem));

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .body(sellRequest)
                .patch("/deliveryAgents/sellVansale")
                .then().log().all()
                .statusCode(200);

        // sellVansale creates via createVanSalesOrder then immediately markOrderAsDelivered
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("sourceChannel", SourceChannelEnum.DELIVERY_AGENT_APP.name())
                .queryParam("statuses", LoadingStatusEnum.DELIVERED.name())
                .get("/salesOrders")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content.find { it.sourceChannel == 'DELIVERY_AGENT_APP' }.salesOrderStatus",
                        equalTo(LoadingStatusEnum.DELIVERED.name()));

        DeliverVanSaleItemRequest deliverRequest = new DeliverVanSaleItemRequest();
        deliverRequest.setRoutePublicId(UUID.fromString(fixture.routePublicId));
        deliverRequest.setCustomersRequest(CustomersRequest.builder()
                .phoneNumber(uniquePhone())
                .firstName("Van")
                .lastName("Buyer")
                .organizationLocationId(LOCATION_ID)
                .organizationRegionId(REGION_ID)
                .build());
        ItemRequest item = new ItemRequest();
        item.setProductItemPublicId(UUID.fromString(fixture.vanReconItemPublicId));
        item.setQuantitySold(1);
        deliverRequest.setItems(List.of(item));

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .body(deliverRequest)
                .patch("/deliveryAgents/markVanSaleItemAsSold")
                .then().log().all()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("sourceChannel", SourceChannelEnum.DELIVERY_AGENT_APP.name())
                .queryParam("statuses", LoadingStatusEnum.DELIVERED.name())
                .get("/salesOrders")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content.find { it.sourceChannel == 'DELIVERY_AGENT_APP' && it.paymentStatus == 'PENDING' }.salesOrderStatus",
                        equalTo(LoadingStatusEnum.DELIVERED.name()));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SalesOrdersRequest baseStaffRequest(UUID customerId, int qty, SourceChannelEnum channel) {
        return SalesOrdersRequest.builder()
                .customerId(customerId)
                .bookedBy("Creation Tests")
                .sourceChannel(channel.name())
                .salesOrderItems(Collections.singleton(
                        SalesOrderItemsRequest.builder().productId(PRODUCT_ID).quantity(qty).build()))
                .build();
    }

    private void ensureActiveVolumeDiscount(UUID productId, int minQty, int maxQty, BigDecimal amountPerUnit) {
        DiscountCreateRequest create = new DiscountCreateRequest();
        create.setDiscountType(DiscountType.VolumeDiscount);
        create.setProductIds(Collections.singletonList(productId));

        int createStatus = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(create)
                .post("/discounts")
                .then()
                .extract().statusCode();

        String discountPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("discountType", DiscountType.VolumeDiscount)
                .get("/discounts")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content.find { it.productIds != null && it.productIds.contains('" + productId + "') }?.publicId");

        if (discountPublicId == null) {
            discountPublicId = given()
                    .contentType(ContentType.JSON)
                    .auth().oauth2(ADMIN_TOKEN)
                    .queryParam("discountType", DiscountType.VolumeDiscount)
                    .get("/discounts")
                    .then()
                    .statusCode(200)
                    .extract().path("content[0].publicId");
        }

        String status = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("discountType", DiscountType.VolumeDiscount)
                .get("/discounts")
                .then()
                .statusCode(200)
                .extract().path("content.find { it.publicId == '" + discountPublicId + "' }.status");

        if (DiscountStatus.Active.name().equals(status)) {
            return;
        }

        if (createStatus == 200 || DiscountStatus.Incomplete.name().equals(status)) {
            VolumeRangeCreateRequest range = new VolumeRangeCreateRequest();
            range.setMinVolume(minQty);
            range.setMaxVolume(maxQty);
            range.setDiscountAmount(amountPerUnit);
            given()
                    .contentType(ContentType.JSON)
                    .auth().oauth2(ADMIN_TOKEN)
                    .body(range)
                    .patch("/discounts/{id}/addVolumeRange", UUID.fromString(discountPublicId))
                    .then()
                    .statusCode(anyOf(is(200), is(400)));
        }

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .patch("/discounts/{id}/approve", UUID.fromString(discountPublicId))
                .then()
                .statusCode(anyOf(is(200), is(400)));
    }

    /**
     * Creates a customer and marks them BNPL-ready via SQL so we avoid H2-incompatible
     * eligibility queries used by {@code /bnpl/onboardCustomer}.
     */
    private UUID seedBnplReadyCustomer() {
        String phone = uniquePhone();
        String payload = createCustomerPayload(
                "BNPL Create " + phone, "Bnpl", "Create", phone,
                SourceChannelEnum.CUSTOMER_CARE_APP.name(),
                REGION_ID.toString(), LOCATION_ID.toString(),
                Boolean.FALSE, Boolean.FALSE, CustomerType.REGULAR);

        String customerPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(payload)
                .post("/customers")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        Long customerId = jdbcTemplate.queryForObject(
                "select id from itibari.customers where public_id = ?",
                Long.class,
                customerPublicId);

        jdbcTemplate.update(
                "update itibari.customers set bnpl_account_created = true, customer_type = 'REGULAR', " +
                        "credit_limit = ?, bnpl_verification_status = 'VERIFIED', activated = true where id = ?",
                new BigDecimal("100000.00"),
                customerId);

        jdbcTemplate.update(
                "insert into itibari.wallet (public_id, deleted, creation_time, customer_id, credit_status, " +
                        "average_order_value, credit_limit_balance, customer_id_number, tin_number, " +
                        "business_license_number, guarantor_name, guarantor_phone_number, wallet_balance) " +
                        "values (?, false, current_timestamp, ?, 'Healthy', ?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID().toString(),
                customerId,
                new BigDecimal("5000.00"),
                new BigDecimal("100000.00"),
                "ID-" + phone,
                "123456789",
                "BL-" + phone,
                "Guarantor Create",
                "0711999888",
                new BigDecimal("100000.00"));

        return UUID.fromString(customerPublicId);
    }

    private String uniquePhone() {
        // local format; createCustomer / van-sale normalize to +255xxxxxxxxx
        return "07" + String.format("%08d", Math.abs(UUID.randomUUID().getLeastSignificantBits() % 100_000_000L));
    }

    private VanSaleFixture prepareRouteWithVanStock(int vanQty) {
        SalesOrdersRequest orderRequest = baseStaffRequest(CUSTOMER_ID, 1, SourceChannelEnum.BACK_OFFICE_APP);
        String orderPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(orderRequest)
                .post("/salesOrders")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        scheduler.runOrderPlanning();

        RoutePlanningRequest routePlanningRequest = new RoutePlanningRequest();
        routePlanningRequest.setLocationPublicId(LOCATION_ID);
        routePlanningRequest.setRegionPublicId(REGION_ID);
        routePlanningRequest.setVehiclePublicId(VEHICLE_ID);
        routePlanningRequest.setDeliveryAgentPublicId(DA_ID);
        routePlanningRequest.setWarehousePublicId(WAREHOUSE_ID);
        routePlanningRequest.setSalesOrdersPublicIds(Collections.singletonList(UUID.fromString(orderPublicId)));

        String routePublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(routePlanningRequest)
                .post("/logistics/routePlanning")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        String productItemPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{id}/loadingProducts", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].productItemPublicId");

        VanSaleRequest vanSaleRequest = new VanSaleRequest();
        vanSaleRequest.setDeliveryAgentPublicId(DA_ID);
        ProductItemRequest vanProduct = new ProductItemRequest();
        vanProduct.setProductItemPublicId(PRODUCT_ID);
        vanProduct.setQuantity(vanQty);
        vanSaleRequest.setProductItemRequestList(List.of(vanProduct));

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(vanSaleRequest)
                .post("/vanSales")
                .then()
                .statusCode(200);

        LoadingRequest loadingRequest = new LoadingRequest();
        LoadingDTO orderLoad = new LoadingDTO();
        orderLoad.setLoadingType(LoadingType.OrderSale);
        orderLoad.setProductItemPublicId(UUID.fromString(productItemPublicId));
        loadingRequest.setLoadingDTOList(Collections.singletonList(orderLoad));

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(loadingRequest)
                .post("/warehouse/agent/{agentPublicId}/loading", DA_ID)
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .queryParam("acceptType", AcceptTypeEnum.AcceptRoute)
                .patch("/deliveryAgents/accept/{routeId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(deliveryAgentToken)
                .header("X-App-Version", "1.0.0")
                .queryParam("routeId", UUID.fromString(routePublicId))
                .queryParam("action", RouteAction.Start)
                .patch("/deliveryAgents/routeActions")
                .then()
                .statusCode(200);

        String productReconId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/productRecons")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");

        String vanReconItemPublicId = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/warehouse/productRecons/{id}/items", UUID.fromString(productReconId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content.find { it.hasVanSale == true || it.vanSaleItem == true }.publicId");

        if (vanReconItemPublicId == null) {
            vanReconItemPublicId = given()
                    .contentType(ContentType.JSON)
                    .auth().oauth2(ADMIN_TOKEN)
                    .get("/warehouse/productRecons/{id}/items", UUID.fromString(productReconId))
                    .then()
                    .statusCode(200)
                    .extract().path("content[0].publicId");
        }

        VanSaleFixture fixture = new VanSaleFixture();
        fixture.routePublicId = routePublicId;
        fixture.orderPublicId = orderPublicId;
        fixture.vanReconItemPublicId = vanReconItemPublicId;
        return fixture;
    }

    private static class VanSaleFixture {
        String routePublicId;
        String orderPublicId;
        String vanReconItemPublicId;
    }
}
