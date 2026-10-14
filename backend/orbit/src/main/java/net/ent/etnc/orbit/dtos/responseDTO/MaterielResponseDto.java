package net.ent.etnc.orbit.dtos.responseDTO;

import lombok.*;
import net.ent.etnc.orbit.models.enums.EtatMateriel;
import net.ent.etnc.orbit.models.enums.TypeSalle;

import java.time.LocalDate;
import java.util.List;

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
    private String type;
    private String nomMachine;
    private List<ProcessusResponseDto> processus;
    private EtatMateriel etat;
    private LocalDate dateArchivage;

    // Salle où se trouve le matériel (absents s'il est archivé)
    private String numSalle;
    private TypeSalle typeSalle;
}