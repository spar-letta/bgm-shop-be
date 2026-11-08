package com.app.itibari.web;

import com.app.itibari.ItibariApplicationTests;
import io.restassured.specification.RequestSpecification;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestPropertySource(properties = {
        "TRA_BASE_URL=http://localhost", "TRA_RECEIPT_ENDPOINT=/receipt",
        "TRA_BRANCH=test", "TRA_DEPARTMENT=test", "TRA_DEVICE_NUMBER=1",
        "TRA_CUSTOMER_ID_TYPE=1", "TRA_PAY_TYPE=1", "TRA_FCODE=test",
        "TRA_FCODE_TOKEN=test", "TRA_INVOICE_PREFIX=T"
})
public class ReconciliationDebitNotesIntegrationTest extends ItibariApplicationTests {
    private static final String ENDPOINT = "/reconciliation/debitNotes";

    @Autowired
    private JdbcTemplate jdbc;

    private final String marker = "debit-filter-" + UUID.randomUUID();
    private final UUID agentA = UUID.randomUUID();
    private final UUID agentB = UUID.randomUUID();
    private final UUID reconA = UUID.randomUUID();
    private final UUID reconB = UUID.randomUUID();
    private final UUID early = UUID.randomUUID();
    private final UUID late = UUID.randomUUID();
    private final UUID nextDay = UUID.randomUUID();
    private final UUID deleted = UUID.randomUUID();
    private final String routeA = "ROUTE-" + UUID.randomUUID();
    private final String routeB = "ROUTE-" + UUID.randomUUID();

    @Before
    public void seedDebitNotes() {
        jdbc.update("insert into itibari.reconciliation (public_id, delivery_id, route_id, deleted) values (?, ?, ?, false)",
                reconA, routeA, UUID.randomUUID().toString());
        jdbc.update("insert into itibari.reconciliation (public_id, delivery_id, route_id, deleted) values (?, ?, ?, false)",
                reconB, routeB, UUID.randomUUID().toString());
        insertNote(early, agentA, reconA, "2026-09-10T00:00:00", false, false);
        insertNote(late, agentB, reconB, "2026-09-10T23:59:59.999999", true, false);
        insertNote(nextDay, agentA, reconB, "2026-09-11T00:00:00", false, false);
        insertNote(deleted, agentA, reconA, "2026-09-10T12:00:00", false, true);
    }

    private void insertNote(UUID id, UUID agent, UUID reconciliation, String time, boolean reconciled, boolean removed) {
        jdbc.update("insert into itibari.debit_notes (public_id, reference_id, debit_reference_type, " +
                        "reconciliation_public_id, description, creation_time, reconciled, deleted, amount, balance) " +
                        "values (?, ?, 'DeliveryAgent', ?, ?, ?, ?, ?, 100, 100)",
                id, agent, reconciliation.toString(), marker, Timestamp.valueOf(LocalDateTime.parse(time)), reconciled, removed);
    }

    @After
    public void removeFixtures() {
        jdbc.update("delete from itibari.debit_notes where description = ?", marker);
        jdbc.update("delete from itibari.reconciliation where public_id in (?, ?)", reconA, reconB);
    }

    private RequestSpecification request() {
        return given().auth().oauth2(ADMIN_TOKEN);
    }

    @Test
    public void noFilters_returnsNotesAcrossAgentsAndStatuses() {
        request().queryParam("size", 2000).get(ENDPOINT).then().statusCode(200)
                .body("content.publicId", hasItems(early.toString(), late.toString(), nextDay.toString()))
                .body("content.publicId", not(hasItem(deleted.toString())));
    }

    @Test
    public void dateRange_includesWholeEndDateAndExcludesNextDay() {
        request().queryParam("searchParam", marker).queryParam("from", "2026-09-10")
                .queryParam("to", "2026-09-10").get(ENDPOINT).then().statusCode(200)
                .body("totalElements", equalTo(2))
                .body("content.publicId", contains(late.toString(), early.toString()));
    }

    @Test
    public void dateRange_supportsEitherBoundaryAlone() {
        request().queryParam("searchParam", marker).queryParam("from", "2026-09-11")
                .get(ENDPOINT).then().statusCode(200).body("content.publicId", contains(nextDay.toString()));
        request().queryParam("searchParam", marker).queryParam("to", "2026-09-10")
                .get(ENDPOINT).then().statusCode(200).body("totalElements", equalTo(2));
    }

    @Test
    public void routeNumber_matchesThroughReconciliationAndCombinesWithOtherFilters() {
        request().queryParam("routeNumber", routeB.toLowerCase(java.util.Locale.ROOT))
                .get(ENDPOINT).then().statusCode(200)
                .body("content.publicId", contains(nextDay.toString(), late.toString()));
        request().queryParam("routeNumber", routeB).queryParam("deliveryAgentPublicId", agentB)
                .queryParam("reconciliationPublicId", reconB).queryParam("reconciled", true)
                .queryParam("searchParam", marker).queryParam("from", "2026-09-10")
                .queryParam("to", "2026-09-10").get(ENDPOINT).then().statusCode(200)
                .body("totalElements", equalTo(1)).body("content.publicId", contains(late.toString()));
        request().queryParam("routeNumber", routeA + "-missing").get(ENDPOINT).then()
                .statusCode(200).body("totalElements", equalTo(0));
    }

    @Test
    public void pagination_preservesTotalAndDefaultDateSort() {
        request().queryParam("searchParam", marker).queryParam("page", 1).queryParam("size", 1)
                .get(ENDPOINT).then().statusCode(200).body("totalElements", equalTo(3))
                .body("content.publicId", contains(late.toString()));
    }

    @Test
    public void invalidDateRange_returnsBadRequest() {
        request().queryParam("from", "2026-09-11").queryParam("to", "2026-09-10")
                .get(ENDPOINT).then().statusCode(400);
        request().queryParam("from", "invalid-date").get(ENDPOINT).then().statusCode(400);
    }
}
