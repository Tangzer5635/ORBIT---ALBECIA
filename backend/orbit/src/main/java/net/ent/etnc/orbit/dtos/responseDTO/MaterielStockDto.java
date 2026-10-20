package net.ent.etnc.orbit.dtos.responseDTO;

import lombok.*;

/** Une ligne de la liste « Affecter un matériel » : juste de quoi choisir (ni état, ni processus). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterielStockDto {
    private Long id;
    private String type;
    private String modele;
    private String numSerie;
    private String nomMachine;  // UC uniquement
    private String numSalle;    // réserve où il est rangé
    private String numBatiment;
}