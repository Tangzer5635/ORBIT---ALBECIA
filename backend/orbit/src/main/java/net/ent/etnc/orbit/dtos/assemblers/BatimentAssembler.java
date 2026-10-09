package net.ent.etnc.orbit.dtos.assemblers;

import net.ent.etnc.orbit.dtos.requestDTO.BatimentRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.BatimentResponseDto;
import net.ent.etnc.orbit.models.entities.Batiment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BatimentAssembler {

    public BatimentResponseDto toDto(Batiment batiment) {
        return BatimentResponseDto.builder()
                .id(batiment.getId())
                .numBatiment(batiment.getNumBatiment())
                .sallesId(batiment.getSalles().stream().map(s -> s.getId()))
                .build();
    }

    public List<BatimentResponseDto> toDtos(List<Batiment> batiments) {
        return batiments.stream()
                .map(this::toDto)
                .toList();
    }

    public Batiment toEntity(BatimentRequestDto batimentDto) {
        Batiment batiment = new Batiment();
        batiment.setNumBatiment(batimentDto.getNumBatiment());
        return batiment;
    }

}