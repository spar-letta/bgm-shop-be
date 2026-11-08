package com.app.itibari.services;

import com.app.itibari.models.report.DistrictPerformanceV2PageDTO;
import com.app.itibari.models.report.DistrictPerformanceV2RowDTO;
import com.app.itibari.models.report.ProductDistributionV2RowDTO;
import com.app.itibari.models.report.SalesCalendarV2DTO;
import com.app.itibari.models.report.SalesTrendV2DTO;
import com.app.itibari.models.report.SummaryCardsV2DTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SupplierPortalDashboardService {

    SummaryCardsV2DTO getDashboardSummaryMetrics(List<UUID> regionIds,
                                                 List<UUID> locationIds,
                                                 LocalDate startDate,
                                                 LocalDate endDate,
                                                 Principal principal);

    SalesTrendV2DTO getSalesTrend(List<UUID> regionIds,
                                  List<UUID> locationIds,
                                  Integer month,
                                  Integer year,
                                  Boolean monthly,
                                  Boolean quarterly,
                                  Boolean yearly,
                                  Principal principal,
                                  Pageable pageable);

    Page<ProductDistributionV2RowDTO> getProductDistributionV2(List<UUID> regionIds,
                                                                List<UUID> locationIds,
                                                                LocalDate startDate,
                                                                LocalDate endDate,
                                                                Principal principal,
                                                                Pageable pageable);

    DistrictPerformanceV2PageDTO getDistrictPerformanceV2(List<UUID> regionIds,
                                                               List<UUID> locationIds,
                                                               LocalDate startDate,
                                                               LocalDate endDate,
                                                               Principal principal,
                                                               Pageable pageable);

    SalesCalendarV2DTO getSalesCalendar(List<UUID> regionIds,
                                          List<UUID> locationIds,
                                          int month,
                                          int year,
                                          Principal principal);
}
