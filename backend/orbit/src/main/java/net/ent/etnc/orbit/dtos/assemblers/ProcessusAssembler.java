package net.ent.etnc.orbit.dtos.assemblers;

import net.ent.etnc.orbit.dtos.requestDTO.ProcessusRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.ProcessusResponseDto;
import net.ent.etnc.orbit.dtos.responseDTO.ProcessusSalleResponseDto;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.entities.Processus;
import net.ent.etnc.orbit.services.SurchargeProcessusService.ProcessusEnSalle;
import org.springframework.stereotype.Component;

@Component
public class ProcessusAssembler {

    public ProcessusResponseDto toDto(Processus processus) {
        return ProcessusResponseDto.builder()
                .id(processus.getId())
                .nom(processus.getNom())
                .liste(processus.getListe())
                .build();
    }

    public ProcessusSalleResponseDto toDto(ProcessusEnSalle enSalle) {
        Processus processus = enSalle.processus();
        Personnel auteur = enSalle.retenue() == null ? null : enSalle.retenue().getFormateur();
        return ProcessusSalleResponseDto.builder()
                .id(processus.getId())
                .nom(processus.getNom())
                .listeGlobale(processus.getListe())
                .liste(enSalle.liste())
                .surchargePar(auteur == null ? null : auteur.getPrenom() + " " + auteur.getNom())
                .maSurcharge(enSalle.maSurcharge() == null ? null : enSalle.maSurcharge().getListe())
                .build();
    }

    public Processus toEntity(ProcessusRequestDto dto) {
        Processus processus = new Processus();
        processus.setNom(dto.getNom());
        processus.setListe(dto.getListe());
        return processus;
    }
}