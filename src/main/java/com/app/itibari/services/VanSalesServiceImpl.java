package com.app.itibari.services;

import com.app.itibari.dataTypes.ActionType;
import com.app.itibari.dataTypes.VanSaleStatus;
import com.app.itibari.models.entities.DeliveryAgents;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.Product;
import com.app.itibari.models.entities.ProductItem;
import com.app.itibari.models.entities.ProductRecon;
import com.app.itibari.models.entities.RoutePlanning;
import com.app.itibari.models.entities.VanSales;
import com.app.itibari.models.request.UpdateVanSales;
import com.app.itibari.models.request.VanSaleRequest;
import com.app.itibari.repository.DeliveryAgentsRepository;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.repository.ProductItemRepository;
import com.app.itibari.repository.ProductReconRepository;
import com.app.itibari.repository.ProductRepository;
import com.app.itibari.repository.RoutePlanningRepository;
import com.app.itibari.repository.VanSalesRepository;
import com.app.itibari.services.helper.StockUpdateHelper;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.specification.VanSalesSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VanSalesServiceImpl implements VanSalesService {

    private final VanSalesRepository vanSalesRepository;

    private final PersonRepository personRepository;

    private final DeliveryAgentsRepository deliveryAgentsRepository;

    private final ProductRepository productRepository;

    private final ProductItemRepository productItemRepository;

    private final RoutePlanningRepository routePlanningRepository;

    private final ProductReconRepository productReconRepository;

    private final StockUpdateHelper stockUpdateHelper;

    @Override
    public VanSales createVanSale(VanSaleRequest vanSaleRequest, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        Optional<DeliveryAgents> optionalDeliveryAgent = deliveryAgentsRepository.findByPublicIdAndDeletedIsFalse(vanSaleRequest.getDeliveryAgentPublicId());

        if (optionalDeliveryAgent.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery agent not found")
                    .userMessage("Sorry. The delivery agent you are looking for does not exist.");
        }
        DeliveryAgents deliveryAgent = optionalDeliveryAgent.get();

        if (deliveryAgent.isLoaded()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Delivery agent is loaded")
                    .userMessage("Sorry. The delivery agent is already loaded.");
        }

        List<VanSales> pendingVanSales = vanSalesRepository.findByDeliveryAgents_PublicIdAndStatusAndDeletedIsFalse(deliveryAgent.getPublicId(),
                                                                                                           VanSaleStatus.Pending);

        if (!pendingVanSales.isEmpty()) {

            throw new BadRequestRestApiException()
                    .developerMessage("Delivery agent has pending van sales")
                    .userMessage("Sorry. The delivery agent has pending van sales.");
        }

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person not found")
                    .userMessage("Sorry. The person you are looking for does not exist.");
        }

        Person person = optionalPerson.get();

        if (vanSaleRequest.getProductItemRequestList().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product item list is empty")
                    .userMessage("Sorry. The product item list is empty.");
        }

        VanSales vanSales = new VanSales();
        vanSales.setDeliveryAgents(deliveryAgent);
        vanSales.setCreatedBy(person);
        vanSales.setCreationTime(LocalDateTime.now());
        vanSales.setStatus(VanSaleStatus.Pending);
        vanSales.setDeleted(false);
        VanSales sales = vanSalesRepository.save(vanSales);

        Map<Product, Long> productIntegerMap = new HashMap<>();
        vanSaleRequest.getProductItemRequestList().forEach(productItemRequest -> {
            if (productItemRequest.getQuantity() <= 0) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product item quantity is less than 0")
                        .userMessage("Sorry. The product item quantity is less than 0.");
            }

            Optional<Product> optionalProduct = productRepository.findByPublicIdAndDeletedIsFalse(productItemRequest.getProductItemPublicId());

            if (optionalProduct.isEmpty()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Product not found")
                        .userMessage("Sorry. The product you are looking for does not exist.");
            }

            Product product = optionalProduct.get();

            stockUpdateHelper.checkIfProductIsAvailable(product, (int) productItemRequest.getQuantity(), deliveryAgent.getWarehouse());

            ProductItem productItem = new ProductItem();
            productItem.setProductCode(product.getSku());
            productItem.setVanSales(sales);
            productItem.setProductName(product.getProductName());
            productItem.setProductSellingPrice(product.getSellPrice());
            productItem.setVanSaleQuantity(productItemRequest.getQuantity());
            productItem.setInitialVanSaleQuantity(productItemRequest.getQuantity());
            productItem.setCreationTime(LocalDateTime.now());
            productItem.setCreatedBy(person);
            productItem.setProductPublicId(product.getPublicId());
            productItem.setVanSaleItem(true);

            ProductItem savedProductItem = productItemRepository.save(productItem);

            sales.addItem(savedProductItem);

            productIntegerMap.put(product, savedProductItem.getVanSaleQuantity());
        });

        stockUpdateHelper.updateBookedVanSales(productIntegerMap, deliveryAgent.getWarehouse());

        return vanSalesRepository.save(sales);
    }

    @Override
    public void updateVanSale(UUID publicId, ActionType actionType, UpdateVanSales vanSaleRequest, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person not found")
                    .userMessage("Sorry. The person you are looking for does not exist.");
        }

        Person person = optionalPerson.get();

        Optional<VanSales> optionalVanSales = vanSalesRepository.findByPublicIdAndDeletedIsFalse(publicId);

        if (optionalVanSales.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Van sales not found")
                    .userMessage("Sorry. The van sales you are looking for does not exist.");
        }

        VanSales vanSales = optionalVanSales.get();

        if (vanSales.getStatus() != VanSaleStatus.Pending) {
            throw new BadRequestRestApiException()
                    .developerMessage("Van sales status is not pending")
                    .userMessage("Sorry. The van sales status is not pending.");
        }

        if (vanSaleRequest.getProductItemRequestList().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product item list is empty")
                    .userMessage("Sorry. The product item list is empty.");
        }

        Map<Product, Long> productLongMap = new HashMap<>();

        if (actionType == ActionType.REMOVE) {
            vanSaleRequest.getProductItemRequestList().forEach(productItemUpdate -> {
                Optional<ProductItem> optionalProductItem = productItemRepository.findByPublicIdAndDeletedIsFalse(productItemUpdate.getProductItemPublicId());

                if (optionalProductItem.isEmpty()) {
                    throw new BadRequestRestApiException()
                            .developerMessage("ProductItem not found")
                            .userMessage("Sorry. The ProductItem you are looking for does not exist.");
                }

                ProductItem productItem = optionalProductItem.get();

                vanSales.removeItem(productItem);

                Product product = productRepository.findByPublicId(productItem.getProductPublicId());

                productLongMap.put(product, productItem.getVanSaleQuantity());
            });

            stockUpdateHelper.UnBookVanSales(productLongMap, vanSales.getDeliveryAgents().getWarehouse());
        } else if (actionType == ActionType.ADD) {
            vanSaleRequest.getProductItemRequestList().forEach(productItemRequest -> {
                if (productItemRequest.getQuantity() <= 0) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Product item quantity is less than 0")
                            .userMessage("Sorry. The product item quantity is less than 0.");
                }

                Optional<Product> optionalProduct = productRepository.findByPublicIdAndDeletedIsFalse(productItemRequest.getProductItemPublicId());

                if (optionalProduct.isEmpty()) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Product not found")
                            .userMessage("Sorry. The product you are looking for does not exist.");
                }

                Product product = optionalProduct.get();

                if (productItemRequest.getQuantity() > product.getAvailableStock()) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Product item quantity is greater than available quantity")
                            .userMessage("Sorry. The product item quantity is greater than available quantity.");
                }

                ProductItem productItem = new ProductItem();
                productItem.setProductCode(product.getSku());
                productItem.setVanSales(vanSales);
                productItem.setProductName(product.getProductName());
                productItem.setProductSellingPrice(product.getSellPrice());
                productItem.setVanSaleQuantity(productItemRequest.getQuantity());
                productItem.setCreationTime(LocalDateTime.now());
                productItem.setCreatedBy(person);
                productItem.setProductPublicId(product.getPublicId());
                productItem.setVanSaleItem(true);

                ProductItem savedProductItem = productItemRepository.save(productItem);

                vanSales.addItem(savedProductItem);

                productLongMap.put(product, savedProductItem.getVanSaleQuantity());
            });

            stockUpdateHelper.updateBookedVanSales(productLongMap, vanSales.getDeliveryAgents().getWarehouse());
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("Action type not supported")
                    .userMessage("Sorry. The action type is not supported.");
        }

        vanSales.setUpdateDate(LocalDateTime.now());
        vanSales.setUpdatedBy(person);
        vanSalesRepository.save(vanSales);
    }

    @Override
    public VanSales getVanSale(UUID publicId, String name) {

        Optional<VanSales> optionalVanSales = vanSalesRepository.findByPublicIdAndDeletedIsFalse(publicId);

        if (optionalVanSales.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Van sales not found")
                    .userMessage("Sorry. The van sales you are looking for does not exist.");
        }

        return optionalVanSales.get();
    }

    @Override
    public Page<VanSales> getVanSales(String searchParam,
                                      UUID deliveryAgentPublicId,
                                      VanSaleStatus status,
                                      String from,
                                      String to,
                                      Pageable pageable) {

        Specification<VanSales> spec = Specification
                .where(VanSalesSpecification.isNotDeleted())
                .and(VanSalesSpecification.hasDeliveryAgent(deliveryAgentPublicId))
                .and(VanSalesSpecification.hasStatus(status))
                .and(VanSalesSpecification.hasFromToDates(from, to))
                .and(VanSalesSpecification.searchParamMatches(searchParam));

        return vanSalesRepository.findAll(spec, pageable);
    }

    @Override
    public void deleteVanSale(UUID publicId, String name) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(name);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person not found")
                    .userMessage("Sorry. The person you are looking for does not exist.");
        }

        Person person = optionalPerson.get();

        Optional<VanSales> optionalVanSales = vanSalesRepository.findByPublicIdAndDeletedIsFalse(publicId);

        if (optionalVanSales.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Van sales not found")
                    .userMessage("Sorry. The van sales you are looking for does not exist.");
        }

        VanSales vanSales = optionalVanSales.get();
        vanSales.setDeleted(true);
        vanSales.setUpdateDate(LocalDateTime.now());
        vanSales.setUpdatedBy(person);
        vanSalesRepository.save(vanSales);

        Map<Product, Long> productLongMap = new HashMap<>();

        vanSales.getProductItems().forEach(productItem -> {

            Product product = productRepository.findByPublicId(productItem.getProductPublicId());

            productLongMap.put(product, productItem.getVanSaleQuantity());
            productItem.setDeleted(true);
            productItem.setUpdateDate(LocalDateTime.now());
            productItem.setUpdatedBy(person);
            productItemRepository.save(productItem);
        });

        stockUpdateHelper.UnBookVanSales(productLongMap, vanSales.getDeliveryAgents().getWarehouse());
    }

    @Override
    public Page<ProductItem> getVanSalesProductItems(UUID publicId, Pageable pageable) {

        Optional<VanSales> optionalVanSales = vanSalesRepository.findByPublicIdAndDeletedIsFalse(publicId);

        if (optionalVanSales.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Van sales not found")
                    .userMessage("Sorry. The van sales you are looking for does not exist.");
        }

        VanSales vanSales = optionalVanSales.get();

        return productItemRepository.findAllByVanSalesAndDeletedIsFalse(vanSales, pageable);
    }

    ///  used by mobile app
    @Override
    public Page<ProductItem> getRoutePlanningVanSalesProducts(UUID publicId, Pageable pageable) {

        Optional<RoutePlanning> optionalRoutePlanning = routePlanningRepository.getRoutePlanningByPublicIdAndDeletedIsFalse(publicId);

        if (optionalRoutePlanning.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Route planning not found")
                    .userMessage("Sorry. The route planning you are looking for does not exist.");
        }

        RoutePlanning routePlanning = optionalRoutePlanning.get();

        Optional<ProductRecon> optionalProductRecon = productReconRepository.findProductReconByDeliveryIdIgnoreCaseAndDeletedIsFalse(
                routePlanning.getRouteId());

        if (optionalProductRecon.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Product recon not found")
                    .userMessage("Sorry. Product recon for this route was not found");
        }

        ProductRecon productRecon = optionalProductRecon.get();

        return productItemRepository.findAllByProductReconAndHasVanSaleAndDeletedIsFalse(productRecon, true, pageable);
    }
}
