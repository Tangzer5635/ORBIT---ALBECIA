package net.ent.etnc.orbit.dtos.responseDTO;

import lombok.*;
import net.ent.etnc.orbit.models.enums.EtatPoste;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosteResponseDto {

    private Long id;
    private String numPoste;
    private Long occupantId;
    private List<Long> materielsId;
    private EtatPoste etat;
}
