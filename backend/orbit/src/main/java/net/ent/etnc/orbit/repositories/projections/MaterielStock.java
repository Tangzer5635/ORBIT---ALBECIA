package net.ent.etnc.orbit.repositories.projections;

import net.ent.etnc.orbit.models.entities.Materiel;

/** Un matériel en stock avec sa réserve et le bâtiment de la réserve (liste « Affecter un matériel »). */
public record MaterielStock(Materiel materiel, String numSalle, String numBatiment) {

    public static final String NEW = "new net.ent.etnc.orbit.repositories.projections.MaterielStock";
}