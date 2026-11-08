package com.app.itibari.models.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BnplLeadPageable {

    private long totalElements;
    private long pageSize;
    private long totalPages;
    private boolean last;
    private boolean first;
    private long pageNumber;
    private List<BnplLeadResponse> content;
}