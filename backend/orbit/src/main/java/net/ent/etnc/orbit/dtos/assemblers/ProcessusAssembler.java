package net.ent.etnc.orbit.dtos.assemblers;

import net.ent.etnc.orbit.dtos.requestDTO.ProcessusRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.ProcessusResponseDto;
import net.ent.etnc.orbit.models.entities.Processus;
import org.springframework.stereotype.Component;

@Component
public class ProcessusAssembler {

    public ProcessusResponseDto toDto(Processus processus) {
        return ProcessusResponseDto.builder()
                .id(processus.getId())
                .nom(processus.getNom())
                .liste(processus.getListe())
                .build();
    }

    public Processus toEntity(ProcessusRequestDto dto) {
        Processus processus = new Processus();
        processus.setNom(dto.getNom());
        processus.setListe(dto.getListe());
        return processus;
    }
}