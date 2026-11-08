package com.app.itibari.models.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SupplierDashboardAnalysis {

    private long totalElements;

    private long pageSize;

    private long totalPages;

    private boolean last;

    private boolean first;

    private long pageNumber;

    private List<ProductAnalysis> productAnalysis;
}
