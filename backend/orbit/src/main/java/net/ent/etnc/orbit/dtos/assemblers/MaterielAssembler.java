package net.ent.etnc.orbit.dtos.assemblers;

import net.ent.etnc.orbit.dtos.requestDTO.MaterielRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.MaterielResponseDto;
import net.ent.etnc.orbit.models.entities.*;
import net.ent.etnc.orbit.models.enums.EtatMateriel;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MaterielAssembler {

    private final ProcessusAssembler processusAssembler;

    @Autowired
    public MaterielAssembler(ProcessusAssembler processusAssembler) {
        this.processusAssembler = processusAssembler;
    }

    public MaterielResponseDto toDto(Materiel materiel, EtatMateriel etat) {
        MaterielResponseDto dto = MaterielResponseDto.builder()
                .id(materiel.getId())
                .numSerie(materiel.getNumSerie())
                .modele(materiel.getModele())
                .dateFinGarantie(materiel.getDateFinGarantie())
                .dateAcquisition(materiel.getDateAcquisition())
                .type(materiel.getType())
                .etat(etat)
                .dateArchivage(materiel.getDateArchivage())
                .build();
        // unproxy : un matériel chargé en LAZY est un proxy de Materiel, pas de UC
        if (Hibernate.unproxy(materiel) instanceof UC uc) {
            dto.setNomMachine(uc.getNomMachine());
            dto.setProcessus(uc.getProcessus().stream().map(processusAssembler::toDto).toList());
        }
        return dto;
    }

    /** Avec la salle où se trouve le matériel (null s'il est archivé). */
    public MaterielResponseDto toDto(Materiel materiel, EtatMateriel etat, Salle salle) {
        MaterielResponseDto dto = toDto(materiel, etat);
        if (salle != null) {
            dto.setNumSalle(salle.getNumSalle());
            dto.setTypeSalle(salle.getType());
        }
        return dto;
    }

    /** Le type est déjà validé par @Pattern sur le DTO. */
    public Materiel toEntity(MaterielRequestDto dto) {
        Materiel materiel = switch (dto.getType()) {
            case UC.TYPE -> {
                UC uc = new UC();
                uc.setNomMachine(dto.getNomMachine());
                yield uc;
            }
            case Ecran.TYPE -> new Ecran();
            case VideoProjecteur.TYPE -> new VideoProjecteur();
            case TableauInteractif.TYPE -> new TableauInteractif();
            default -> throw new IllegalArgumentException("Type de matériel inconnu : " + dto.getType());
        };
        materiel.setNumSerie(dto.getNumSerie());
        materiel.setModele(dto.getModele());
        materiel.setDateFinGarantie(dto.getDateFinGarantie());
        materiel.setDateAcquisition(dto.getDateAcquisition());
        return materiel;
    }
}