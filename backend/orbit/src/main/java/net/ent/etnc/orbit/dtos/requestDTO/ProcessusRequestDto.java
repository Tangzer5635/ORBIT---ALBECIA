package net.ent.etnc.orbit.dtos.requestDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import net.ent.etnc.orbit.models.enums.ListeProcessus;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessusRequestDto {
    @NotBlank(message = "nom doit contenir des caractères lisibles")
    @Length(max = 100, message = "nom doit avoir au plus 100 caractères")
    private String nom;

    @NotNull(message = "liste ne doit pas être null")
    private ListeProcessus liste;
}