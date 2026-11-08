package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class UpdateVanSales {

    private List<ProductItemUpdate> productItemRequestList = new ArrayList<>();
}
