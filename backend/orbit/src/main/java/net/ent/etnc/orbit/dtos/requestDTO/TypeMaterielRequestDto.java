package net.ent.etnc.orbit.dtos.requestDTO;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypeMaterielRequestDto {
    @NotBlank(message = "libelle doit contenir des caractères lisibles")
    @Length(max = 50, message = "libelle doit avoir au plus 50 caractères")
    private String libelle;
}