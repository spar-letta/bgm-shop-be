package com.app.itibari.utils.helpers;

import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.Product;
import com.app.itibari.models.entities.SalesOrderItems;
import com.app.itibari.models.entities.SalesOrders;
import com.app.itibari.models.entities.Suppliers;
import com.app.itibari.models.response.ProductAnalysis;
import com.app.itibari.models.response.SupplierDashboardAnalysis;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.repository.ProductRepository;
import com.app.itibari.repository.SalesOrdersRepository;
import com.app.itibari.repository.SupplierRepository;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class SupplierDashboardAnalysisReport {

    private final PersonRepository personRepository;

    private final ProductRepository productRepository;

    private final SupplierRepository supplierRepository;

    private final SalesOrdersRepository salesOrdersRepository;

    public SupplierDashboardAnalysisReport(PersonRepository personRepository,
                                           ProductRepository productRepository,
                                           SupplierRepository supplierRepository,
                                           SalesOrdersRepository salesOrdersRepository) {

        this.personRepository = personRepository;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.salesOrdersRepository = salesOrdersRepository;
    }

    public SupplierDashboardAnalysis getSupplierDashboardAnalysis(String sku,
                                                                  UUID regionPublicId,
                                                                  UUID locationPublicId,
                                                                  LocalDate startDate,
                                                                  LocalDate endDate,
                                                                  String name,
                                                                  Pageable pageable) {

        SupplierDashboardAnalysis supplierDashboardAnalysis = new SupplierDashboardAnalysis();

        List<ProductAnalysis> productAnalyses = new ArrayList<>();

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            log.error("User with email {} not found", name);
            throw new BadRequestRestApiException()
                    .developerMessage("User with email " + name + " not found.")
                    .userMessage("Sorry, the user not found. Please contact support.");
        }

        Person person = optionalPerson.get();

        if (person.getHasSupplierAccount() == null || !person.getHasSupplierAccount()) {
            throw new BadRequestRestApiException()
                    .developerMessage("User with email " + name + " does not have a supplier account.")
                    .userMessage("Sorry, you do not have a supplier account. Please contact support.");
        }

        if (person.getSupplierPublicId() == null) {
            throw new BadRequestRestApiException()
                    .developerMessage("User with email " + name + " does not have a valid supplier account.")
                    .userMessage("Sorry, you do not have a valid supplier account. Please contact support.");
        }

        UUID supplierPublicId = UUID.fromString(person.getSupplierPublicId());

        Suppliers supplier = supplierRepository.findByPublicIdAndDeletedIsFalse(supplierPublicId)
                .orElseThrow(() -> {
                    log.error("Supplier with publicId {} not found", supplierPublicId);
                    return new BadRequestRestApiException()
                            .developerMessage("Supplier with publicId " + supplierPublicId + " not found.")
                            .userMessage("Sorry, the supplier account not found. Please contact support.");
                });

        List<Product> supplierProducts = productRepository
                .findBySuppliersAndDeletedIsFalse(Collections.singletonList(supplier), Pageable.unpaged())
                .getContent();

        if (supplierProducts.isEmpty()) {
            return supplierDashboardAnalysis;
        }

        List<SalesOrders> allOrders = salesOrdersRepository.findAllByCreationTimeBetweenAndDeletedIsFalse(startDate.atStartOfDay(),
                                                                                                          endDate.atStartOfDay());

        if (allOrders.isEmpty()) {
            for (Product supplierProduct : supplierProducts) {
                ProductAnalysis productAnalysis = new ProductAnalysis();
                productAnalysis.setProductName(supplierProduct.getProductName());
                productAnalysis.setSku(supplierProduct.getSku());
                productAnalysis.setQuantitySold(0);
                productAnalysis.setRegion(null);
                productAnalysis.setLocation(null);
                productAnalysis.setLocationLat(null);
                productAnalysis.setLocationLng(null);
                productAnalyses.add(productAnalysis);
            }

            supplierDashboardAnalysis.setProductAnalysis(productAnalyses);
            supplierDashboardAnalysis.setTotalElements(productAnalyses.size());
            supplierDashboardAnalysis.setPageSize(pageable.getPageSize());
            supplierDashboardAnalysis.setTotalPages((long) Math.ceil((double) productAnalyses.size() / pageable.getPageSize()));
            supplierDashboardAnalysis.setLast(true);
            supplierDashboardAnalysis.setFirst(true);
            supplierDashboardAnalysis.setPageNumber(0L);
            return supplierDashboardAnalysis;
        }

        List<SalesOrders> filteredOrders = allOrders.stream()
                .filter(regionPublicId == null ? order -> true : order -> order.getCustomer().getOrganizationRegions() != null && order
                        .getCustomer()
                        .getOrganizationRegions()
                        .getPublicId()
                        .equals(regionPublicId))
                .filter(locationPublicId == null ? order -> true : order -> order.getCustomer().getOrganizationLocation() != null && order
                        .getCustomer()
                        .getOrganizationLocation()
                        .getPublicId()
                        .equals(locationPublicId))
                .toList();

        List<SalesOrderItems> allOrderItems = filteredOrders.stream()
                .flatMap(order -> order.getSalesOrderItems().stream())
                .toList();

        for (Product supplierProduct : supplierProducts) {

            List<SalesOrderItems> matchingOrderItems = allOrderItems.stream()
                    .filter(item -> item.getProduct().getPublicId().equals(supplierProduct.getPublicId()))
                    .filter(sku == null ? item -> true : item -> item.getProduct().getSku().equalsIgnoreCase(sku))
                    .toList();
            // group by region and location
            Map<UUID, Map<UUID, List<SalesOrderItems>>> groupedByRegionAndLocation = new HashMap<>();
            for (SalesOrderItems item : matchingOrderItems) {
                UUID region = item.getSalesOrder().getCustomer().getOrganizationRegions() != null ? item
                        .getSalesOrder()
                        .getCustomer()
                        .getOrganizationRegions()
                        .getPublicId() : null;
                UUID location = item.getSalesOrder().getCustomer().getOrganizationLocation() != null ? item
                        .getSalesOrder()
                        .getCustomer()
                        .getOrganizationLocation()
                        .getPublicId() : null;
                groupedByRegionAndLocation
                        .computeIfAbsent(region, k -> new HashMap<>())
                        .computeIfAbsent(location, k -> new ArrayList<>())
                        .add(item);
            }

            if (groupedByRegionAndLocation.isEmpty()) {
                ProductAnalysis productAnalysis = new ProductAnalysis();
                productAnalysis.setProductName(supplierProduct.getProductName());
                productAnalysis.setSku(supplierProduct.getSku());
                productAnalysis.setQuantitySold(0);
                productAnalysis.setRegion(null);
                productAnalysis.setLocation(null);
                productAnalysis.setLocationLat(null);
                productAnalysis.setLocationLng(null);
                productAnalyses.add(productAnalysis);
            } else {
                for (Map.Entry<UUID, Map<UUID, List<SalesOrderItems>>> regionEntry : groupedByRegionAndLocation.entrySet()) {
                    UUID regionId = regionEntry.getKey();
                    String regionName = null;
                    if (regionId != null) {
                        regionName = filteredOrders.stream()
                                .filter(order -> order.getCustomer().getOrganizationRegions() != null && order
                                        .getCustomer()
                                        .getOrganizationRegions()
                                        .getPublicId()
                                        .equals(regionId))
                                .findFirst()
                                .map(order -> order.getCustomer().getOrganizationRegions().getName())
                                .orElse(null);
                    }

                    for (Map.Entry<UUID, List<SalesOrderItems>> locationEntry : regionEntry.getValue().entrySet()) {
                        UUID locationId = locationEntry.getKey();
                        String locationName = null;
                        String locationLat = null;
                        String locationLng = null;
                        if (locationId != null) {
                            locationName = filteredOrders.stream()
                                    .filter(order -> order.getCustomer().getOrganizationLocation() != null && order
                                            .getCustomer()
                                            .getOrganizationLocation()
                                            .getPublicId()
                                            .equals(locationId))
                                    .findFirst()
                                    .map(order -> order.getCustomer().getOrganizationLocation().getName())
                                    .orElse(null);
                            locationLat = filteredOrders.stream()
                                    .filter(order -> order.getCustomer().getOrganizationLocation() != null && order
                                            .getCustomer()
                                            .getOrganizationLocation()
                                            .getPublicId()
                                            .equals(locationId))
                                    .findFirst()
                                    .map(order -> order.getCustomer().getOrganizationLocation().getLatitude())
                                    .orElse(null);
                            locationLng = filteredOrders.stream()
                                    .filter(order -> order.getCustomer().getOrganizationLocation() != null && order
                                            .getCustomer()
                                            .getOrganizationLocation()
                                            .getPublicId()
                                            .equals(locationId))
                                    .findFirst()
                                    .map(order -> order.getCustomer().getOrganizationLocation().getLongitude())
                                    .orElse(null);
                        }
                        long totalQuantity = locationEntry.getValue().stream()
                                .mapToLong(SalesOrderItems::getQuantity)
                                .sum();
                        ProductAnalysis productAnalysis = new ProductAnalysis();
                        productAnalysis.setProductName(supplierProduct.getProductName());
                        productAnalysis.setSku(supplierProduct.getSku());
                        productAnalysis.setQuantitySold(totalQuantity);
                        productAnalysis.setRegion(regionName);
                        productAnalysis.setLocation(locationName);
                        productAnalysis.setLocationLat(locationLat);
                        productAnalysis.setLocationLng(locationLng);
                        productAnalyses.add(productAnalysis);
                    }
                }
            }
        }

        // no pagination

        supplierDashboardAnalysis.setProductAnalysis(productAnalyses);
        supplierDashboardAnalysis.setTotalElements(productAnalyses.size());
        supplierDashboardAnalysis.setPageSize(pageable.getPageSize());
        supplierDashboardAnalysis.setTotalPages((long) Math.ceil((double) productAnalyses.size() / pageable.getPageSize()));
        supplierDashboardAnalysis.setLast(true);
        supplierDashboardAnalysis.setFirst(true);
        supplierDashboardAnalysis.setPageNumber(0L);
        return supplierDashboardAnalysis;
    }
}
