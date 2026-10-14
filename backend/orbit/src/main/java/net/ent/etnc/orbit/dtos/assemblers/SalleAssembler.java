package net.ent.etnc.orbit.dtos.assemblers;

import net.ent.etnc.orbit.dtos.requestDTO.SalleRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.SalleResponseDto;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Salle;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class SalleAssembler {

    public SalleResponseDto toDto(Salle salle) {
        return SalleResponseDto.builder()
                .id(salle.getId())
                .numSalle(salle.getNumSalle())
                .etage(salle.getEtage())
                .type(salle.getType())
                .materielsId(salle.getMateriels().stream().map(Materiel::getId).toList())
                .nbPostes(salle.getPostes().size())
                .materielsParType(salle.getMateriels().stream()
                        .collect(Collectors.groupingBy(Materiel::getType, Collectors.counting())))
                .stageId(salle.getStage() != null ? salle.getStage().getId() : null)
                .stageNom(salle.getStage() != null ? salle.getStage().getNom() : null)
                .build();
    }

    public Salle toEntity(SalleRequestDto salleDto) {
        Salle salle = new Salle();
        salle.setNumSalle(salleDto.getNumSalle());
        salle.setEtage(salleDto.getEtage());
        salle.setType(salleDto.getType());
        return salle;
    }

}