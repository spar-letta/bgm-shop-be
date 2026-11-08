package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import com.app.itibari.dataTypes.LoadingStatusEnum;
import com.app.itibari.dataTypes.SourceChannelEnum;
import com.app.itibari.models.request.RoutePlanningRequest;
import com.app.itibari.models.request.SalesOrderItemsRequest;
import com.app.itibari.models.request.SalesOrdersRequest;
import com.app.itibari.scheduler.Scheduler;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.app.itibari.dataTypes.InTransitTransferType;
import com.app.itibari.dataTypes.RouteStatus;
import com.app.itibari.dataTypes.ReconStatus;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.request.TransferInTransitRequest;
import com.app.itibari.services.ItibariSmsService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.hasItem;
import java.util.Collections;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

public class LogisticsRestControllerTests extends ItibariApplicationTests {

    @Autowired
    private Scheduler scheduler;

    @Test
    public void testCreateVehicle(){
        String payload = "{\n" +
                "  \"numberPlate\": \"Discovery\",\n" +
                "  \"ownerName\": \"Tonie\",\n" +
                "  \"ownerPhoneNumber\": \"404.103.3362\",\n" +
                "  \"assignedDriver\": \"Malka\",\n" +
                "  \"registrationStatus\": \"Registered\",\n" +
                "  \"insuranceExpiryDate\": \"2025-04-07\",\n" +
                "  \"vehicleType\": \"Bicycle\",\n" +
                "  \"weight\": 247,\n" +
                "  \"length\":2.0,\n" +
                "  \"width\":3.0,\n" +
                "  \"height\":9.2,\n" +
                "  \"payloadCapacity\":240\n" +
                "}";
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(payload).log().all()
                .post("/logistics/vehicle")
                .then().log().all()
                .statusCode(200);
    }

