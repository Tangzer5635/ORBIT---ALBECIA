package net.ent.etnc.orbit.dtos.affectDTO;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupprimerSalleABatiment {
    @NotNull(message = "idSalle ne doit pas être null")
    private Long idSalle;

    @NotNull(message = "idBatiment ne doit pas être null")
    private Long idBatiment;
}