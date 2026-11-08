package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PriceManagementRequest {

    private List<PriceManagementRequestItem> items = new ArrayList<>();
}
