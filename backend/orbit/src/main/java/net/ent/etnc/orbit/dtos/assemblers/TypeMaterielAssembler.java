package net.ent.etnc.orbit.dtos.assemblers;

import net.ent.etnc.orbit.dtos.requestDTO.TypeMaterielRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.TypeMaterielResponseDto;
import net.ent.etnc.orbit.models.entities.TypeMateriel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Stream;

@Component
public class TypeMaterielAssembler {

    public TypeMaterielResponseDto toDto(TypeMateriel type) {
        return new TypeMaterielResponseDto(type.getId(), type.getLibelle(), false);
    }

    /** Liste complète pour le front : les 4 types de base d'abord, puis les types ajoutés. */
    public List<TypeMaterielResponseDto> toListe(List<TypeMateriel> ajoutes) {
        return Stream.concat(
                TypeMateriel.DE_BASE.stream().map(code -> new TypeMaterielResponseDto(null, code, true)),
                ajoutes.stream().map(this::toDto)
        ).toList();
    }

    public TypeMateriel toEntity(TypeMaterielRequestDto dto) {
        TypeMateriel type = new TypeMateriel();
        type.setLibelle(dto.getLibelle());
        return type;
    }
}