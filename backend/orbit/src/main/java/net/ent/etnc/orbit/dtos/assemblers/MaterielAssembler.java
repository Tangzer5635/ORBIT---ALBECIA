package net.ent.etnc.orbit.dtos.assemblers;

import net.ent.etnc.orbit.dtos.requestDTO.MaterielRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.MaterielResponseDto;
import net.ent.etnc.orbit.dtos.responseDTO.MaterielStockDto;
import net.ent.etnc.orbit.models.entities.*;
import net.ent.etnc.orbit.models.enums.EtatMateriel;
import net.ent.etnc.orbit.repositories.projections.MaterielStock;
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
            dto.setAdresseMac(uc.getAdresseMac());
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

    /** Ligne de la liste « Affecter un matériel » : tout vient de la projection, aucune requête en plus. */
    public MaterielStockDto toStockDto(MaterielStock ligne) {
        Materiel materiel = ligne.materiel();
        return MaterielStockDto.builder()
                .id(materiel.getId())
                .type(materiel.getType())
                .modele(materiel.getModele())
                .numSerie(materiel.getNumSerie())
                .nomMachine(Hibernate.unproxy(materiel) instanceof UC uc ? uc.getNomMachine() : null)
                .numSalle(ligne.numSalle())
                .numBatiment(ligne.numBatiment())
                .build();
    }

    /** Le type est déjà validé par @Pattern sur le DTO. */
    /** État actuel du matériel sous forme de DTO de requête : base d'un PATCH (le type et le n° de série ne changent pas). */
    public MaterielRequestDto toRequestDto(Materiel materiel) {
        MaterielRequestDto dto = MaterielRequestDto.builder()
                .numSerie(materiel.getNumSerie())
                .modele(materiel.getModele())
                .dateFinGarantie(materiel.getDateFinGarantie())
                .dateAcquisition(materiel.getDateAcquisition())
                .type(materiel.getType())
                .build();
        // unproxy : un matériel chargé en LAZY est un proxy de Materiel, pas de UC
        if (Hibernate.unproxy(materiel) instanceof UC uc) {
            dto.setNomMachine(uc.getNomMachine());
            dto.setAdresseMac(uc.getAdresseMac());
        }
        return dto;
    }

    public Materiel toEntity(MaterielRequestDto dto) {
        Materiel materiel = switch (dto.getType()) {
            case UC.TYPE -> {
                UC uc = new UC();
                uc.setNomMachine(dto.getNomMachine());
                uc.setAdresseMac(dto.getAdresseMac());
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