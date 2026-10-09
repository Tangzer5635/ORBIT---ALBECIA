package net.ent.etnc.orbit.services;

import net.ent.etnc.orbit.models.entities.Batiment;
import net.ent.etnc.orbit.services.commons.Service;
import net.ent.etnc.orbit.services.commons.ServiceException;

public interface BatimentService extends Service<Batiment, Long> {
    Batiment affecterSalle(Long idBatiment, Long idSalle) throws ServiceException;
    void retirerSalle(Long idBatiment, Long idSalle) throws ServiceException;
}