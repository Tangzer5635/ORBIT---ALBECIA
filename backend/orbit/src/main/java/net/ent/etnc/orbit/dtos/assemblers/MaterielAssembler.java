package net.ent.etnc.orbit.dtos.assemblers;

import net.ent.etnc.orbit.dtos.requestDTO.MaterielRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.MaterielResponseDto;
import net.ent.etnc.orbit.models.entities.Materiel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MaterielAssembler {

    public MaterielResponseDto toDto(Materiel materiel) {
        return MaterielResponseDto.builder()
                .id(materiel.getId())
                .numSerie(materiel.getNumSerie())
                .modele(materiel.getModele())
                .dateFinGarantie(materiel.getDateFinGarantie())

                .build();

    }

    public List<MaterielResponseDto> toDtos(List<Materiel> materiels) {
        return materiels.stream()
                .map(this::toDto)
                .toList();
    }

    public Materiel toEntity(MaterielRequestDto materielRequestDto) {
        Materiel materiel = new Materiel();
        materiel.setNumSerie(materielRequestDto.getNumSerie());
        materiel.setModele(materielRequestDto.getModele());
        materiel.setDateFinGarantie(materielRequestDto.getDateFinGarantie());
        materiel.setDateAcquisition(materielRequestDto.getDateAcquisition());
        return materiel;
    }

}