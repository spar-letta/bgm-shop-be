package com.app.itibari.transformer;

import com.app.itibari.models.entities.OrganizationLocations;
import com.app.itibari.models.entities.SalesOrderItems;
import com.app.itibari.models.report.ProductPerformancePerZone;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ProductPerformancePerZoneTransformer {
    public List<ProductPerformancePerZone> convert(List<SalesOrderItems> salesOrderItems) {

        return salesOrderItems.stream()
                .collect(Collectors.groupingBy(
                        item -> {
                            OrganizationLocations zone = item.getSalesOrder().getCustomer().getOrganizationLocation();
                            return zone != null ? zone.getName() : "Unknown Zone";
                        },
                        Collectors.teeing(
                                Collectors.summingLong(SalesOrderItems::getQuantity),
                                Collectors.reducing(BigDecimal.ZERO,
                                        item -> item.getPricePerUnit().multiply(BigDecimal.valueOf(item.getQuantity())),
                                        BigDecimal::add
                                ),
                                (units, revenue) -> {
                                    ProductPerformancePerZone dto = new ProductPerformancePerZone();
                                    dto.setUnitsSold(units);
                                    dto.setOrderValueInThousands(revenue.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP));
                                    return dto;
                                }
                        )
                ))
                .entrySet().stream()
                .peek(entry -> entry.getValue().setZoneName(entry.getKey()))
                .map(Map.Entry::getValue)
                .sorted(Comparator.comparing(ProductPerformancePerZone::getOrderValueInThousands).reversed())
                .toList();
    }
}
