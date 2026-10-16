package net.ent.etnc.orbit.repositories.projections;

/** Numéro du bâtiment d'une salle. */
public record SalleBatiment(Long idSalle, String numBatiment) {

    public static final String NEW = "new net.ent.etnc.orbit.repositories.projections.SalleBatiment";
}
