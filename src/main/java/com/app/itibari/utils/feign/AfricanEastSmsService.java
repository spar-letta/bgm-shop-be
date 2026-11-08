package com.app.itibari.utils.feign;

import com.app.itibari.models.request.SmsRequest;
import com.app.itibari.models.response.SmsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "AfricanEastSmsService", url = "https://messaging-service.co.tz/api/sms/v1")
public interface AfricanEastSmsService {

    @PostMapping(value = "/text/single", consumes = "application/json", produces = "application/json")
    SmsResponse sendSMS(
            @RequestHeader("Authorization") String authorization,
            @RequestBody SmsRequest request
    );
}
