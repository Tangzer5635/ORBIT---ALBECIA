package net.ent.etnc.orbit.dtos.requestDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import net.ent.etnc.orbit.models.entities.Materiel;
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
    @Pattern(regexp = Materiel.TYPES, message = "type inconnu")
    private String type;


    @Length(max = 50, message = "nomMachine doit avoir au plus 50 caractères")
    private String nomMachine;

    private Long idStockage;
}
