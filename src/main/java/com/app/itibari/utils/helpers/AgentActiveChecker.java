package com.app.itibari.utils.helpers;

import com.app.itibari.dataTypes.AgentType;
import com.app.itibari.models.entities.SalesAgents;
import com.app.itibari.repository.SalesAgentsRepository;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class AgentActiveChecker {

    private final SalesAgentsRepository salesAgentsRepository;

    public AgentActiveChecker(SalesAgentsRepository salesAgentsRepository) {

        this.salesAgentsRepository = salesAgentsRepository;
    }

    public void checkIfAgentIsActive(UUID agentPublicId, AgentType agentType) {

        if (agentType == AgentType.SALES_AGENT) {
            Optional<SalesAgents> salesAgentOptional = salesAgentsRepository
                    .findByPublicIdAndDeletedIsFalse(agentPublicId);

            if (salesAgentOptional.isPresent()) {
                SalesAgents salesAgent = salesAgentOptional.get();
                if (!salesAgent.isActive()) {
                    log.error("Sales agent with publicId: {} is not active", agentPublicId);
                    throw new BadRequestRestApiException()
                            .developerMessage("Sales agent with publicId: " + agentPublicId + " is not active.")
                            .userMessage("Sorry, the sales agent not active. Please contact support.");
                }
            } else {
                log.error("Sales agent with publicId: {} not found", agentPublicId);
                throw new BadRequestRestApiException()
                        .developerMessage("Sales agent with publicId: " + agentPublicId + " not found.")
                        .userMessage("Sorry, the sales agent not found. Please contact support.");
            }
        } else {
            // checked later
        }
    }
}
