package com.app.itibari.repository;

import com.app.itibari.models.entities.HelpDesk;
import com.app.itibari.models.entities.TicketViewer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketViewerRepository extends JpaRepository<TicketViewer, Long> {
    List<TicketViewer> findByHelpDeskOrderByViewedAtDesc(HelpDesk helpDesk);
}
