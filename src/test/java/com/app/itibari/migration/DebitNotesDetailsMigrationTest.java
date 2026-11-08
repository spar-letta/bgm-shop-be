package com.app.itibari.migration;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.DirectoryResourceAccessor;
import org.junit.Test;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.UUID;

import static org.junit.Assert.*;

public class DebitNotesDetailsMigrationTest {
    @Test
    public void initializesDebitNotePaidAmountAndCanRunAgain() throws Exception {
        Path directory = Files.createTempDirectory("reconciliation-debit-note-payment-");
        Path changelog = directory.resolve("changelog.xml");
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            var document = factory.newDocumentBuilder().parse(
                    Path.of("src/main/resources/db/changelog/liquibase.change-log.xml").toFile());
            var root = document.getDocumentElement();
            for (var node = root.getLastChild(); node != null;) {
                var previous = node.getPreviousSibling();
                if (!(node instanceof Element element)
                        || !element.getAttribute("id").equals("20261013 reconciliation-debit-note-paid-amount")) {
                    root.removeChild(node);
                }
                node = previous;
            }
            TransformerFactory.newInstance().newTransformer()
                    .transform(new DOMSource(document), new StreamResult(changelog.toFile()));
            try (var connection = DriverManager.getConnection(
                    "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL", "sa", "")) {
                try (var statement = connection.createStatement()) {
                    statement.execute("CREATE SCHEMA itibari");
                    statement.execute("CREATE TABLE itibari.reconciliation (id bigint PRIMARY KEY)");
                    statement.execute("INSERT INTO itibari.reconciliation VALUES (1)");
                }
                var database = new liquibase.database.core.H2Database();
                database.setConnection(new JdbcConnection(connection));
                try (var accessor = new DirectoryResourceAccessor(directory)) {
                    var liquibase = new Liquibase("changelog.xml", accessor, database);
                    liquibase.update(new Contexts(), new LabelExpression());
                    liquibase.update(new Contexts(), new LabelExpression());
                    try (var statement = connection.createStatement();
                         var result = statement.executeQuery("SELECT debit_note_paid_amount FROM itibari.reconciliation")) {
                        assertTrue(result.next());
                        assertEquals(0, result.getBigDecimal(1).signum());
                    }
                }
            }
        } finally {
            Files.deleteIfExists(changelog);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void addsAndBackfillsColumnsAndCanRunAgain() throws Exception {
        verifyMigration(false);
    }

    @Test
    public void handlesAnAlreadyExistingColumnAndPreservesItsValue() throws Exception {
        verifyMigration(true);
    }

    private void verifyMigration(boolean existingNameColumn) throws Exception {
        Path directory = Files.createTempDirectory("debit-notes-migration-");
        Path changelog = directory.resolve("changelog.xml");
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            var document = factory.newDocumentBuilder().parse(
                    Path.of("src/main/resources/db/changelog/liquibase.change-log.xml").toFile());
            var root = document.getDocumentElement();
            for (var node = root.getLastChild(); node != null;) {
                var previous = node.getPreviousSibling();
                if (!(node instanceof Element element)
                        || !(element.getAttribute("id").startsWith("20261008 debit-notes-details-")
                        || element.getAttribute("id").startsWith("20261008 debit-notes-route-"))) {
                    root.removeChild(node);
                }
                node = previous;
            }
            TransformerFactory.newInstance().newTransformer()
                    .transform(new DOMSource(document), new StreamResult(changelog.toFile()));

            try (var connection = DriverManager.getConnection(
                    "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL", "sa", "")) {
                try (var statement = connection.createStatement()) {
                    statement.execute("CREATE SCHEMA itibari");
                    statement.execute("CREATE TABLE itibari.reconciliation (public_id uuid PRIMARY KEY, "
                            + "delivery_id varchar(255), route_id varchar(255))");
                    statement.execute("INSERT INTO itibari.reconciliation VALUES "
                            + "('00000000-0000-0000-0000-000000000010', 'RT-001', "
                            + "'00000000-0000-0000-0000-000000000020')");
                    statement.execute("CREATE TABLE itibari.delivery_agents (public_id uuid PRIMARY KEY, "
                            + "first_name varchar(255), last_name varchar(255), debt_zone varchar(255), deleted boolean)");
                    statement.execute("CREATE TABLE itibari.debit_notes (id bigint PRIMARY KEY, reference_id uuid, "
                            + "debit_reference_type varchar(255), reconciled boolean)");
                    statement.execute("INSERT INTO itibari.delivery_agents VALUES "
                            + "('00000000-0000-0000-0000-000000000001', 'Asha', 'Mrema', 'YellowZone', false)");
                    statement.execute("INSERT INTO itibari.debit_notes VALUES "
                            + "(1, '00000000-0000-0000-0000-000000000001', 'DeliveryAgent', false), "
                            + "(2, '00000000-0000-0000-0000-000000000001', 'DeliveryAgent', true), "
                            + "(3, '00000000-0000-0000-0000-000000000002', 'DeliveryAgent', false)");
                    statement.execute("ALTER TABLE itibari.debit_notes ADD reconciliation_public_id varchar(255)");
                    statement.execute("UPDATE itibari.debit_notes SET reconciliation_public_id = "
                            + "'00000000-0000-0000-0000-000000000010' WHERE id IN (1, 2)");
                    if (existingNameColumn) {
                        statement.execute("ALTER TABLE itibari.debit_notes ADD route varchar(255)");
                        statement.execute("UPDATE itibari.debit_notes SET route = 'Existing route' WHERE id = 1");
                        statement.execute("ALTER TABLE itibari.debit_notes ADD delivery_agent_name varchar(255)");
                        statement.execute("UPDATE itibari.debit_notes SET delivery_agent_name = 'Existing name' WHERE id = 1");
                    }
                }
                try (var accessor = new DirectoryResourceAccessor(directory);
                     var liquibase = new Liquibase("changelog.xml", accessor, new JdbcConnection(connection))) {
                    liquibase.update(new Contexts(), new LabelExpression());
                    liquibase.update(new Contexts(), new LabelExpression());
                    try (var statement = connection.createStatement();
                         var rows = statement.executeQuery("SELECT * FROM itibari.debit_notes ORDER BY id")) {
                        assertTrue(rows.next());
                        assertEquals(existingNameColumn ? "Existing name" : "Asha Mrema",
                                rows.getString("delivery_agent_name"));
                        assertEquals("00000000-0000-0000-0000-000000000001",
                                rows.getString("delivery_agent_public_id"));
                        assertEquals("YellowZone", rows.getString("debt_level"));
                        assertEquals("PENDING", rows.getString("status"));
                        assertEquals(existingNameColumn ? "Existing route" : "RT-001", rows.getString("route"));
                        assertEquals("00000000-0000-0000-0000-000000000020", rows.getString("route_public_id"));
                        assertTrue(rows.next());
                        assertEquals("RECONCILED", rows.getString("status"));
                        assertEquals("RT-001", rows.getString("route"));
                        assertEquals("00000000-0000-0000-0000-000000000020", rows.getString("route_public_id"));
                        assertTrue(rows.next());
                        assertEquals("00000000-0000-0000-0000-000000000002",
                                rows.getString("delivery_agent_public_id"));
                        assertNull(rows.getString("delivery_agent_name"));
                        assertNull(rows.getString("debt_level"));
                        assertEquals("PENDING", rows.getString("status"));
                        assertNull(rows.getString("route"));
                        assertNull(rows.getString("route_public_id"));
                        assertFalse(rows.next());
                    }
                }
            }
        } finally {
            Files.deleteIfExists(changelog);
            Files.deleteIfExists(directory);
        }
    }
}
