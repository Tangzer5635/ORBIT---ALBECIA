package net.ent.etnc.orbit.dtos.responseDTO;

import lombok.*;
import net.ent.etnc.orbit.models.enums.TypeAction;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalisationResponseDto {
    private Long id;
    private LocalDateTime dateHeure;
    private Long auteurId;
    private TypeAction action;
    private String detail;
}