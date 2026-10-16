package net.ent.etnc.orbit.services;

import net.ent.etnc.orbit.models.entities.Personnel;

public interface StageAffectationService {
    Personnel ajouterAStage(Long idStage, Long idStagiaire);
    Personnel supprimerAStage(Long idStage, Long idStagiaire);
}