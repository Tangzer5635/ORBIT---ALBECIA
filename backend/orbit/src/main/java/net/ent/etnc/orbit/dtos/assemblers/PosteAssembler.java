package net.ent.etnc.orbit.dtos.assemblers;

import net.ent.etnc.orbit.dtos.requestDTO.PosteRequestDto;
import net.ent.etnc.orbit.dtos.requestDTO.PosteUpdateRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.MonPosteResponseDto;
import net.ent.etnc.orbit.dtos.responseDTO.PlanPosteDto;
import net.ent.etnc.orbit.dtos.responseDTO.PosteResponseDto;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.enums.EtatPoste;
import net.ent.etnc.orbit.repositories.projections.MonPosteLigne;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PosteAssembler {

    private final MaterielAssembler materielAssembler;

    @Autowired
    public PosteAssembler(MaterielAssembler materielAssembler) {
        this.materielAssembler = materielAssembler;
    }

    public PosteResponseDto toDto(Poste poste) {
        return PosteResponseDto.builder()
                .id(poste.getId())
                .numPoste(poste.getNumPoste())
                .occupantId(poste.getOccupant() == null ? null : poste.getOccupant().getId())
                .occupant(poste.getOccupant() == null ? null : poste.getOccupant().getPrenom() + " " + poste.getOccupant().getNom())
                .materielsId(poste.getMateriels().stream().map(Materiel::getId).toList())
                .materielsParType(poste.getMateriels().stream()
                        .collect(Collectors.groupingBy(Materiel::getType, Collectors.counting())))
                .anomalies(poste.anomalies())
                .build();
    }

    public PosteResponseDto toDto(Poste poste, EtatPoste etat) {
        PosteResponseDto dto = toDto(poste);
        dto.setEtat(etat);
        return dto;
    }
    public Poste toEntity(PosteRequestDto posteDto) {
        Poste poste = new Poste();
        poste.setNumPoste(posteDto.getNumPoste());
        return poste;
    }

    public Poste toEntity(PosteUpdateRequestDto posteDto) {
        Poste poste = new Poste();
        poste.setNumPoste(posteDto.getNumPoste());
        return poste;
    }

    /**
     * Construit « mon poste » à partir des lignes de la requête (au moins une : le poste lui-même).
     * Un matériel posé sur un poste n'est jamais en stock, d'où {@code enStock = false}.
     */
    public MonPosteResponseDto toDtoMonPoste(List<MonPosteLigne> lignes) {
        MonPosteLigne poste = lignes.get(0);
        return MonPosteResponseDto.builder()
                .id(poste.idPoste())
                .numPoste(poste.numPoste())
                .idSalle(poste.idSalle())
                .numSalle(poste.numSalle())
                .etage(poste.etage())
                .idBatiment(poste.idBatiment())
                .numBatiment(poste.numBatiment())
                .materiels(lignes.stream()
                        .filter(l -> l.materiel() != null)
                        .map(l -> materielAssembler.toDto(l.materiel(),
                                l.materiel().calculerEtat(false, l.defaillanceOuverte())))
                        .toList())
                .build();
    }

    /** Ajoute à « mon poste » son état, le stage de la salle et le mini-plan (postes stagiaires de la salle). */
    public void completerPlan(MonPosteResponseDto dto, List<Poste> postesSalle, Map<Long, EtatPoste> etats, String stageNom) {
        dto.setStageNom(stageNom);
        dto.setEtat(etats.get(dto.getId()));
        dto.setPlan(postesSalle.stream()
                .filter(p -> !p.getNumPoste().endsWith("-F"))
                .sorted(Comparator.comparing(Poste::getNumPoste))
                .map(p -> new PlanPosteDto(p.getNumPoste(), etats.get(p.getId())))
                .toList());
    }
}