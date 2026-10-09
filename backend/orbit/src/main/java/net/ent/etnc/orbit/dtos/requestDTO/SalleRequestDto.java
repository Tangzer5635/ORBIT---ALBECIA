package net.ent.etnc.orbit.dtos.requestDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import net.ent.etnc.orbit.models.enums.TypeSalle;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalleRequestDto {
    @NotBlank(message = "numSalle doit contenir des caractères lisibles")
    private String numSalle;

    @NotBlank(message = "etage doit contenir des caractères lisibles")
    private String etage;

    @NotNull(message = "type ne doit pas être null")
    private TypeSalle type;
}