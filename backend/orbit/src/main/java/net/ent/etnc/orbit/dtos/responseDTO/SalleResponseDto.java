package net.ent.etnc.orbit.dtos.responseDTO;

import lombok.*;
import net.ent.etnc.orbit.models.enums.TypeSalle;

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
}