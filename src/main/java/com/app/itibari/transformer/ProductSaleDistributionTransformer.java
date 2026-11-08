package com.app.itibari.transformer;

import com.app.itibari.models.entities.Product;
import com.app.itibari.models.entities.SalesOrderItems;
import com.app.itibari.models.report.ProductSaleDistributionDTO;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class ProductSaleDistributionTransformer implements Converter<Map<Product, List<SalesOrderItems>>,List<ProductSaleDistributionDTO>> {
    @Override
    public @Nullable List<ProductSaleDistributionDTO> convert(Map<Product, List<SalesOrderItems>> source) {
        return source.entrySet().stream().map(entry -> {
            Product product = entry.getKey();
            List<SalesOrderItems> orderItems = entry.getValue();

            BigDecimal totalOrderValue = orderItems.stream()
                    .map(item -> item.getProduct().getBuyingPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int totalQuantity = orderItems.stream()
                    .mapToInt(SalesOrderItems::getQuantity)
                    .sum();

            ProductSaleDistributionDTO dto = new ProductSaleDistributionDTO();
            dto.setProductName(product.getProductName());
            dto.setSku(product.getSku());
            dto.setUom(product.getUnitOfMeasure());
            dto.setOrderValue(totalOrderValue);
            dto.setQuantity(totalQuantity);

            return dto;
        }).toList();

    }
}
