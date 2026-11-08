package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class LoadingRequest {

    private List<LoadingDTO> loadingDTOList = new ArrayList<>();
}
