package net.ent.etnc.orbit.services;

import net.ent.etnc.orbit.models.entities.Processus;
import net.ent.etnc.orbit.models.enums.ListeProcessus;
import net.ent.etnc.orbit.services.commons.Service;
import net.ent.etnc.orbit.services.commons.ServiceException;

public interface ProcessusService extends Service<Processus, Long> {

    /**
     * Passe le processus en liste blanche ou noire. La liste blanche a toujours le dessus :
     * un processus qui fait partie d'un master ne peut pas passer en liste noire.
     * En liste noire, il est désinstallé de toutes les UC.
     */
    Processus changerListe(Long id, ListeProcessus liste) throws ServiceException;
}