package com.app.itibari.services;

import com.app.itibari.dataTypes.CashCreditReconAdjustmentStatus;
import com.app.itibari.dataTypes.CashCreditReconAdjustmentType;
import com.app.itibari.dataTypes.CashCreditReconReportStatus;
import com.app.itibari.dataTypes.ReconStatus;
import com.app.itibari.dataTypes.DebitReferenceType;
import com.app.itibari.dataTypes.DebtZone;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.app.itibari.utils.views.BaseView;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.models.entities.Reconciliation;
import com.app.itibari.models.entities.RoutePlanning;
import com.app.itibari.models.request.CashCreditReconAdjustmentRequest;
import com.app.itibari.models.entities.ProductRecon;
import com.app.itibari.models.entities.DeliveryAgents;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.DebitNotes;
import com.app.itibari.models.request.ReconciliationRequest;
import com.app.itibari.models.request.DebitNoteRequest;
import com.app.itibari.services.helper.LogisticsHelper;
import com.app.itibari.models.entities.CashCreditReconAdjustment;
import com.app.itibari.models.entities.CashCreditReconReport;
import com.app.itibari.models.entities.CashCreditReconReportLine;
import com.app.itibari.models.response.CashCreditReconPendingAdjustmentsDTO;
import com.app.itibari.repository.*;
import com.app.itibari.utils.helpers.CreditReconHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ReconciliationServiceImplTest {

    @Mock
    private ReconciliationRepository reconciliationRepository;
    @Mock
    private PersonRepository personRepository;
    @Mock
    private ProductReconRepository productReconRepository;
    @Mock
    private DeliveryAgentsRepository deliveryAgentsRepository;
    @Mock
    private LogisticsHelper logisticsHelper;
    @Mock
    private DebitNoteRepository debitNoteRepository;
    @Mock
    private RoutePlanningRepository routePlanningRepository;
    @Mock
    private CreditReconHelper creditReconHelper;
    @Mock
    private CreditReconRepository creditReconRepository;
    @Mock
    private PaymentItemRepository paymentItemRepository;
    @Mock
    private CreditLimitItemRepository creditLimitItemRepository;
    @Mock
    private CreditReconItemRepository creditReconItemRepository;
    @Mock
    private EventsRepository eventsRepository;
    @Mock
    private CashCreditReconReportRepository cashCreditReconReportRepository;
    @Mock
    private CashCreditReconReportLineRepository cashCreditReconReportLineRepository;
    @Mock
    private CashCreditReconAdjustmentRepository cashCreditReconAdjustmentRepository;

    @InjectMocks
    private ReconciliationServiceImpl reconciliationService;

    @Test
    public void updateDebitNote_countsEveryPartialPaymentAndSettlesRemainingBalance() {
        DebitNotes note = new DebitNotes();
        note.setPublicId(UUID.randomUUID());
        note.setReferenceType(DebitReferenceType.DeliveryAgent);
        note.setReferenceId(UUID.randomUUID());
        note.setAmount(new BigDecimal("100"));
        note.setBalance(new BigDecimal("100"));
        Reconciliation recon = new Reconciliation();
        recon.setPublicId(UUID.randomUUID());
        recon.setDeliveryId("RT-001");
        recon.setDeliveryAmount(new BigDecimal("200"));
        recon.setDepositAmount(new BigDecimal("100"));
        note.setReconciliationPublicId(recon.getPublicId().toString());
        Person person = new Person();
        DeliveryAgents agent = new DeliveryAgents();
        agent.setPublicId(note.getReferenceId());
        RoutePlanning route = new RoutePlanning();
        when(debitNoteRepository.findByPublicIdForUpdate(note.getPublicId())).thenReturn(Optional.of(note));
        when(debitNoteRepository.save(note)).thenReturn(note);
        when(personRepository.findByEmailAndDeletedIsFalse("admin@example.com")).thenReturn(Optional.of(person));
        when(reconciliationRepository.findByPublicIdForUpdate(recon.getPublicId())).thenReturn(Optional.of(recon));
        when(deliveryAgentsRepository.findByPublicIdAndDeletedIsFalse(agent.getPublicId())).thenReturn(Optional.of(agent));
        when(routePlanningRepository.findByRouteIdAndDeletedIsFalse("RT-001")).thenReturn(Optional.of(route));

        reconciliationService.updateDebitNote(note.getPublicId(), new BigDecimal("40"), "admin@example.com");

        assertEquals(new BigDecimal("60"), note.getBalance());
        assertEquals(false, note.getReconciled());
        assertEquals(new BigDecimal("40"), recon.getDebitNotePaidAmount());
        assertEquals(new BigDecimal("140"), recon.getDepositAmount());
        assertEquals(new BigDecimal("60"), recon.getOutstandingAmount());
        assertEquals(new BigDecimal("60"), route.getBalance());

        reconciliationService.updateDebitNote(note.getPublicId(), new BigDecimal("60"), "admin@example.com");

        assertEquals(BigDecimal.ZERO, note.getBalance());
        assertTrue(note.getReconciled());
        assertEquals(new BigDecimal("100"), recon.getDebitNotePaidAmount());
        assertEquals(new BigDecimal("200"), recon.getDepositAmount());
        assertEquals(BigDecimal.ZERO, recon.getOutstandingAmount());
        assertEquals(BigDecimal.ZERO, route.getBalance());
        verify(reconciliationRepository, times(2)).save(recon);
    }

    @Test
    public void updateDebitNote_rejectsPaymentsAboveRemainingBalance() {
        DebitNotes note = new DebitNotes();
        note.setPublicId(UUID.randomUUID());
        note.setReferenceType(DebitReferenceType.DeliveryAgent);
        note.setAmount(new BigDecimal("100"));
        note.setBalance(new BigDecimal("20"));
        when(debitNoteRepository.findByPublicIdForUpdate(note.getPublicId())).thenReturn(Optional.of(note));
        when(personRepository.findByEmailAndDeletedIsFalse("admin@example.com")).thenReturn(Optional.of(new Person()));

        assertThrows(BadRequestRestApiException.class, () -> reconciliationService.updateDebitNote(
                note.getPublicId(), new BigDecimal("21"), "admin@example.com"));

        assertEquals(new BigDecimal("20"), note.getBalance());
        verify(debitNoteRepository, never()).save(any());
        verifyNoInteractions(reconciliationRepository);
    }

    @Test
    public void updateDebitNote_rejectsNonPositivePayments() {
        for (BigDecimal amount : java.util.Arrays.asList(null, BigDecimal.ZERO, BigDecimal.ONE.negate())) {
            assertThrows(BadRequestRestApiException.class, () -> reconciliationService.updateDebitNote(
                    UUID.randomUUID(), amount, "admin@example.com"));
        }
        verifyNoInteractions(debitNoteRepository, reconciliationRepository);
    }

    @Test
    public void reconcile_releasesDeliveryAgentAfterCompletion() {
        Reconciliation reconciliation = reconciliationReadyToComplete();
        ReconciliationRequest request = new ReconciliationRequest();
        request.setPublicId(reconciliation.getPublicId());
        request.setAmount(BigDecimal.TEN);

        reconciliationService.reconcile(request, "admin@example.com");

        assertTrue(reconciliation.isReconciled());
        var ordered = inOrder(reconciliationRepository, logisticsHelper);
        ordered.verify(reconciliationRepository).save(reconciliation);
        ordered.verify(logisticsHelper).updateAgentAvailability(
                UUID.fromString(reconciliation.getDeliveryAgentPublicId()), true);
    }

    @Test
    public void reconcile_walkInDoesNotReleaseDeliveryAgent() {
        Reconciliation reconciliation = reconciliationReadyToComplete();
        reconciliation.setWalkIn(true);
        ReconciliationRequest request = new ReconciliationRequest();
        request.setPublicId(reconciliation.getPublicId());
        request.setAmount(BigDecimal.TEN);

        reconciliationService.reconcile(request, "admin@example.com");

        assertTrue(reconciliation.isReconciled());
        verifyNoInteractions(logisticsHelper);
    }

    @Test
    public void createDebitNote_releasesDeliveryAgentAfterReconciliation() {
        Reconciliation reconciliation = reconciliationReadyToComplete();
        reconciliation.setDepositAmount(BigDecimal.TEN);
        UUID agentId = UUID.fromString(reconciliation.getDeliveryAgentPublicId());
        DeliveryAgents agent = new DeliveryAgents();
        agent.setPublicId(agentId);
        agent.setFirstName("Asha");
        agent.setLastName("Mrema");
        when(personRepository.findByEmailAndDeletedIsFalse("admin@example.com"))
                .thenReturn(Optional.of(new Person()));
        when(deliveryAgentsRepository.findByPublicIdForUpdate(agentId))
                .thenReturn(Optional.of(agent));
        when(debitNoteRepository.save(any(DebitNotes.class))).thenAnswer(call -> call.getArgument(0));
        DebitNoteRequest request = new DebitNoteRequest();
        request.setReconciliationPublicId(reconciliation.getPublicId());
        request.setDeliveryAgentPublicId(agentId);
        request.setAmount(BigDecimal.TEN);

        DebitNotes note = reconciliationService.createDebitNote(request, "admin@example.com");

        assertEquals(reconciliation.getDeliveryId(), note.getRoute());
        assertEquals(reconciliation.getRoutePublicId(), note.getRoutePublicId());
        assertEquals("Asha Mrema", note.getDeliveryAgentName());
        assertEquals(agentId, note.getDeliveryAgentPublicId());
        assertEquals(agent.getDebtZone(), note.getDebtLevel());
        assertEquals("PENDING", note.getStatus());
        assertTrue(reconciliation.isReconciled());
        var ordered = inOrder(reconciliationRepository, logisticsHelper);
        ordered.verify(reconciliationRepository).save(reconciliation);
        ordered.verify(logisticsHelper).updateAgentAvailability(agentId, true);
        verify(debitNoteRepository).existsForDeliveryAgentAndRoute(agentId, DebitReferenceType.DeliveryAgent,
                reconciliation.getPublicId().toString(), reconciliation.getRoutePublicId());
    }

    @Test
    public void createDebitNote_rejectsExistingNoteForDeliveryAgentAndRoute() {
        UUID agentId = UUID.randomUUID();
        Reconciliation reconciliation = new Reconciliation();
        reconciliation.setPublicId(UUID.randomUUID());
        reconciliation.setRoutePublicId(UUID.randomUUID().toString());
        reconciliation.setDeliveryId("RT-001");
        reconciliation.setDeliveryAgentPublicId(agentId.toString());
        reconciliation.setDepositAmount(BigDecimal.TEN);
        // A paid debit note still prevents a second note if reconciliation is reopened.
        reconciliation.setReconciled(false);
        DeliveryAgents agent = new DeliveryAgents();
        agent.setPublicId(agentId);
        when(personRepository.findByEmailAndDeletedIsFalse("admin@example.com"))
                .thenReturn(Optional.of(new Person()));
        when(reconciliationRepository.findByPublicIdAndDeletedIsFalse(reconciliation.getPublicId()))
                .thenReturn(Optional.of(reconciliation));
        when(deliveryAgentsRepository.findByPublicIdForUpdate(agentId)).thenReturn(Optional.of(agent));
        when(debitNoteRepository.existsForDeliveryAgentAndRoute(agentId, DebitReferenceType.DeliveryAgent,
                reconciliation.getPublicId().toString(), reconciliation.getRoutePublicId())).thenReturn(true);
        DebitNoteRequest request = new DebitNoteRequest();
        request.setReconciliationPublicId(reconciliation.getPublicId());
        request.setDeliveryAgentPublicId(agentId);
        request.setAmount(BigDecimal.TEN);

        assertThrows(BadRequestRestApiException.class,
                () -> reconciliationService.createDebitNote(request, "admin@example.com"));

        verify(debitNoteRepository, never()).save(any());
        verify(deliveryAgentsRepository, never()).save(any());
        verify(reconciliationRepository, never()).save(any());
        verifyNoInteractions(logisticsHelper);
    }

    @Test
    public void getDebitNotes_populatesCurrentAgentDetailsWithOneBatchLookup() {
        DeliveryAgents agent = new DeliveryAgents();
        agent.setPublicId(UUID.randomUUID());
        agent.setFirstName("Asha");
        agent.setLastName("Mrema");
        agent.setDebtZone(DebtZone.YellowZone);
        DebitNotes pending = new DebitNotes();
        pending.setReferenceId(agent.getPublicId());
        pending.setReferenceType(DebitReferenceType.DeliveryAgent);
        DebitNotes reconciled = new DebitNotes();
        reconciled.setReferenceId(agent.getPublicId());
        reconciled.setReferenceType(DebitReferenceType.DeliveryAgent);
        reconciled.setReconciled(true);
        Pageable pageable = PageRequest.of(0, 20);
        when(debitNoteRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(pending, reconciled), pageable, 2));
        when(deliveryAgentsRepository.findByPublicIdInAndDeletedIsFalse(List.of(agent.getPublicId())))
                .thenReturn(List.of(agent));

        Page<DebitNotes> notes = reconciliationService.getDebitNotes(null, null, null, null,
                null, null, null, pageable);

        assertEquals(2, notes.getTotalElements());
        assertEquals("Asha Mrema", pending.getDeliveryAgentName());
        assertEquals(DebtZone.YellowZone, pending.getDebtLevel());
        assertEquals("PENDING", pending.getStatus());
        assertEquals("RECONCILED", reconciled.getStatus());
        verify(deliveryAgentsRepository).findByPublicIdInAndDeletedIsFalse(List.of(agent.getPublicId()));
        verifyNoMoreInteractions(deliveryAgentsRepository);
    }

    @Test
    public void getDebitNoteById_exposesNewPropertiesInDebitNotesJsonView() throws Exception {
        DeliveryAgents agent = new DeliveryAgents();
        agent.setPublicId(UUID.randomUUID());
        agent.setFirstName("Asha");
        agent.setLastName("Mrema");
        agent.setDebtZone(DebtZone.GreenZone);
        DebitNotes note = new DebitNotes();
        note.setPublicId(UUID.randomUUID());
        note.setReferenceId(agent.getPublicId());
        note.setReferenceType(DebitReferenceType.DeliveryAgent);
        note.setRoute("RT-001");
        note.setRoutePublicId(UUID.randomUUID().toString());
        when(debitNoteRepository.findByPublicIdAndDeletedIsFalse(note.getPublicId())).thenReturn(Optional.of(note));
        when(deliveryAgentsRepository.findByPublicIdAndDeletedIsFalse(agent.getPublicId()))
                .thenReturn(Optional.of(agent));

        DebitNotes result = reconciliationService.getDebitNoteById(note.getPublicId());
        ObjectMapper mapper = new ObjectMapper();
        var json = mapper.readTree(mapper.writerWithView(BaseView.DebitNotesView.class).writeValueAsString(result));

        assertEquals("Asha Mrema", json.get("deliveryAgentName").asText());
        assertEquals(agent.getPublicId().toString(), json.get("deliveryAgentPublicId").asText());
        assertEquals("GreenZone", json.get("debtLevel").asText());
        assertEquals("RT-001", json.get("route").asText());
        assertEquals(note.getRoutePublicId(), json.get("routePublicId").asText());
        assertEquals("PENDING", json.get("status").asText());
        note.setReconciled(true);
        assertEquals("RECONCILED", note.getStatus());
    }

    @Test
    public void getDebitNoteById_preservesNoteWhenAgentIsMissingOrDeleted() {
        DebitNotes note = new DebitNotes();
        note.setPublicId(UUID.randomUUID());
        note.setReferenceId(UUID.randomUUID());
        note.setReferenceType(DebitReferenceType.DeliveryAgent);
        when(debitNoteRepository.findByPublicIdAndDeletedIsFalse(note.getPublicId())).thenReturn(Optional.of(note));

        DebitNotes result = reconciliationService.getDebitNoteById(note.getPublicId());

        assertEquals(note.getReferenceId(), result.getDeliveryAgentPublicId());
        assertNull(result.getDeliveryAgentName());
        assertNull(result.getDebtLevel());
        assertEquals("PENDING", result.getStatus());
    }

    private Reconciliation reconciliationReadyToComplete() {
        Reconciliation reconciliation = new Reconciliation();
        reconciliation.setPublicId(UUID.randomUUID());
        reconciliation.setRoutePublicId(UUID.randomUUID().toString());
        reconciliation.setDeliveryId("RT-001");
        reconciliation.setDeliveryAgentPublicId(UUID.randomUUID().toString());
        UUID productReconId = UUID.randomUUID();
        reconciliation.setProductReconPublicId(productReconId.toString());
        ProductRecon productRecon = new ProductRecon();
        productRecon.setStatus(ReconStatus.Reconciled);
        when(reconciliationRepository.findByPublicIdAndDeletedIsFalse(reconciliation.getPublicId()))
                .thenReturn(Optional.of(reconciliation));
        when(productReconRepository.findByPublicIdAndDeletedIsFalse(productReconId))
                .thenReturn(Optional.of(productRecon));
        return reconciliation;
    }

    @Test
    public void getCashCreditReconAdjustments_groupsPendingAdjustmentsByReportLine() {
        Pageable pageable = PageRequest.of(1, 20);
        CashCreditReconReport report = new CashCreditReconReport();
        report.setReportDate(LocalDate.of(2026, 7, 21));
        report.setReportStatus(CashCreditReconReportStatus.RECONCILED);

        UUID reportLineId = UUID.randomUUID();
        CashCreditReconReportLine reportLine = new CashCreditReconReportLine();
        reportLine.setPublicId(reportLineId);
        reportLine.setReport(report);
        reportLine.setRoutePublicId("route-public-id");
        reportLine.setRouteId("RT-001");
        reportLine.setDeliveryAgentPublicId("agent-public-id");
        reportLine.setDeliveryAgentName("Asha Mrema");
        reportLine.setVehicleNumber("T 123 ABC");

        CashCreditReconAdjustment parkingFee = adjustment(CashCreditReconAdjustmentType.PARKING_FEE,
                new BigDecimal("10000"));
        CashCreditReconAdjustment ferryCost = adjustment(CashCreditReconAdjustmentType.FERRY_COST,
                new BigDecimal("5000"));

        when(cashCreditReconReportLineRepository.findLinesWithAdjustments(null,null,null,null,
                pageable))
                .thenReturn(new PageImpl<>(List.of(reportLine), pageable, 3));
        when(cashCreditReconAdjustmentRepository.findByReportLineAndDeletedIsFalse(reportLine))
                .thenReturn(List.of(parkingFee, ferryCost));

        Page<CashCreditReconPendingAdjustmentsDTO> response =
                reconciliationService.getCashCreditReconAdjustments(null,null,null,null,pageable);

        assertEquals(21, response.getTotalElements());
        assertEquals(1, response.getNumberOfElements());
        CashCreditReconPendingAdjustmentsDTO group = response.getContent().get(0);
        assertEquals(reportLineId, group.getReportLinePublicId());
        assertEquals("route-public-id", group.getRoutePublicId());
        assertEquals("RT-001", group.getRouteId());
        assertEquals("agent-public-id", group.getDeliveryAgentPublicId());
        assertEquals("Asha Mrema", group.getDeliveryAgentName());
        assertEquals(LocalDate.of(2026, 7, 21), group.getReportDate());
        assertEquals("T 123 ABC", group.getVehicleNumber());
        assertTrue(group.isReconciled());
        assertEquals(2, group.getPendingAdjustments().size());
        assertEquals(parkingFee.getPublicId(), group.getPendingAdjustments().get(0).getPublicId());
        assertEquals(ferryCost.getPublicId(), group.getPendingAdjustments().get(1).getPublicId());
        assertEquals(CashCreditReconAdjustmentStatus.PENDING,
                group.getPendingAdjustments().get(0).getApprovalStatus());

        verify(cashCreditReconReportLineRepository).findLinesWithAdjustments(null,null,null,null,
                pageable);
        verify(cashCreditReconAdjustmentRepository).findByReportLineAndDeletedIsFalse(reportLine);
    }

    @Test
    public void getCashCreditReconAdjustments_returnsAnEmptyPageWhenNoLinesHavePendingAdjustments() {
        Pageable pageable = PageRequest.of(0, 20);
        when(cashCreditReconReportLineRepository.findLinesWithAdjustments(null,null,null,null,
                pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<CashCreditReconPendingAdjustmentsDTO> response =
                reconciliationService.getCashCreditReconAdjustments(null,null,null,null,pageable);

        assertEquals(0, response.getTotalElements());
        assertEquals(0, response.getNumberOfElements());
        verifyNoInteractions(cashCreditReconAdjustmentRepository);
    }

    @Test
    public void addCashCreditReconAdjustment_usesExistingLineWithoutGeneratingReport() {
        RoutePlanning route = adjustmentRoute();
        CashCreditReconReportLine line = new CashCreditReconReportLine();
        line.setReport(new CashCreditReconReport());
        line.getReport().setPublicId(UUID.randomUUID());
        line.getReport().setReportStatus(CashCreditReconReportStatus.GENERATED);
        when(cashCreditReconReportLineRepository.findByDeliveryAgentPublicIdAndRoutePublicIdAndDeletedIsFalse(
                route.getDeliveryAgentId(), route.getPublicId().toString())).thenReturn(Optional.of(line));
        when(cashCreditReconAdjustmentRepository.save(any())).thenAnswer(call -> {
            CashCreditReconAdjustment saved = call.getArgument(0);
            saved.setPublicId(UUID.randomUUID());
            return saved;
        });

        String publicId = reconciliationService.addCashCreditReconAdjustment(adjustmentRequest(route), "admin@example.com");

        assertEquals(line.getAdjustments().get(0).getPublicId().toString(), publicId);
        assertEquals(CashCreditReconAdjustmentStatus.PENDING, line.getAdjustments().get(0).getApprovalStatus());
        verifyNoInteractions(cashCreditReconReportRepository);
        verify(cashCreditReconReportLineRepository, times(1))
                .findByDeliveryAgentPublicIdAndRoutePublicIdAndDeletedIsFalse(
                        route.getDeliveryAgentId(), route.getPublicId().toString());
    }

    @Test
    public void addCashCreditReconAdjustment_generatesMissingReportForRouteCreationDate() {
        RoutePlanning route = adjustmentRoute();
        List<CashCreditReconReportLine> generatedLines = stubReportGeneration(route, List.of(route));
        when(cashCreditReconReportLineRepository.findByDeliveryAgentPublicIdAndRoutePublicIdAndDeletedIsFalse(
                route.getDeliveryAgentId(), route.getPublicId().toString()))
                .thenAnswer(call -> generatedLines.stream().findFirst());
        when(cashCreditReconAdjustmentRepository.save(any())).thenAnswer(call -> {
            CashCreditReconAdjustment saved = call.getArgument(0);
            saved.setPublicId(UUID.randomUUID());
            return saved;
        });

        String publicId = reconciliationService.addCashCreditReconAdjustment(adjustmentRequest(route), "admin@example.com");

        CashCreditReconReportLine line = generatedLines.get(0);
        assertEquals(route.getCreationTime().toLocalDate(), line.getReport().getReportDate());
        assertEquals(route.getPublicId().toString(), line.getRoutePublicId());
        assertEquals(line.getAdjustments().get(0).getPublicId().toString(), publicId);
        verify(routePlanningRepository).findByCreationTimeBetweenAndDeletedIsFalse(
                route.getCreationTime().toLocalDate().atStartOfDay(),
                route.getCreationTime().toLocalDate().atTime(LocalTime.MAX));
    }

    @Test
    public void addCashCreditReconAdjustment_generatesMissingReportWhenActorIsNotAPerson() {
        RoutePlanning route = adjustmentRoute();
        List<CashCreditReconReportLine> generatedLines = stubReportGeneration(route, List.of(route));
        when(personRepository.findByEmailAndDeletedIsFalse("+255700000000")).thenReturn(Optional.empty());
        when(cashCreditReconReportLineRepository.findByDeliveryAgentPublicIdAndRoutePublicIdAndDeletedIsFalse(
                route.getDeliveryAgentId(), route.getPublicId().toString()))
                .thenAnswer(call -> generatedLines.stream().findFirst());
        when(cashCreditReconAdjustmentRepository.save(any())).thenAnswer(call -> {
            CashCreditReconAdjustment saved = call.getArgument(0);
            saved.setPublicId(UUID.randomUUID());
            return saved;
        });

        String publicId = reconciliationService.addCashCreditReconAdjustment(adjustmentRequest(route), "+255700000000");

        CashCreditReconAdjustment saved = generatedLines.get(0).getAdjustments().get(0);
        assertEquals(saved.getPublicId().toString(), publicId);
        assertNull(saved.getCreatedBy());
        verify(cashCreditReconReportRepository, atLeastOnce()).save(any());
    }

    @Test
    public void addCashCreditReconAdjustment_rejectsMissingLineAfterGeneration() {
        RoutePlanning route = adjustmentRoute();
        stubReportGeneration(route, List.of());

        assertThrows(BadRequestRestApiException.class, () -> reconciliationService
                .addCashCreditReconAdjustment(adjustmentRequest(route), "admin@example.com"));

        verify(cashCreditReconAdjustmentRepository, never()).save(any());
    }

    @Test
    public void addCashCreditReconAdjustment_rejectsReconciledReport() {
        RoutePlanning route = adjustmentRoute();
        CashCreditReconReportLine line = new CashCreditReconReportLine();
        line.setReport(new CashCreditReconReport());
        line.getReport().setReportStatus(CashCreditReconReportStatus.RECONCILED);
        when(cashCreditReconReportLineRepository.findByDeliveryAgentPublicIdAndRoutePublicIdAndDeletedIsFalse(
                route.getDeliveryAgentId(), route.getPublicId().toString())).thenReturn(Optional.of(line));

        assertThrows(BadRequestRestApiException.class, () -> reconciliationService
                .addCashCreditReconAdjustment(adjustmentRequest(route), "admin@example.com"));

        verifyNoInteractions(cashCreditReconAdjustmentRepository, cashCreditReconReportRepository);
    }

    @Test
    public void addCashCreditReconAdjustment_rejectsDeletedRoute() {
        RoutePlanning route = adjustmentRoute();
        route.setDeleted(true);

        assertThrows(BadRequestRestApiException.class, () -> reconciliationService
                .addCashCreditReconAdjustment(adjustmentRequest(route), "admin@example.com"));

        verifyNoInteractions(cashCreditReconReportLineRepository, cashCreditReconReportRepository,
                cashCreditReconAdjustmentRepository);
    }

    private RoutePlanning adjustmentRoute() {
        RoutePlanning route = new RoutePlanning();
        route.setPublicId(UUID.randomUUID());
        route.setDeliveryAgentId(UUID.randomUUID().toString());
        route.setCreationTime(LocalDate.of(2026, 7, 21).atTime(23, 59, 59, 500_000_000));
        when(routePlanningRepository.findByPublicId(route.getPublicId())).thenReturn(route);
        return route;
    }

    private CashCreditReconAdjustmentRequest adjustmentRequest(RoutePlanning route) {
        CashCreditReconAdjustmentRequest request = new CashCreditReconAdjustmentRequest();
        request.setRoutePublicId(route.getPublicId());
        request.setType(CashCreditReconAdjustmentType.PARKING_FEE);
        request.setAmount(BigDecimal.TEN);
        return request;
    }

    private List<CashCreditReconReportLine> stubReportGeneration(RoutePlanning route, List<RoutePlanning> routes) {
        List<CashCreditReconReportLine> lines = new ArrayList<>();
        when(personRepository.findByEmailAndDeletedIsFalse("admin@example.com"))
                .thenReturn(Optional.of(new Person()));
        when(cashCreditReconReportRepository.save(any())).thenAnswer(call -> {
            CashCreditReconReport report = call.getArgument(0);
            report.setId(1L);
            if (report.getPublicId() == null) {
                report.setPublicId(UUID.randomUUID());
            }
            return report;
        });
        when(routePlanningRepository.findByCreationTimeBetweenAndDeletedIsFalse(
                route.getCreationTime().toLocalDate().atStartOfDay(),
                route.getCreationTime().toLocalDate().atTime(LocalTime.MAX))).thenReturn(routes);
        when(cashCreditReconReportLineRepository.findByReportAndDeletedIsFalse(any())).thenReturn(lines);
        if (!routes.isEmpty()) {
            when(cashCreditReconReportLineRepository.save(any())).thenAnswer(call -> {
                CashCreditReconReportLine line = call.getArgument(0);
                if (line.getId() == null) {
                    line.setId(1L);
                    line.setPublicId(UUID.randomUUID());
                    lines.add(line);
                }
                return line;
            });
        }
        return lines;
    }

    private CashCreditReconAdjustment adjustment(CashCreditReconAdjustmentType type, BigDecimal amount) {
        CashCreditReconAdjustment adjustment = new CashCreditReconAdjustment();
        adjustment.setPublicId(UUID.randomUUID());
        adjustment.setAdjustmentType(type);
        adjustment.setAmount(amount);
        adjustment.setApprovalStatus(CashCreditReconAdjustmentStatus.PENDING);
        return adjustment;
    }
}
