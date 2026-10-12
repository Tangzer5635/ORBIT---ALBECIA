package net.ent.etnc.orbit.dtos.requestDTO;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.validator.constraints.Length;

/**
 * Création et modification d'un poste.
 * {@code idSalle} est obligatoire à la création et ignoré à la modification
 * (un poste ne change pas de salle).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosteRequestDto {

    @NotBlank(message = "numPoste doit contenir des caractères lisibles")
    @Length(max = 50, message = "numPoste doit avoir au plus 50 caractères")
    private String numPoste;

    private Long idSalle;
}
