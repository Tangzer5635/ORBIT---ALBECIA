package net.ent.etnc.orbit.dtos.assemblers;

import net.ent.etnc.orbit.dtos.requestDTO.StageRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.StageResponseDto;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.entities.Stage;
import net.ent.etnc.orbit.repositories.projections.SalleDuStage;
import org.springframework.stereotype.Component;

@Component
public class StageAssembler {

    /** @param salle salle qui accueille le stage, ou null s'il n'en a pas */
    public StageResponseDto toDto(Stage stage, SalleDuStage salle) {
        Personnel f = stage.getFormateur();
        return StageResponseDto.builder()
                .id(stage.getId())
                .nom(stage.getNom())
                .type(stage.getType())
                .dateDebut(stage.getDateDebut())
                .dateFin(stage.getDateFin())
                .formateurId(f.getId())
                .formateur(f.getPrenom() + " " + f.getNom())
                .salleId(salle != null ? salle.idSalle() : null)
                .numSalle(salle != null ? salle.numSalle() : null)
                .numBatiment(salle != null ? salle.numBatiment() : null)
                .build();
    }

    /** Le formateur est résolu par le service à partir de formateurId. */
    public Stage toEntity(StageRequestDto dto) {
        Stage stage = new Stage();
        stage.setNom(dto.getNom());
        stage.setType(dto.getType());
        stage.setDateDebut(dto.getDateDebut());
        stage.setDateFin(dto.getDateFin());
        return stage;
    }
}
