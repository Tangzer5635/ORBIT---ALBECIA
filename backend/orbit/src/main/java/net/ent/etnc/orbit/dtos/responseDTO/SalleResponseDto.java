package net.ent.etnc.orbit.dtos.responseDTO;

import lombok.*;
import net.ent.etnc.orbit.models.enums.TypeMateriel;
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
    private Map<TypeMateriel, Long> materielsParType;
}
