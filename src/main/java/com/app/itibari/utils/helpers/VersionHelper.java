package com.app.itibari.utils.helpers;

import com.app.itibari.dataTypes.MobileUserType;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import jakarta.validation.constraints.NotEmpty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class VersionHelper {

    /**
     * Checks if the provided app version is valid for the given mobile user type.
     * If the app version is empty, it logs a warning.
     * If the app version is not empty, it can perform additional checks based on the mobile user type.
     *
     * @param appVersion      The version of the app to check.
     * @param mobileUserType  The type of mobile user (e.g., DeliveryAgent).
     */
    @Value("${da.app.version}")
    private String appVersion;

    @Value("${sa.app.version}")
    private String saAppVersion;

    public void checkIfAppVersionIsValid(@NotEmpty String version, @NotEmpty MobileUserType mobileUserType) {

        if (mobileUserType == MobileUserType.DeliveryAgent) {
            if (!appVersion.equalsIgnoreCase(version)) {
                log.warn("++++++++++++++Invalid app version for Delivery Agent: expected {}, got {}+++++++++++++++++++",
                         appVersion,
                         version);

                throw new BadRequestRestApiException()
                        .developerMessage("Invalid app version. Please update the app to the latest version.")
                        .userMessage("Invalid app version. Please update the app to the latest version.");
            }
        } else if (mobileUserType == MobileUserType.SalesAgent) {
            if (!saAppVersion.equalsIgnoreCase(version)) {
                log.warn("++++++++++++++Invalid app version for Sales Agent: expected {}, got {}+++++++++++++++++++",
                         saAppVersion,
                         version);

                throw new BadRequestRestApiException()
                        .developerMessage("Invalid app version. Please update the app to the latest version.")
                        .userMessage("Invalid app version. Please update the app to the latest version.");
            }
        }
    }
}