    @Test
    public void fetchVehiclesWorks(){
        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/vehicles")
                .then().log().all()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(2));

    }


    @Test
    @Ignore // works alone
    public void routeActionsWorks(){

        SalesOrderItemsRequest orderItems = SalesOrderItemsRequest
                .builder()
                .productId(UUID.fromString("08ebb084-2633-428f-82b9-e8d98a22ec53"))
                .quantity(5)
                .build();
        SalesOrdersRequest request = SalesOrdersRequest.builder()
                .customerId(UUID.fromString("d7c5bf34-fda1-46e2-b5ee-40db98aed06d"))
                .bookedBy("Itibari Test")
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .salesOrderItems(Collections.singleton(orderItems))
                .build();


        String orderPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .post("/salesOrders")
                .then()
                .statusCode(200)
                .extract().path("publicId");

        String orderItemPublicId = given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{salesOrderPublicId}/items", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content[0].publicId");



        scheduler.runOrderPlanning();

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(request)
                .get("/salesOrders/{salesOrderPublicId}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("salesOrderStatus", equalTo(LoadingStatusEnum.PLANNING.name()));


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


        String body = """
                {
                    "deliveryAgentPublicId": "8ffd638c-b83c-4b87-8eb1-11811abdc071",
                    "vehiclePublicId": "2910a4a3-67e7-4d6b-a1d7-953cb8bd9314",
                    "action":"TRANSFER"
                }""";

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .body(body)
                .patch("/logistics/routePlanning/{routeId}/actions", UUID.fromString(routePublicId))
                .then()
                .statusCode(200);

        given()
                .contentType("application/json")
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{dispatchId}/orders/slim", UUID.fromString(routePublicId))
                .then().log().all()
                .statusCode(200);

    }
    @Autowired
    private org.springframework.security.oauth2.jwt.JwtEncoder transferJwtEncoder;

    private String transferToken() {
        java.time.Instant now = java.time.Instant.now();
        var claims = org.springframework.security.oauth2.jwt.JwtClaimsSet.builder()
                .issuer("self").subject("itibari@gmail.com").issuedAt(now).expiresAt(now.plusSeconds(3600))
                .claim("roles", "ROLE_ADMIN")
                .claim("permissions", "UPDATE_ROUTE_PLANNING,VIEW_ROUTE_ORDER,VIEW_PRODUCT_RECONCILIATIONS")
                .build();
        return transferJwtEncoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(claims)).getTokenValue();
    }

    @PersistenceContext
    private EntityManager transferEntityManager;
    @Autowired
    private PlatformTransactionManager transferTransactionManager;
    @MockBean
    private ItibariSmsService transferSmsService;

    @Test
    public void transferLoadedToReadyToGoPersistsOrdersItemsAndRecalculatesTotals() {
        TransferFixture fixture = transferFixture(RouteStatus.Loaded, RouteStatus.ReadyToGo);
        transfer(fixture, InTransitTransferType.ORDER, 200);
        assertTransferred(fixture, true);
    }

    @Test
    public void transferOrdersBetweenAllActiveRouteStatuses() {
        for (RouteStatus source : transferStatuses()) {
            for (RouteStatus target : transferStatuses()) {
                TransferFixture fixture = transferFixture(source, target);
                transfer(fixture, InTransitTransferType.ORDER, 200);
                assertTransferred(fixture, true);
            }
        }
    }

    @Test
    public void transferEntireRouteIntoEveryActiveTargetStatus() {
        for (RouteStatus sourceStatus : transferStatuses()) {
            for (RouteStatus targetStatus : transferStatuses()) {
                TransferFixture fixture = transferFixture(sourceStatus, targetStatus);
                transfer(fixture, InTransitTransferType.ROUTE, 200);
                UUID[] targetIds = transferTransaction().execute(status -> {
                    RoutePlanning source = transferEntityManager.find(RoutePlanning.class, fixture.sourceId);
                    RoutePlanning target = transferredRoute(fixture);
                    assertTrue(source.getSalesOrders().isEmpty());
                    assertEquals(RouteStatus.TRANSFERRED, source.getRouteStatus());
                    assertEquals(targetStatus, target.getRouteStatus());
                    assertEquals(3, target.getSalesOrders().size());
                    for (SalesOrders order : target.getSalesOrders()) {
                        assertEquals(fixture.targetAgentPublicId, order.getDeliveryAgent().getPublicId());
                        assertEquals(target.getPublicId().toString(), order.getRoutePublicId());
                    }
                    ProductRecon sourceRecon = reconFor(source);
                    ProductRecon targetRecon = reconFor(target);
                    assertTrue(sourceRecon.getProductItems().isEmpty());
                    assertEquals(2, targetRecon.getProductItems().size());
                    ProductItem shared = targetRecon.getProductItems().stream()
                            .filter(item -> item.getProductPublicId().equals(fixture.firstProductPublicId)).findFirst().orElseThrow();
                    assertEquals(11, shared.getQuantityPicked());
                    assertEquals(9, shared.getOrderQuantity());
                    assertEquals(2, shared.getVanSaleQuantity());
                    assertMoney("0", source.getBalance());
                    assertMoney("0", cashFor(source).getTotalAmountPicked());
                    assertMoney("0", cashFor(source).getDepositAmount());
                    assertMoney("1200", pickedValue(targetRecon));
                    assertMoney("1200", cashFor(target).getTotalAmountPicked());
                    assertMoney("30", cashFor(target).getDepositAmount());
                    assertMoney("30", cashFor(target).getCreditAmount());
                    assertMoney("1140", target.getBalance());
                    return new UUID[]{target.getPublicId(), targetRecon.getPublicId()};
                });
                given().auth().oauth2(transferToken()).get("/logistics/routePlanning/{id}/orders", targetIds[0])
                        .then().statusCode(200).body("content.size()", equalTo(3))
                        .body("content.publicId", hasItem(fixture.orderPublicId.toString()));
                given().auth().oauth2(transferToken()).get("/warehouse/productRecons/{id}/items", targetIds[1])
                        .then().statusCode(200).body("content.size()", equalTo(2))
                        .body("content.productPublicId", hasItem(fixture.secondProductPublicId.toString()));
            }
        }
    }

    @Test
    public void transferRejectsInactiveSourceRoutesWithoutChangingData() {
        for (RouteStatus sourceStatus : List.of(RouteStatus.CREATED, RouteStatus.SUBMITTED,
                RouteStatus.TRANSFERRED, RouteStatus.DELIVERED, RouteStatus.TRIP_COMPLETED, RouteStatus.UPDATED)) {
            for (InTransitTransferType type : InTransitTransferType.values()) {
                TransferFixture fixture = transferFixture(sourceStatus, RouteStatus.ReadyToGo);
                transfer(fixture, type, 400);
                assertTransferUnchanged(fixture);
            }
        }
    }

    @Test
    public void transferDuplicateOrderIdsMovesStockOnceAndRetryIsRejected() {
        TransferFixture fixture = transferFixture(RouteStatus.Loaded, RouteStatus.ReadyToGo);
        TransferInTransitRequest request = transferRequest(fixture, InTransitTransferType.ORDER);
        request.setOrderPublicIds(List.of(fixture.orderPublicId, fixture.orderPublicId));
        postTransfer(request, 200);
        assertTransferred(fixture, true);
        postTransfer(request, 400);
        assertTransferred(fixture, true);
    }

    @Test
    public void transferRollsBackOrderAndStockChangesWhenReasonIsInvalid() {
        TransferFixture fixture = transferFixture(RouteStatus.Loaded, RouteStatus.ReadyToGo);
        TransferInTransitRequest request = transferRequest(fixture, InTransitTransferType.ORDER);
        request.setReasonCodePublicId(UUID.randomUUID());
        postTransfer(request, 400);
        assertTransferUnchanged(fixture);
    }

    private void assertTransferUnchanged(TransferFixture fixture) {
        transferTransaction().executeWithoutResult(status -> {
            RoutePlanning source = transferEntityManager.find(RoutePlanning.class, fixture.sourceId);
            RoutePlanning target = transferredRoute(fixture);
            assertEquals(2, source.getSalesOrders().size());
            assertEquals(1, target.getSalesOrders().size());
            assertEquals(fixture.sourceStatus, source.getRouteStatus());
            assertMoney("800", pickedValue(reconFor(source)));
            assertMoney("400", pickedValue(reconFor(target)));
            assertMoney("800", cashFor(source).getTotalAmountPicked());
            assertMoney("400", cashFor(target).getTotalAmountPicked());
            assertMoney("750", source.getBalance());
            assertMoney("390", target.getBalance());
        });
    }

    @Test
    public void transferOrdersToNewRouteAtEveryActiveStatus() {
        for (RouteStatus source : transferStatuses()) {
            TransferFixture fixture = transferFixture(source, null);
            transfer(fixture, InTransitTransferType.ORDER, 200);
            assertTransferred(fixture, false);
        }
    }

    @Test
    public void transferEntireRouteAtEveryActiveStatus() {
        for (RouteStatus source : transferStatuses()) {
            TransferFixture fixture = transferFixture(source, null);
            transfer(fixture, InTransitTransferType.ROUTE, 200);
            transferTransaction().executeWithoutResult(status -> {
                RoutePlanning old = transferEntityManager.find(RoutePlanning.class, fixture.sourceId);
                RoutePlanning target = transferredRoute(fixture);
                assertEquals(RouteStatus.TRANSFERRED, old.getRouteStatus());
                assertTrue(old.getSalesOrders().isEmpty());
                assertEquals(2, target.getSalesOrders().size());
                assertEquals(source, target.getRouteStatus());
                for (SalesOrders order : target.getSalesOrders()) {
                    assertEquals(fixture.targetAgentPublicId, order.getDeliveryAgent().getPublicId());
                    assertEquals(target.getPublicId().toString(), order.getRoutePublicId());
                }
                ProductRecon recon = reconFor(target);
                assertEquals(2, recon.getProductItems().size());
                assertMoney("800", pickedValue(recon));
                assertMoney("800", cashFor(target).getTotalAmountPicked());
                assertEquals(0L, transferEntityManager.createQuery(
                        "select count(r) from ProductRecon r where r.deliveryId = :route and r.deleted = false", Long.class)
                        .setParameter("route", old.getRouteId()).getSingleResult().longValue());
                assertEquals(0L, transferEntityManager.createQuery(
                        "select count(r) from Reconciliation r where r.routePublicId = :route and r.deleted = false", Long.class)
                        .setParameter("route", old.getPublicId().toString()).getSingleResult().longValue());
                assertMoney("0", old.getBalance());
                assertMoney("750", target.getBalance());
            });
        }
    }

    @Test
    public void transferRejectsCombinedWeightAboveTargetCapacity() {
        TransferFixture fixture = transferFixture(RouteStatus.Loaded, RouteStatus.ReadyToGo);
        transferTransaction().executeWithoutResult(status -> transferredRoute(fixture).setVehicleWeight("1"));
        for (InTransitTransferType type : InTransitTransferType.values()) {
            transfer(fixture, type, 400);
            assertTransferUnchanged(fixture);
        }
    }

    @Test
    public void transferRejectsAmbiguousActiveTargetRoutesBeforeChangingSource() {
        TransferFixture fixture = transferFixture(RouteStatus.Loaded, RouteStatus.ReadyToGo);
        transferTransaction().executeWithoutResult(status -> {
            RoutePlanning another = new RoutePlanning();
            another.setRouteId("TRANSFER-" + UUID.randomUUID());
            another.setRouteStatus(RouteStatus.Loaded);
            another.setDeliveryAgentId(fixture.targetAgentPublicId.toString());
            another.setVehicleId(fixture.targetVehiclePublicId.toString());
            transferEntityManager.persist(another);
        });
        for (InTransitTransferType type : InTransitTransferType.values()) {
            transfer(fixture, type, 400);
        }
        transferTransaction().executeWithoutResult(status -> {
            RoutePlanning source = transferEntityManager.find(RoutePlanning.class, fixture.sourceId);
            assertEquals(2, source.getSalesOrders().size());
            assertMoney("800", pickedValue(reconFor(source)));
            assertMoney("750", source.getBalance());
        });
    }

    @Test
    public void transferRejectsInsufficientStockWithoutMovingOrdersOrChangingTotals() {
        TransferFixture fixture = transferFixture(RouteStatus.Loaded, RouteStatus.ReadyToGo);
        transferTransaction().executeWithoutResult(status -> {
            RoutePlanning route = transferEntityManager.find(RoutePlanning.class, fixture.sourceId);
            reconFor(route).getProductItems().get(0).setQuantityPicked(1);
        });
        transfer(fixture, InTransitTransferType.ORDER, 400);
        transferTransaction().executeWithoutResult(status -> {
            RoutePlanning source = transferEntityManager.find(RoutePlanning.class, fixture.sourceId);
            assertEquals(2, source.getSalesOrders().size());
            assertEquals(1, transferredRoute(fixture).getSalesOrders().size());
            assertMoney("800", cashFor(source).getTotalAmountPicked());
        });
    }

    private List<RouteStatus> transferStatuses() {
        return List.of(RouteStatus.Loading, RouteStatus.Loaded, RouteStatus.ReadyToGo, RouteStatus.IN_TRANSIT);
    }

    private TransactionTemplate transferTransaction() {
        return new TransactionTemplate(transferTransactionManager);
    }

    @org.junit.Before
    public void advanceTransferFixtureIdentitiesPastSeedData() {
        // Explicit IDs in my-data.sql do not advance H2's identity counters.
        transferTransaction().executeWithoutResult(status -> {
            for (String table : List.of("vehicles", "delivery_agents", "products", "sales_orders",
                    "sales_order_items", "route_planning", "product_recon", "product_items", "reconciliation")) {
                Number maximum = (Number) transferEntityManager.createNativeQuery(
                        "select coalesce(max(id), 0) from itibari." + table).getSingleResult();
                transferEntityManager.createNativeQuery("alter table itibari." + table
                        + " alter column id restart with " + (maximum.longValue() + 1000)).executeUpdate();
            }
        });
    }

    private TransferFixture transferFixture(RouteStatus sourceStatus, RouteStatus targetStatus) {
        return transferTransaction().execute(status -> {
            Warehouse warehouse = transferEntityManager.createQuery("select w from Warehouse w", Warehouse.class)
                    .setMaxResults(1).getSingleResult();
            DeliveryAgents sourceAgent = transferAgent(warehouse);
            DeliveryAgents targetAgent = transferAgent(warehouse);
            Vehicle sourceVehicle = transferVehicle();
            Vehicle targetVehicle = transferVehicle();
            Product first = transferProduct("100", "1");
            Product second = transferProduct("50", "2");
            SalesOrders moved = transferOrder(sourceAgent, warehouse, first, 3);
            transferOrderItem(moved, second, 2);
            moved.setTotalAmount(new BigDecimal("400"));
            SalesOrders retained = transferOrder(sourceAgent, warehouse, first, 2);
            RoutePlanning source = transferRoute(sourceAgent, sourceVehicle, warehouse, sourceStatus, List.of(moved, retained));
            ProductRecon sourceRecon = transferRecon(source, sourceAgent);
            ProductItem firstItem = transferProductItem(sourceRecon, first, 7, 5);
            firstItem.setHasVanSale(true);
            firstItem.setVanSaleQuantity(2);
            transferProductItem(sourceRecon, second, 2, 2);
            transferCash(source, sourceRecon, "800", "20", "30");
            if (targetStatus != null) {
                SalesOrders existing = transferOrder(targetAgent, warehouse, first, 4);
                RoutePlanning target = transferRoute(targetAgent, targetVehicle, warehouse, targetStatus, List.of(existing));
                ProductRecon targetRecon = transferRecon(target, targetAgent);
                transferProductItem(targetRecon, first, 4, 4);
                transferCash(target, targetRecon, "400", "10", "0");
            }
            transferEntityManager.flush();
            return new TransferFixture(source.getId(), source.getPublicId(), moved.getPublicId(), targetAgent.getPublicId(),
                    targetVehicle.getPublicId(), first.getPublicId(), second.getPublicId(), sourceStatus, targetStatus);
        });
    }

    private DeliveryAgents transferAgent(Warehouse warehouse) {
        DeliveryAgents agent = new DeliveryAgents();
        agent.setFirstName("Transfer");
        agent.setLastName(UUID.randomUUID().toString());
        agent.setWarehouse(warehouse);
        agent.setPhoneNumber("+255714394472");
        transferEntityManager.persist(agent);
        return agent;
    }

    private Vehicle transferVehicle() {
        Vehicle vehicle = new Vehicle();
        vehicle.setNumberPlate("TRANSFER-" + UUID.randomUUID().toString().substring(0, 8));
        vehicle.setWeight(100000);
        transferEntityManager.persist(vehicle);
        return vehicle;
    }

    private Product transferProduct(String price, String weight) {
        Product product = new Product();
        product.setProductName("Transfer " + UUID.randomUUID());
        product.setSku(UUID.randomUUID().toString());
        product.setSellPrice(new BigDecimal(price));
        product.setWeight(new BigDecimal(weight));
        transferEntityManager.persist(product);
        return product;
    }

    private SalesOrders transferOrder(DeliveryAgents agent, Warehouse warehouse, Product product, int quantity) {
        SalesOrders order = new SalesOrders();
        order.setSalesOrderNumber("TRANSFER-" + UUID.randomUUID());
        order.setDeliveryAgent(agent);
        order.setWarehouse(warehouse);
        order.setCustomer(transferEntityManager.createQuery(
                "select c from Customers c where c.deleted = false and c.parentCustomer is null", Customers.class)
                .setMaxResults(1).getSingleResult());
        order.setSalesOrderStatus(LoadingStatusEnum.LOADED);
        order.setTotalAmount(product.getSellPrice().multiply(BigDecimal.valueOf(quantity)));
        transferEntityManager.persist(order);
        transferOrderItem(order, product, quantity);
        return order;
    }

    private void transferOrderItem(SalesOrders order, Product product, int quantity) {
        SalesOrderItems item = new SalesOrderItems();
        item.setSalesOrder(order);
        item.setSalesOrderPublicId(order.getPublicId());
        item.setProduct(product);
        item.setProductId(product.getPublicId());
        item.setQuantity(quantity);
        item.setPricePerUnit(product.getSellPrice());
        transferEntityManager.persist(item);
        order.getSalesOrderItems().add(item);
    }

    private RoutePlanning transferRoute(DeliveryAgents agent, Vehicle vehicle, Warehouse warehouse,
                                        RouteStatus status, List<SalesOrders> orders) {
        RoutePlanning route = new RoutePlanning();
        route.setRouteId("TRANSFER-" + UUID.randomUUID());
        route.setRouteStatus(status);
        route.setDeliveryAgentId(agent.getPublicId().toString());
        route.setDeliveryAgentName(agent.getFullName());
        route.setVehicleId(vehicle.getPublicId().toString());
        route.setVehicleName(vehicle.getNumberPlate());
        route.setVehicleWeight("100000");
        route.setWarehouseId(warehouse.getPublicId().toString());
        route.setWarehouseName(warehouse.getWarehouseName());
        route.setSalesOrders(new ArrayList<>(orders));
        agent.setAvailable(false);
        vehicle.setAvailable(false);
        transferEntityManager.persist(route);
        orders.forEach(order -> order.setRoutePublicId(route.getPublicId().toString()));
        return route;
    }

    private ProductRecon transferRecon(RoutePlanning route, DeliveryAgents agent) {
        ProductRecon recon = new ProductRecon();
        recon.setDeliveryId(route.getRouteId());
        recon.setDeliveryAgent(agent.getFullName());
        recon.setDeliveryAgentPublicId(agent.getPublicId().toString());
        recon.setVehicleNumber(route.getVehicleName());
        recon.setStatus(ReconStatus.Pending);
        transferEntityManager.persist(recon);
        return recon;
    }

    private ProductItem transferProductItem(ProductRecon recon, Product product, long picked, long ordered) {
        ProductItem item = new ProductItem();
        item.setProductRecon(recon);
        item.setProductReconPublicId(recon.getPublicId().toString());
        item.setProductPublicId(product.getPublicId());
        item.setProductName(product.getProductName());
        item.setProductCode(product.getSku());
        item.setProductSellingPrice(product.getSellPrice());
        item.setQuantityPicked(picked);
        item.setTotalUnits(picked);
        item.setOrderQuantity(ordered);
        item.setHasOrdered(true);
        item.setLoaded(true);
        transferEntityManager.persist(item);
        recon.addProductItem(item);
        return item;
    }

    private void transferCash(RoutePlanning route, ProductRecon recon, String picked, String deposit, String credit) {
        Reconciliation cash = new Reconciliation();
        cash.setRoutePublicId(route.getPublicId().toString());
        cash.setProductReconPublicId(recon.getPublicId().toString());
        cash.setDeliveryId(route.getRouteId());
        cash.setDeliveryAgentPublicId(recon.getDeliveryAgentPublicId());
        cash.setDeliveryAmount(new BigDecimal(picked));
        cash.setTotalAmountPicked(new BigDecimal(picked));
        cash.setDepositAmount(new BigDecimal(deposit));
        cash.setCreditAmount(new BigDecimal(credit));
        transferEntityManager.persist(cash);
        route.setBalance(cash.getDeliveryAmount().subtract(cash.getDepositAmount()).subtract(cash.getCreditAmount()));
    }

    private TransferInTransitRequest transferRequest(TransferFixture fixture, InTransitTransferType type) {
        TransferInTransitRequest request = new TransferInTransitRequest();
        request.setSourceRoutePublicId(fixture.sourcePublicId);
        request.setTargetDeliveryAgentPublicId(fixture.targetAgentPublicId);
        request.setTargetVehiclePublicId(fixture.targetVehiclePublicId);
        request.setOrderPublicIds(List.of(fixture.orderPublicId));
        request.setTransferType(type);
        return request;
    }

    private void transfer(TransferFixture fixture, InTransitTransferType type, int expectedStatus) {
        postTransfer(transferRequest(fixture, type), expectedStatus);
    }

    private void postTransfer(TransferInTransitRequest request, int expectedStatus) {
        given().auth().oauth2(transferToken()).contentType("application/json").body(request)
                .post("/logistics/routePlanning/transfer-in-transit").then().statusCode(expectedStatus);
    }

    private RoutePlanning transferredRoute(TransferFixture fixture) {
        return transferEntityManager.createQuery("select r from RoutePlanning r where r.deliveryAgentId = :agent and r.deleted = false", RoutePlanning.class)
                .setParameter("agent", fixture.targetAgentPublicId.toString()).getSingleResult();
    }

    private ProductRecon reconFor(RoutePlanning route) {
        return transferEntityManager.createQuery("select r from ProductRecon r where r.deliveryId = :route and r.deleted = false", ProductRecon.class)
                .setParameter("route", route.getRouteId()).getSingleResult();
    }

    private Reconciliation cashFor(RoutePlanning route) {
        return transferEntityManager.createQuery("select r from Reconciliation r where r.routePublicId = :route and r.deleted = false", Reconciliation.class)
                .setParameter("route", route.getPublicId().toString()).getSingleResult();
    }

    private BigDecimal pickedValue(ProductRecon recon) {
        return recon.getProductItems().stream().map(ProductItem::getTotalAmountPicked).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    private void assertTransferred(TransferFixture fixture, boolean existingTarget) {
        UUID[] targetIds = transferTransaction().execute(status -> {
            RoutePlanning source = transferEntityManager.find(RoutePlanning.class, fixture.sourceId);
            RoutePlanning target = transferredRoute(fixture);
            assertEquals(1, source.getSalesOrders().size());
            assertEquals(existingTarget ? 2 : 1, target.getSalesOrders().size());
            assertEquals(existingTarget ? fixture.targetStatus : fixture.sourceStatus, target.getRouteStatus());
            SalesOrders order = target.getSalesOrders().stream().filter(o -> o.getPublicId().equals(fixture.orderPublicId)).findFirst().orElseThrow();
            assertEquals(fixture.targetAgentPublicId, order.getDeliveryAgent().getPublicId());
            assertEquals(target.getPublicId().toString(), order.getRoutePublicId());
            ProductRecon sourceRecon = reconFor(source);
            ProductRecon targetRecon = reconFor(target);
            assertEquals(1, sourceRecon.getProductItems().size());
            assertEquals(2, targetRecon.getProductItems().size());
            ProductItem sourceItem = sourceRecon.getProductItems().get(0);
            assertEquals(4, sourceItem.getQuantityPicked());
            assertEquals(2, sourceItem.getOrderQuantity());
            assertEquals(2, sourceItem.getVanSaleQuantity());
            assertMoney("400", pickedValue(sourceRecon));
            assertMoney(existingTarget ? "800" : "400", pickedValue(targetRecon));
            assertMoney("400", cashFor(source).getTotalAmountPicked());
            assertMoney(existingTarget ? "800" : "400", cashFor(target).getTotalAmountPicked());
            assertMoney("350", source.getBalance());
            assertMoney(existingTarget ? "790" : "400", target.getBalance());
            return new UUID[]{target.getPublicId(), targetRecon.getPublicId()};
        });
        given().auth().oauth2(transferToken()).get("/logistics/routePlanning/{id}/orders", targetIds[0])
                .then().statusCode(200).body("content.publicId", hasItem(fixture.orderPublicId.toString()));
        given().auth().oauth2(transferToken()).get("/warehouse/productRecons/{id}/items", targetIds[1])
                .then().statusCode(200).body("content.productPublicId", hasItem(fixture.secondProductPublicId.toString()));
    }

    private record TransferFixture(Long sourceId, UUID sourcePublicId, UUID orderPublicId, UUID targetAgentPublicId,
                                   UUID targetVehiclePublicId, UUID firstProductPublicId, UUID secondProductPublicId,
                                   RouteStatus sourceStatus, RouteStatus targetStatus) {}

}
