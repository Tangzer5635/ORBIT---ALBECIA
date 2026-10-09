package net.ent.etnc.orbit.services;

import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.services.commons.Service;
import net.ent.etnc.orbit.services.commons.ServiceException;

public interface SalleService extends Service<Salle, Long> {

    Salle changerGestionnaire(Long idSalle, Long idGestionnaire) throws ServiceException;
    Salle affecterMateriel(Long idSalle, Long idMateriel) throws ServiceException;
    void remettreEnStock(Long idSalle, Long idMateriel) throws ServiceException;

}