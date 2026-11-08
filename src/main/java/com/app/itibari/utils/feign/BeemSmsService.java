package com.app.itibari.utils.feign;

import com.app.itibari.models.request.BeemRequest;
import com.app.itibari.models.response.BeemResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "BeemSmsService", url = "https://apisms.beem.africa/v1")
public interface BeemSmsService {

    @PostMapping(value = "/send", consumes = "application/json", produces = "application/json")
    BeemResponse sendBeemText(@RequestHeader("Authorization") String authorization, @RequestBody BeemRequest request);
}
