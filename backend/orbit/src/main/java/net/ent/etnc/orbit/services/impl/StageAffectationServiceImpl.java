package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.entities.Stage;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.models.enums.TypeAction;
import net.ent.etnc.orbit.services.JournalisationService;
import net.ent.etnc.orbit.services.PersonnelService;
import net.ent.etnc.orbit.services.PosteService;
import net.ent.etnc.orbit.services.StageAffectationService;
import net.ent.etnc.orbit.services.StageService;
import net.ent.etnc.orbit.services.commons.Libelles;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StageAffectationServiceImpl implements StageAffectationService {

    private final StageService stageService;
    private final PersonnelService personnelService;
    private final PosteService posteService;
    private final JournalisationService journalisationService;

    public StageAffectationServiceImpl(StageService stageService, PersonnelService personnelService,
                                       PosteService posteService, JournalisationService journalisationService) {
        this.stageService = stageService;
        this.personnelService = personnelService;
        this.posteService = posteService;
        this.journalisationService = journalisationService;
    }

    /** Un stagiaire n'est que dans un stage, et on ne le change pas de stage. */
    @Override
    @Transactional
    public Personnel ajouterAStage(Long idStage, Long idStagiaire) {
        Stage stage = stageService.findById(idStage)
                .orElseThrow(() -> new ServiceException("Stage " + idStage + " introuvable"));
        Personnel stagiaire = stagiaire(idStagiaire);

        Stage actuel = stagiaire.getStage();
        if (actuel != null && actuel.getId().equals(idStage))
            throw new ServiceException(nom(stagiaire) + " est déjà dans le stage " + stage.getNom());
        if (actuel != null)
            throw new ServiceException(nom(stagiaire) + " est déjà dans le stage " + actuel.getNom() + " : on ne change pas de stage");

        stagiaire.setStage(stage);
        journalisationService.journaliser(TypeAction.AFFECTATION,
                "Stagiaire " + Libelles.personne(stagiaire) + " ajouté au " + Libelles.stage(stage));
        return stagiaire;
    }

    /** Sorti du stage, le stagiaire n'a plus sa place dans la salle du stage : son poste est libéré. */
    @Override
    @Transactional
    public Personnel supprimerAStage(Long idStage, Long idStagiaire) {
        Personnel stagiaire = stagiaire(idStagiaire);
        if (stagiaire.getStage() == null || !stagiaire.getStage().getId().equals(idStage))
            throw new ServiceException(nom(stagiaire) + " n'est pas dans ce stage");
        Stage stage = stagiaire.getStage();
        stagiaire.setStage(null);
        posteService.libererPosteDe(idStagiaire);
        journalisationService.journaliser(TypeAction.RETRAIT,
                "Stagiaire " + Libelles.personne(stagiaire) + " retiré du " + Libelles.stage(stage) + " (poste libéré)");
        return stagiaire;
    }

    /** Rôle exact : un formateur hérite des droits stagiaire mais n'entre pas dans un stage. */
    private Personnel stagiaire(Long idStagiaire) {
        Personnel p = personnelService.findById(idStagiaire)
                .orElseThrow(() -> new ServiceException("Stagiaire " + idStagiaire + " introuvable"));
        if (p.getRole() != Role.STAGIAIRE)
            throw new ServiceException(nom(p) + " n'est pas un stagiaire");
        return p;
    }

    private static String nom(Personnel p) {
        return p.getPrenom() + " " + p.getNom();
    }
}