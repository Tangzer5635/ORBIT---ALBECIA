package net.ent.etnc.orbit.models.enums;

public enum TypeFormation {
    ESR("Réseaux"),
    ESI("Admin Système"),
    PDI("Développement"),
    CYB("Cybersécurité"),
    EST("Télécommunication");

    private final String intitule;

    TypeFormation(String intitule) {
        this.intitule = intitule;
    }

    public String getIntitule() {
        return intitule;
    }
}