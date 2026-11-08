package com.app.itibari.repository;

import com.app.itibari.models.entities.HelpDesk;
import com.app.itibari.models.entities.HelpDeskComment;
import com.app.itibari.models.entities.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface HelpDeskCommentRepository extends JpaRepository<HelpDeskComment, Long> {
    List<HelpDeskComment> findAllByHelpDesk(HelpDesk helpDesk);

    @Query("select c from HelpDeskComment c where c.deleted = false and c.helpDesk = ?1 and c.createdBy <> ?2")
    List<HelpDeskComment> findAllByHelpDeskAndNotCreator(HelpDesk helpDesk, Person loggedIn);
}
