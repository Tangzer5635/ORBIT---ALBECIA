package net.ent.etnc.orbit.services;

import net.ent.etnc.orbit.dtos.responseDTO.SalleCarteDto;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.services.commons.Service;
import net.ent.etnc.orbit.services.commons.ServiceException;

import java.util.List;
import java.util.Optional;

public interface SalleService extends Service<Salle, Long> {
    boolean estEnStock(Long idMateriel);
    Optional<Salle> salleDuMateriel(Long idMateriel);

    Optional<Salle> salleDuPoste(Long idPoste);
    Salle trouverStockage(Long idSalle) throws ServiceException;
    Salle trouverStockageDuPoste(Long idPoste) throws ServiceException;

    /** Crée un poste dans une salle de cours (le numéro de poste est unique). */
    Poste creerPoste(Long idSalle, Poste poste) throws ServiceException;

    /** Supprime un poste : son matériel retourne dans la réserve du bâtiment. */
    void supprimerPoste(Long idPoste) throws ServiceException;

    /** Page /salles?batiment=XX : tout est compté en base, nombre de requêtes fixe. */
    List<SalleCarteDto> findCartesDuBatiment(Long idBatiment);
}