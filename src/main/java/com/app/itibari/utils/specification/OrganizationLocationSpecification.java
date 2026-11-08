package com.app.itibari.utils.specification;

import com.app.itibari.models.entities.OrganizationLocations;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

public class OrganizationLocationSpecification {

    public static Specification<OrganizationLocations> hasNameLike(String name) {

        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) {
                return null;
            }
            return cb.like(cb.upper(root.get("name")), "%" + name.toUpperCase() + "%");
        };
    }

    public static Specification<OrganizationLocations> hasOrganizationRegion(UUID organizationRegionId) {

        return (root, query, cb) -> {
            if (organizationRegionId == null) {
                return null;
            }
            return cb.equal(root.get("organizationRegion").get("publicId"), organizationRegionId);
        };
    }

    public static Specification<OrganizationLocations> hasOrganizationRegionsIn(List<UUID> organizationRegionIds) {

        return (root, query, cb) -> {
            if (organizationRegionIds == null || organizationRegionIds.isEmpty()) {
                return null;
            }
            return root.get("organizationRegion").get("publicId").in(organizationRegionIds);
        };
    }

    public static Specification<OrganizationLocations> searchParamMatches(String searchParam) {

        return (root, query, cb) -> {
            if (searchParam == null || searchParam.trim().length() < 3) {
                return null;
            }
            String searchParamUpper = "%" + searchParam.toUpperCase() + "%";
            return cb.or(
                    cb.like(cb.upper(root.get("name")), searchParamUpper),
                    cb.like(cb.upper(root.get("code")), searchParamUpper)
            );
        };
    }

    public static Specification<OrganizationLocations> isNotDeleted() {

        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }
}
