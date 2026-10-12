package net.ent.etnc.orbit.services;

import net.ent.etnc.orbit.models.entities.Defaillance;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.enums.StatutDefaillance;
import net.ent.etnc.orbit.services.commons.Service;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DefaillanceService extends Service<Defaillance, Long> {

    Defaillance signaler(Long idMateriel, String description, Personnel auteur) throws ServiceException;

    Defaillance resoudre(Long id, String noteSolution, Personnel technicien) throws ServiceException;

    List<Defaillance> findByMateriel(Long idMateriel) throws ServiceException;

    Page<Defaillance> findByStatut(StatutDefaillance statut, Pageable pageable) throws ServiceException;
}
