package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import com.app.itibari.dataTypes.AcceptTypeEnum;
import com.app.itibari.dataTypes.InTransitTransferType;
import com.app.itibari.dataTypes.LoadingType;
import com.app.itibari.dataTypes.RouteAction;
import com.app.itibari.dataTypes.RouteStatus;
import com.app.itibari.dataTypes.SourceChannelEnum;
import com.app.itibari.models.request.LoadingDTO;
import com.app.itibari.models.request.LoadingRequest;
import com.app.itibari.models.request.RoutePlanningRequest;
import com.app.itibari.models.request.SalesOrderItemsRequest;
import com.app.itibari.models.request.SalesOrdersRequest;
import com.app.itibari.models.request.TransferInTransitRequest;
import com.app.itibari.scheduler.Scheduler;
import io.restassured.http.ContentType;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Integration tests for POST /logistics/routePlanning/transfer-in-transit.
 *
 * Stateful tests (A, B, H–K) are marked {@code @Ignore} because they require
 * freshly-available seed DA / vehicle entities and cannot be safely combined with
 * other stateful tests in the same H2 run.
 * Run them individually with:
 *   mvn test -Dtest=TransferInTransitRestControllerTests#testA_…
 *
 * Pure validation tests (C–G) run in every CI build.
 *
 * ┌──────┬─────────────────────────────────────────────────────────────────────┐
 * │ Test │ Description                                                         │
 * ├──────┼─────────────────────────────────────────────────────────────────────┤
 * │  A   │ ROUTE transfer while Loaded (@Ignore)                               │
 * │  B   │ ORDER transfer while IN_TRANSIT (@Ignore)                           │
 * │  C   │ 403 when customer token used (always runs)                          │
 * │  D   │ 400 when required fields missing (always runs)                      │
 * │  E   │ 400 when source route not found (always runs)                       │
 * │  F   │ 400 when ORDER type submitted with empty order list (always runs)   │
 * │  G   │ 400 when transferType value is invalid (always runs)                │
 * │  H   │ 400 when route status is CREATED (@Ignore)                         │
 * │  I   │ 400 when ORDER transfer selects ALL orders (@Ignore)               │
 * │  J   │ ORDER transfer merges into target DA's existing active route        │
 * │      │ instead of creating a new one (@Ignore)                             │
 * │  K   │ ROUTE transfer succeeds when route is in ReadyToGo status (@Ignore)│
 * └──────┴─────────────────────────────────────────────────────────────────────┘
 *
 * Seed IDs (from my-data.sql):
 *  - DA 1    : af485765-69c5-4dd0-8fa0-560058b85c65  (Erick Massawe,   available=true)
 *  - DA 2    : 8ffd638c-b83c-4b87-8eb1-11811abdc071  (Dennis Kipchiri, available=true)
 *  - Vehicle1 : fa4bf2ee-6665-493a-8dbd-5d56dc8a7d93  weight=100 000, available=true
 *  - Vehicle2 : 2910a4a3-67e7-4d6b-a1d7-953cb8bd9314  weight=100 000, available=true
 *  - Warehouse: 3849b1be-a9dd-4f11-abbf-229ad2780a56 (Dar North)
 *  - Region   : 8c5d4db0-f9a1-47ed-a160-56780e4e4d14
 *  - Product 1: 7c4a6d52-5da2-47fe-92a8-ea2fce2602af (KissKids Baby Pants M-32, weight=7.20)
 *  - Product 2: 08ebb084-2633-428f-82b9-e8d98a22ec53 (Bwana Sukari Brown Sugar 5KG, weight=20.00)
 *  - Customer : afbcaadc-45ff-4f6d-8267-e811b9dfc434
 */
public class TransferInTransitRestControllerTests extends ItibariApplicationTests {

