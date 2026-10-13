package net.ent.etnc.orbit.repositories.projections;

public record StockageCompte(Long idBatiment, Long id, String numSalle, Long nbMateriels) {
    public static final String NEW = "new net.ent.etnc.orbit.repositories.projections.StockageCompte";
}