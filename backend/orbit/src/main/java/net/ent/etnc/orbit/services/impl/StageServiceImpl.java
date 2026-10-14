package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.entities.Stage;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.repositories.StageRepository;
import net.ent.etnc.orbit.repositories.projections.SalleDuStage;
import net.ent.etnc.orbit.services.PersonnelService;
import net.ent.etnc.orbit.services.StageService;
import net.ent.etnc.orbit.services.commons.AbstractService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StageServiceImpl extends AbstractService<Stage, StageRepository> implements StageService {

    private final PersonnelService personnelService;

    @Autowired
    public StageServiceImpl(StageRepository stageRepository, PersonnelService personnelService) {
        super(stageRepository);
        this.personnelService = personnelService;
    }

    @Override
    @Transactional
    public Stage creer(Stage stage, Long idFormateur) {
        stage.setFormateur(formateur(idFormateur));
        return create(stage);
    }

    @Override
    @Transactional
    public Stage modifier(Long id, Stage stage, Long idFormateur) {
        stage.setId(id);
        stage.setFormateur(formateur(idFormateur));
        return update(stage);
    }

    @Override
    @Transactional
    public Stage create(Stage stage) {
        verifierDates(stage);
        if (repository.existsByNom(stage.getNom()))
            throw new ServiceException("Le stage " + stage.getNom() + " existe déjà");
        return super.create(stage);
    }

    @Override
    @Transactional
    public Stage update(Stage stage) {
        verifierDates(stage);
        if (repository.existsByNomAndIdNot(stage.getNom(), stage.getId()))
            throw new ServiceException("Le stage " + stage.getNom() + " existe déjà");
        return super.update(stage);
    }

    /** On ne supprime pas un stage qui a encore des stagiaires ou une salle : il faut d'abord les retirer. */
    @Override
    @Transactional
    public void deleteById(Long id) {
        Stage stage = repository.findById(id)
                .orElseThrow(() -> new ServiceException("Stage " + id + " introuvable"));
        if (personnelService.existeDansStage(id))
            throw new ServiceException("Le stage " + stage.getNom() + " contient encore des stagiaires");
        if (repository.nbSalles(id) > 0)
            throw new ServiceException("Le stage " + stage.getNom() + " est affecté à une salle : retirez-le d'abord");
        repository.delete(stage);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SalleDuStage> salleDuStage(Long idStage) {
        return Optional.ofNullable(sallesDesStages(List.of(idStage)).get(idStage));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, SalleDuStage> sallesDesStages(Collection<Long> idsStages) {
        if (idsStages.isEmpty()) return Map.of();
        return repository.sallesDesStages(idsStages).stream()
                .collect(Collectors.toMap(SalleDuStage::idStage, Function.identity(), (a, b) -> a));
    }

    private Personnel formateur(Long idFormateur) {
        Personnel formateur = personnelService.findById(idFormateur)
                .orElseThrow(() -> new ServiceException("Formateur " + idFormateur + " introuvable"));
        if (formateur.getRole() != Role.FORMATEUR)
            throw new ServiceException("Un stage est forcément géré par un formateur");
        return formateur;
    }

    private void verifierDates(Stage stage) {
        if (stage.getDateDebut() != null && stage.getDateFin() != null && stage.getDateFin().isBefore(stage.getDateDebut()))
            throw new ServiceException("La date de fin doit être après la date de début");
    }
}
