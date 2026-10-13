package net.ent.etnc.orbit.dtos.requestDTO;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SalleEquipeeRequestDto {
    @NotNull(message = "idBatiment ne doit pas être null")
    private Long idBatiment;

    @NotBlank(message = "numSalle doit contenir des caractères lisibles")
    private String numSalle;

    @NotBlank(message = "etage doit contenir des caractères lisibles")
    private String etage;

    @NotNull @Min(0)
    private Integer nbPostes;

    @NotNull @Min(1) @Max(2)
    private Integer ecransParPoste;

    @Min(0)
    private int nbVideoProjecteurs;

    @Min(0)
    private int nbTableaux;
}