package net.ent.etnc.orbit.services;

import net.ent.etnc.orbit.dtos.agent.ProcessusPosteDto;
import net.ent.etnc.orbit.services.commons.ServiceException;

/**
 * Processus lancés sur les postes, lus et arrêtés via l'Agent ORBIT de la machine.
 * La machine d'un poste = son UC : l'agent est joint à l'adresse IP de l'UC, et la machine
 * qui répond doit porter le nom machine de l'UC (sinon le matériel du poste n'est pas conforme).
 * N'étend pas {@code Service<T, ID>} : aucune entité, rien n'est stocké en base.
 */
public interface ProcessusLanceService {

    /** Processus lancés sur la machine du poste, regroupés par nom. */
    ProcessusPosteDto lister(Long idPoste) throws ServiceException;

    /**
     * Arrête toutes les instances de ce processus sur la machine du poste.
     *
     * @return le nombre d'instances arrêtées
     */
    int tuer(Long idPoste, String nom) throws ServiceException;
}