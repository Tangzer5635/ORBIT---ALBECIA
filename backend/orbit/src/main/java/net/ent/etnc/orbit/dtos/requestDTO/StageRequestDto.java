package net.ent.etnc.orbit.dtos.requestDTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import net.ent.etnc.orbit.models.enums.TypeFormation;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageRequestDto {
    @NotNull(message = "nom ne doit pas être null")
    @Pattern(regexp = "^[A-Z]{2}[0-9]{2}$", message = "nom : 2 lettres majuscules puis 2 chiffres (ex. WB47)")
    private String nom;

    @NotNull(message = "type ne doit pas être null")
    private TypeFormation type;

    @NotNull(message = "dateDebut ne doit pas être null")
    private LocalDate dateDebut;

    @NotNull(message = "dateFin ne doit pas être null")
    private LocalDate dateFin;

    @NotNull(message = "formateurId ne doit pas être null")
    private Long formateurId;
}