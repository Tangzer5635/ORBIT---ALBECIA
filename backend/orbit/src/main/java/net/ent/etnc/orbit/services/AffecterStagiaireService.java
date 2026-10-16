package net.ent.etnc.orbit.services;

import net.ent.etnc.orbit.models.entities.Poste;

public interface AffecterStagiaireService {
    Poste affecterAPoste(Long idPoste, Long idStagiaire);
    Poste retirerDePoste(Long idPoste, Long idStagiaire);
}
