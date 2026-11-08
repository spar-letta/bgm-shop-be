package com.app.itibari.converter;

import com.app.itibari.models.entities.Department;
import com.app.itibari.models.response.DepartmentDtoResponse;
import org.springframework.stereotype.Component;

@Component
public class DepartmentToDtoResponse implements Converter<Department, DepartmentDtoResponse> {

    @Override
    public DepartmentDtoResponse convert(Department department) {

        if (department == null){
            return null;
        }

        return DepartmentDtoResponse.builder()
                .name(department.getName())
                .publicId(department.getPublicId())
                .build();
    }
}
