package com.app.itibari.services;

import com.app.itibari.dataTypes.Module;
import com.app.itibari.models.entities.RejectionsCode;
import com.app.itibari.models.request.RejectionCodeRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RejectionCodeService {

    RejectionsCode createRejectionCode(RejectionCodeRequest rejectionCodeRequest, String name);

    void updateRejectionCode(UUID publicId, RejectionCodeRequest rejectionCodeRequest, String name);

    void deleteRejectionCode(UUID publicId, String name);

    Page<RejectionsCode> getRejectionCodes(String searchParam, Module module,UUID  departmentPublicId, Pageable pageable);
}
