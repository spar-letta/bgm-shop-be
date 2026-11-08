package com.app.itibari.repository;

import com.app.itibari.models.entities.DeliveryAgentLoginSession;
import com.app.itibari.models.entities.DeliveryAgents;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryAgentLoginSessionRepository extends JpaRepository<DeliveryAgentLoginSession, Long>,
        JpaSpecificationExecutor<DeliveryAgentLoginSession> {

    Optional<DeliveryAgentLoginSession> findFirstByDeliveryAgentAndActiveTrueAndDeletedIsFalse(DeliveryAgents deliveryAgent);

    Optional<DeliveryAgentLoginSession> findByPublicIdAndActiveTrueAndDeletedIsFalse(UUID publicId);
}
