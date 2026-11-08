package com.app.itibari.transformer;

import com.app.itibari.models.entities.SalesOrderItems;
import com.app.itibari.models.report.ProductPerformanceDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProductPerformanceTransformer {

    record ProductMonthKey(String product, int year, Month month) {}

    record Stats(long unitsSold, BigDecimal revenue) {
        Stats add(Stats other) {
            return new Stats(this.unitsSold + other.unitsSold, this.revenue.add(other.revenue));
        }
    }

    public List<ProductPerformanceDTO> convert(List<SalesOrderItems> salesOrderItems) {
        return salesOrderItems.stream()
                .collect(Collectors.groupingBy(
                        item -> {
                            LocalDate date = item.getSalesOrder().getCreationTime().toLocalDate();
                            String product =  item.getProduct().getProductName();
                            return new ProductMonthKey(product, date.getYear(), date.getMonth());
                        },
                        Collectors.reducing(
                                new Stats(0L, BigDecimal.ZERO),
                                item -> new Stats(
                                        item.getQuantity(),
                                        item.getPricePerUnit().multiply(BigDecimal.valueOf(item.getQuantity()))
                                ),
                                Stats::add
                        )
                ))
                .entrySet().stream()
                .map(entry -> {
                    ProductMonthKey key = entry.getKey();
                    Stats stats = entry.getValue();

                    ProductPerformanceDTO dto = new ProductPerformanceDTO();
                    dto.setProductId(null);
                    dto.setUnitsSold(stats.unitsSold);
                    dto.setOrderValueInThousands(stats.revenue.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP));
                    dto.setYear(key.year);
                    dto.setMonth(key.month);
                    dto.setProductName(key.product);
                    return dto;
                })
                .sorted(Comparator
                        .comparing(ProductPerformanceDTO::getYear)
                        .thenComparing(ProductPerformanceDTO::getMonth)
                        .thenComparing(ProductPerformanceDTO::getProductId))
                .toList();
    }
}
