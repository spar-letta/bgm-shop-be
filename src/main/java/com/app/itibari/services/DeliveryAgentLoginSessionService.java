package com.app.itibari.services;

import com.app.itibari.models.entities.DeliveryAgentLoginSession;
import com.app.itibari.models.entities.DeliveryAgents;
import com.app.itibari.models.response.DeliveryAgentLoginSessionResponse;
import com.app.itibari.repository.DeliveryAgentLoginSessionRepository;
import com.app.itibari.repository.DeliveryAgentsRepository;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.exceptions.rest.UnauthorizedRestApiException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryAgentLoginSessionService {

    public static final long SESSION_DAYS = 30;
    private final DeliveryAgentLoginSessionRepository sessionRepository;
    private final DeliveryAgentsRepository deliveryAgentsRepository;

    @Transactional
    public DeliveryAgentLoginSession openSession(DeliveryAgents agent, String deviceId, String deviceName,
                                                  String deviceModel, String platform, String appVersion) {
        if (StringUtils.isBlank(deviceId)) {
            throw new BadRequestRestApiException().developerMessage("Device ID is required for delivery-agent login")
                    .userMessage("Device ID is required");
        }

        DeliveryAgents lockedAgent = deliveryAgentsRepository.findByIdForUpdate(agent.getId()).orElseThrow();
        LocalDateTime now = LocalDateTime.now();
        sessionRepository.findFirstByDeliveryAgentAndActiveTrueAndDeletedIsFalse(lockedAgent).ifPresent(existing -> {
            if (existing.getExpiresAt().isBefore(now)) {
                close(existing, "EXPIRED", now);
            } else if (!existing.getDeviceId().equals(deviceId)) {
                close(existing, "LOGIN_FROM_ANOTHER_DEVICE", now);
            } else {
                close(existing, "RELOGIN_SAME_DEVICE", now);
            }
            // Hibernate can order the replacement INSERT before this UPDATE. Flush the
            // deactivation first so PostgreSQL's partial unique index permits the new row.
            sessionRepository.flush();
        });

        DeliveryAgentLoginSession session = new DeliveryAgentLoginSession();
        session.setDeliveryAgent(lockedAgent);
        session.setDeviceId(deviceId.trim());
        session.setDeviceName(deviceName);
        session.setDeviceModel(deviceModel);
        session.setPlatform(platform);
        session.setAppVersion(appVersion);
        session.setLoginTime(now);
        session.setLastActivityTime(now);
        session.setExpiresAt(now.plusDays(SESSION_DAYS));
        session.setActive(true);
        return sessionRepository.saveAndFlush(session);
    }

    @Transactional
    public DeliveryAgentLoginSession validate(UUID sessionId, String phoneNumber) {
        DeliveryAgentLoginSession session = sessionRepository.findByPublicIdAndActiveTrueAndDeletedIsFalse(sessionId)
                .orElseThrow(() -> new UnauthorizedRestApiException().developerMessage("Delivery-agent session is inactive")
                        .userMessage("Your login session has ended. Please log in again"));
        if (!session.getDeliveryAgent().getPhoneNumber().equals(phoneNumber)
                || session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedRestApiException().developerMessage("Invalid or expired delivery-agent session")
                    .userMessage("Your login session has expired. Please log in again");
        }
        LocalDateTime now = LocalDateTime.now();
        if (session.getLastActivityTime() == null || session.getLastActivityTime().isBefore(now.minusMinutes(5))) {
            session.setLastActivityTime(now);
            sessionRepository.save(session);
        }
        return session;
    }

    @Transactional
    public void logout(UUID sessionId, String phoneNumber) {
        DeliveryAgentLoginSession session = validate(sessionId, phoneNumber);
        close(session, "USER_LOGOUT", LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public Page<DeliveryAgentLoginSessionResponse> report(UUID agentPublicId, Boolean active,
                                                          LocalDateTime from, LocalDateTime to, Pageable pageable) {
        Specification<DeliveryAgentLoginSession> spec = (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.isFalse(root.get("deleted")));
            if (agentPublicId != null) predicates.add(cb.equal(root.get("deliveryAgent").get("publicId"), agentPublicId));
            if (active != null) predicates.add(cb.equal(root.get("active"), active));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("loginTime"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("loginTime"), to));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return sessionRepository.findAll(spec, pageable).map(this::toResponse);
    }

    private void close(DeliveryAgentLoginSession session, String reason, LocalDateTime now) {
        session.setActive(false);
        session.setLogoutTime(now);
        session.setLogoutReason(reason);
        sessionRepository.save(session);
    }

    private DeliveryAgentLoginSessionResponse toResponse(DeliveryAgentLoginSession s) {
        DeliveryAgents a = s.getDeliveryAgent();
        return new DeliveryAgentLoginSessionResponse(s.getPublicId(), a.getPublicId(), a.getFullName(), a.getPhoneNumber(),
                s.getDeviceId(), s.getDeviceName(), s.getDeviceModel(), s.getPlatform(), s.getAppVersion(), s.getLoginTime(),
                s.getLastActivityTime(), s.getLogoutTime(), s.getExpiresAt(), s.isActive(), s.getLogoutReason());
    }
}
