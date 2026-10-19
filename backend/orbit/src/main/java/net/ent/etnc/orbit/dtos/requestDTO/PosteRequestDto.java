package net.ent.etnc.orbit.dtos.requestDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Création d'un poste dans une salle. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosteRequestDto {

    @NotBlank(message = "numPoste doit contenir des caractères lisibles")
    @Size(max = 50, message = "numPoste de max 50 caractères")
    private String numPoste;

    @NotNull(message = "idSalle ne doit pas être null")
    private Long idSalle;
}
