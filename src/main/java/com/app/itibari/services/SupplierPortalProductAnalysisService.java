package com.app.itibari.services;

import com.app.itibari.models.report.ProductAnalysisDistrictCogsV2DTO;
import com.app.itibari.models.report.ProductAnalysisDistrictRankingV2DTO;
import com.app.itibari.models.report.ProductAnalysisProductRankingCogsRowV2DTO;
import com.app.itibari.models.report.ProductAnalysisSalesCalendarV2DTO;
import com.app.itibari.models.report.ProductAnalysisSalesTrendV2DTO;
import com.app.itibari.models.report.ProductAnalysisSummaryMetricsV2DTO;
import com.app.itibari.models.report.ProductAnalysisWardPerformanceRowDTO;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SupplierPortalProductAnalysisService {

    ProductAnalysisSummaryMetricsV2DTO getSummaryMetrics(UUID productId,
                                                         List<UUID> regionIds,
                                                         List<UUID> locationIds,
                                                         LocalDate startDate,
                                                         LocalDate endDate,
                                                         Principal principal);

    ProductAnalysisSalesTrendV2DTO getSalesTrend(UUID productId,
                                                List<UUID> regionIds,
                                                List<UUID> locationIds,
                                                Integer month,
                                                Integer year,
                                                Principal principal);

    ProductAnalysisDistrictRankingV2DTO getDistrictRanking(UUID productId,
                                                           List<UUID> regionIds,
                                                           List<UUID> locationIds,
                                                           LocalDate startDate,
                                                           LocalDate endDate,
                                                           Integer limit,
                                                           Principal principal);

    ProductAnalysisDistrictCogsV2DTO getDistrictCogs(UUID productId,
                                                     List<UUID> regionIds,
                                                     List<UUID> locationIds,
                                                     LocalDate startDate,
                                                     LocalDate endDate,
                                                     Integer limit,
                                                     Principal principal);

    Page<ProductAnalysisWardPerformanceRowDTO> getWardPerformance(UUID productId,
                                                                  List<UUID> regionIds,
                                                                  List<UUID> locationIds,
                                                                  LocalDate startDate,
                                                                  LocalDate endDate,
                                                                  Principal principal,
                                                                  Pageable pageable);

    ProductAnalysisSalesCalendarV2DTO getSalesCalendar(UUID productId,
                                                       List<UUID> regionIds,
                                                       List<UUID> locationIds,
                                                       Integer month,
                                                       Integer year,
                                                       Principal principal);

    ProductAnalysisSummaryMetricsV2DTO getAllProductsSummaryMetrics(List<UUID> regionIds,
                                                                    List<UUID> locationIds,
                                                                    LocalDate startDate,
                                                                    LocalDate endDate,
                                                                    Principal principal);

    ProductAnalysisSalesTrendV2DTO getAllProductsSalesTrend(List<UUID> regionIds,
                                                            List<UUID> locationIds,
                                                            Integer month,
                                                            Integer year,
                                                            Principal principal);

    ProductAnalysisDistrictRankingV2DTO getAllProductsDistrictRanking(List<UUID> regionIds,
                                                                      List<UUID> locationIds,
                                                                      LocalDate startDate,
                                                                      LocalDate endDate,
                                                                      Integer limit,
                                                                      Principal principal);

    ProductAnalysisDistrictCogsV2DTO getAllProductsDistrictCogs(List<UUID> regionIds,
                                                               List<UUID> locationIds,
                                                               LocalDate startDate,
                                                               LocalDate endDate,
                                                               Integer limit,
                                                               Principal principal);

    Page<ProductAnalysisProductRankingCogsRowV2DTO> getAllProductsProductRankingCogs(List<UUID> regionIds,
                                                                                        List<UUID> locationIds,
                                                                                        LocalDate startDate,
                                                                                        LocalDate endDate,
                                                                                        Principal principal,
                                                                                        Pageable pageable);

    Page<ProductAnalysisWardPerformanceRowDTO> getAllProductsWardPerformance(List<UUID> regionIds,
                                                                             List<UUID> locationIds,
                                                                             LocalDate startDate,
                                                                             LocalDate endDate,
                                                                             Principal principal,
                                                                             Pageable pageable);

    /** Export-safe variant: accepts page sizes up to 2000; bypasses the UI page-size cap. */
    Page<ProductAnalysisWardPerformanceRowDTO> getWardPerformanceForExport(UUID productId,
                                                                           List<UUID> regionIds,
                                                                           List<UUID> locationIds,
                                                                           LocalDate startDate,
                                                                           LocalDate endDate,
                                                                           Principal principal,
                                                                           Pageable pageable);

    /** Export-safe variant: accepts page sizes up to 2000; bypasses the UI page-size cap. */
    Page<ProductAnalysisWardPerformanceRowDTO> getAllProductsWardPerformanceForExport(List<UUID> regionIds,
                                                                                     List<UUID> locationIds,
                                                                                     LocalDate startDate,
                                                                                     LocalDate endDate,
                                                                                     Principal principal,
                                                                                     Pageable pageable);

    /** Export-safe variant: accepts page sizes up to 2000; bypasses the UI page-size cap. */
    Page<ProductAnalysisProductRankingCogsRowV2DTO> getAllProductsProductRankingCogsForExport(List<UUID> regionIds,
                                                                                              List<UUID> locationIds,
                                                                                              LocalDate startDate,
                                                                                              LocalDate endDate,
                                                                                              Principal principal,
                                                                                              Pageable pageable);

    ProductAnalysisSalesCalendarV2DTO getAllProductsSalesCalendar(List<UUID> regionIds,
                                                                  List<UUID> locationIds,
                                                                  Integer month,
                                                                  Integer year,
                                                                  Principal principal);
}
