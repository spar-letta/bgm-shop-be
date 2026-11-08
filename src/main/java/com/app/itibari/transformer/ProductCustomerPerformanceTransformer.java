package com.app.itibari.transformer;

import com.app.itibari.models.entities.Customers;
import com.app.itibari.models.entities.SalesOrderItems;
import com.app.itibari.models.report.ProductCustomerPerformanceDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ProductCustomerPerformanceTransformer {

    public List<ProductCustomerPerformanceDTO> convert(List<SalesOrderItems> salesOrderItems) {

        if (salesOrderItems == null || salesOrderItems.isEmpty()) {
            return List.of();
        }

        return salesOrderItems.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getSalesOrder().getCustomer(),  // Key = Customer object
                        Collectors.teeing(
                                // Count distinct orders
                                Collectors.mapping(SalesOrderItems::getSalesOrder, Collectors.toSet()),
                                // Sum total revenue
                                Collectors.reducing(
                                        BigDecimal.ZERO,
                                        item -> item.getPricePerUnit().multiply(BigDecimal.valueOf(item.getQuantity())),
                                        BigDecimal::add
                                ),
                                (orderSet, totalRevenue) -> {
                                    long orderCount = orderSet.size();
                                    BigDecimal aov = orderCount > 0
                                            ? totalRevenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
                                            : BigDecimal.ZERO;

                                    // This is the key: use the grouping key directly!
                                    Customers customer = orderSet.iterator().next().getCustomer();

                                    ProductCustomerPerformanceDTO dto = new ProductCustomerPerformanceDTO();
                                    dto.setCustomerName(customer != null ? customer.getName() : "Unknown Customer");
                                    dto.setNumberOfOrders(orderCount);
                                    dto.setAov(aov);
                                    return dto;
                                }
                        )
                ))
                .values().stream()
                .sorted(Comparator.comparing(ProductCustomerPerformanceDTO::getAov).reversed())
                .toList();
    }
}
