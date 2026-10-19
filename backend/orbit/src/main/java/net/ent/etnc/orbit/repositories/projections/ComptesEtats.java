package net.ent.etnc.orbit.repositories.projections;

public record ComptesEtats(Long id, Long defectueux, Long aChanger, Long libre, Long total) {

    public static final String NEW = "new net.ent.etnc.orbit.repositories.projections.ComptesEtats";

    private static final String OUVERTE = "exists (select d.id from Defaillance d where d.materiel = m"
            + " and d.statut = net.ent.etnc.orbit.models.enums.StatutDefaillance.SIGNALEE)";

    public static final String COLONNES =
            "sum(case when " + OUVERTE + " then 1 else 0 end), "
                    + "sum(case when not " + OUVERTE + " and m.dateAcquisition < :seuil then 1 else 0 end), "
                    + "sum(case when not " + OUVERTE + " and m.dateAcquisition >= :seuil"
                    + " and s.type = net.ent.etnc.orbit.models.enums.TypeSalle.SalleStockage then 1 else 0 end), "
                    + "count(m)";
}