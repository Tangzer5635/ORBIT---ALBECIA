package net.ent.etnc.orbit.services;

import net.ent.etnc.orbit.models.entities.Salle;

public interface AffecterStageService {
    Salle affecterASalle(Long idSalle, Long idStage);
    Salle retirerDeSalle(Long idSalle, Long idStage);
}
