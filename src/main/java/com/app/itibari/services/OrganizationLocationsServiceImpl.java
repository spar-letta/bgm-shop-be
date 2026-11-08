package com.app.itibari.services;

import com.app.itibari.models.entities.OrganizationLocations;
import com.app.itibari.models.entities.OrganizationRegions;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.request.OrganizationLocationsRequest;
import com.app.itibari.repository.OrganizationLocationsRepository;
import com.app.itibari.repository.OrganizationRegionsRepository;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.specification.OrganizationLocationSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrganizationLocationsServiceImpl implements OrganizationLocationsService {

    private final OrganizationLocationsRepository organizationLocationsRepository;

    private final OrganizationRegionsRepository organizationRegionsRepository;

    private final PersonRepository personRepository;

    public OrganizationLocationsServiceImpl(OrganizationLocationsRepository organizationLocationsRepository,
                                            OrganizationRegionsRepository organizationRegionsRepository,
                                            PersonRepository personRepository) {

        this.organizationLocationsRepository = organizationLocationsRepository;
        this.organizationRegionsRepository = organizationRegionsRepository;
        this.personRepository = personRepository;
    }

    @Override
    public OrganizationLocations createOrganizationLocation(OrganizationLocationsRequest organizationLocationsRequest, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);

        Optional<OrganizationLocations> organizationLocationsByNameOptional = organizationLocationsRepository
                .findByNameAndDeletedIsFalse(organizationLocationsRequest.getName());

        if (organizationLocationsByNameOptional.isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Organization Location with name " + organizationLocationsByNameOptional
                            .get()
                            .getName() + " already exists")
                    .userMessage("Sorry. Organization Location with name " + organizationLocationsByNameOptional
                            .get()
                            .getName() + " already exists");
        }

        Optional<OrganizationRegions> organizationRegionOptional = organizationRegionsRepository
                .findByPublicIdAndDeletedIsFalse(organizationLocationsRequest.getOrganizationRegionId());

        if (organizationRegionOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Organization Region with public Id " + organizationLocationsRequest.getOrganizationRegionId() + " not found")
                    .userMessage("Sorry. Organization Region not found with public Id " + organizationLocationsRequest.getOrganizationRegionId());
        }

        OrganizationLocations organizationLocation = new OrganizationLocations();
        organizationLocation.setCode(organizationLocationsRequest.getCode());
        organizationLocation.setName(organizationLocationsRequest.getName());
        organizationLocation.setOrganizationRegion(organizationRegionOptional.get());
        organizationLocation.setLatitude(organizationLocationsRequest.getLatitude());
        organizationLocation.setLongitude(organizationLocationsRequest.getLongitude());
        personOptional.ifPresent(organizationLocation::setCreatedBy);

        return organizationLocationsRepository.save(organizationLocation);
    }

    @Override
    public void updateOrganizationLocation(UUID organizationLocationPublicId,
                                           OrganizationLocationsRequest organizationLocationsRequest,
                                           String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<OrganizationLocations> organizationLocationOptional = organizationLocationsRepository
                .findByPublicIdAndDeletedIsFalse(organizationLocationPublicId);

        if (organizationLocationOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Organization Location with publicId " + organizationLocationPublicId + " not found")
                    .userMessage("Sorry. Organization Location with publicId " + organizationLocationPublicId + " not found");
        }

        Optional<OrganizationRegions> organizationRegionOptional = organizationRegionsRepository
                .findByPublicIdAndDeletedIsFalse(organizationLocationsRequest.getOrganizationRegionId());

        if (organizationRegionOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Organization Region with public ID " + organizationLocationsRequest.getOrganizationRegionId() + " not found")
                    .userMessage("Sorry. Organization Region not found for public ID " + organizationLocationsRequest.getOrganizationRegionId());
        }

        OrganizationLocations organizationLocation = organizationLocationOptional.get();
        organizationLocation.setCode(organizationLocationsRequest.getCode());
        organizationLocation.setName(organizationLocationsRequest.getName());
        organizationLocation.setOrganizationRegion(organizationRegionOptional.get());
        organizationLocation.setLatitude(organizationLocationsRequest.getLatitude());
        organizationLocation.setLongitude(organizationLocationsRequest.getLongitude());
        personOptional.ifPresent(organizationLocation::setUpdatedBy);
        organizationLocation.setUpdateDate(LocalDateTime.now());

        organizationLocationsRepository.save(organizationLocation);
    }

    @Override
    public OrganizationLocations getOrganizationLocation(UUID organizationLocationPublicId) {

        Optional<OrganizationLocations> organizationLocationOptional = organizationLocationsRepository
                .findByPublicIdAndDeletedIsFalse(organizationLocationPublicId);

        if (organizationLocationOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Organization Location with publicId " + organizationLocationPublicId + " not found")
                    .userMessage("Sorry. Organization Location with publicId " + organizationLocationPublicId + " not found");
        }

        return organizationLocationOptional.get();
    }

    @Override
    public Page<OrganizationLocations> getAllOrganizationLocations(String searchParam,
                                                                   String name,
                                                                   UUID organizationRegionId,
                                                                   Pageable pageable) {

        Specification<OrganizationLocations> spec = Specification
                .where(OrganizationLocationSpecification.searchParamMatches(searchParam))
                .and(OrganizationLocationSpecification.hasNameLike(name))
                .and(OrganizationLocationSpecification.hasOrganizationRegion(organizationRegionId))
                .and(OrganizationLocationSpecification.isNotDeleted());

        return organizationLocationsRepository.findAll(spec, pageable);
    }

    @Override
    public void deleteOrganizationLocation(UUID organizationLocationPublicId, String name) {

        Optional<Person> personOptional = personRepository.findByEmailAndDeletedIsFalse(name);
        Optional<OrganizationLocations> organizationLocationOptional = organizationLocationsRepository
                .findByPublicIdAndDeletedIsFalse(organizationLocationPublicId);

        if (organizationLocationOptional.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Organization Location with publicId " + organizationLocationPublicId + " not found")
                    .userMessage("Sorry. Organization Location with publicId " + organizationLocationPublicId + " not found");
        }

        OrganizationLocations organizationLocation = organizationLocationOptional.get();
        personOptional.ifPresent(organizationLocation::setUpdatedBy);
        organizationLocation.setDeleted(true);
        organizationLocation.setUpdateDate(LocalDateTime.now());

        organizationLocationsRepository.save(organizationLocation);
    }
}
