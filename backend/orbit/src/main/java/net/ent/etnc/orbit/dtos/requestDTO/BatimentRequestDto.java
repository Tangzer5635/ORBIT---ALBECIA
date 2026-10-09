package net.ent.etnc.orbit.dtos.requestDTO;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatimentRequestDto {
    @NotBlank(message = "numBatiment doit contenir des caractères lisibles")
    private String numBatiment;
}
