package net.ent.etnc.orbit.dtos.requestDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterielRequestDto {
    @NotBlank(message = "numSerie doit contenir des caractères lisibles")
    private String numSerie;

    @NotBlank(message = "modele doit contenir des caractères lisibles")
    private String modele;

    @NotNull(message = "dateFinGarantie ne doit pas être null")
    private LocalDate dateFinGarantie;

    @NotNull(message = "dateAcquisition ne doit pas être null")
    private LocalDate dateAcquisition;
}
