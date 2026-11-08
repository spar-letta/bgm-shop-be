package com.app.itibari.transformer;


/**
 *  author Christopher Oigo
 */

import com.app.itibari.models.entities.SalesOrderItems;
import com.app.itibari.models.report.SalesRevenueDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SalesRevenueTransformer {
    public List<SalesRevenueDTO> convert(List<SalesOrderItems> salesOrderItems,
                                         Boolean monthly, Boolean quarterly, Boolean yearly) {

        if (Boolean.TRUE.equals(monthly)) {
            return salesOrderItems.stream()
                    .collect(Collectors.groupingBy(
                            item -> {
                                LocalDateTime date = item.getSalesOrder().getCreationTime();
                                return YearMonth.of(date.getYear(), date.getMonth());
                            },
                            Collectors.mapping(
                                    item -> item.getPricePerUnit().multiply(BigDecimal.valueOf(item.getQuantity())),
                                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                            )
                    ))
                    .entrySet().stream()
                    .map(entry -> {
                        YearMonth ym = entry.getKey();
                        SalesRevenueDTO dto = new SalesRevenueDTO();
                        dto.setTotalRevenue(entry.getValue());
                        dto.setYear(ym.getYear());
                        dto.setMonth(ym.getMonthValue());
                        dto.setQuarter(null);
                        dto.setLabel(ym.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
                        return dto;
                    })
                    .sorted(Comparator.comparing(dto -> dto.getYear() * 100 + dto.getMonth()))
                    .toList();

        } else if (Boolean.TRUE.equals(quarterly)) {
            return salesOrderItems.stream()
                    .collect(Collectors.groupingBy(
                            item -> {
                                LocalDateTime date = item.getSalesOrder().getCreationTime();
                                int year = date.getYear();
                                int quarter = (date.getMonthValue() - 1) / 3 + 1;
                                return new AbstractMap.SimpleEntry<>(year, quarter);
                            },
                            Collectors.mapping(
                                    item -> item.getPricePerUnit().multiply(BigDecimal.valueOf(item.getQuantity())),
                                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                            )
                    ))
                    .entrySet().stream()
                    .map(entry -> {
                        int year = entry.getKey().getKey();
                        int quarter = entry.getKey().getValue();
                        SalesRevenueDTO dto = new SalesRevenueDTO();
                        dto.setTotalRevenue(entry.getValue());
                        dto.setYear(year);
                        dto.setMonth(null);
                        dto.setQuarter(quarter);
                        dto.setLabel("Q" + quarter + " " + year);
                        return dto;
                    })
                    .sorted(Comparator.comparing((SalesRevenueDTO d) -> d.getYear() * 10 + d.getQuarter()))
                    .toList();

        } else if (Boolean.TRUE.equals(yearly)) {
            return salesOrderItems.stream()
                    .collect(Collectors.groupingBy(
                            item -> item.getSalesOrder().getCreationTime().getYear(),
                            Collectors.mapping(
                                    item -> item.getPricePerUnit().multiply(BigDecimal.valueOf(item.getQuantity())),
                                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                            )
                    ))
                    .entrySet().stream()
                    .map(entry -> {
                        SalesRevenueDTO dto = new SalesRevenueDTO();
                        dto.setTotalRevenue(entry.getValue());
                        dto.setYear(entry.getKey());
                        dto.setMonth(null);
                        dto.setQuarter(null);
                        dto.setLabel(String.valueOf(entry.getKey()));
                        return dto;
                    })
                    .sorted(Comparator.comparing(SalesRevenueDTO::getYear))
                    .toList();
        }

        return Collections.emptyList();
    }
}
