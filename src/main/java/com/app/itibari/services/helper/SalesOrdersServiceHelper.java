package com.app.itibari.services.helper;

import com.app.itibari.dataTypes.CounterType;
import com.app.itibari.dataTypes.LoadingStatusEnum;
import com.app.itibari.dataTypes.OrderItemStatus;
import com.app.itibari.dataTypes.PaymentStatusEnum;
import com.app.itibari.dataTypes.SourceChannelEnum;
import com.app.itibari.models.entities.Customers;
import com.app.itibari.models.entities.DeliveryAgents;
import com.app.itibari.models.entities.OrganizationRegions;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.Product;
import com.app.itibari.models.entities.SalesAgents;
import com.app.itibari.models.entities.SalesOrderItems;
import com.app.itibari.models.entities.SalesOrders;
import com.app.itibari.models.entities.Warehouse;
import com.app.itibari.models.request.SalesOrderItemsRequest;
import com.app.itibari.models.request.SalesOrdersRequest;
import com.app.itibari.repository.CustomersRepository;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.repository.ProductRepository;
import com.app.itibari.repository.SalesAgentsRepository;
import com.app.itibari.repository.SalesOrderItemsRepository;
import com.app.itibari.repository.SalesOrdersRepository;
import com.app.itibari.repository.WarehouseRepository;
import com.app.itibari.services.CounterService;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class SalesOrdersServiceHelper {

    private final CustomersRepository customersRepository;

    private final ProductRepository productRepository;

    private final CounterService counterService;

    private final PersonRepository personRepository;

    private final WarehouseRepository warehouseRepository;

    private final SalesOrdersRepository salesOrdersRepository;

    private final SalesOrderItemsRepository salesOrderItemsRepository;

    private final SalesAgentsRepository salesAgentsRepository;

    private final WarehouseGetterHelper warehouseGetterHelper;

    private final StockUpdateHelper stockUpdateHelper;

    public void validateSalesOrderRequest(@Valid SalesOrdersRequest salesOrdersRequest,SalesOrders existingOrder) {

        if (null != salesOrdersRequest.getCustomerId()) {
            Optional<Customers> customersOptional = customersRepository.findByPublicIdAndDeletedIsFalse(salesOrdersRequest.getCustomerId());
            if (customersOptional.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("CustomerId with publicId " + salesOrdersRequest.getCustomerId() + " not found")
                        .userMessage("Customer not found");
            }
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("CustomerId cannot be null")
                    .userMessage("CustomerId cannot be null");
        }

        if (CollectionUtils.isEmpty(salesOrdersRequest.getSalesOrderItems())) {
            throw new BadRequestRestApiException()
                    .developerMessage("SalesOrderItems cannot be null or empty")
                    .userMessage("SalesOrderItems cannot be null or empty");
        }

        Warehouse warehouse = warehouseGetterHelper.getWarehouse(getOrganizationRegion(salesOrdersRequest));

        validateSalesOrderItems(salesOrdersRequest.getSalesOrderItems(),existingOrder.getSalesOrderItems(), warehouse);
    }

    private void validateSalesOrderItems(Set<SalesOrderItemsRequest> salesOrderItems,Set<SalesOrderItems> existingSalesOrderItems, Warehouse warehouse) {

        Map<UUID, Integer> productSkuMap = new HashMap<>();

        // add unique product SKUs and their quantities

        salesOrderItems.forEach(item -> {
            if (item.getProductId() == null || item.getQuantity() <= 0) {
                throw new BadRequestRestApiException()
                        .developerMessage("ProductId and Quantity must be provided and Quantity must be greater than zero")
                        .userMessage("ProductId and Quantity must be provided and Quantity must be greater than zero");
            }
        });

        salesOrderItems.forEach(item -> {
            if (productSkuMap.containsKey(item.getProductId())) {
                productSkuMap.put(item.getProductId(), productSkuMap.get(item.getProductId()) + item.getQuantity());
            } else {
                productSkuMap.put(item.getProductId(), item.getQuantity());
            }
        });

        // validate each product SKU and quantity

        productSkuMap.forEach((productId, quantity) -> {
            Optional<Product> productOptional = productRepository.findByPublicIdAndDeletedIsFalse(productId);
            if (productOptional.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product with publicId " + productId + " not found")
                        .userMessage("Product not found");
            }
            Product product = productOptional.get();
            if (product.getSellPrice() == null || product.getSellPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product with publicId " + productId + " has invalid sell price")
                        .userMessage("Product has invalid sell price");
            }
            SalesOrderItems existingItem = existingSalesOrderItems.stream()
                    .filter(item -> item.getProductId().equals(productId))
                    .findFirst()
                    .orElse(null);

            if (existingItem != null) {
                stockUpdateHelper.checkIfProductIsAvailableForUpdate(product, quantity,existingItem.getQuantity(), warehouse);

            }else {

                stockUpdateHelper.checkIfProductIsAvailableForUpdate(product, quantity,0, warehouse);
            }
        });
    }

    private OrganizationRegions getOrganizationRegion(@Valid SalesOrdersRequest salesOrdersRequest) {

        return customersRepository.findOrganizationRegionByCustomerPublicId(salesOrdersRequest.getCustomerId());
    }

}
