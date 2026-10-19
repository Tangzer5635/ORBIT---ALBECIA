package net.ent.etnc.orbit.dtos.responseDTO;

import net.ent.etnc.orbit.models.enums.EtatPoste;
import net.ent.etnc.orbit.repositories.projections.ComptesEtats;

import java.util.Collection;

/**
 * Anneau d'une salle de cours : ses postes comptés par état.
 * L'anneau d'un bâtiment additionne ceux de ses salles (moyenne pondérée par le nombre de postes).
 * Une réserve n'a pas de poste : son anneau compte son matériel en stock.
 */
public record AnneauDto(long libre, long bonEtat, long defectueux, long aChanger) {

    public static final AnneauDto VIDE = new AnneauDto(0, 0, 0, 0);

    /** Un poste à compléter n'est pas utilisable : il compte avec les défectueux. */
    public static AnneauDto dePostes(Collection<EtatPoste> etats) {
        long libre = 0, bonEtat = 0, defectueux = 0, aChanger = 0;
        for (EtatPoste e : etats) {
            switch (e) {
                case DISPONIBLE -> libre++;
                case UTILISE -> bonEtat++;
                case A_CHANGER -> aChanger++;
                case DEFECTUEUX, A_COMPLETER -> defectueux++;
            }
        }
        return new AnneauDto(libre, bonEtat, defectueux, aChanger);
    }

    /** Matériel d'une réserve compté par état. */
    public static AnneauDto de(ComptesEtats c) {
        long bonEtat = c.total() - c.defectueux() - c.aChanger() - c.libre();
        return new AnneauDto(c.libre(), bonEtat, c.defectueux(), c.aChanger());
    }

    public AnneauDto plus(AnneauDto o) {
        return new AnneauDto(libre + o.libre, bonEtat + o.bonEtat, defectueux + o.defectueux, aChanger + o.aChanger);
    }
}