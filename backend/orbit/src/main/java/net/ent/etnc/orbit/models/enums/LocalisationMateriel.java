package net.ent.etnc.orbit.models.enums;

/**
 * Filtre de localisation de GET /materiels/ (?localisation=).
 * PARTOUT = tout le matériel en service (hors archivés).
 */
public enum LocalisationMateriel {
    PARTOUT,
    SALLE,
    STOCK,
    ARCHIVE
}
