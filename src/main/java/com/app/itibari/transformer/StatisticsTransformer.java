package com.app.itibari.transformer;

import com.app.itibari.models.entities.SalesOrderItems;
import com.app.itibari.models.entities.SalesOrders;
import com.app.itibari.models.report.StatisticsDTO;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class StatisticsTransformer implements Converter<List<SalesOrderItems>, List<StatisticsDTO>> {

    @Override
    public List<StatisticsDTO> convert(List<SalesOrderItems> salesOrderItems) {

        List<StatisticsDTO> statisticsDTOList = new ArrayList<>();

        StatisticsDTO statisticsDTO = new StatisticsDTO();

        Set<SalesOrders> distinctOrders = salesOrderItems.stream()
                .map(SalesOrderItems::getSalesOrder)
                .collect(Collectors.toSet());

        BigDecimal tonnes = BigDecimal.ZERO;
        BigDecimal revenue = BigDecimal.ZERO;
        Long activeCustomers = distinctOrders.stream()
                .map(SalesOrders::getCustomer)
                .distinct()
                .count();

        for (SalesOrderItems item : salesOrderItems) {
            BigDecimal itemTonnes = item.getProduct().getWeight().divide(BigDecimal.valueOf(1000), 2, RoundingMode.DOWN);
            tonnes = tonnes.add(itemTonnes);
            BigDecimal itemRevenue = item.getProduct().getBuyingPrice();
            revenue = revenue.add(itemRevenue);
        }

        statisticsDTO.setTotalOrders((long) distinctOrders.size());
        statisticsDTO.setTotalTonnes(tonnes);
        statisticsDTO.setActiveCustomers(activeCustomers);
        statisticsDTO.setRevenue(revenue);

        statisticsDTOList.add(statisticsDTO);
        return statisticsDTOList;
    }
}
