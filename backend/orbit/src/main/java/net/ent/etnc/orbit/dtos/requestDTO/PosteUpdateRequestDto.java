package net.ent.etnc.orbit.dtos.requestDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Modification d'un poste : seul le numéro change (un poste ne change pas de salle). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosteUpdateRequestDto {
    @NotBlank(message = "numPoste doit contenir des caractères lisibles")
    @Size(max = 50, message = "numPoste de max 50 caractères")
    private String numPoste;
}
