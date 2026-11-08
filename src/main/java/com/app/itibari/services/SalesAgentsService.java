package com.app.itibari.services;

import com.app.itibari.dataTypes.StatementExportType;
import com.app.itibari.models.entities.DiscountApproval;
import com.app.itibari.models.entities.SalesAgents;
import com.app.itibari.models.request.AgentAction;
import com.app.itibari.models.request.SalesAgentsRequest;
import com.app.itibari.models.response.SalesAgentPerformance;
import com.itextpdf.io.source.ByteArrayOutputStream;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SalesAgentsService {

    SalesAgents createSalesAgent(@Valid SalesAgentsRequest salesAgentsRequest, String name);

    void updateSalesAgent(UUID salesAgentPublicId, @Valid SalesAgentsRequest salesAgentsRequest, String name);

    SalesAgents getSalesAgent(UUID salesAgentPublicId);

    Page<SalesAgents> getAllSalesAgents(String searchParam, UUID organisationRegionPublicId,
                                        UUID organisationLocationPublicId, Pageable pageable);

    void deleteSalesAgent(UUID salesAgentPublicId, String name);

    SalesAgents getSalesAgentProfile(String name);

    SalesAgentPerformance getSalesAgentPerformance(UUID salesAgentPublicId, String from, String to, Pageable pageable);

    void deactivateSalesAgent(UUID salesAgentPublicId, String name);

    void activateSalesAgent(UUID salesAgentPublicId, String name);

    Page<DiscountApproval> fetchSpecialRequests(LocalDate startDate, LocalDate endDate, String name, Pageable pageable);

    void actionSalesAgent(UUID salesAgentPublicId, @Valid AgentAction agentAction, String name);

    ByteArrayOutputStream generateSalesAgentCustomerReportExport(UUID organisationLocationPublicId, UUID organisationRegionPublicId, String searchParam, StatementExportType statementExportType, String name, Pageable pageable);

    List<SalesAgents> getAllSalesAgentsMiniData(String searchParam);
}
