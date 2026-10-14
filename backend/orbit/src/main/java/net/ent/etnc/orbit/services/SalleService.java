package net.ent.etnc.orbit.services;

import net.ent.etnc.orbit.dtos.requestDTO.SalleEquipeeRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.SalleCarteDto;
import net.ent.etnc.orbit.dtos.responseDTO.SalleEquipeeResponseDto;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.services.commons.Service;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    /** Crée une salle dans un bâtiment (vérifie le bâtiment et le numéro en double). */
    Salle create(Long idBatiment, Salle salle) throws ServiceException;

    /** Page de salles avec matériels et postes chargés en 2 requêtes : pas de N+1 à l'assemblage du DTO. */
    Page<Salle> findAllAvecDetails(Pageable pageable);

    /** Une salle avec matériels et postes chargés en 2 requêtes. */
    Optional<Salle> findByIdAvecDetails(Long id);

    /** Supprime une salle, stockage compris, lors de la suppression de son bâtiment (seul cas autorisé pour un stockage). */
    void supprimerAvecBatiment(Long idSalle) throws ServiceException;

    /** Crée une salle de cours dans un bâtiment et l'équipe avec le stock de sa réserve. */
    SalleEquipeeResponseDto creerSalleEquipee(SalleEquipeeRequestDto dto) throws ServiceException;

    /** Page /salles?batiment=XX : tout est compté en base, nombre de requêtes fixe. */
    List<SalleCarteDto> findCartesDuBatiment(Long idBatiment);

    /** Salle où se trouve le matériel : posé directement dans la salle (stockage compris) ou sur un de ses postes. */
    Optional<Salle> localiser(Long idMateriel);
}