package com.app.itibari.services;

import com.app.itibari.models.entities.CallStatus;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.request.CallStatusRequest;
import com.app.itibari.repository.CallStatusRepository;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.specification.CallStatusSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CallStatusServiceImpl implements CallStatusService {

    private final CallStatusRepository callStatusRepository;
    private final PersonRepository personRepository;

    public CallStatusServiceImpl(CallStatusRepository callStatusRepository, PersonRepository personRepository) {
        this.callStatusRepository = callStatusRepository;
        this.personRepository = personRepository;
    }

    @Override
    public CallStatus createCallStatus(CallStatusRequest request, String principalName) {
        if (callStatusRepository.findByCodeIgnoreCaseAndDeletedIsFalse(request.getCode()).isPresent()) {
            throw new BadRequestRestApiException()
                    .developerMessage("Call status code already exists: " + request.getCode())
                    .userMessage("A call status with this code already exists");
        }

        CallStatus callStatus = new CallStatus();
        callStatus.setCode(request.getCode());
        callStatus.setDescription(request.getDescription());
        callStatus.setActive(Boolean.TRUE);
        personRepository.findByEmailAndDeletedIsFalse(principalName)
                .ifPresent(callStatus::setCreatedBy);

        return callStatusRepository.save(callStatus);
    }

    @Override
    public void updateCallStatus(UUID publicId, CallStatusRequest request, String principalName) {
        CallStatus callStatus = findActiveOrThrow(publicId);

        callStatusRepository.findByCodeIgnoreCaseAndDeletedIsFalse(request.getCode())
                .filter(existing -> !existing.getPublicId().equals(publicId))
                .ifPresent(existing -> {
                    throw new BadRequestRestApiException()
                            .developerMessage("Call status code already exists: " + request.getCode())
                            .userMessage("A call status with this code already exists");
                });

        callStatus.setCode(request.getCode());
        callStatus.setDescription(request.getDescription());
        personRepository.findByEmailAndDeletedIsFalse(principalName)
                .ifPresent(callStatus::setUpdatedBy);

        callStatusRepository.save(callStatus);
    }

    @Override
    public void deactivateCallStatus(UUID publicId, String principalName) {
        CallStatus callStatus = findActiveOrThrow(publicId);

        callStatus.setActive(Boolean.FALSE);
        personRepository.findByEmailAndDeletedIsFalse(principalName)
                .ifPresent(callStatus::setUpdatedBy);

        callStatusRepository.save(callStatus);
    }

    @Override
    public void activateCallStatus(UUID publicId, String principalName) {
        CallStatus callStatus = callStatusRepository.findByPublicIdAndDeletedIsFalse(publicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Call status not found: " + publicId)
                        .userMessage("Call status not found"));

        callStatus.setActive(Boolean.TRUE);
        personRepository.findByEmailAndDeletedIsFalse(principalName)
                .ifPresent(callStatus::setUpdatedBy);

        callStatusRepository.save(callStatus);
    }

    @Override
    public Page<CallStatus> getCallStatuses(String searchParam, Boolean active, Pageable pageable) {
        Specification<CallStatus> spec = Specification
                .where(CallStatusSpecification.isNotDeleted())
                .and(CallStatusSpecification.isActive(active))
                .and(CallStatusSpecification.searchParamMatches(searchParam));

        return callStatusRepository.findAll(spec, pageable);
    }

    private CallStatus findActiveOrThrow(UUID publicId) {
        return callStatusRepository.findByPublicIdAndDeletedIsFalse(publicId)
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Call status not found: " + publicId)
                        .userMessage("Call status not found"));
    }
}