    private static final UUID DA_1       = UUID.fromString("af485765-69c5-4dd0-8fa0-560058b85c65");
    private static final UUID DA_2       = UUID.fromString("8ffd638c-b83c-4b87-8eb1-11811abdc071");
    private static final UUID VEHICLE_1  = UUID.fromString("fa4bf2ee-6665-493a-8dbd-5d56dc8a7d93");
    private static final UUID VEHICLE_2  = UUID.fromString("2910a4a3-67e7-4d6b-a1d7-953cb8bd9314");
    private static final UUID WAREHOUSE  = UUID.fromString("3849b1be-a9dd-4f11-abbf-229ad2780a56");
    private static final UUID REGION     = UUID.fromString("8c5d4db0-f9a1-47ed-a160-56780e4e4d14");
    private static final UUID PRODUCT_1  = UUID.fromString("7c4a6d52-5da2-47fe-92a8-ea2fce2602af");
    private static final UUID PRODUCT_2  = UUID.fromString("08ebb084-2633-428f-82b9-e8d98a22ec53");
    private static final UUID CUSTOMER   = UUID.fromString("afbcaadc-45ff-4f6d-8267-e811b9dfc434");

    @Autowired
    private Scheduler scheduler;

    @Test
    @Ignore("Stateful test – run alone: mvn test -Dtest=TransferInTransitRestControllerTests#testA_transferRouteType_whileLoaded")
    public void testA_transferRouteType_whileLoaded() {

        String orderPublicId = createSalesOrder(PRODUCT_1, 2);

        scheduler.runOrderPlanning();

        String routePublicId = createRoute(DA_1, VEHICLE_1, Collections.singletonList(UUID.fromString(orderPublicId)));

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{id}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.CREATED.name()));

        loadRoute(routePublicId, DA_1);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{id}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.Loaded.name()));

        // 7. Transfer the entire route to DA 2 + Vehicle 2

        TransferInTransitRequest req = new TransferInTransitRequest();
        req.setSourceRoutePublicId(UUID.fromString(routePublicId));
        req.setTargetDeliveryAgentPublicId(DA_2);
        req.setTargetVehiclePublicId(VEHICLE_2);
        req.setTransferType(InTransitTransferType.ROUTE);
        req.setReason("Vehicle breakdown during loading");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(req)
                .post("/logistics/routePlanning/transfer-in-transit")
                .then()
                .statusCode(200);

        // 8. Source route must now be TRANSFERRED
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{id}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.TRANSFERRED.name()));

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("deliveryAgentPublicId", DA_2.toString())
                .queryParam("status", RouteStatus.Loaded.name())
                .get("/logistics/routePlannings")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].deliveryAgentId", equalTo(DA_2.toString()))
                .body("content[0].routeStatus", equalTo(RouteStatus.Loaded.name()));

        // 10. The sales order should now point to DA 2
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{id}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("deliveryAgent", notNullValue());
    }

    @Test
    @Ignore("Stateful test – run alone: mvn test -Dtest=TransferInTransitRestControllerTests#testB_transferOrderType_whileInTransit")
    public void testB_transferOrderType_whileInTransit() {

        // 1. Place two sales orders
        String orderOneId = createSalesOrder(PRODUCT_1, 2);
        String orderTwoId = createSalesOrder(PRODUCT_2, 1);

        scheduler.runOrderPlanning();

        // 2. Create route with both orders on DA 1 + Vehicle 1
        String routePublicId = createRoute(
                DA_1, VEHICLE_1,
                List.of(UUID.fromString(orderOneId), UUID.fromString(orderTwoId))
        );

        // 3. Load all products
        loadRoute(routePublicId, DA_1);

        // 4. DA accepts route → ReadyToGo
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(DELIVERY_AGENT_TOKEN)
                .header("X-App-Version", "1.0.0")
                .queryParam("acceptType", AcceptTypeEnum.AcceptRoute)
                .patch("/deliveryAgents/accept/{routeId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200);

        // 5. DA starts route → IN_TRANSIT
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(DELIVERY_AGENT_TOKEN)
                .header("X-App-Version", "1.0.0")
                .queryParam("routeId", UUID.fromString(routePublicId))
                .queryParam("action", RouteAction.Start)
                .patch("/deliveryAgents/routeActions")
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{id}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.IN_TRANSIT.name()));

        // 6. Transfer only order 1 to DA 2 + Vehicle 2 (breakdown scenario)
        TransferInTransitRequest req = new TransferInTransitRequest();
        req.setSourceRoutePublicId(UUID.fromString(routePublicId));
        req.setTargetDeliveryAgentPublicId(DA_2);
        req.setTargetVehiclePublicId(VEHICLE_2);
        req.setTransferType(InTransitTransferType.ORDER);
        req.setOrderPublicIds(Collections.singletonList(UUID.fromString(orderOneId)));
        req.setReason("Breakdown on highway – transferring single order");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(req)
                .post("/logistics/routePlanning/transfer-in-transit")
                .then()
                .statusCode(200);

        // 7. Source route should still be IN_TRANSIT (still has order 2)
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{id}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.IN_TRANSIT.name()));

        // 8. A new route for DA 2 must exist with the same status (IN_TRANSIT)
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("deliveryAgentPublicId", DA_2.toString())
                .queryParam("status", RouteStatus.IN_TRANSIT.name())
                .get("/logistics/routePlannings")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].deliveryAgentId", equalTo(DA_2.toString()));

        // 9. Source route should only have order 2 left
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{id}/orders", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].publicId", equalTo(orderTwoId));

        // 10. Transferred order 1 should now point to DA 2
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{id}", UUID.fromString(orderOneId))
                .then()
                .statusCode(200)
                .body("deliveryAgent", notNullValue());

        // 11. A new reconciliation for new route should exist
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("deliveryAgentPublicId", DA_2.toString())
                .get("/reconciliation/reconciliations")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1));
    }

    @Test
    public void testC_transferInTransit_returnsUnauthorized_whenCustomerToken() {

        TransferInTransitRequest req = new TransferInTransitRequest();
        req.setSourceRoutePublicId(UUID.randomUUID());
        req.setTargetDeliveryAgentPublicId(DA_2);
        req.setTargetVehiclePublicId(VEHICLE_2);
        req.setTransferType(InTransitTransferType.ROUTE);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(CUSTOMER_TOKEN)
                .body(req)
                .post("/logistics/routePlanning/transfer-in-transit")
                .then()
                .statusCode(401);
    }

    @Test
    public void testD_transferInTransit_returnsBadRequest_whenRequiredFieldsMissing() {

        // Empty body → all @NotNull fields missing → 400
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body("{}")
                .post("/logistics/routePlanning/transfer-in-transit")
                .then()
                .statusCode(400);
    }

    @Test
    public void testE_transferInTransit_returnsBadRequest_whenSourceRouteNotFound() {

        TransferInTransitRequest req = new TransferInTransitRequest();
        req.setSourceRoutePublicId(UUID.randomUUID());   // does not exist
        req.setTargetDeliveryAgentPublicId(DA_2);
        req.setTargetVehiclePublicId(VEHICLE_2);
        req.setTransferType(InTransitTransferType.ROUTE);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(req)
                .post("/logistics/routePlanning/transfer-in-transit")
                .then()
                .statusCode(400);
    }

    @Test
    public void testF_transferInTransit_returnsBadRequest_whenOrderTypeHasNoOrderIds() {

        TransferInTransitRequest req = new TransferInTransitRequest();
        req.setSourceRoutePublicId(UUID.randomUUID());   // won't reach validation; route not found → 400 first
        req.setTargetDeliveryAgentPublicId(DA_2);
        req.setTargetVehiclePublicId(VEHICLE_2);
        req.setTransferType(InTransitTransferType.ORDER);
        req.setOrderPublicIds(Collections.emptyList());  // ORDER type with no ids

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(req)
                .post("/logistics/routePlanning/transfer-in-transit")
                .then()
                .statusCode(400);
    }

    @Test
    public void testG_transferInTransit_returnsBadRequest_whenTransferTypeInvalid() {

        String badBody = """
                {
                    "sourceRoutePublicId": "00000000-0000-0000-0000-000000000001",
                    "targetDeliveryAgentPublicId": "00000000-0000-0000-0000-000000000002",
                    "targetVehiclePublicId": "00000000-0000-0000-0000-000000000003",
                    "transferType": "INVALID_TYPE"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(badBody)
                .post("/logistics/routePlanning/transfer-in-transit")
                .then()
                .statusCode(400);
    }

    @Test
    @Ignore("Stateful test – run alone: mvn test -Dtest=TransferInTransitRestControllerTests#testH_transferFails_whenRouteIsCreated")
    public void testH_transferFails_whenRouteIsCreated() {

        // Create an order and place it on a route (status stays CREATED until loaded)
        String orderPublicId = createSalesOrder(PRODUCT_1, 1);
        scheduler.runOrderPlanning();

        String routePublicId = createRoute(DA_1, VEHICLE_1, Collections.singletonList(UUID.fromString(orderPublicId)));

        // Confirm CREATED
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{id}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.CREATED.name()));

        // Attempt transfer while in CREATED → must fail with 400
        TransferInTransitRequest req = new TransferInTransitRequest();
        req.setSourceRoutePublicId(UUID.fromString(routePublicId));
        req.setTargetDeliveryAgentPublicId(DA_2);
        req.setTargetVehiclePublicId(VEHICLE_2);
        req.setTransferType(InTransitTransferType.ROUTE);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(req)
                .post("/logistics/routePlanning/transfer-in-transit")
                .then()
                .statusCode(400);
    }

    /**
     * testI – ORDER transfer with ALL orders selected must return 400.
     *
     * The service explicitly rejects selecting every order via ORDER type; the caller
     * should use ROUTE type instead. Steps:
     *  1. Create two orders, run planning, build a route containing both on DA 1 + Vehicle 1.
     *  2. Load the route so it reaches Loaded status.
     *  3. Submit an ORDER transfer that names both orders → expect HTTP 400.
     */
    @Test
    @Ignore("Stateful test – run alone: mvn test -Dtest=TransferInTransitRestControllerTests#testI_orderTransfer_failsWhenAllOrdersSelected")
    public void testI_orderTransfer_failsWhenAllOrdersSelected() {

        String orderOneId = createSalesOrder(PRODUCT_1, 2);
        String orderTwoId = createSalesOrder(PRODUCT_2, 1);

        scheduler.runOrderPlanning();

        String routePublicId = createRoute(
                DA_1, VEHICLE_1,
                List.of(UUID.fromString(orderOneId), UUID.fromString(orderTwoId))
        );

        loadRoute(routePublicId, DA_1);

        // Confirm Loaded
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{id}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.Loaded.name()));

        // Attempt ORDER transfer selecting ALL orders → must fail with 400
        TransferInTransitRequest req = new TransferInTransitRequest();
        req.setSourceRoutePublicId(UUID.fromString(routePublicId));
        req.setTargetDeliveryAgentPublicId(DA_2);
        req.setTargetVehiclePublicId(VEHICLE_2);
        req.setTransferType(InTransitTransferType.ORDER);
        req.setOrderPublicIds(List.of(UUID.fromString(orderOneId), UUID.fromString(orderTwoId)));

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(req)
                .post("/logistics/routePlanning/transfer-in-transit")
                .then()
                .statusCode(400);
    }

    /**
     * testJ – ORDER transfer merges into the target DA's existing active route
     *          instead of creating a brand-new one.
     *
     * Setup:
     *  - Route A: DA 1 + Vehicle 1 – contains order 1 (Loaded)
     *  - Route B: DA 2 + Vehicle 2 – contains order 2 (Loaded)
     *
     * Action:
     *  - Transfer ORDER type: move order 1 from route A to DA 2.
     *
     * Expected:
     *  - Route A becomes TRANSFERRED (no orders left).
     *  - No brand-new route is created for DA 2.
     *  - Route B now holds both order 1 and order 2.
     *  - Order 1 reports DA 2 as its delivery agent.
     */
    @Test
    @Ignore("Stateful test – run alone: mvn test -Dtest=TransferInTransitRestControllerTests#testJ_orderTransfer_mergesIntoExistingRoute")
    public void testJ_orderTransfer_mergesIntoExistingRoute() {

        // 1. Create two separate orders
        String orderOneId = createSalesOrder(PRODUCT_1, 2);
        String orderTwoId = createSalesOrder(PRODUCT_2, 1);

        scheduler.runOrderPlanning();

        // 2. Route A: DA 1 + Vehicle 1 with order 1
        String routeAPublicId = createRoute(DA_1, VEHICLE_1, Collections.singletonList(UUID.fromString(orderOneId)));
        loadRoute(routeAPublicId, DA_1);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{id}", UUID.fromString(routeAPublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.Loaded.name()));

        // 3. Route B: DA 2 + Vehicle 2 with order 2
        String routeBPublicId = createRoute(DA_2, VEHICLE_2, Collections.singletonList(UUID.fromString(orderTwoId)));
        loadRoute(routeBPublicId, DA_2);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{id}", UUID.fromString(routeBPublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.Loaded.name()));

        // 4. Transfer order 1 from route A to DA 2 (who already has route B)
        TransferInTransitRequest req = new TransferInTransitRequest();
        req.setSourceRoutePublicId(UUID.fromString(routeAPublicId));
        req.setTargetDeliveryAgentPublicId(DA_2);
        req.setTargetVehiclePublicId(VEHICLE_2);
        req.setTransferType(InTransitTransferType.ORDER);
        req.setOrderPublicIds(Collections.singletonList(UUID.fromString(orderOneId)));
        req.setReason("Merging into existing DA route");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(req)
                .post("/logistics/routePlanning/transfer-in-transit")
                .then()
                .statusCode(200);

        // 5. Route A must now be TRANSFERRED (it had only one order)
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{id}", UUID.fromString(routeAPublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.TRANSFERRED.name()));

        // 6. Route B (existing DA 2 route) must now hold both orders
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{id}/orders", UUID.fromString(routeBPublicId))
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(2));

        // 7. Order 1 must now reference DA 2
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{id}", UUID.fromString(orderOneId))
                .then()
                .statusCode(200)
                .body("deliveryAgent", notNullValue());

        // 8. Only route B should remain active for DA 2 (no duplicate new route)
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("deliveryAgentPublicId", DA_2.toString())
                .queryParam("status", RouteStatus.Loaded.name())
                .get("/logistics/routePlannings")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].publicId", equalTo(routeBPublicId));
    }

    /**
     * testK – ROUTE transfer succeeds when the source route is in ReadyToGo status.
     *
     * ReadyToGo was added to TRANSFERABLE_STATUSES.  This test confirms the endpoint
     * returns 200 and the source route transitions to TRANSFERRED.
     *
     * Steps:
     *  1. Place an order → run planning → create route → load → DA accepts (→ ReadyToGo).
     *  2. ROUTE-transfer to DA 2 + Vehicle 2 → expect 200.
     *  3. Source route must be TRANSFERRED.
     *  4. A new Loaded/ReadyToGo route must exist for DA 2.
     */
    @Test
    @Ignore("Stateful test – run alone: mvn test -Dtest=TransferInTransitRestControllerTests#testK_transferRouteType_whileReadyToGo")
    public void testK_transferRouteType_whileReadyToGo() {

        // 1. Order + route
        String orderPublicId = createSalesOrder(PRODUCT_1, 2);
        scheduler.runOrderPlanning();
        String routePublicId = createRoute(DA_1, VEHICLE_1, Collections.singletonList(UUID.fromString(orderPublicId)));

        // 2. Load the route
        loadRoute(routePublicId, DA_1);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{id}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.Loaded.name()));

        // 3. DA accepts → ReadyToGo
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(DELIVERY_AGENT_TOKEN)
                .header("X-App-Version", "1.0.0")
                .queryParam("acceptType", AcceptTypeEnum.AcceptRoute)
                .patch("/deliveryAgents/accept/{routeId}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{id}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.ReadyToGo.name()));

        // 4. ROUTE transfer while ReadyToGo → must succeed
        TransferInTransitRequest req = new TransferInTransitRequest();
        req.setSourceRoutePublicId(UUID.fromString(routePublicId));
        req.setTargetDeliveryAgentPublicId(DA_2);
        req.setTargetVehiclePublicId(VEHICLE_2);
        req.setTransferType(InTransitTransferType.ROUTE);
        req.setReason("Driver reassignment before departure");

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(req)
                .post("/logistics/routePlanning/transfer-in-transit")
                .then()
                .statusCode(200);

        // 5. Source route must now be TRANSFERRED
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{id}", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("routeStatus", equalTo(RouteStatus.TRANSFERRED.name()));

        // 6. A new route for DA 2 with ReadyToGo status must exist
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .queryParam("deliveryAgentPublicId", DA_2.toString())
                .queryParam("status", RouteStatus.ReadyToGo.name())
                .get("/logistics/routePlannings")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .body("content[0].deliveryAgentId", equalTo(DA_2.toString()))
                .body("content[0].routeStatus", equalTo(RouteStatus.ReadyToGo.name()));

        // 7. The order must be assigned to DA 2
        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/salesOrders/{id}", UUID.fromString(orderPublicId))
                .then()
                .statusCode(200)
                .body("deliveryAgent", notNullValue());
    }

    /** Creates and persists a single-product sales order, returns the publicId. */
    private String createSalesOrder(UUID productId, int qty) {
        SalesOrderItemsRequest item = SalesOrderItemsRequest.builder()
                .productId(productId)
                .quantity(qty)
                .build();
        SalesOrdersRequest body = SalesOrdersRequest.builder()
                .customerId(CUSTOMER)
                .bookedBy("Transfer-Test")
                .sourceChannel(SourceChannelEnum.BACK_OFFICE_APP.name())
                .salesOrderItems(Collections.singleton(item))
                .build();

        return given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(body)
                .post("/salesOrders")
                .then()
                .statusCode(200)
                .extract().path("publicId");
    }

    /** Creates a route planning for the given DA / vehicle / orders, returns publicId. */
    private String createRoute(UUID daId, UUID vehicleId, List<UUID> orderIds) {
        RoutePlanningRequest req = new RoutePlanningRequest();
        req.setLocationPublicId(UUID.fromString("90ad01f1-4a13-41f7-b825-c428b41f35d0"));
        req.setRegionPublicId(REGION);
        req.setVehiclePublicId(vehicleId);
        req.setDeliveryAgentPublicId(daId);
        req.setWarehousePublicId(WAREHOUSE);
        req.setSalesOrdersPublicIds(orderIds);

        return given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(req)
                .post("/logistics/routePlanning")
                .then()
                .statusCode(200)
                .extract().path("publicId");
    }

    /**
     * Fetches the loading product list for the route, then fires the warehouse loading endpoint
     * for every product item. After this call the route status becomes Loaded.
     */
    private void loadRoute(String routePublicId, UUID daId) {
        List<String> productItemIds = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .get("/logistics/routePlanning/{id}/loadingProducts", UUID.fromString(routePublicId))
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(1))
                .extract().path("content.productItemPublicId");

        List<LoadingDTO> dtos = productItemIds.stream()
                .map(id -> {
                    LoadingDTO dto = new LoadingDTO();
                    dto.setProductItemPublicId(UUID.fromString(id));
                    dto.setLoadingType(LoadingType.OrderSale);
                    return dto;
                }).toList();

        LoadingRequest loadingReq = new LoadingRequest();
        loadingReq.setLoadingDTOList(dtos);

        given()
                .contentType(ContentType.JSON)
                .auth().oauth2(ADMIN_TOKEN)
                .body(loadingReq)
                .post("/warehouse/agent/{agentId}/loading", daId)
                .then()
                .statusCode(200);
    }
}

