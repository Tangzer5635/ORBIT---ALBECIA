package net.ent.etnc.orbit.services;

import net.ent.etnc.orbit.models.entities.Materiel;

public interface AffecterMaterielService {
    Materiel affecterASalle(Long idSalle, Long idMateriel);
    Materiel affecterAPoste(Long idPoste, Long idMateriel);
    Materiel retirerDeSalle(Long idSalle, Long idMateriel);
    Materiel retirerDuPoste(Long idPoste, Long idMateriel);
}