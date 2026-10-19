package net.ent.etnc.orbit.models.enums;

public enum TypeSalle {
    SalleDeCours("Salle de cours"),
    SalleStockage("Salle de stockage");

    private final String intitule;

    TypeSalle(String intitule) {
        this.intitule = intitule;
    }

    public String getIntitule() {
        return intitule;
    }
}
