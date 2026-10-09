package net.ent.etnc.orbit.dtos.affectDTO;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterielAffectDto {
    @NotNull(message = "idMateriel ne doit pas être null")
    private Long idMateriel;

    @NotNull(message = "idSalle ne doit pas être null")
    private Long idSalle;
}