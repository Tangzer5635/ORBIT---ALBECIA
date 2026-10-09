package net.ent.etnc.orbit.dtos.assemblers;

import net.ent.etnc.orbit.dtos.requestDTO.SalleRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.SalleResponseDto;
import net.ent.etnc.orbit.models.entities.Salle;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SalleAssembler {

    public SalleResponseDto toDto(Salle salle) {
        return SalleResponseDto.builder()
                .id(salle.getId())
                .numSalle(salle.getNumSalle())
                .etage(salle.getEtage())
                .type(salle.getType())
                .build();
    }

    public List<SalleResponseDto> toDtos(List<Salle> salles) {
        return salles.stream()
                .map(this::toDto)
                .toList();
    }

    public Salle toEntity(SalleRequestDto salleDto) {
        Salle salle = new Salle();
        salle.setNumSalle(salleDto.getNumSalle());
        salle.setEtage(salleDto.getEtage());
        salle.setType(salleDto.getType());
        return salle;
    }

}