package net.ent.etnc.orbit.dtos.requestDTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/** « Modifier la salle » : nouvelle composition (le matériel manquant vient du stock, celui en trop y retourne). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SalleModificationRequestDto {
    @NotNull @Min(0) @Max(50)
    private Integer nbPostes; // postes stagiaires (le poste formateur ne change pas)

    @Min(0) @Max(5)
    private int nbVideoProjecteurs;

    @Min(0) @Max(5)
    private int nbTableaux;
}