package net.ent.etnc.orbit.dtos.responseDTO;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterielResponseDto {

    private Long id;
    private String numSerie;
    private String modele;
    private LocalDate dateFinGarantie;

}