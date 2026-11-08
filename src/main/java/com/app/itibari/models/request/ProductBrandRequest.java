package com.app.itibari.models.request;

import jakarta.validation.constraints.NotEmpty;

public record ProductBrandRequest(@NotEmpty String name) {
}
