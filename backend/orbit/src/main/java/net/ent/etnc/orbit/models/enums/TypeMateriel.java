package net.ent.etnc.orbit.models.enums;

public enum TypeMateriel {
    TableauInteractif("Tableau Interactif"),
    Imprimante("Imprimante"),
    Ecran("Ecran"),
    VideoProjecteur("Vidéo projecteur"),
    UC("Unité Centrale");

    private final String libelle;

    TypeMateriel(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

}
