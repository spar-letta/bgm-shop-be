package com.app.itibari.services;

import com.app.itibari.models.entities.CallStatus;
import com.app.itibari.models.request.CallStatusRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CallStatusService {

    CallStatus createCallStatus(CallStatusRequest request, String principalName);

    void updateCallStatus(UUID publicId, CallStatusRequest request, String principalName);

    void deactivateCallStatus(UUID publicId, String principalName);

    void activateCallStatus(UUID publicId, String principalName);

    Page<CallStatus> getCallStatuses(String searchParam, Boolean active, Pageable pageable);
}
