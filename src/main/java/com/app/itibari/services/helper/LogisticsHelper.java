package com.app.itibari.services.helper;

import com.app.itibari.models.entities.DeliveryAgents;
import com.app.itibari.models.entities.Reconciliation;
import com.app.itibari.models.entities.Vehicle;
import com.app.itibari.repository.DeliveryAgentsRepository;
import com.app.itibari.repository.ReconciliationRepository;
import com.app.itibari.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Created by christopher oigo (oigoc60@gmail.com) on May 2025
 */
@Component
@RequiredArgsConstructor
public class LogisticsHelper {

    private final DeliveryAgentsRepository deliveryAgentsRepository;

    private final VehicleRepository vehicleRepository;

    private final ReconciliationRepository reconciliationRepository;

    public void updateAgentAvailability(UUID agentId, Boolean isAvailable) {

        Optional<DeliveryAgents> agentOptional = deliveryAgentsRepository.findByPublicIdAndDeletedIsFalse(agentId);

        if (agentOptional.isPresent()) {
            DeliveryAgents agent = agentOptional.get();
            agent.setAvailable(isAvailable);
            agent.setLoaded(false);
            deliveryAgentsRepository.save(agent);
        }
    }

    // vehicle availability helper

    public void updateVehicleAvailability(UUID vehicleId, Boolean isAvailable) {

        Optional<Vehicle> vehicleOptional = vehicleRepository.findByPublicIdAndDeletedIsFalse(vehicleId);

        if (vehicleOptional.isPresent()) {
            Vehicle vehicle = vehicleOptional.get();
            vehicle.setAvailable(isAvailable);
            vehicleRepository.save(vehicle);
        }
    }

    public void reconUpdater(UUID reconciliationId, String deliveryStatus) {

        Optional<Reconciliation> optionalReconciliation = reconciliationRepository.findByPublicIdAndDeletedIsFalse(reconciliationId);
        if (optionalReconciliation.isPresent()) {
            Reconciliation reconciliation = optionalReconciliation.get();
            reconciliation.setDeliveryStatus(deliveryStatus);
            reconciliationRepository.save(reconciliation);
        }
    }
}
