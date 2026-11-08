package com.app.itibari.utils.helpers;

import com.app.itibari.dataTypes.LoadingStatusEnum;
import com.app.itibari.dataTypes.StatementExportType;
import com.app.itibari.models.entities.Category;
import com.app.itibari.models.entities.SalesAgents;
import com.app.itibari.models.entities.SalesOrderItems;
import com.app.itibari.models.entities.SalesOrders;
import com.app.itibari.models.response.SAUpdateReport;
import com.app.itibari.models.response.SalesAgentOrderPageable;
import com.app.itibari.models.response.SalesAgentReportPegable;
import com.app.itibari.repository.CategoryRepository;
import com.app.itibari.repository.SalesAgentsRepository;
import com.app.itibari.repository.SalesOrdersRepository;
import com.itextpdf.io.source.ByteArrayOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SalesAgentReport {

    private final SalesOrdersRepository salesOrdersRepository;

    private final CategoryRepository categoryRepository;

    private final SalesAgentsRepository salesAgentsRepository;

    public SalesAgentReport(SalesOrdersRepository salesOrdersRepository,
                            CategoryRepository categoryRepository,
                            SalesAgentsRepository salesAgentsRepository) {

        this.salesOrdersRepository = salesOrdersRepository;
        this.categoryRepository = categoryRepository;
        this.salesAgentsRepository = salesAgentsRepository;
    }

    public SalesAgentReportPegable getSalesReport(LocalDate startDate,
                                                  LocalDate endDate,
                                                  LocalDate date,
                                                  UUID salesAgentPublicId,
                                                  String name,
                                                  Pageable pageable) {

        SalesAgentReportPegable salesAgentReportPegable = new SalesAgentReportPegable();

        List<SAUpdateReport> saUpdateReportList = new ArrayList<>();

        // get orders on the date range

        if (date == null) {
            if (endDate != null) {
                date = endDate;
            } else {
                date = LocalDate.now();
            }
        }

        if (endDate == null) {
            endDate = LocalDate.now();
        }

        List<SalesOrders> orders = salesOrdersRepository.findAllByCreationTimeBetweenAndSalesOrderStatusAndDeletedIsFalse(endDate.atStartOfDay(),
                                                                                                                          endDate
                                                                                                                                  .atStartOfDay()
                                                                                                                                  .plusHours(
                                                                                                                                          24),
                                                                                                                          LoadingStatusEnum.DELIVERED);

        if (salesAgentPublicId != null) {
            orders = orders.stream()
                    .filter(Objects::nonNull)
                    .filter(order -> order.getSalesOrderNumber() != null && order
                            .getSalesAgentPublicId()
                            .equals(salesAgentPublicId.toString()))
                    .toList();
        }

        orders = orders.stream()
                .filter(salesOrders -> salesOrders.getSalesAgentPublicId() != null)
                .toList();

        Map<String, List<SalesOrders>> ordersMap = orders.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(SalesOrders::getSalesAgentPublicId));

        ordersMap.forEach((key, value) -> {

            SAUpdateReport report = new SAUpdateReport();
            SalesAgents salesAgents = salesAgentsRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(key))
                    .orElse(null);

            List<SalesOrderItems> items = new ArrayList<>();

            value.forEach(salesOrders -> {
                items.addAll(salesOrders.getSalesOrderItems());
            });

            BigDecimal totalGMV = BigDecimal.ZERO;

            // group by categories

            List<Category> categories = categoryRepository.findAllByDeletedIsFalse();

            Map<String, List<SalesOrderItems>> itemsGroup = items
                    .stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(item -> item.getProduct().getCategory().getName()));

            itemsGroup.forEach((key1, value1) -> {
                BigDecimal palmOil = BigDecimal.ZERO;
                BigDecimal sunflowerOil = BigDecimal.ZERO;
                BigDecimal sugar = BigDecimal.ZERO;
                BigDecimal detergents = BigDecimal.ZERO;
                BigDecimal pasta = BigDecimal.ZERO;
                BigDecimal beverages = BigDecimal.ZERO;
                BigDecimal babyCare = BigDecimal.ZERO;
                BigDecimal hygiene = BigDecimal.ZERO;
                BigDecimal soap = BigDecimal.ZERO;
                BigDecimal vegetable = BigDecimal.ZERO;
                BigDecimal food = BigDecimal.ZERO;
                BigDecimal other = BigDecimal.ZERO;
                if (key1.contains("Palm")) {
                    palmOil = getPalmAmounts(value1);
                } else if (key1.contains("Sunflower")) {
                    sunflowerOil = getPalmAmounts(value1);
                } else if (key1.contains("Sugar")) {
                    sugar = getPalmAmounts(value1);
                } else if (key1.contains("Detergent")) {
                    detergents = getPalmAmounts(value1);
                } else if (key1.contains("Pasta")) {
                    pasta = getPalmAmounts(value1);
                } else if (key1.contains("Beverage")) {
                    beverages = getPalmAmounts(value1);
                } else if (key1.contains("Baby Care")) {
                    babyCare = getPalmAmounts(value1);
                } else if (key1.contains("Hygiene")) {
                    hygiene = getPalmAmounts(value1);
                } else if (key1.contains("Soap")) {
                    soap = getPalmAmounts(value1);
                } else if (key1.contains("Vegetable")) {
                    vegetable = getPalmAmounts(value1);
                } else if (key1.contains("Food")) {
                    food = getPalmAmounts(value1);
                } else if (!key1.contains("hygiene") && !key1.contains("soap") &&
                        !key1.contains("vegetable") && !key1.contains("food") && !key1.contains("pasta")
                        && !key1.contains("baby care") && !key1.contains("detergent") && !key1.contains("palm") && !key1.contains(
                        "sunflower")) {
                    other = getPalmAmounts(value1);
                }

                report.setSalesAgent(salesAgents != null ? salesAgents.getName() : "");
                report.setDate(startDate);
                report.setZone(null);
                report.setPalmOil(palmOil);
                report.setBeverages(beverages);
                report.setSunflowerOil(sunflowerOil);
                report.setDetergents(detergents);
                report.setPasta(pasta);
                report.setHygiene(hygiene);
                report.setBabyCare(babyCare);
                report.setOther(other);
                report.setSugar(sugar);
                report.setSoap(soap);
                report.setVegetable(vegetable);
                report.setFood(food);
            });

            totalGMV = getPalmAmounts(items);

            report.setTotalGMV(totalGMV);

            saUpdateReportList.add(report);
        });

        if (!saUpdateReportList.isEmpty()) {
            PageImpl<SAUpdateReport> saUpdateReportPage = new PageImpl<>(saUpdateReportList, pageable, saUpdateReportList.size());
            salesAgentReportPegable.setContent(saUpdateReportPage.getContent());
            salesAgentReportPegable.setTotalElements(saUpdateReportPage.getTotalElements());
            salesAgentReportPegable.setPageSize(saUpdateReportPage.getSize());
            salesAgentReportPegable.setLast(saUpdateReportPage.isLast());
            salesAgentReportPegable.setFirst(saUpdateReportPage.isFirst());
            salesAgentReportPegable.setPageNumber(saUpdateReportPage.getNumber());
            salesAgentReportPegable.setTotalPages(saUpdateReportPage.getTotalPages());
        }

        return salesAgentReportPegable;
    }

    private BigDecimal getPalmAmounts(List<SalesOrderItems> value1) {

        BigDecimal total = BigDecimal.ZERO;
        for (SalesOrderItems salesOrderItem : value1) {
            int quantity = salesOrderItem.getQuantityDelivered();
            BigDecimal price = salesOrderItem.getPricePerUnit();
            total = price.multiply(new BigDecimal(quantity)).setScale(4, BigDecimal.ROUND_HALF_UP);
        }
        return total;
    }

    public ByteArrayOutputStream getSalesReportExport(LocalDate startDate,
                                                      LocalDate endDate,
                                                      LocalDate date,
                                                      UUID salesAgentPublicId,
                                                      StatementExportType statementExportType,
                                                      String name) {

        return null;
    }
}
