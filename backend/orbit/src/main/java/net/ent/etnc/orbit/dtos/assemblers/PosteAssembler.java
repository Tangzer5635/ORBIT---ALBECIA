package net.ent.etnc.orbit.dtos.assemblers;

import net.ent.etnc.orbit.dtos.requestDTO.PosteRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.PosteResponseDto;
import net.ent.etnc.orbit.models.entities.Poste;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PosteAssembler {

    public PosteResponseDto toDto(Poste poste) {
        return PosteResponseDto.builder()
                .id(poste.getId())

                .build();
    }

    public List<PosteResponseDto> toDtos(List<Poste> postes) {
        return postes.stream()
                .map(this::toDto)
                .toList();
    }

    public Poste toEntity(PosteRequestDto posteDto) {
        Poste poste = new Poste();
        return poste;
    }

}