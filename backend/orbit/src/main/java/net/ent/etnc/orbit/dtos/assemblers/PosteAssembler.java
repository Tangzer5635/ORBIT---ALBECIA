package net.ent.etnc.orbit.dtos.assemblers;

import net.ent.etnc.orbit.dtos.requestDTO.PosteRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.MonPosteResponseDto;
import net.ent.etnc.orbit.dtos.responseDTO.PosteResponseDto;
import net.ent.etnc.orbit.models.entities.Batiment;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.services.BatimentService;
import net.ent.etnc.orbit.services.MaterielService;
import net.ent.etnc.orbit.services.SalleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PosteAssembler {

    private final MaterielAssembler materielAssembler;
    private final MaterielService materielService;
    private final SalleService salleService;
    private final BatimentService batimentService;

    @Autowired
    public PosteAssembler(MaterielAssembler materielAssembler, MaterielService materielService, SalleService salleService, BatimentService batimentService) {
        this.materielAssembler = materielAssembler;
        this.materielService = materielService;
        this.salleService = salleService;
        this.batimentService = batimentService;
    }

    public PosteResponseDto toDto(Poste poste) {
        return PosteResponseDto.builder()
                .id(poste.getId())
                .numPoste(poste.getNumPoste())
                .occupantId(poste.getOccupant() == null ? null : poste.getOccupant().getId())
                .materielsId(poste.getMateriels().stream().map(Materiel::getId).toList())
                .build();
    }

    public List<PosteResponseDto> toDtos(List<Poste> postes) {
        return postes.stream()
                .map(this::toDto)
                .toList();
    }

    public Poste toEntity(PosteRequestDto posteDto) {
        Poste poste = new Poste();
        poste.setNumPoste(posteDto.getNumPoste());
        return poste;
    }

    public MonPosteResponseDto toDtoMonPoste(Poste poste){
        Salle salle = salleService.salleDuPoste(poste.getId()).orElse(null);
        Batiment batiment = batimentService.batimentDeLaSalle(salle.getId()).orElse(null);

        return MonPosteResponseDto.builder()
                .id(poste.getId())
                .numPoste(poste.getNumPoste())
                .idSalle(salle == null ? null : salle.getId())
                .numSalle(salle == null ? null : salle.getNumSalle())
                .etage(salle == null ? null : salle.getEtage())
                .idBatiment(batiment == null ? null : batiment.getId())
                .numBatiment(batiment == null ? null : batiment.getNumBatiment())
                .materiels(poste.getMateriels().stream()
                        .map(m -> materielAssembler.toDto(m, materielService.etatDe(m)))
                        .toList())
                .build();
    }
}
