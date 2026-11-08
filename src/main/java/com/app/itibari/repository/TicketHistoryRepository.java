package com.app.itibari.repository;

import com.app.itibari.models.entities.HelpDesk;
import com.app.itibari.models.entities.TicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {
    List<TicketHistory> findByHelpDeskOrderByActionedTimeDesc(HelpDesk helpDesk);
}
