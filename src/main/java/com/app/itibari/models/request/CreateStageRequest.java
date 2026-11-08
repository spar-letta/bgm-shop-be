package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateStageRequest {

    private List<StageReturnsRequest> stageReturnsRequest;
}
