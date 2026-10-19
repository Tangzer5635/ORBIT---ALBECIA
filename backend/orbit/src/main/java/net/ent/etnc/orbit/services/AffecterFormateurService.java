package net.ent.etnc.orbit.services;

import net.ent.etnc.orbit.models.entities.Salle;

import java.time.LocalDate;

public interface AffecterFormateurService {
    /** Attribue la salle au formateur jusqu'à la date limite (incluse, obligatoire, pas dans le passé). */
    Salle affecter(Long idSalle, Long idFormateur, LocalDate jusqu);

    /** Change la date limite d'un formateur déjà attribué (prolonger, ou réactiver un accès expiré). */
    Salle prolonger(Long idSalle, Long idFormateur, LocalDate jusqu);

    Salle retirer(Long idSalle, Long idFormateur);
}