package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.dtos.responseDTO.AffectationMaterielDto;
import net.ent.etnc.orbit.models.entities.Batiment;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.entities.Stage;
import net.ent.etnc.orbit.models.enums.TypeAction;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.services.AffecterMaterielService;
import net.ent.etnc.orbit.services.BatimentService;
import net.ent.etnc.orbit.services.JournalisationService;
import net.ent.etnc.orbit.services.MaterielService;
import net.ent.etnc.orbit.services.PosteService;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.commons.Libelles;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Affectations du matériel. Chaque méthode publique écrit une seule ligne dans le journal :
 * les méthodes privées ne journalisent pas, donc un déplacement (retrait + affectation) = une ligne.
 */
@Service
public class AffecterMaterielServiceImpl implements AffecterMaterielService {

    private final MaterielService materielService;
    private final SalleService salleService;
    private final PosteService posteService;
    private final BatimentService batimentService;
    private final JournalisationService journalisationService;

    public AffecterMaterielServiceImpl(MaterielService materielService, SalleService salleService, PosteService posteService,
                                       BatimentService batimentService, JournalisationService journalisationService) {
        this.materielService = materielService;
        this.salleService = salleService;
        this.posteService = posteService;
        this.batimentService = batimentService;
        this.journalisationService = journalisationService;
    }

    @Override
    @Transactional(readOnly = true)
    public AffectationMaterielDto affectation(Long idMateriel) {
        Materiel materiel = trouverMateriel(idMateriel);
        if (materiel.isArchive()) return AffectationMaterielDto.vide();
        // salle de cours, salle de stockage, ou salle du poste qui porte le matériel
        Salle salle = salleService.localiser(idMateriel).orElse(null);
        if (salle == null) return AffectationMaterielDto.vide();

        Poste poste = posteService.posteDuMateriel(idMateriel).orElse(null);
        Personnel occupant = poste != null ? poste.getOccupant() : null;
        Stage stage = salle.getStage();
        String numBatiment = batimentService.batimentDeLaSalle(salle.getId()).map(Batiment::getNumBatiment).orElse(null);

        return new AffectationMaterielDto(
                salle.getId(), salle.getNumSalle(), salle.getType(), salle.getEtage(), numBatiment,
                poste != null ? poste.getId() : null,
                poste != null ? poste.getNumPoste() : null,
                occupant != null ? occupant.getPrenom() + " " + occupant.getNom() : null,
                stage != null ? stage.getNom() : null,
                stage != null ? stage.getDateDebut() : null,
                stage != null ? stage.getDateFin() : null,
                salle.getCreePar());
    }

    @Override
    @Transactional
    public Materiel affecterASalle(Long idSalle, Long idMateriel) throws ServiceException {
        Salle salle = salleDeCours(idSalle);
        Materiel materiel = trouverMateriel(idMateriel);
        sortirDuStock(materiel);
        salle.addMateriel(materiel);
        journalisationService.journaliser(TypeAction.AFFECTATION,
                Libelles.materiel(materiel) + " affecté à la " + Libelles.salle(salle));
        return materiel;
    }

    @Override
    @Transactional
    public Materiel affecterAPoste(Long idPoste, Long idMateriel) throws ServiceException {
        Poste poste = trouverPoste(idPoste);
        Materiel materiel = trouverMateriel(idMateriel);
        sortirDuStock(materiel);
        poste.addMateriel(materiel);
        journalisationService.journaliser(TypeAction.AFFECTATION,
                Libelles.materiel(materiel) + " affecté au " + Libelles.poste(poste));
        return materiel;
    }

    @Override
    @Transactional
    public Materiel retirerDeSalle(Long idSalle, Long idMateriel) throws ServiceException {
        Salle salle = salleService.findById(idSalle).orElseThrow(() -> new ServiceException("Salle " + idSalle + " introuvable"));
        Materiel materiel = enleverDeSalle(salle, idMateriel);
        journalisationService.journaliser(TypeAction.RETRAIT,
                Libelles.materiel(materiel) + " retiré de la " + Libelles.salle(salle) + " et remis en stock");
        return materiel;
    }

    @Override
    @Transactional
    public Materiel retirerDuPoste(Long idPoste, Long idMateriel) throws ServiceException {
        Poste poste = trouverPoste(idPoste);
        Materiel materiel = enleverDuPoste(poste, idMateriel);
        journalisationService.journaliser(TypeAction.RETRAIT,
                Libelles.materiel(materiel) + " retiré du " + Libelles.poste(poste) + " et remis en stock");
        return materiel;
    }

