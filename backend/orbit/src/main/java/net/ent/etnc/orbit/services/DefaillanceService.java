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

    /** Défaillances remontées par cette personne, les plus récentes d'abord (« Mon poste »). */
    List<Defaillance> findBySignalePar(Long idPersonnel);

    /** {@code true} si le matériel a une défaillance signalée et non résolue (état DEFECTUEUX). */
    boolean aUneDefaillanceOuverte(Long idMateriel);

    /** {@code true} si le matériel a un historique de défaillances (il ne peut alors qu'être archivé). */
    boolean aDesDefaillances(Long idMateriel);

    Page<Defaillance> findByStatut(StatutDefaillance statut, Pageable pageable) throws ServiceException;

    /** Onglet Défaillances : le formateur ne voit que celles de ses salles, gestionnaire et admin voient tout. */
    Page<Defaillance> findPourUtilisateur(Personnel utilisateur, StatutDefaillance statut, Pageable pageable) throws ServiceException;
}