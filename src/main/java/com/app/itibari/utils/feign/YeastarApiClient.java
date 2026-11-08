package com.app.itibari.utils.feign;

import com.app.itibari.configuration.FeignLoggingConfiguration;
import com.app.itibari.models.request.YeastarCdrRequest;
import com.app.itibari.models.request.YeastarOutboundCallRequest;
import com.app.itibari.models.request.YeastarTokenRequest;
import com.app.itibari.models.response.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "YeastarApiClient", url = "${yeastar.pbx.url}", configuration = FeignLoggingConfiguration.class)
public interface YeastarApiClient {

    @PostMapping(value = "openapi/v1.0/get_token", consumes = "application/json", produces = "application/json")
    YeastarTokenResponse getToken(@RequestBody YeastarTokenRequest request);

    @PostMapping(value = "openapi/v1.0/get_token", consumes = "application/json", produces = "application/json")
    YeastarTokenResponse refreshToken(@RequestBody YeastarTokenRequest request);

    @PostMapping(value = "/openapi/v1.0/call/dial", consumes = "application/json", produces = "application/json")
    YeastarApiResponse dial(@RequestParam("access_token") String token,
                            @RequestBody YeastarOutboundCallRequest request);

    @PostMapping(value = "/openapi/v1.0/call/hangup", consumes = "application/json", produces = "application/json")
    YeastarApiResponse hangup(@RequestParam("access_token") String token,
                              @RequestBody Map<String, String> body);

    @PostMapping(value = "/openapi/v1.0/call/transfer", consumes = "application/json", produces = "application/json")
    YeastarApiResponse transfer(@RequestParam("access_token") String token,
                                @RequestBody Map<String, String> body);

    @PostMapping(value = "/openapi/v1.0/cdr/search", consumes = "application/json", produces = "application/json")
    YeastarCdrResponse searchCdr(@RequestParam("access_token") String token,
                                 @RequestBody YeastarCdrRequest request);

    @GetMapping(value = "/openapi/v1.0/extension/list", produces = "application/json")
    YeastarExtensionListResponse listExtensions(@RequestParam("access_token") String token);
}
