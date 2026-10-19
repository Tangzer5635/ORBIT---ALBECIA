package net.ent.etnc.orbit.dtos.assemblers;

import net.ent.etnc.orbit.dtos.requestDTO.SalleRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.FormateurDto;
import net.ent.etnc.orbit.dtos.responseDTO.SalleResponseDto;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.entities.Stage;
import net.ent.etnc.orbit.models.enums.Role;
import org.springframework.stereotype.Component;

import java.util.List;
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
                .masterId(salle.getMaster() != null ? salle.getMaster().getId() : null)
                .masterNom(salle.getMaster() != null
                        ? salle.getMaster().getNom() + " (" + salle.getMaster().getVersionMaster() + ")" : null)
                .creePar(salle.getCreePar())
                .suspendu(salle.isSuspendu())
                .gelMotif(salle.getGelMotif())
                .gelNote(salle.getGelNote())
                .gelDepuis(salle.getGelDepuis())
                .gelJusqu(salle.getGelJusqu())
                .build();
    }

    /** Formateurs attribués à la salle (charge les personnels : à n'appeler que pour une salle). */
    public List<FormateurDto> formateurs(Salle salle) {
        return salle.getPersonnels().stream()
                .filter(p -> p.getRole() == Role.FORMATEUR)
                .map(p -> new FormateurDto(p.getId(), p.getPrenom(), p.getNom()))
                .toList();
    }

    /** Ajoute le détail du stage accueilli (charge le stage et son formateur : à n'appeler que pour une salle). */
    public void completerStage(SalleResponseDto dto, Salle salle) {
        Stage stage = salle.getStage();
        if (stage == null) return;
        dto.setStageType(stage.getType());
        dto.setStageDateDebut(stage.getDateDebut());
        dto.setStageDateFin(stage.getDateFin());
        dto.setStageFormateur(stage.getFormateur().getPrenom() + " " + stage.getFormateur().getNom());
    }

    public Salle toEntity(SalleRequestDto salleDto) {
        Salle salle = new Salle();
        salle.setNumSalle(salleDto.getNumSalle());
        salle.setEtage(salleDto.getEtage());
        salle.setType(salleDto.getType());
        return salle;
    }

}