package net.ent.etnc.orbit.dtos.requestDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefaillanceRequestDto {
    @NotNull(message = "idMateriel ne doit pas être null")
    private Long idMateriel;

    @NotBlank(message = "description doit contenir des caractères lisibles")
    @Length(min = 5, max = 500, message = "description doit avoir entre 5 et 500 caractères")
    private String description;
}
