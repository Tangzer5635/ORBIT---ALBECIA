package net.ent.etnc.orbit.dtos.assemblers;

import net.ent.etnc.orbit.dtos.requestDTO.MaterielRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.MaterielResponseDto;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.enums.EtatMateriel;
import org.springframework.stereotype.Component;

@Component
public class MaterielAssembler {

    public MaterielResponseDto toDto(Materiel materiel, EtatMateriel etat) {
        return MaterielResponseDto.builder()
                .id(materiel.getId())
                .numSerie(materiel.getNumSerie())
                .modele(materiel.getModele())
                .dateFinGarantie(materiel.getDateFinGarantie())
                .dateAcquisition(materiel.getDateAcquisition())
                .type(materiel.getType())
                .etat(etat)
                .dateArchivage(materiel.getDateArchivage())
                .build();
    }

    public Materiel toEntity(MaterielRequestDto dto) {
        Materiel materiel = new Materiel();
        materiel.setNumSerie(dto.getNumSerie());
        materiel.setModele(dto.getModele());
        materiel.setDateFinGarantie(dto.getDateFinGarantie());
        materiel.setDateAcquisition(dto.getDateAcquisition());
        materiel.setType(dto.getType());
        return materiel;
    }
}