package net.ent.etnc.orbit.services;

import net.ent.etnc.orbit.models.entities.Salle;

public interface AffecterFormateurService {
    Salle affecter(Long idSalle, Long idFormateur);
    Salle retirer(Long idSalle, Long idFormateur);
}
