package net.ent.etnc.orbit.dtos.responseDTO;

import lombok.*;
import net.ent.etnc.orbit.models.enums.TypeFormation;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageResponseDto {
    private Long id;
    private String nom;
    private TypeFormation type;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Long formateurId;
    private String formateur; // "Prénom Nom"
    private Long salleId;       // salle qui accueille le stage, absente s'il n'en a pas
    private String numSalle;
    private String numBatiment;
}
