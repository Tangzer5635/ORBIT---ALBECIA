package net.ent.etnc.orbit.dtos.assemblers;

import net.ent.etnc.orbit.dtos.responseDTO.DefaillanceResponseDto;
import net.ent.etnc.orbit.models.entities.Defaillance;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Personnel;
import org.springframework.stereotype.Component;

/**
 * Pas de toEntity : une défaillance se crée uniquement par DefaillanceService.signaler()
 * (auteur et date fixés côté serveur, jamais par le client).
 */
@Component
public class DefaillanceAssembler {

    public DefaillanceResponseDto toDto(Defaillance d) {
        Materiel m = d.getMateriel();
        Personnel signale = d.getSignalePar();
        Personnel traite = d.getTraitePar();
        return DefaillanceResponseDto.builder()
                .id(d.getId())
                .description(d.getDescription())
                .dateSignalement(d.getDateSignalement())
                .statut(d.getStatut())
                .idMateriel(m.getId())
                .numSerieMateriel(m.getNumSerie())
                .typeMateriel(m.getType())
                .idSignalePar(signale.getId())
                .signalePar(nomComplet(signale))
                .idTraitePar(traite != null ? traite.getId() : null)
                .traitePar(traite != null ? nomComplet(traite) : null)
                .noteSolution(d.getNoteSolution())
                .dateResolution(d.getDateResolution())
                .build();
    }

    private static String nomComplet(Personnel p) {
        return p.getPrenom() + " " + p.getNom();
    }
}
