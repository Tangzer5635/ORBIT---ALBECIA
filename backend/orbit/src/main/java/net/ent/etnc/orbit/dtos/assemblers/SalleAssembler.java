package net.ent.etnc.orbit.dtos.assemblers;

import net.ent.etnc.orbit.dtos.requestDTO.SalleRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.SalleResponseDto;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.EtatMateriel;
import net.ent.etnc.orbit.services.DefaillanceService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SalleAssembler {

    private final DefaillanceService defaillanceService;

    public SalleAssembler(DefaillanceService defaillanceService) {
        this.defaillanceService = defaillanceService;
    }

    public SalleResponseDto toDto(Salle salle) {
        return SalleResponseDto.builder()
                .id(salle.getId())
                .numSalle(salle.getNumSalle())
                .etage(salle.getEtage())
                .type(salle.getType())
                .materielsId(salle.getMateriels().stream().map(Materiel::getId).toList())
                .nbPostes(salle.getPostes().size())
                .comptes(comptes(salle))
                .materielsParType(salle.getMateriels().stream()
                        .collect(Collectors.groupingBy(Materiel::getType, Collectors.counting())))
                .build();
    }

    public SalleResponseDto.ComptesDto comptes(Salle salle) {
        SalleResponseDto.ComptesDto c = new SalleResponseDto.ComptesDto();
        for (Poste poste : salle.getPostes()) {
            List<EtatMateriel> etats = poste.getMateriels().stream().map(this::etat).toList();
            if (etats.isEmpty()) c.setInc(c.getInc() + 1);
            else if (etats.contains(EtatMateriel.DEFECTUEUX)) c.setDef(c.getDef() + 1);
            else if (etats.contains(EtatMateriel.A_REMPLACER)) c.setChg(c.getChg() + 1);
            else if (poste.getOccupant() != null) c.setUse(c.getUse() + 1);
            else c.setDispo(c.getDispo() + 1);
        }
        return c;
    }

    // un matériel posé sur un poste n'est jamais en stock
    private EtatMateriel etat(Materiel materiel) {
        return materiel.calculerEtat(false, defaillanceService.aUneDefaillanceOuverte(materiel.getId()));
    }

    public List<SalleResponseDto> toDtos(List<Salle> salles) {
        return salles.stream()
                .map(this::toDto)
                .toList();
    }

    public Salle toEntity(SalleRequestDto salleDto) {
        Salle salle = new Salle();
        salle.setNumSalle(salleDto.getNumSalle());
        salle.setEtage(salleDto.getEtage());
        salle.setType(salleDto.getType());
        return salle;
    }

}