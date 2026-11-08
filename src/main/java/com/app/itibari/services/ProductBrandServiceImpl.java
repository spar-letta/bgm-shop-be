package com.app.itibari.services;

import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.ProductBrand;
import com.app.itibari.models.request.ProductBrandRequest;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.repository.ProductBrandRepository;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProductBrandServiceImpl implements ProductBrandService {

    private final ProductBrandRepository productBrandRepository;
    private final PersonRepository personRepository;

    public ProductBrandServiceImpl(ProductBrandRepository productBrandRepository, PersonRepository personRepository) {
        this.productBrandRepository = productBrandRepository;
        this.personRepository = personRepository;
    }

    @Override
    public ProductBrand createProductBrand(ProductBrandRequest productBrandRequest, String name) {
        Person logInUser = getPerson(name);

        ProductBrand productBrand = productBrandRepository.findByNameIgnoreCaseAndDeletedIsFalse(productBrandRequest.name().trim()).orElse(null);

        if (productBrand == null) {
            ProductBrand newProductBrand = new ProductBrand();
            newProductBrand.setName(productBrandRequest.name().trim());
            newProductBrand.setCreatedBy(logInUser);
            return productBrandRepository.save(newProductBrand);
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage(String.format("%s already exists.", productBrandRequest.name()))
                    .userMessage(String.format("Sorry, %s already exists.", productBrandRequest.name()));
        }
    }

    @Override
    public Page<ProductBrand> fetchProductBrands(String searchParam, Pageable pageable, Principal principal) {
        getPerson(principal.getName());
        if (searchParam != null && searchParam.length() > 2) {
            return productBrandRepository.fetchBySearchParam(searchParam, pageable);
        } else {
            return productBrandRepository.findAllByDeletedIsFalse(pageable);
        }
    }

    @Override
    public ProductBrand updateProductBrand(UUID publicId, ProductBrandRequest productBrandRequest, String name) {
        Person logInUser = getPerson(name);

        ProductBrand productBrand = productBrandRepository
                .findByPublicIdAndDeletedIsFalse(publicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Product brand not found")
                        .userMessage("Product brand not found"));

        ProductBrand productBrandWithName = productBrandRepository
                .findByNameIgnoreCaseAndDeletedIsFalse(productBrandRequest.name().trim())
                .orElse(null);

        if (productBrandWithName != null && !productBrandWithName.getPublicId().equals(productBrand.getPublicId())) {
            throw new BadRequestRestApiException()
                    .developerMessage(String.format("%s already exists.", productBrandRequest.name()))
                    .userMessage(String.format("Sorry, %s already exists.", productBrandRequest.name()));
        }

        productBrand.setName(productBrandRequest.name().trim());
        productBrand.setUpdatedBy(logInUser);

        return productBrandRepository.save(productBrand);
    }

    @Override
    public void deleteProductBrand(UUID publicId, String name) {
        Person logInUser = getPerson(name);
        ProductBrand productBrand = productBrandRepository.findByPublicIdAndDeletedIsFalse(publicId).orElse(null);
        if (productBrand != null) {
            productBrand.setDeleted(true);
            productBrand.setUpdatedBy(logInUser);
            productBrandRepository.save(productBrand);
        }
    }

    public Person getPerson(String username) {
        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(username);
        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("User not found")
                    .userMessage("Sorry, user not found");
        }
        return optionalPerson.get();
    }
}
