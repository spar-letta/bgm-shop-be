package com.app.itibari.services;

import com.app.itibari.models.report.CustomerAnalysisActivityCalendarV2DTO;
import com.app.itibari.models.report.CustomerAnalysisCustomerRowV2DTO;
import com.app.itibari.models.report.CustomerAnalysisDistrictCogsV2DTO;
import com.app.itibari.models.report.CustomerAnalysisDistrictRankingV2DTO;
import com.app.itibari.models.report.CustomerAnalysisOrdersTrendV2DTO;
import com.app.itibari.models.report.CustomerAnalysisSummaryMetricsV2DTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SupplierPortalCustomerAnalysisService {

    CustomerAnalysisSummaryMetricsV2DTO getSummaryMetrics(List<UUID> regionIds,
                                                          List<UUID> locationIds,
                                                          List<UUID> productIds,
                                                          LocalDate startDate,
                                                          LocalDate endDate,
                                                          Principal principal);

    Page<CustomerAnalysisCustomerRowV2DTO> getCustomers(List<UUID> regionIds,
                                                        List<UUID> locationIds,
                                                        List<UUID> productIds,
                                                        LocalDate startDate,
                                                        LocalDate endDate,
                                                        String search,
                                                        Principal principal,
                                                        Pageable pageable);

    /**
     * CUSTOMERS export rows: one row per product in each delivered order.
     * Uncapped for the active filter window — no row LIMIT. UI table uses {@link #getCustomers}.
     */
    Page<CustomerAnalysisCustomerRowV2DTO> getCustomersForExport(UUID regionId,
                                                                 UUID locationId,
                                                                 List<UUID> productIds,
                                                                 LocalDate startDate,
                                                                 LocalDate endDate,
                                                                 String search,
                                                                 Principal principal,
                                                                 Pageable pageable);

    CustomerAnalysisDistrictRankingV2DTO getDistrictRanking(List<UUID> regionIds,
                                                            List<UUID> locationIds,
                                                            List<UUID> productIds,
                                                            LocalDate startDate,
                                                            LocalDate endDate,
                                                            Integer limit,
                                                            Principal principal);

    CustomerAnalysisDistrictCogsV2DTO getDistrictCogs(List<UUID> regionIds,
                                                     List<UUID> locationIds,
                                                     List<UUID> productIds,
                                                     LocalDate startDate,
                                                     LocalDate endDate,
                                                     Integer limit,
                                                     Principal principal);

    CustomerAnalysisOrdersTrendV2DTO getOrdersTrend(List<UUID> regionIds,
                                                    List<UUID> locationIds,
                                                    List<UUID> productIds,
                                                    Integer month,
                                                    Integer year,
                                                    Principal principal);

    CustomerAnalysisActivityCalendarV2DTO getActivityCalendar(List<UUID> regionIds,
                                                                List<UUID> locationIds,
                                                                List<UUID> productIds,
                                                                Integer month,
                                                                Integer year,
                                                                Principal principal);
}
