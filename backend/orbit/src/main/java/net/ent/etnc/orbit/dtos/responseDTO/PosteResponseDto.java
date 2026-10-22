package net.ent.etnc.orbit.dtos.responseDTO;

import lombok.*;
import net.ent.etnc.orbit.models.enums.EtatPoste;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosteResponseDto {

    private Long id;
    private String numPoste;
    private Long occupantId;
    private String occupant; // "Prénom Nom", absent si le poste est libre
    private List<Long> materielsId;
    private Map<String, Long> materielsParType; // ex. {"UC": 1, "Ecran": 2}
    private EtatPoste etat;
    private List<String> anomalies; // composition hors règle (ex. 2 UC) : vide si tout va bien
}