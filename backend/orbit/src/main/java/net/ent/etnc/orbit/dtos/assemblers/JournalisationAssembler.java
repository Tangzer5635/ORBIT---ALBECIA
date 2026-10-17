package net.ent.etnc.orbit.dtos.assemblers;

import net.ent.etnc.orbit.dtos.responseDTO.JournalisationResponseDto;
import net.ent.etnc.orbit.models.entities.Journalisation;
import org.springframework.stereotype.Component;

/** Lecture seule : pas de toEntity, une ligne de journal n'est jamais créée depuis un DTO. */
@Component
public class JournalisationAssembler {

    public JournalisationResponseDto toDto(Journalisation journalisation) {
        return JournalisationResponseDto.builder()
                .id(journalisation.getId())
                .dateHeure(journalisation.getDateHeure())
                .auteurId(journalisation.getAuteurId())
                .action(journalisation.getAction())
                .detail(journalisation.getDetail())
                .build();
    }
}