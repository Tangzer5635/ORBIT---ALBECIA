package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.entities.Stage;
import net.ent.etnc.orbit.models.enums.TypeAction;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.services.AffecterStageService;
import net.ent.etnc.orbit.services.JournalisationService;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.StageService;
import net.ent.etnc.orbit.services.commons.Libelles;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;

@Service
public class AffecterStageServiceImpl implements AffecterStageService {

    private final StageService stageService;
    private final SalleService salleService;
    private final JournalisationService journalisationService;

    public AffecterStageServiceImpl(StageService stageService, SalleService salleService,
                                    JournalisationService journalisationService) {
        this.stageService = stageService;
        this.salleService = salleService;
        this.journalisationService = journalisationService;
    }

    /** Une salle de cours accueille un seul stage à la fois, et un stage n'a qu'une salle. */
    @Override
    @Transactional
    public Salle affecterASalle(Long idSalle, Long idStage) throws ServiceException {
        Salle salle = salleService.findById(idSalle)
                .orElseThrow(() -> new ServiceException("Salle " + idSalle + " introuvable"));
        Stage stage = stageService.findById(idStage)
                .orElseThrow(() -> new ServiceException("Stage " + idStage + " introuvable"));

        if (salle.getType() != TypeSalle.SalleDeCours)
            throw new ServiceException("Seule une salle de cours accueille un stage");
        if (salle.isSuspendu())
            throw new ServiceException("La salle " + salle.getNumSalle() + " est gelée : dégelez-la avant d'y affecter un stage");
        if (salle.getStage() != null)
            throw new ServiceException("La salle " + salle.getNumSalle() + " accueille déjà le stage " + salle.getStage().getNom());
        if (stageService.salleDuStage(idStage).isPresent())
            throw new ServiceException("Le stage " + stage.getNom() + " est déjà affecté à une salle");
        if (stage.getDateFin().isBefore(LocalDate.now()))
            throw new ServiceException("Le stage " + stage.getNom() + " est terminé");

        salle.setStage(stage);
        journalisationService.journaliser(TypeAction.AFFECTATION,
                Libelles.stage(stage) + " affecté à la " + Libelles.salle(salle));
        return salle;
    }

    /** Les postes de la salle repartent libres : plus de stage, plus de stagiaires assis dedans. */
    @Override
    @Transactional
    public Salle retirerDeSalle(Long idSalle, Long idStage) throws ServiceException {
        Salle salle = salleService.findById(idSalle)
                .orElseThrow(() -> new ServiceException("Salle " + idSalle + " introuvable"));
        if (salle.getStage() == null || !Objects.equals(salle.getStage().getId(), idStage))
            throw new ServiceException("Ce stage n'est pas dans cette salle");

        Stage stage = salle.getStage();
        salle.setStage(null);
        salle.getPostes().forEach(poste -> poste.setOccupant(null));
        journalisationService.journaliser(TypeAction.RETRAIT,
                Libelles.stage(stage) + " retiré de la " + Libelles.salle(salle) + " (postes libérés)");
        return salle;
    }
}