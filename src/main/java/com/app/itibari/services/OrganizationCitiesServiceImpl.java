package com.app.itibari.services;

import com.app.itibari.models.entities.OrganizationCities;
import com.app.itibari.models.entities.OrganizationCountries;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.request.OrganizationCitiesRequest;
import com.app.itibari.repository.OrganizationCitiesRepository;
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
public class OrganizationCitiesServiceImpl implements OrganizationCitiesService {

    private final OrganizationCitiesRepository organizationCitiesRepository;

    private final OrganizationCountriesRepository organizationCountriesRepository;

    private final PersonRepository personRepository;

    public OrganizationCitiesServiceImpl(OrganizationCitiesRepository organizationCitiesRepository,
                                         OrganizationCountriesRepository organizationCountriesRepository,
                                         PersonRepository personRepository) {

        this.organizationCitiesRepository = organizationCitiesRepository;
        this.organizationCountriesRepository = organizationCountriesRepository;
        this.personRepository = personRepository;
    }

    @Override
    public OrganizationCities createOrganizationCity(OrganizationCitiesRequest organizationCitiesRequest, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<OrganizationCities> organizationCitiesByNameOptional = organizationCitiesRepository
                .findByNameAndDeletedIsFalse(organizationCitiesRequest.getName());

        if (organizationCitiesByNameOptional.isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Organization City with name " + organizationCitiesByNameOptional.get().getName() + " already exists")
                    .userMessage("Sorry. Organization City with name " + organizationCitiesByNameOptional
                            .get()
                            .getName() + " already exists");
        }

        Optional<OrganizationCountries> organizationCountryOptional = organizationCountriesRepository
                .findByPublicIdAndDeletedIsFalse(organizationCitiesRequest.getOrganizationCountryId());

        if (organizationCountryOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Organization Country with public Id " + organizationCitiesRequest.getOrganizationCountryId() + " not found")
                    .userMessage("Sorry. Organization Country not found with public Id " + organizationCitiesRequest.getOrganizationCountryId());
        }

        OrganizationCities organizationCity = new OrganizationCities();
        organizationCity.setCode(organizationCitiesRequest.getCode());
        organizationCity.setName(organizationCitiesRequest.getName());
        organizationCity.setOrganizationCountry(organizationCountryOptional.get());
        personOptional.ifPresent(organizationCity::setCreatedBy);

        return organizationCitiesRepository.save(organizationCity);
    }

    @Override
    public void updateOrganizationCity(UUID organizationCityPublicId, OrganizationCitiesRequest organizationCitiesRequest, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<OrganizationCities> organizationCityOptional = organizationCitiesRepository
                .findByPublicIdAndDeletedIsFalse(organizationCityPublicId);

        if (organizationCityOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Organization City with publicId " + organizationCityPublicId + " not found")
                    .userMessage("Sorry. Organization City with publicId " + organizationCityPublicId + " not found");
        }

        Optional<OrganizationCountries> organizationCountryOptional = organizationCountriesRepository
                .findByPublicIdAndDeletedIsFalse(organizationCitiesRequest.getOrganizationCountryId());

        if (organizationCountryOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Organization Country with public ID " + organizationCitiesRequest.getOrganizationCountryId() + " not found")
                    .userMessage("Sorry. Organization Country not found for public ID " + organizationCitiesRequest.getOrganizationCountryId());
        }

        OrganizationCities organizationCity = organizationCityOptional.get();
        organizationCity.setCode(organizationCitiesRequest.getCode());
        organizationCity.setName(organizationCitiesRequest.getName());
        organizationCity.setOrganizationCountry(organizationCountryOptional.get());
        personOptional.ifPresent(organizationCity::setUpdatedBy);
        organizationCity.setUpdateDate(LocalDateTime.now());

        organizationCitiesRepository.save(organizationCity);
    }

    @Override
    public OrganizationCities getOrganizationCity(UUID organizationCityPublicId) {

        Optional<OrganizationCities> organizationCityOptional = organizationCitiesRepository
                .findByPublicIdAndDeletedIsFalse(organizationCityPublicId);

        if (organizationCityOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Organization City with publicId " + organizationCityPublicId + " not found")
                    .userMessage("Sorry. Organization City with publicId " + organizationCityPublicId + " not found");
        }

        return organizationCityOptional.get();
    }

    @Override
    public Page<OrganizationCities> getAllOrganizationCities(String searchParam, Pageable pageable) {

        if (searchParam != null) {
            return organizationCitiesRepository
                    .findByNameContainingIgnoreCaseAndDeletedIsFalseOrCodeContainingIgnoreCaseAndDeletedIsFalse(searchParam,
                                                                                                                searchParam,
                                                                                                                pageable);
        } else {
            return organizationCitiesRepository.findByDeletedIsFalse(pageable);
        }
    }

    @Override
    public void deleteOrganizationCity(UUID organizationCityPublicId, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<OrganizationCities> organizationCityOptional = organizationCitiesRepository
                .findByPublicIdAndDeletedIsFalse(organizationCityPublicId);

        if (organizationCityOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Organization City with publicId " + organizationCityPublicId + " not found")
                    .userMessage("Sorry. Organization City with publicId " + organizationCityPublicId + " not found");
        }

        OrganizationCities organizationCity = organizationCityOptional.get();
        personOptional.ifPresent(organizationCity::setUpdatedBy);
        organizationCity.setDeleted(true);
        organizationCity.setUpdateDate(LocalDateTime.now());

        organizationCitiesRepository.save(organizationCity);
    }
}
