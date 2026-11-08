package com.app.itibari.services.supplierportal;

import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Parses supplier-portal geography query params that may arrive as bracketed arrays,
 * comma-separated, repeated, or mixed {@code regionId}/{@code locationId} values.
 */
public final class SupplierPortalGeographyQueryParams {

    private SupplierPortalGeographyQueryParams() {
    }

    public static List<UUID> parseUuidList(MultiValueMap<String, String> queryParams, String paramName) {
        if (queryParams == null || queryParams.isEmpty()) {
            return List.of();
        }
        List<String> rawValues = new ArrayList<>();
        List<String> normalValues = queryParams.get(paramName);
        if (normalValues != null) {
            rawValues.addAll(normalValues);
        }
        List<String> arrayKeyValues = queryParams.get(paramName + "[]");
        if (arrayKeyValues != null) {
            rawValues.addAll(arrayKeyValues);
        }
        return parseUuidList(rawValues, paramName);
    }

    public static List<UUID> parseUuidList(List<String> rawValues, String paramName) {
        if (rawValues == null || rawValues.isEmpty()) {
            return List.of();
        }
        Set<UUID> seen = new LinkedHashSet<>();
        for (String raw : rawValues) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String normalized = raw.trim();
            if (normalized.startsWith("[") && normalized.endsWith("]")) {
                normalized = normalized.substring(1, normalized.length() - 1);
            }
            for (String token : normalized.split(",")) {
                if (token.isBlank()) {
                    continue;
                }
                try {
                    seen.add(UUID.fromString(token.trim()));
                } catch (IllegalArgumentException ex) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Invalid " + paramName + ": " + token.trim())
                            .userMessage("Invalid filter");
                }
            }
        }
        return List.copyOf(seen);
    }
}
