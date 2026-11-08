package com.app.itibari.services;

import com.app.itibari.models.entities.OrganizationCountries;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.request.OrganizationCountriesRequest;
import com.app.itibari.repository.OrganizationCountriesRepository;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrganizationCountriesServiceImpl implements OrganizationCountriesService {

    private final OrganizationCountriesRepository organizationCountriesRepository;

    private final PersonRepository personRepository;

    public OrganizationCountriesServiceImpl(OrganizationCountriesRepository organizationCountriesRepository,
                                            PersonRepository personRepository) {

        this.organizationCountriesRepository = organizationCountriesRepository;
        this.personRepository = personRepository;
    }

    @Override
    public OrganizationCountries createOrganizationCountry(OrganizationCountriesRequest organizationCountriesRequest, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<OrganizationCountries> organizationCountriesOptional = organizationCountriesRepository
                .findByCodeAndDeletedIsFalse(organizationCountriesRequest.getCode());

        if (organizationCountriesOptional.isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Organization Country with code " + organizationCountriesOptional.get().getCode() + " already exists")
                    .userMessage("Sorry. Organization Country with code " + organizationCountriesOptional
                            .get()
                            .getCode() + " already exists");
        }

        Optional<OrganizationCountries> organizationCountriesByNameOptional = organizationCountriesRepository
                .findByNameAndDeletedIsFalse(organizationCountriesRequest.getName());

        if (organizationCountriesByNameOptional.isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Organization Country with name " + organizationCountriesOptional.get().getName() + " already exists")
                    .userMessage("Sorry. Organization Country with name " + organizationCountriesOptional
                            .get()
                            .getName() + " already exists");
        }

        OrganizationCountries organizationCountry = new OrganizationCountries();
        organizationCountry.setCode(organizationCountriesRequest.getCode());
        organizationCountry.setName(organizationCountriesRequest.getName());
        organizationCountry.setCustomerCareSupervisor(organizationCountriesRequest.getCustomerCareSupervisor());
        personOptional.ifPresent(organizationCountry::setCreatedBy);

        return organizationCountriesRepository.save(organizationCountry);
    }

    @Override
    public void updateOrganizationCountry(UUID organizationCountryPublicId,
                                          OrganizationCountriesRequest organizationCountriesRequest,
                                          String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<OrganizationCountries> organizationCountryOptional = organizationCountriesRepository
                .findByPublicIdAndDeletedIsFalse(organizationCountryPublicId);

        if (organizationCountryOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Organization country with publicId " + organizationCountryPublicId + " not found")
                    .userMessage("Sorry. Organization country with publicId " + organizationCountryPublicId + " not found");
        }

        OrganizationCountries organizationCountry = organizationCountryOptional.get();
        organizationCountry.setCode(organizationCountriesRequest.getCode());
        organizationCountry.setName(organizationCountriesRequest.getName());
        organizationCountry.setCustomerCareSupervisor(organizationCountriesRequest.getCustomerCareSupervisor());
        personOptional.ifPresent(organizationCountry::setUpdatedBy);
        organizationCountry.setUpdateDate(LocalDateTime.now());

        organizationCountriesRepository.save(organizationCountry);
    }

    @Override
    public OrganizationCountries getOrganizationCountry(UUID organizationCountriesPublicId) {

        Optional<OrganizationCountries> organizationCountryOptional = organizationCountriesRepository
                .findByPublicIdAndDeletedIsFalse(organizationCountriesPublicId);

        if (organizationCountryOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Organization country with publicId " + organizationCountriesPublicId + " not found")
                    .userMessage("Sorry. Organization country with publicId " + organizationCountriesPublicId + " not found");
        }

        return organizationCountryOptional.get();
    }

    @Override
    public Page<OrganizationCountries> getAllOrganizationCountries(String searchParam, Pageable pageable) {

        if (searchParam != null) {
            return organizationCountriesRepository
                    .findByNameContainingIgnoreCaseAndDeletedIsFalseOrCodeContainingIgnoreCaseAndDeletedIsFalse(searchParam,
                                                                                                                searchParam,
                                                                                                                pageable);
        } else {
            return organizationCountriesRepository.findByDeletedIsFalse(pageable);
        }
    }

    @Override
    public void deleteOrganizationCountry(UUID organizationCountriesPublicId, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<OrganizationCountries> organizationCountryOptional = organizationCountriesRepository
                .findByPublicIdAndDeletedIsFalse(organizationCountriesPublicId);

        if (organizationCountryOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Organization country with publicId " + organizationCountriesPublicId + " not found")
                    .userMessage("Sorry. Organization country with publicId " + organizationCountriesPublicId + " not found");
        }

        OrganizationCountries organizationCountry = organizationCountryOptional.get();
        personOptional.ifPresent(organizationCountry::setUpdatedBy);
        organizationCountry.setDeleted(true);
        organizationCountry.setUpdateDate(LocalDateTime.now());

        organizationCountriesRepository.save(organizationCountry);
    }
}
