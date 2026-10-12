package net.ent.etnc.orbit.dtos.assemblers;

import net.ent.etnc.orbit.dtos.requestDTO.PosteRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.PosteResponseDto;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Poste;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PosteAssembler {

    public PosteResponseDto toDto(Poste poste) {
        return PosteResponseDto.builder()
                .id(poste.getId())
                .numPoste(poste.getNumPoste())
                .occupantId(poste.getOccupant() == null ? null : poste.getOccupant().getId())
                .materielsId(poste.getMateriels().stream().map(Materiel::getId).toList())
                .build();
    }

    public List<PosteResponseDto> toDtos(List<Poste> postes) {
        return postes.stream()
                .map(this::toDto)
                .toList();
    }

    public Poste toEntity(PosteRequestDto posteDto) {
        Poste poste = new Poste();
        poste.setNumPoste(posteDto.getNumPoste());
        return poste;
    }
}
