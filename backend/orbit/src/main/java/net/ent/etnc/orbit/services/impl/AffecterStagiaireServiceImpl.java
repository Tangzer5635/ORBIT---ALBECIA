package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.entities.Stage;
import net.ent.etnc.orbit.models.enums.EtatPoste;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.models.enums.TypeAction;
import net.ent.etnc.orbit.services.AffecterStagiaireService;
import net.ent.etnc.orbit.services.JournalisationService;
import net.ent.etnc.orbit.services.PersonnelService;
import net.ent.etnc.orbit.services.PosteService;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.commons.Libelles;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AffecterStagiaireServiceImpl implements AffecterStagiaireService {

    private final PosteService posteService;
    private final SalleService salleService;
    private final PersonnelService personnelService;
    private final JournalisationService journalisationService;

    public AffecterStagiaireServiceImpl(PosteService posteService, SalleService salleService,
                                        PersonnelService personnelService, JournalisationService journalisationService) {
        this.posteService = posteService;
        this.salleService = salleService;
        this.personnelService = personnelService;
        this.journalisationService = journalisationService;
    }

    /**
     * Un poste stagiaire libre (ni défectueux, ni à compléter) reçoit un stagiaire du stage de la salle,
     * qui n'a pas encore de poste. Un stagiaire déjà assis reste sur son poste s'il devient défectueux.
     */
    @Override
    @Transactional
    public Poste affecterAPoste(Long idPoste, Long idStagiaire) throws ServiceException {
        Poste poste = poste(idPoste);
        Personnel stagiaire = personnelService.findById(idStagiaire)
                .orElseThrow(() -> new ServiceException("Stagiaire " + idStagiaire + " introuvable"));

        if (stagiaire.getRole() != Role.STAGIAIRE)
            throw new ServiceException(nom(stagiaire) + " n'est pas un stagiaire");
        if (!stagiaire.isActive())
            throw new ServiceException("Le compte de " + nom(stagiaire) + " est désactivé");
        if (poste.getNumPoste().endsWith("-F"))
            throw new ServiceException("Le poste formateur ne reçoit pas de stagiaire");
        if (poste.getOccupant() != null)
            throw new ServiceException("Le poste " + poste.getNumPoste() + " est déjà occupé par " + nom(poste.getOccupant()));
        // seul un poste « libre » reçoit un stagiaire : pas défectueux, et complet (au moins une UC et un écran)
        EtatPoste etat = posteService.etats(List.of(poste)).get(poste.getId());
        if (etat == EtatPoste.DEFECTUEUX)
            throw new ServiceException("Le poste " + poste.getNumPoste() + " est défectueux : on n'y affecte pas de stagiaire");
        if (etat == EtatPoste.A_COMPLETER)
            throw new ServiceException("Le poste " + poste.getNumPoste()
                    + " est à compléter (il faut au moins une unité centrale et un écran) : on n'y affecte pas de stagiaire");

        Salle salle = salleService.salleDuPoste(idPoste)
                .orElseThrow(() -> new ServiceException("Le poste " + poste.getNumPoste() + " n'est dans aucune salle"));
        if (salle.isSuspendu())
            throw new ServiceException("La salle " + salle.getNumSalle() + " est gelée : on n'y affecte plus de stagiaire");
        Stage stage = salle.getStage();
        if (stage == null)
            throw new ServiceException("La salle " + salle.getNumSalle() + " n'accueille aucun stage");
        if (stagiaire.getStage() == null || !stagiaire.getStage().getId().equals(stage.getId()))
            throw new ServiceException(nom(stagiaire) + " n'est pas dans le stage " + stage.getNom());
        if (posteService.occupeUnPoste(idStagiaire))
            throw new ServiceException(nom(stagiaire) + " a déjà un poste");

        poste.setOccupant(stagiaire);
        journalisationService.journaliser(TypeAction.AFFECTATION,
                "Stagiaire " + Libelles.personne(stagiaire) + " affecté au " + Libelles.poste(poste));
        return poste;
    }

    @Override
    @Transactional
    public Poste retirerDePoste(Long idPoste, Long idStagiaire) throws ServiceException {
        Poste poste = poste(idPoste);
        if (poste.getOccupant() == null || !poste.getOccupant().getId().equals(idStagiaire))
            throw new ServiceException("Ce stagiaire n'est pas sur le poste " + poste.getNumPoste());
        Personnel stagiaire = poste.getOccupant();
        poste.setOccupant(null);
        journalisationService.journaliser(TypeAction.RETRAIT,
                "Stagiaire " + Libelles.personne(stagiaire) + " retiré du " + Libelles.poste(poste));
        return poste;
    }

    private Poste poste(Long idPoste) throws ServiceException {
        return posteService.findById(idPoste)
                .orElseThrow(() -> new ServiceException("Poste " + idPoste + " introuvable"));
    }

    private static String nom(Personnel p) {
        return p.getPrenom() + " " + p.getNom();
    }
}