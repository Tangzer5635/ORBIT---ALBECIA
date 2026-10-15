package net.ent.etnc.orbit.dtos.responseDTO;

import lombok.*;
import net.ent.etnc.orbit.models.enums.TypeFormation;
import net.ent.etnc.orbit.models.enums.TypeSalle;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalleResponseDto {
    private Long id;
    private String numSalle;
    private String etage;
    private TypeSalle type;
    private List<Long> materielsId;
    private int nbPostes;
    private Map<String, Long> materielsParType;
    private AnneauDto anneau;
    private Long stageId;     // stage accueilli par la salle, absent s'il n'y en a pas
    private String stageNom;
    // détail du stage, seulement sur GET /salles/{id}/ (panneau « Vue de la salle »)
    private TypeFormation stageType;
    private LocalDate stageDateDebut;
    private LocalDate stageDateFin;
    private String stageFormateur; // "Prénom Nom" du formateur qui gère le stage
    private List<FormateurDto> formateurs; // formateurs attribués, seulement sur GET /salles/{id}/
}