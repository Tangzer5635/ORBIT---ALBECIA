package net.ent.etnc.orbit.dtos.responseDTO;

import lombok.*;
import net.ent.etnc.orbit.models.enums.StatutDefaillance;
import net.ent.etnc.orbit.models.enums.TypeMateriel;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefaillanceResponseDto {
    private Long id;
    private String description;
    private LocalDateTime dateSignalement;
    private StatutDefaillance statut;

    private Long idMateriel;
    private String numSerieMateriel;
    private TypeMateriel typeMateriel;

    private Long idSignalePar;
    private String signalePar;

    private Long idTraitePar;
    private String traitePar;
    private String noteSolution;
    private LocalDateTime dateResolution;
}
