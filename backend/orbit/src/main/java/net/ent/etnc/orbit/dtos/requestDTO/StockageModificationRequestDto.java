package net.ent.etnc.orbit.dtos.requestDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/** « Modifier la salle de stockage » : nouveau numéro et nouvel étage. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockageModificationRequestDto {
    @NotBlank(message = "numSalle doit contenir des caractères lisibles")
    @Size(max = 10, message = "numSalle de max 10 caractères")
    private String numSalle;

    @NotBlank(message = "etage doit contenir des caractères lisibles")
    @Size(max = 5, message = "etage de max 5 caractères")
    private String etage;
}