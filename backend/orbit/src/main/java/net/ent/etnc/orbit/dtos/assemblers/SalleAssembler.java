package net.ent.etnc.orbit.dtos.assemblers;

import net.ent.etnc.orbit.dtos.requestDTO.SalleRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.FormateurDto;
import net.ent.etnc.orbit.dtos.responseDTO.SalleResponseDto;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.entities.Stage;
import net.ent.etnc.orbit.models.enums.Role;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

    /** Formateurs attribués à la salle, accès expiré compris (charge les personnels : à n'appeler que pour une salle). */
    public List<FormateurDto> formateurs(Salle salle) {
        return salle.getAcces().entrySet().stream()
                .filter(e -> e.getKey().getRole() == Role.FORMATEUR)
                .sorted(Comparator.comparing((Map.Entry<Personnel, LocalDate> e) -> e.getKey().getNom())
                        .thenComparing(e -> e.getKey().getPrenom()))
                .map(e -> new FormateurDto(e.getKey().getId(), e.getKey().getPrenom(), e.getKey().getNom(),
                        e.getValue(), !Salle.accesActif(e.getValue())))
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