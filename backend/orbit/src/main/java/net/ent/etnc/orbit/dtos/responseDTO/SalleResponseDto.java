package net.ent.etnc.orbit.dtos.responseDTO;

import lombok.*;
import net.ent.etnc.orbit.models.enums.TypeSalle;

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
    private List<FormateurDto> formateurs; // formateurs attribués, seulement sur GET /salles/{id}/
}