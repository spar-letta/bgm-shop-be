package com.app.itibari.services;

import com.app.itibari.dataTypes.ActionType;
import com.app.itibari.models.entities.OrganizationRegions;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.RegionWarehouseMapping;
import com.app.itibari.models.entities.Warehouse;
import com.app.itibari.models.request.RegionMappingUpdateRequest;
import com.app.itibari.models.request.RegionWarehouseMappingRequest;
import com.app.itibari.repository.OrganizationRegionsRepository;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.repository.RegionWarehouseMappingRepository;
import com.app.itibari.repository.WarehouseRepository;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.helpers.Validators;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegionWarehouseMappingServiceImpl implements RegionWarehouseMappingService {

    private final RegionWarehouseMappingRepository regionWarehouseMappingRepository;

    private final PersonRepository personRepository;

    private final WarehouseRepository warehouseRepository;

    private final OrganizationRegionsRepository organizationRegionsRepository;

    @Override
    public RegionWarehouseMapping createRegionWarehouseMapping(RegionWarehouseMappingRequest regionWarehouseMappingRequest,
                                                               String createdBy) {

        Optional<Person> optionalPerson = personRepository.findByEmailAndDeletedIsFalse(createdBy);

        if (optionalPerson.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Person with email " + createdBy + " does not exist or is deleted.")
                    .userMessage("Invalid user credentials. Please check your email and try again.");
        }

        Optional<Warehouse> optionalWarehouse = warehouseRepository.findByPublicIdAndDeletedIsFalse(regionWarehouseMappingRequest.getWarehousePublicId());
        if (optionalWarehouse.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Warehouse with public ID " + regionWarehouseMappingRequest.getWarehousePublicId() + " does not exist or is deleted.")
                    .userMessage("Invalid warehouse. Please check the warehouse ID and try again.");
        }

        if (Validators.anyEqualNull(regionWarehouseMappingRequest.getRegionPublicIds())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Region public IDs cannot be null or empty.")
                    .userMessage("Please provide valid region IDs.");
        }

        if (regionWarehouseMappingRequest.getRegionPublicIds().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Region public IDs cannot be empty.")
                    .userMessage("Please provide at least one region ID.");
        }

        Warehouse warehouse = optionalWarehouse.get();

        RegionWarehouseMapping regionWarehouseMapping;

        Optional<RegionWarehouseMapping> existingMapping = regionWarehouseMappingRepository
                .findByWarehouseAndDeletedIsFalse(warehouse);

        if (existingMapping.isPresent()) {
            regionWarehouseMapping = existingMapping.get();
            regionWarehouseMapping.setUpdateDate(LocalDateTime.now());
            regionWarehouseMapping.setUpdatedBy(optionalPerson.get());
        } else {
            regionWarehouseMapping = new RegionWarehouseMapping();
            regionWarehouseMapping.setWarehouse(warehouse);
            regionWarehouseMapping.setCreatedBy(optionalPerson.get());
            regionWarehouseMapping.setCreationTime(LocalDateTime.now());
        }

        RegionWarehouseMapping savedRegionWarehouseMapping = regionWarehouseMappingRepository.save(regionWarehouseMapping);

        regionWarehouseMappingRequest.getRegionPublicIds().forEach(regionPublicId -> {
            Optional<OrganizationRegions> optionalRegion = organizationRegionsRepository.findByPublicIdAndDeletedIsFalse(regionPublicId);
            if (optionalRegion.isPresent()) {
                OrganizationRegions region = optionalRegion.get();
                // check if the region is already mapped
                if (region.getRegionWarehouseMapping() != null) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Region with public ID " + regionPublicId + " is already mapped to a warehouse.")
                            .userMessage(String.format("Sorry, the region  %s is already associated with a warehouse.", region.getName()));
                }

                savedRegionWarehouseMapping.addRegion(region);
                region.setRegionWarehouseMapping(savedRegionWarehouseMapping);
                organizationRegionsRepository.save(region);
                log.info("Region with public ID {} successfully mapped to warehouse with public ID {}.",
                         regionPublicId,
                         warehouse.getPublicId());
            } else {
                log.warn("Region with public ID {} not found or is deleted.", regionPublicId);
            }
        });

        RegionWarehouseMapping updatedRegionWarehouseMapping = regionWarehouseMappingRepository.save(savedRegionWarehouseMapping);

        if (updatedRegionWarehouseMapping.getMappedRegions().isEmpty()) {
            // delete the mapping if no regions are mapped
            regionWarehouseMappingRepository.delete(updatedRegionWarehouseMapping);
        }

        return updatedRegionWarehouseMapping;
    }

    @Override
    public void updateRegionWarehouseMapping(UUID publicId, RegionMappingUpdateRequest regionWarehouseMappingRequest, String name) {

        Optional<RegionWarehouseMapping> optionalRegionWarehouseMapping = regionWarehouseMappingRepository.findByPublicIdAndDeletedIsFalse(
                publicId);
        if (optionalRegionWarehouseMapping.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Region warehouse mapping with public ID " + publicId + " does not exist or is deleted.")
                    .userMessage("Invalid region warehouse mapping. Please check the ID and try again.");
        }

        RegionWarehouseMapping regionWarehouseMapping = optionalRegionWarehouseMapping.get();
        regionWarehouseMapping.setUpdateDate(LocalDateTime.now());
        regionWarehouseMapping.setUpdatedBy(personRepository.findByEmailAndDeletedIsFalse(name).orElseThrow(() ->
                                                                                                                    new BadRequestRestApiException()
                                                                                                                            .developerMessage(
                                                                                                                                    "Person with email " + name + " does not exist or is deleted.")
                                                                                                                            .userMessage(
                                                                                                                                    "Invalid user credentials. Please check your email and try again.")));

        if (Validators.anyEqualNull(regionWarehouseMappingRequest.getRegionPublicIds())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Region public IDs cannot be null or empty.")
                    .userMessage("Please provide valid region IDs.");
        }

        if (regionWarehouseMappingRequest.getRegionPublicIds().isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Region public IDs cannot be empty.")
                    .userMessage("Please provide at least one region ID.");
        }

        regionWarehouseMappingRequest.getRegionPublicIds().forEach(regionPublicId -> {
            Optional<OrganizationRegions> optionalRegion = organizationRegionsRepository.findByPublicIdAndDeletedIsFalse(regionPublicId);
            if (optionalRegion.isPresent()) {
                OrganizationRegions region = optionalRegion.get();
                // check if the region is already mapped
                if (regionWarehouseMappingRequest.getActionType().equals(ActionType.ADD)) {
                    if (region.getRegionWarehouseMapping() != null && !region.getRegionWarehouseMapping().getPublicId().equals(publicId)) {
                        throw new BadRequestRestApiException()
                                .developerMessage("Region with public ID " + regionPublicId + " is already mapped to a different warehouse.")
                                .userMessage(String.format("Sorry, the region  %s is already associated with a different warehouse.",
                                                           region.getName()));
                    }
                    regionWarehouseMapping.addRegion(region);
                    region.setRegionWarehouseMapping(regionWarehouseMapping);
                    organizationRegionsRepository.save(region);
                } else if (regionWarehouseMappingRequest.getActionType().equals(ActionType.REMOVE)) {
                    regionWarehouseMapping.removeRegion(region);
                    region.setRegionWarehouseMapping(null);
                    organizationRegionsRepository.save(region);
                    log.info("Region with public ID {} successfully unmapped from warehouse with public ID {}.",
                             regionPublicId,
                             regionWarehouseMapping.getWarehouse().getPublicId());
                }
            }
        });
        regionWarehouseMappingRepository.save(regionWarehouseMapping);
    }

    @Override
    public RegionWarehouseMapping getRegionWarehouseMappingByPublicId(UUID publicId, String name) {

        Optional<RegionWarehouseMapping> optionalRegionWarehouseMapping = regionWarehouseMappingRepository.findByPublicIdAndDeletedIsFalse(
                publicId);
        if (optionalRegionWarehouseMapping.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Region warehouse mapping with public ID " + publicId + " does not exist or is deleted.")
                    .userMessage("Invalid region warehouse mapping. Please check the ID and try again.");
        }

        RegionWarehouseMapping regionWarehouseMapping = optionalRegionWarehouseMapping.get();
        Person person = personRepository.findByEmailAndDeletedIsFalse(name)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Person with email " + name + " does not exist or is deleted.")
                        .userMessage("Invalid user credentials. Please check your email and try again."));

        regionWarehouseMapping.setUpdatedBy(person);
        return regionWarehouseMapping;
    }

    @Override
    public Page<RegionWarehouseMapping> getAllRegionWarehouseMappings(Pageable pageable, String name) {

        Person person = personRepository.findByEmailAndDeletedIsFalse(name)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Person with email " + name + " does not exist or is deleted.")
                        .userMessage("Invalid user credentials. Please check your email and try again."));

        return regionWarehouseMappingRepository.findAllByDeletedIsFalse(pageable);
    }

    @Override
    public void deleteRegionWarehouseMapping(UUID publicId, String name) {

        Optional<RegionWarehouseMapping> optionalRegionWarehouseMapping = regionWarehouseMappingRepository.findByPublicIdAndDeletedIsFalse(
                publicId);
        if (optionalRegionWarehouseMapping.isEmpty()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Region warehouse mapping with public ID " + publicId + " does not exist or is deleted.")
                    .userMessage("Invalid region warehouse mapping. Please check the ID and try again.");
        }

        RegionWarehouseMapping regionWarehouseMapping = optionalRegionWarehouseMapping.get();
        Person person = personRepository.findByEmailAndDeletedIsFalse(name)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Person with email " + name + " does not exist or is deleted.")
                        .userMessage("Invalid user credentials. Please check your email and try again."));

        regionWarehouseMapping.setDeleted(true);
        regionWarehouseMapping.setUpdateDate(LocalDateTime.now());
        regionWarehouseMapping.setUpdatedBy(person);

        regionWarehouseMappingRepository.save(regionWarehouseMapping);
        log.info("Region warehouse mapping with public ID {} successfully deleted by user {}.", publicId, name);
        // Optionally, you can also delete the regions associated with this mapping if needed
        List<OrganizationRegions> mappedRegions = new ArrayList<>(regionWarehouseMapping.getMappedRegions());
        for (OrganizationRegions region : mappedRegions) {
            region.setRegionWarehouseMapping(null);
            organizationRegionsRepository.save(region);
            log.info("Region with public ID {} successfully unmapped from warehouse with public ID {}.",
                     region.getPublicId(),
                     regionWarehouseMapping.getWarehouse().getPublicId());
        }
        regionWarehouseMapping.getMappedRegions().clear(); // Clear the list to avoid any further associations
        regionWarehouseMappingRepository.save(regionWarehouseMapping); // Save the changes to the mapping
        log.info("All regions associated with the region warehouse mapping with public ID {} have been unmapped.", publicId);
    }
}
