package net.ent.etnc.orbit.dtos.responseDTO;

import net.ent.etnc.orbit.models.enums.TypeSalle;

import java.time.LocalDate;

/**
 * Où se trouve un matériel (fiche garantie du calendrier) : GET /materiels/{id}/affectation/.
 * Tout est null pour un matériel archivé ; numPoste est null pour un matériel posé dans la salle (VP, TBI)
 * ou rangé en stock.
 */
public record AffectationMaterielDto(
        Long idSalle,
        String numSalle,
        TypeSalle typeSalle,
        String etage,
        String numBatiment,
        Long idPoste,
        String numPoste,         // "B201-S302-P07", "B201-S302-F" pour le poste formateur
        String occupant,         // "Prénom Nom" du stagiaire du poste, null si libre
        String stageNom,         // stage de la salle
        LocalDate stageDateDebut,
        LocalDate stageDateFin,
        String responsable       // gestionnaire qui a créé la salle
) {
    public static AffectationMaterielDto vide() {
        return new AffectationMaterielDto(null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
