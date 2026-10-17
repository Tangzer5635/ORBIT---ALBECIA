package net.ent.etnc.orbit.services.commons;

import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.entities.Stage;

/**
 * Libellés des objets cités dans le détail d'une ligne du journal, pour qu'ils s'écrivent partout pareil.
 * Une personne est désignée par son ID et son login, jamais par son nom ni son prénom (CRE 3).
 */
public final class Libelles {

    private Libelles() {}

    /** "ID 15 (j.dupont)" */
    public static String personne(Personnel p) {
        return "ID " + p.getId() + " (" + p.getLogin() + ")";
    }

    /** "matériel SN-123 (écran)" */
    public static String materiel(Materiel m) {
        String type = switch (m.getType()) {
            case "UC" -> "unité centrale";
            case "Ecran" -> "écran";
            case "VideoProjecteur" -> "vidéoprojecteur";
            case "TableauInteractif" -> "tableau interactif";
            default -> m.getType();
        };
        return "matériel " + m.getNumSerie() + " (" + type + ")";
    }

    /** "salle 105" */
    public static String salle(Salle s) {
        return "salle " + s.getNumSalle();
    }

    /** "poste B2-S105-P07" (le numéro contient déjà le bâtiment et la salle) */
    public static String poste(Poste p) {
        return "poste " + p.getNumPoste();
    }

    /** "stage WB47" */
    public static String stage(Stage s) {
        return "stage " + s.getNom();
    }
}