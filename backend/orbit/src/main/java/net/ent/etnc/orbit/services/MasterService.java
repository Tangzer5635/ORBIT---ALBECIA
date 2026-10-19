package net.ent.etnc.orbit.services;

import net.ent.etnc.orbit.models.entities.Master;
import net.ent.etnc.orbit.models.entities.Processus;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.services.commons.Service;
import net.ent.etnc.orbit.services.commons.ServiceException;

import java.util.List;

public interface MasterService extends Service<Master, Long> {

    /** Crée un master avec ses processus (liste blanche uniquement, nom unique). */
    Master creer(Master master, List<Long> processusIds) throws ServiceException;

    /**
     * Modifie nom, version et processus du master. Les salles où il est plaqué
     * reçoivent la nouvelle liste de processus sur leurs UC.
     */
    Master modifier(Long id, Master donnees, List<Long> processusIds) throws ServiceException;

    /**
     * Plaque le master sur une salle de cours non gelée : chaque UC de ses postes
     * reçoit exactement les processus du master (l'ancienne image est remplacée).
     */
    Salle plaquer(Long idSalle, Long idMaster) throws ServiceException;

    /** Masters qui contiennent ce processus. */
    List<Master> mastersAvec(Long idProcessus);

    /** Retire ce processus de tous les masters (avant sa suppression). */
    void retirerProcessusPartout(Processus processus);
}