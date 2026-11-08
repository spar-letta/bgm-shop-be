package com.app.itibari.services.helper;

import com.app.itibari.models.entities.Customers;
import com.app.itibari.models.request.CustomersRequest;
import com.app.itibari.repository.CustomersRepository;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.helpers.Validators;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CustomerServiceHelper {

    private final CustomersRepository customersRepository;

    public CustomerServiceHelper(CustomersRepository customersRepository) {

        this.customersRepository = customersRepository;
    }

    public void validateCustomerRequest(CustomersRequest customersRequest) {

        if (customersRequest.getPhoneNumber() == null || customersRequest.getPhoneNumber().isBlank()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Phone number is mandatory")
                    .userMessage("Sorry, phone number is required");
        }

        // Check if the customer with the given email already exists
        if (StringUtils.isNotEmpty(customersRequest.getEmail())) {
            Optional<Customers> existingCustomerByEmail = customersRepository.findByEmailAndDeletedIsFalse(customersRequest.getEmail());
            if (existingCustomerByEmail.isPresent()) {
                throw new BadRequestRestApiException()
                        .developerMessage("Customer with email " + customersRequest.getEmail() + " already exists")
                        .userMessage("A customer with this email already exists");
            }
        }

        // Check if the customer with the given phone number already exists
        Optional<Customers> existingCustomerByPhoneNumber = customersRepository.findByPhoneNumberAndDeletedIsFalse(customersRequest.getPhoneNumber());
        if (existingCustomerByPhoneNumber.isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Customer with phone number " + customersRequest.getPhoneNumber() + " already exists")
                    .userMessage("A customer with this phone number already exists");
        }

        // Check if the customer with the given name already exists
//        Optional<Customers> existingCustomerByName = customersRepository.findByNameAndDeletedIsFalse(customersRequest.getName());
//        if (existingCustomerByName.isPresent()) {
//            throw new BadRequestRestApiException()
//                    .developerMessage("Customer with name " + customersRequest.getName() + " already exists")
//                    .userMessage("A customer with this name already exists");
//        }
    }
}
