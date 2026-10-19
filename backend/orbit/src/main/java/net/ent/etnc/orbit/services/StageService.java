package net.ent.etnc.orbit.services;

import net.ent.etnc.orbit.dtos.responseDTO.BilanImportStagesDto;
import net.ent.etnc.orbit.models.entities.Stage;
import net.ent.etnc.orbit.repositories.projections.SalleDuStage;
import net.ent.etnc.orbit.services.commons.Service;
import net.ent.etnc.orbit.services.commons.ServiceException;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface StageService extends Service<Stage, Long> {

    Stage creer(Stage stage, Long idFormateur) throws ServiceException;

    Stage modifier(Long id, Stage stage, Long idFormateur) throws ServiceException;

    /**
     * Import CSV (format de init/stages.csv, séparateur « ; ») : nom;type;dateDebut;dateFin;nidCDSF.
     * Ligne par ligne : un stage existant (même nom) est mis à jour, les autres sont créés,
     * une ligne invalide est ignorée et notée dans le bilan.
     */
    BilanImportStagesDto importer(String contenu) throws ServiceException;

    /** Salle du stage, absente si le stage n'est dans aucune salle. */
    Optional<SalleDuStage> salleDuStage(Long idStage);

    /** Salle de chaque stage qui en a une (clé : id du stage). */
    Map<Long, SalleDuStage> sallesDesStages(Collection<Long> idsStages);
}