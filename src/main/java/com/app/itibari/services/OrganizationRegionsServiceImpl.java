package com.app.itibari.services;

import com.app.itibari.models.entities.OrganizationCities;
import com.app.itibari.models.entities.OrganizationRegions;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.request.OrganizationRegionsRequest;
import com.app.itibari.repository.OrganizationCitiesRepository;
import com.app.itibari.repository.OrganizationRegionsRepository;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrganizationRegionsServiceImpl implements OrganizationRegionsService {

    private final OrganizationRegionsRepository organizationRegionsRepository;

    private final OrganizationCitiesRepository organizationCitiesRepository;

    private final PersonRepository personRepository;

    public OrganizationRegionsServiceImpl(OrganizationRegionsRepository organizationRegionsRepository,
                                          OrganizationCitiesRepository organizationCitiesRepository,
                                          PersonRepository personRepository) {

        this.organizationRegionsRepository = organizationRegionsRepository;
        this.organizationCitiesRepository = organizationCitiesRepository;
        this.personRepository = personRepository;
    }

    @Override
    public OrganizationRegions createOrganizationRegion(OrganizationRegionsRequest organizationRegionsRequest, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);

        Optional<OrganizationRegions> organizationRegionsByNameOptional = organizationRegionsRepository
                .findByNameAndDeletedIsFalse(organizationRegionsRequest.getName());

        if (organizationRegionsByNameOptional.isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Organization Region with name " + organizationRegionsByNameOptional
                            .get()
                            .getName() + " already exists")
                    .userMessage("Sorry. Organization Region with name " + organizationRegionsByNameOptional
                            .get()
                            .getName() + " already exists");
        }

        Optional<OrganizationCities> organizationCityOptional = organizationCitiesRepository
                .findByPublicIdAndDeletedIsFalse(organizationRegionsRequest.getOrganizationCityId());

        if (organizationCityOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Organization City with public Id " + organizationRegionsRequest.getOrganizationCityId() + " not found")
                    .userMessage("Sorry. Organization City not found with public Id " + organizationRegionsRequest.getOrganizationCityId());
        }

        OrganizationRegions organizationRegion = new OrganizationRegions();
        organizationRegion.setCode(organizationRegionsRequest.getCode());
        organizationRegion.setName(organizationRegionsRequest.getName());
        organizationRegion.setOrganizationCity(organizationCityOptional.get());
        organizationRegion.setLatitude(organizationRegionsRequest.getLatitude());
        organizationRegion.setLongitude(organizationRegionsRequest.getLongitude());
        personOptional.ifPresent(organizationRegion::setCreatedBy);

        return organizationRegionsRepository.save(organizationRegion);
    }

    @Override
    public void updateOrganizationRegion(UUID organizationRegionPublicId,
                                         OrganizationRegionsRequest organizationRegionsRequest,
                                         String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<OrganizationRegions> organizationRegionOptional = organizationRegionsRepository
                .findByPublicIdAndDeletedIsFalse(organizationRegionPublicId);

        if (organizationRegionOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Organization Region with publicId " + organizationRegionPublicId + " not found")
                    .userMessage("Sorry. Organization Region with publicId " + organizationRegionPublicId + " not found");
        }

        Optional<OrganizationCities> organizationCityOptional = organizationCitiesRepository
                .findByPublicIdAndDeletedIsFalse(organizationRegionsRequest.getOrganizationCityId());

        if (organizationCityOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Organization City with public ID " + organizationRegionsRequest.getOrganizationCityId() + " not found")
                    .userMessage("Sorry. Organization City not found for public ID " + organizationRegionsRequest.getOrganizationCityId());
        }

        OrganizationRegions organizationRegion = organizationRegionOptional.get();
        organizationRegion.setCode(organizationRegionsRequest.getCode());
        organizationRegion.setName(organizationRegionsRequest.getName());
        organizationRegion.setOrganizationCity(organizationCityOptional.get());
        personOptional.ifPresent(organizationRegion::setUpdatedBy);
        organizationRegion.setLatitude(organizationRegionsRequest.getLatitude());
        organizationRegion.setLongitude(organizationRegionsRequest.getLongitude());
        organizationRegion.setUpdateDate(LocalDateTime.now());

        organizationRegionsRepository.save(organizationRegion);
    }

    @Override
    public OrganizationRegions getOrganizationRegion(UUID organizationRegionPublicId) {

        Optional<OrganizationRegions> organizationRegionOptional = organizationRegionsRepository
                .findByPublicIdAndDeletedIsFalse(organizationRegionPublicId);

        if (organizationRegionOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Organization Region with publicId " + organizationRegionPublicId + " not found")
                    .userMessage("Sorry. Organization Region with publicId " + organizationRegionPublicId + " not found");
        }

        return organizationRegionOptional.get();
    }

    @Override
    public Page<OrganizationRegions> getAllOrganizationRegions(String searchParam, Pageable pageable) {

        if (searchParam != null) {
            return organizationRegionsRepository
                    .findByNameContainingIgnoreCaseAndDeletedIsFalseOrCodeContainingIgnoreCaseAndDeletedIsFalse(searchParam,
                                                                                                                searchParam,
                                                                                                                pageable);
        } else {
            return organizationRegionsRepository.findByDeletedIsFalse(pageable);
        }
    }

    @Override
    public void deleteOrganizationRegion(UUID organizationRegionPublicId, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<OrganizationRegions> organizationRegionOptional = organizationRegionsRepository
                .findByPublicIdAndDeletedIsFalse(organizationRegionPublicId);

        if (organizationRegionOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Organization Region with publicId " + organizationRegionPublicId + " not found")
                    .userMessage("Sorry. Organization Region with publicId " + organizationRegionPublicId + " not found");
        }

        OrganizationRegions organizationRegion = organizationRegionOptional.get();
        personOptional.ifPresent(organizationRegion::setUpdatedBy);
        organizationRegion.setDeleted(true);
        organizationRegion.setUpdateDate(LocalDateTime.now());

        organizationRegionsRepository.save(organizationRegion);
    }
}
