package com.app.itibari.models.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerOrderPageable {

    private long totalElements;

    private long pageSize;

    private long totalPages;

    private boolean last;

    private boolean first;

    private long pageNumber;

    private List<CustomerOrderDTO> content;
}
