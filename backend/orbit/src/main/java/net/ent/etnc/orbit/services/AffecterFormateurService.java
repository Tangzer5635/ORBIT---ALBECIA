package net.ent.etnc.orbit.services;

import net.ent.etnc.orbit.models.entities.Salle;

public interface AffecterFormateurService {
    /** Attribue la salle au formateur, sans date de fin (on le retire avec {@link #retirer}). */
    Salle affecter(Long idSalle, Long idFormateur);

    Salle retirer(Long idSalle, Long idFormateur);
}