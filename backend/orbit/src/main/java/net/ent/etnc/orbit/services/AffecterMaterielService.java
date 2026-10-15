package net.ent.etnc.orbit.services;

import net.ent.etnc.orbit.models.entities.Materiel;

public interface AffecterMaterielService {
    Materiel affecterASalle(Long idSalle, Long idMateriel);
    Materiel affecterAPoste(Long idPoste, Long idMateriel);
    Materiel retirerDeSalle(Long idSalle, Long idMateriel);
    Materiel retirerDuPoste(Long idPoste, Long idMateriel);

    /** Déplace un matériel déjà affecté (salle ou poste) vers une autre salle de cours. */
    Materiel deplacerVersSalle(Long idSalle, Long idMateriel);

    /** Déplace un matériel déjà affecté (salle ou poste) vers un autre poste. */
    Materiel deplacerVersPoste(Long idPoste, Long idMateriel);
}