package com.app.itibari.models.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
@ToString
public class SmsGroupExcelDto {
    private String name;
    private String phone;
    private Map<String, String> errorList = new HashMap<>();
    private Integer rowIndex;
}
