package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryDTO {

    private String name;

    private String description;

    public CategoryDTO() {

    }

    public CategoryDTO(String name, String description) {

        this.name = name;
        this.description = description;
    }
}
