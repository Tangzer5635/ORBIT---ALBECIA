package net.ent.etnc.orbit.dtos.responseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.ent.etnc.orbit.models.enums.TypeSalle;

/** Carte d'une salle sur la page /salles?batiment=XX. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalleCarteDto {
    private Long id;
    private String numSalle;
    private String numBatiment; // renseigné seulement pour « mes salles » (salles de plusieurs bâtiments)
    private String etage;
    private TypeSalle type;
    private long nbPostes;
    private AnneauDto anneau;
}