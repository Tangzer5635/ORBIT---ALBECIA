package net.ent.etnc.orbit.services;

import net.ent.etnc.orbit.dtos.requestDTO.SalleEquipeeRequestDto;
import net.ent.etnc.orbit.dtos.requestDTO.SalleModificationRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.AnneauDto;
import net.ent.etnc.orbit.dtos.responseDTO.SalleCarteDto;
import net.ent.etnc.orbit.dtos.responseDTO.SalleEquipeeResponseDto;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.MotifGel;
import net.ent.etnc.orbit.services.commons.Service;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

    /**
     * Une salle de cours devient une salle de stockage. Elle doit être vide de monde : ni stage, ni stagiaire
     * sur un poste, ni formateur attribué, et ne pas être gelée. Le master est retiré, les postes sont supprimés
     * et tout leur matériel (avec le matériel de salle) devient le premier stock de la salle.
     */
    Salle convertirEnStockage(Long idSalle) throws ServiceException;

    /**
     * Une salle de stockage devient une salle de cours (sans poste). Ses vidéoprojecteurs et tableaux interactifs
     * restent dans la salle, le reste de son matériel part dans une autre salle de stockage du même bâtiment.
     * Refusé si c'est la seule salle de stockage du bâtiment.
     */
    Salle convertirEnSalleDeCours(Long idSalle, Long idStockageCible) throws ServiceException;

    /** Change le numéro et l'étage d'une salle de stockage (numéro unique dans le bâtiment). */
    Salle modifierStockage(Long idSalle, String numSalle, String etage) throws ServiceException;

    /** Page de salles avec matériels et postes chargés en 2 requêtes : pas de N+1 à l'assemblage du DTO. */
    Page<Salle> findAllAvecDetails(Pageable pageable);

    /** Une salle avec matériels et postes chargés en 2 requêtes. */
    Optional<Salle> findByIdAvecDetails(Long id);

    /** Supprime une salle, stockage compris, lors de la suppression de son bâtiment (seul cas autorisé pour un stockage). */
    void supprimerAvecBatiment(Long idSalle) throws ServiceException;

    /** Crée une salle de cours dans un bâtiment et l'équipe avec le stock de sa réserve. */
    SalleEquipeeResponseDto creerSalleEquipee(SalleEquipeeRequestDto dto) throws ServiceException;

    /**
     * « Modifier la salle » : nombre de postes stagiaires, de vidéoprojecteurs et de tableaux.
     * Le matériel manquant est pris dans le stock du bâtiment, celui en trop y retourne ;
     * les postes en trop (les derniers numéros) sont supprimés et leur stagiaire perd son poste.
     */
    SalleEquipeeResponseDto modifierComposition(Long idSalle, SalleModificationRequestDto dto) throws ServiceException;

    /** Gèle une salle de cours : plus d'affectation de stage, de stagiaire ni de master. */
    Salle geler(Long idSalle, MotifGel motif, String note, LocalDate jusqu) throws ServiceException;

    Salle degeler(Long idSalle) throws ServiceException;

    /** Salles où ce master est plaqué. */
    List<Salle> sallesDuMaster(Long idMaster);

    /**
     * « Déplacer la salle » : postes (formateur compris), matériel de salle, stage, formateurs et master
     * passent dans la salle cible, qui doit avoir au moins autant de postes stagiaires, de vidéoprojecteurs
     * et de tableaux. L'ancien contenu de la cible retourne au stock de son bâtiment ; la source reste vide.
     * @return la salle cible
     */
    Salle deplacer(Long idSalle, Long idSalleCible) throws ServiceException;

    /** Page /salles?batiment=XX : tout est compté en base, nombre de requêtes fixe. */
    List<SalleCarteDto> findCartesDuBatiment(Long idBatiment);

    /** Cartes des salles affectées à ce formateur (tous bâtiments), nombre de requêtes fixe. */
    List<SalleCarteDto> findCartesDuFormateur(Long idFormateur);

    /**
     * Un formateur n'accède qu'aux salles qui lui sont affectées ; les autres rôles ne sont pas limités.
     * @throws AccessDeniedException si c'est un formateur et que la salle ne lui est pas affectée
     */
    void verifierAcces(Long idSalle, Personnel utilisateur) throws AccessDeniedException;

    /** Salle où se trouve le matériel : posé directement dans la salle (stockage compris) ou sur un de ses postes. */
    Optional<Salle> localiser(Long idMateriel);

    /** Anneau d'une salle : ses postes comptés par état (carte de la salle et page détail). */
    AnneauDto anneauDe(Long idSalle);

    /** Anneau de chaque salle (clé : id de la salle). */
    Map<Long, AnneauDto> anneaux(List<Long> idsSalles);
}