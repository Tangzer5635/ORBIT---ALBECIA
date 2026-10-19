package net.ent.etnc.orbit.repositories.projections;

/** Salle (et son bâtiment) qui accueille un stage. */
public record SalleDuStage(Long idStage, Long idSalle, String numSalle, String numBatiment) {

    public static final String NEW = "new net.ent.etnc.orbit.repositories.projections.SalleDuStage";
}
