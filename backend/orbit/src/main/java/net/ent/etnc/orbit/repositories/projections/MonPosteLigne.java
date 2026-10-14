package net.ent.etnc.orbit.repositories.projections;

import net.ent.etnc.orbit.models.entities.Materiel;

/**
 * Une ligne de la requête « mon poste » : le poste, sa salle, son bâtiment et un de ses matériels
 * (matériel vide si le poste n'en a pas), avec l'indicateur de défaillance ouverte de ce matériel.
 */
public record MonPosteLigne(Long idPoste, String numPoste,
                            Long idSalle, String numSalle, String etage,
                            Long idBatiment, String numBatiment,
                            Materiel materiel, Boolean defaillanceOuverte) {

    public static final String NEW = "new net.ent.etnc.orbit.repositories.projections.MonPosteLigne";
}
