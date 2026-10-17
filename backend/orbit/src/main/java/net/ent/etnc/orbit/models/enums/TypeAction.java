package net.ent.etnc.orbit.models.enums;

/**
 * Type d'une action enregistrée dans le journal (table journalisation).
 * Les consultations ne sont pas journalisées.
 */
public enum TypeAction {
    CONNEXION,
    ECHEC_CONNEXION,
    DECONNEXION,
    CREATION,
    MODIFICATION,
    SUPPRESSION,
    AFFECTATION,
    /** Retrait d'une affectation (matériel remis en stock, stagiaire retiré d'un poste...). */
    RETRAIT,
    /** Défaillance remontée. */
    SIGNALEMENT,
    /** Défaillance résolue. */
    RESOLUTION,
    /** Import d'un fichier (stages). */
    IMPORT
}