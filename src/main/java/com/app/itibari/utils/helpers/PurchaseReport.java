package com.app.itibari.utils.helpers;

import com.app.itibari.dataTypes.LoadingStatusEnum;
import com.app.itibari.models.entities.*;
import com.app.itibari.models.response.CustomerOrderDTO;
import com.app.itibari.models.response.CustomerOrderPageable;
import com.app.itibari.repository.*;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PurchaseReport {

    private final PersonRepository personRepository;

    private final SupplierRepository supplierRepository;

    private final ProductRepository productRepository;

    private final SalesOrdersRepository salesOrdersRepository;
    private final OrganizationRegionsRepository organizationRegionsRepository;

    public PurchaseReport(PersonRepository personRepository,
                          SupplierRepository supplierRepository,
                          ProductRepository productRepository,
                          SalesOrdersRepository salesOrdersRepository, OrganizationRegionsRepository organizationRegionsRepository) {

        this.personRepository = personRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.salesOrdersRepository = salesOrdersRepository;
        this.organizationRegionsRepository = organizationRegionsRepository;
    }

    public CustomerOrderPageable getPurchaseReport(String searchParm,
                                                   UUID regionId,
                                                   LocalDate startDate,
                                                   LocalDate endDate,
                                                   String name,
                                                   Pageable pageable) {

        if (startDate == null) {
            startDate = LocalDate.now();
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        Person person = personRepository.findByEmailAndDeletedIsFalse(name)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("user not found")
                        .userMessage("Sorry!. user not found"));

        if (person.getSupplierPublicId() == null || person.getSupplierPublicId().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("no associated supplier")
                    .userMessage("Sorry!. no associated supplier");
        }

        Suppliers supplier = supplierRepository.findByPublicIdAndDeletedIsFalse(UUID.fromString(person.getSupplierPublicId()))
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("supplier not found")
                        .userMessage("Sorry! Supplier not found"));

        List<Product> supplierProducts = productRepository
                .findBySuppliersAndDeletedIsFalse(Collections.singletonList(supplier), Pageable.unpaged())
                .getContent();

        if (supplierProducts.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("supplier has no defined products")
                    .userMessage("Sorry! Supplier has no defined products");
        }

        List<UUID> supplierProductIds = supplierProducts.stream()
                .map(Product::getPublicId)
                .toList();

        Page<SalesOrders> orders;
        if (regionId != null) {
            OrganizationRegions organizationRegions =
                    organizationRegionsRepository.findByPublicIdAndDeletedIsFalse(regionId).orElse(null);

            orders = salesOrdersRepository.findAllByOrganizationRegionsAndCreationTimeBetweenAndSalesOrderStatusAndDeletedIsFalse(
                    organizationRegions,
                    startDate.atStartOfDay(),
                    endDate.atStartOfDay(),
                    LoadingStatusEnum.DELIVERED,
                    pageable);
        } else {
            orders = salesOrdersRepository.findAllByCreationTimeBetweenAndSalesOrderStatusAndDeletedIsFalse(
                    startDate.atStartOfDay(),
                    endDate.atStartOfDay(),
                    LoadingStatusEnum.DELIVERED,
                    pageable);
        }

        Map<Customers, List<SalesOrders>> customersOrderMap = orders.stream()
                .filter(order -> order.getCustomer() != null)
                .collect(Collectors.groupingBy(SalesOrders::getCustomer));

        List<CustomerOrderDTO> customerOrderDTOList = new ArrayList<>();

        for (Map.Entry<Customers, List<SalesOrders>> entry : customersOrderMap.entrySet()) {
            Customers customer = entry.getKey();
            List<SalesOrders> customerOrders = entry.getValue();

            List<SalesOrderItems> allItems = customerOrders.stream()
                    .flatMap(order -> order.getSalesOrderItems().stream())
                    .toList();

            List<SalesOrderItems> filteredItems = allItems.stream()
                    .filter(item -> item.getProduct() != null && supplierProductIds.contains(item.getProduct().getPublicId()))
                    .toList();

            Map<Product, List<SalesOrderItems>> itemsByProduct = filteredItems.stream()
                    .collect(Collectors.groupingBy(SalesOrderItems::getProduct));

            for (Map.Entry<Product, List<SalesOrderItems>> itemEntry : itemsByProduct.entrySet()) {
                Product product = itemEntry.getKey();
                List<SalesOrderItems> items = itemEntry.getValue().stream()
                        .filter(item -> item.getQuantityDelivered() > 0)
                        .toList();

                for (SalesOrderItems item : items) {
                    CustomerOrderDTO dto = new CustomerOrderDTO();
                    dto.setPurchaseDate(item.getCreationTime());
                    dto.setCustomerName(customer.getName());
                    dto.setRegion(customer.getOrganizationRegions() != null ? customer.getOrganizationRegions().getName() : null);
                    dto.setLocation(customer.getOrganizationLocation() != null ? customer.getOrganizationLocation().getName() : null);
                    dto.setProductName(product.getProductName());
                    dto.setSkuCode(product.getSku());
                    dto.setQuantityPurchased((long) item.getQuantityDelivered());
                    dto.setSalesOrderNumber(item.getSalesOrder().getSalesOrderNumber());
                    customerOrderDTOList.add(dto);
                }
            }
        }

        customerOrderDTOList = filterOrders(customerOrderDTOList, searchParm);

        CustomerOrderPageable summary = new CustomerOrderPageable();
        if (!customerOrderDTOList.isEmpty()) {
            Page<CustomerOrderDTO> page = new PageImpl<>(customerOrderDTOList, pageable, customerOrderDTOList.size());

            summary.setContent(page.getContent());
            summary.setTotalElements(page.getTotalElements());
            summary.setPageSize(page.getSize());
            summary.setLast(page.isLast());
            summary.setFirst(page.isFirst());
            summary.setPageNumber(page.getNumber());
        }

        return summary;
    }

    private List<CustomerOrderDTO> filterOrders(List<CustomerOrderDTO> orders, String searchParm) {
        if (searchParm == null || searchParm.isBlank()) return orders;
        final String search = searchParm.trim().toLowerCase();

        return orders.stream()
                .filter(order -> (order.getCustomerName() != null && order.getCustomerName().toLowerCase().contains(search))
                        || (order.getSkuCode() != null && order.getSkuCode().toLowerCase().contains(search))
                        || (order.getSalesOrderNumber() != null && order.getSalesOrderNumber().toLowerCase().contains(search))

                )
                .toList();
    }

}
