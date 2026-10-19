package net.ent.etnc.orbit.dtos.requestDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatimentRequestDto {
    @NotBlank(message = "numBatiment doit contenir des caractères lisibles")
    private String numBatiment;

    /** Salle de stockage créée avec le bâtiment : « 199 » si non renseigné. */
    @Size(max = 10, message = "numSalleStockage de max 10 caractères")
    private String numSalleStockage;

    /** Étage de cette salle de stockage : rez-de-chaussée (« 0 ») si non renseigné. */
    @Size(max = 5, message = "etageStockage de max 5 caractères")
    private String etageStockage;
}