    /** Une seule transaction : si l'affectation échoue, le retrait est annulé (le matériel ne reste pas en stock). */
    @Override
    @Transactional
    public Materiel deplacerVersSalle(Long idSalle, Long idMateriel) throws ServiceException {
        String origine = remettreEnStock(idMateriel);
        Salle salle = salleDeCours(idSalle);
        Materiel materiel = trouverMateriel(idMateriel);
        sortirDuStock(materiel);
        salle.addMateriel(materiel);
        journalisationService.journaliser(TypeAction.AFFECTATION,
                Libelles.materiel(materiel) + " déplacé " + origine + " vers la " + Libelles.salle(salle));
        return materiel;
    }

    @Override
    @Transactional
    public Materiel deplacerVersPoste(Long idPoste, Long idMateriel) throws ServiceException {
        String origine = remettreEnStock(idMateriel);
        Poste poste = trouverPoste(idPoste);
        Materiel materiel = trouverMateriel(idMateriel);
        sortirDuStock(materiel);
        poste.addMateriel(materiel);
        journalisationService.journaliser(TypeAction.AFFECTATION,
                Libelles.materiel(materiel) + " déplacé " + origine + " vers le " + Libelles.poste(poste));
        return materiel;
    }

    /** Salle qui peut recevoir du matériel : pas une salle de stockage. */
    private Salle salleDeCours(Long idSalle) throws ServiceException {
        Salle salle = salleService.findById(idSalle).orElseThrow(() -> new ServiceException("Salle " + idSalle + " introuvable"));
        if (salle.getType() == TypeSalle.SalleStockage)
            throw new ServiceException("On n'affecte pas un matériel à une salle de stockage");
        return salle;
    }

    /** Remet en stock un matériel de la salle (sans journaliser). */
    private Materiel enleverDeSalle(Salle salle, Long idMateriel) throws ServiceException {
        if (salle.getType() == TypeSalle.SalleStockage)
            throw new ServiceException("Ce matériel est déjà en stock");
        Materiel materiel = chercher(salle.getMateriels(), idMateriel);
        if (materiel == null)
            throw new ServiceException("Ce matériel n'est pas dans cette salle");
        salle.removeMateriel(materiel);
        Salle stockage = salleService.trouverStockage(salle.getId());
        stockage.addMateriel(materiel);
        return materiel;
    }

    /** Remet en stock un matériel du poste (sans journaliser). */
    private Materiel enleverDuPoste(Poste poste, Long idMateriel) throws ServiceException {
        Materiel materiel = chercher(poste.getMateriels(), idMateriel);
        if (materiel == null)
            throw new ServiceException("Ce matériel n'est pas sur ce poste");
        poste.removeMateriel(materiel);
        Salle stockage = salleService.trouverStockageDuPoste(poste.getId());
        stockage.addMateriel(materiel);
        return materiel;
    }

    /**
     * Retire le matériel de son poste ou de sa salle de cours actuelle.
     * @return d'où il vient, pour le journal : "du poste B2-S105-P07" ou "de la salle 105"
     */
    private String remettreEnStock(Long idMateriel) throws ServiceException {
        Poste poste = posteService.posteDuMateriel(idMateriel).orElse(null);
        if (poste != null) {
            enleverDuPoste(poste, idMateriel);
            return "du " + Libelles.poste(poste);
        }
        Salle salle = salleService.salleDuMateriel(idMateriel)
                .orElseThrow(() -> new ServiceException("Ce matériel n'est affecté à aucune salle"));
        enleverDeSalle(salle, idMateriel);
        return "de la " + Libelles.salle(salle);
    }

    private void sortirDuStock(Materiel materiel) throws ServiceException {
        if (materiel.isArchive())
            throw new ServiceException("Un matériel archivé ne peut pas être affecté");
        Salle salle = salleService.salleDuMateriel(materiel.getId()).orElse(null);
        if (salle == null || salle.getType() != TypeSalle.SalleStockage)
            throw new ServiceException("Seul un matériel en stock peut être affecté");
        salle.removeMateriel(materiel);
    }

    private Poste trouverPoste(Long idPoste) throws ServiceException {
        return posteService.findById(idPoste).orElseThrow(() -> new ServiceException("Poste " + idPoste + " introuvable"));
    }

    private Materiel trouverMateriel(Long idMateriel) throws ServiceException {
        return materielService.findById(idMateriel).orElseThrow(() -> new ServiceException("Matériel " + idMateriel + " introuvable"));
    }

    private Materiel chercher(List<Materiel> materiels, Long idMateriel) throws ServiceException {
        for (Materiel m : materiels) {
            if (m.getId().equals(idMateriel)) {
                return m;
            }
        }
        return null;
    }
}