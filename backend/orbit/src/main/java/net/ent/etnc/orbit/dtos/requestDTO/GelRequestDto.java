package net.ent.etnc.orbit.dtos.requestDTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import net.ent.etnc.orbit.models.enums.MotifGel;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GelRequestDto {
    @NotNull(message = "Choisissez un motif")
    private MotifGel motif;

    @Size(max = 200, message = "note de max 200 caractères")
    private String note;   // précision, facultative

    private LocalDate jusqu; // fin prévue, facultative
}