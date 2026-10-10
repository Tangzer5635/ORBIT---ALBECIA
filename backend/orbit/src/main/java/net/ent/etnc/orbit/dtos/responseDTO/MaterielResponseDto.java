package net.ent.etnc.orbit.dtos.responseDTO;

import lombok.*;
import net.ent.etnc.orbit.models.enums.EtatMateriel;
import net.ent.etnc.orbit.models.enums.TypeMateriel;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterielResponseDto {

    private Long id;
    private String numSerie;
    private String modele;
    private LocalDate dateFinGarantie;
    private LocalDate dateAcquisition;
    private TypeMateriel type;
    private EtatMateriel etat;

}