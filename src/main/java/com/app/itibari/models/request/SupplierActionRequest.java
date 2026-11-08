package com.app.itibari.models.request;

import com.app.itibari.dataTypes.ActionType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class SupplierActionRequest {

    private List<UUID> productPublicIds;

    private ActionType actionType;
}
