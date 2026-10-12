package net.ent.etnc.orbit.dtos.requestDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import net.ent.etnc.orbit.models.enums.TypeMateriel;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterielRequestDto {
    @NotBlank(message = "numSerie doit contenir des caractères lisibles")
    @Length(min = 3, max = 50, message = "numSerie doit avoir entre 3 et 50 caractères")
    private String numSerie;

    @NotBlank(message = "modele doit contenir des caractères lisibles")
    @Length(max = 50, message = "modele doit avoir au plus 50 caractères")
    private String modele;

    @NotNull(message = "dateFinGarantie ne doit pas être null")
    private LocalDate dateFinGarantie;

    @NotNull(message = "dateAcquisition ne doit pas être null")
    private LocalDate dateAcquisition;

    @NotNull(message = "type ne doit pas être null")
    private TypeMateriel type;

    private Long idStockage;
}
