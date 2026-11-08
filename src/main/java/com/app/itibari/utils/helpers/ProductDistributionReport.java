package com.app.itibari.utils.helpers;

import com.app.itibari.dataTypes.LoadingStatusEnum;
import com.app.itibari.models.entities.Customers;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.Product;
import com.app.itibari.models.entities.SalesOrderItems;
import com.app.itibari.models.entities.SalesOrders;
import com.app.itibari.models.entities.Suppliers;
import com.app.itibari.models.response.ProductDistributionDTO;
import com.app.itibari.models.response.ProductDistributionPageableDTO;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.repository.ProductRepository;
import com.app.itibari.repository.SalesOrdersRepository;
import com.app.itibari.repository.SupplierRepository;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ProductDistributionReport {

    private final PersonRepository personRepository;

    private final SalesOrdersRepository salesOrdersRepository;

    private final ProductRepository productRepository;

    private final SupplierRepository supplierRepository;

    public ProductDistributionReport(PersonRepository personRepository,
                                     SalesOrdersRepository salesOrdersRepository,
                                     ProductRepository productRepository,
                                     SupplierRepository supplierRepository) {

        this.personRepository = personRepository;
        this.salesOrdersRepository = salesOrdersRepository;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
    }

    public ProductDistributionPageableDTO getProductDistributionReport(UUID productId,
                                                                       UUID categoryId,
                                                                       UUID regionId,
                                                                       UUID locationId,
                                                                       UUID customerId,
                                                                       LocalDate startDate,
                                                                       LocalDate endDate,
                                                                       String name,
                                                                       Pageable pageable) {

        LocalDate start = startDate == null ? LocalDate.now() : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);
        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("User with email " + name + " not found.")
                    .userMessage("Sorry, we could not find the specified user.");
        }

        Person person = optionalPerson.get();
        log.info("-user email  {}", person.getEmail());

        UUID supplierPublicId = person.getSupplierPublicId() == null ? null : UUID.fromString(person.getSupplierPublicId());

        // fetch product of the supplier
        if (supplierPublicId == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("User with email " + name + " is not a supplier.")
                    .userMessage("Sorry, the specified user is not a supplier.");
        }
        Suppliers supplier = supplierRepository.findByPublicIdAndDeletedIsFalse(supplierPublicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Supplier with id " + supplierPublicId + " not found.")
                        .userMessage("Sorry, we could not find the specified supplier."));

        List<Product> supplierProducts = productRepository
                .findBySuppliersAndDeletedIsFalse(Collections.singletonList(supplier), Pageable.unpaged())
                .getContent();
        log.info("-----supplierProducts size {}", supplierProducts.size());
        List<UUID> supplierProductIds = supplierProducts.stream().map(Product::getPublicId).toList();

        // fetch sales orders by date range and status APPROVED or COMPLETED

        Page<SalesOrders> orders = salesOrdersRepository.findAllByCreationTimeBetweenAndSalesOrderStatusAndDeletedIsFalse(start.atStartOfDay(),
                                                                                                                          end.atTime(23,
                                                                                                                                     59,
                                                                                                                                     59),
                                                                                                                          LoadingStatusEnum.DELIVERED,
                                                                                                                          pageable);

        log.info("-----orders size {}", orders.getContent().size());
        List<SalesOrders> filteredOrders = orders.stream()
                .filter(order -> order.getSalesOrderItems().stream()
                        .anyMatch(item -> item.getProduct() != null && supplierProductIds.contains(item.getProduct().getPublicId())))
                .filter(regionId == null ? order -> true : order -> order.getOrganizationRegions() != null && order
                        .getOrganizationRegions()
                        .getPublicId()
                        .equals(regionId))
                .filter(locationId == null ? order -> true : order -> order.getCustomer().getOrganizationLocation() != null && order
                        .getCustomer()
                        .getOrganizationLocation()
                        .getPublicId()
                        .equals(locationId))
                .filter(productId == null ? order -> true : order -> order
                        .getSalesOrderItems()
                        .stream()
                        .anyMatch(item -> item.getProduct() != null && item.getProduct().getPublicId().equals(productId)))
                .filter(categoryId == null ? order -> true : order -> order
                        .getSalesOrderItems()
                        .stream()
                        .anyMatch(item -> item.getProduct() != null && item.getProduct().getCategory() != null && item
                                .getProduct()
                                .getCategory()
                                .getPublicId()
                                .equals(categoryId)))
                .filter(customerId == null ? order -> true : order -> order.getCustomer() != null && order
                        .getCustomer()
                        .getPublicId()
                        .equals(customerId))
                .toList();

        ProductDistributionPageableDTO summary = new ProductDistributionPageableDTO();

        List<ProductDistributionDTO> productDistributionDTOS = new ArrayList<>();

        if (!filteredOrders.isEmpty()) {
            // group by customer

            Map<Customers, List<SalesOrders>> ordersByCustomer = filteredOrders
                    .stream()
                    .filter(order -> order.getCustomer() != null)
                    .collect(Collectors.groupingBy(SalesOrders::getCustomer));

            for (Map.Entry<Customers, List<SalesOrders>> entry : ordersByCustomer.entrySet()) {
                Customers customer = entry.getKey();
                List<SalesOrders> customerOrders = entry.getValue();

                List<SalesOrderItems> allItems = customerOrders.stream()
                        .flatMap(order -> order.getSalesOrderItems().stream())
                        .toList();

                // filter items by supplier products ids

                List<SalesOrderItems> filteredItems = allItems.stream()
                        .filter(item -> item.getProduct() != null && supplierProductIds.contains(item.getProduct().getPublicId()))
                        .toList();

                // group by product

                Map<Product, List<SalesOrderItems>> itemsByProduct = filteredItems.stream()
                        .collect(Collectors.groupingBy(SalesOrderItems::getProduct));

                for (Map.Entry<Product, List<SalesOrderItems>> itemEntry : itemsByProduct.entrySet()) {
                    Product product = itemEntry.getKey();
                    List<SalesOrderItems> items1 = itemEntry.getValue();

                    List<SalesOrderItems> items = items1.stream()
                            .filter(item -> item.getQuantityDelivered() > 0)
                            .toList();

                    ProductDistributionDTO productDistributionDTO = new ProductDistributionDTO();

                    productDistributionDTO.setCustomerName(customer.getName());
                    productDistributionDTO.setCustomerPublicId(customer.getPublicId().toString());
                    productDistributionDTO.setProductName(product.getProductName());
                    productDistributionDTO.setSku(product.getSku());
                    productDistributionDTO.setQuantity(items.stream().mapToLong(SalesOrderItems::getQuantityDelivered).sum());
                    productDistributionDTO.setRegion(customer.getOrganizationRegions() != null ? customer
                            .getOrganizationRegions()
                            .getName() : null);
                    productDistributionDTO.setLocation(customer.getOrganizationLocation() != null ? customer
                            .getOrganizationLocation()
                            .getName() : null);
                    productDistributionDTO.setCity(null);
                    productDistributionDTO.setLocationLat(customer.getLatitude() != null ? customer.getLatitude() : null);
                    productDistributionDTO.setLocationLng(customer.getLongitude() != null ? customer.getLongitude() : null);
                    productDistributionDTO.setPrice(!items.isEmpty() && items.get(0).getProduct().getSellPrice() != null ?
                                                            items.get(0).getProduct().getSellPrice() : BigDecimal.ZERO);
                    productDistributionDTO.setDate(!items.isEmpty() ? items.get(0).getCreationTime().toLocalDate().toString() : null);

                    productDistributionDTOS.add(productDistributionDTO);
                }
            }
        }

        if (!productDistributionDTOS.isEmpty()) {
            PageImpl<ProductDistributionDTO> page = new PageImpl<>(productDistributionDTOS,
                                                                   Pageable.unpaged(),
                                                                   productDistributionDTOS.size());
            summary.setContent(page.getContent());
            summary.setTotalElements(page.getTotalElements());
            summary.setPageSize(page.getSize());
            summary.setLast(page.isLast());
            summary.setFirst(page.isFirst());
            summary.setPageNumber(page.getNumber());
            summary.setTotalProducts(productDistributionDTOS.size());
        }

        return summary;
    }
}
