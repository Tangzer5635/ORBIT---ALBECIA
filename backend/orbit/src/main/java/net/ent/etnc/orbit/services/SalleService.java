package net.ent.etnc.orbit.services;

import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.services.commons.Service;
import net.ent.etnc.orbit.services.commons.ServiceException;

import java.util.Optional;

public interface SalleService extends Service<Salle, Long> {
    Salle changerGestionnaire(Long idSalle, Long idGestionnaire) throws ServiceException;
    boolean estEnStock(Long idMateriel);
    Optional<Salle> salleDuMateriel(Long idMateriel);
    Salle trouverStockage(Long idSalle) throws ServiceException;
    Salle trouverStockageDuPoste(Long idPoste) throws ServiceException;
}