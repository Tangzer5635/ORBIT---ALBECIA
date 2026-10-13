package net.ent.etnc.orbit.dtos.responseDTO;

import net.ent.etnc.orbit.repositories.projections.ComptesEtats;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Anneau des pages Bâtiments et Salles : matériels comptés par état.
 */
public record AnneauDto(long libre, long bonEtat, long defectueux, long aChanger) {

    public static final AnneauDto VIDE = new AnneauDto(0, 0, 0, 0);

    public static AnneauDto de(ComptesEtats c) {
        long bonEtat = c.total() - c.defectueux() - c.aChanger() - c.libre();
        return new AnneauDto(c.libre(), bonEtat, c.defectueux(), c.aChanger());
    }

    public AnneauDto plus(AnneauDto o) {
        return new AnneauDto(libre + o.libre, bonEtat + o.bonEtat, defectueux + o.defectueux, aChanger + o.aChanger);
    }

    public static Map<Long, AnneauDto> parId(List<ComptesEtats> enSalle, List<ComptesEtats> surPoste) {
        Map<Long, AnneauDto> anneaux = new HashMap<>();
        Stream.concat(enSalle.stream(), surPoste.stream())
                .forEach(c -> anneaux.merge(c.id(), de(c), AnneauDto::plus));
        return anneaux;
    }
}