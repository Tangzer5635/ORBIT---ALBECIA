package net.ent.etnc.orbit.dtos.assemblers;

import net.ent.etnc.orbit.dtos.requestDTO.BatimentRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.BatimentResponseDto;
import net.ent.etnc.orbit.dtos.responseDTO.SalleResponseDto;
import net.ent.etnc.orbit.models.entities.Batiment;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BatimentAssembler {

    private final SalleAssembler salleAssembler;

    public BatimentAssembler(SalleAssembler salleAssembler) {
        this.salleAssembler = salleAssembler;
    }


    public BatimentResponseDto toDto(Batiment batiment) {
        List<Salle> stockages = batiment.getSalles().stream()
                .filter(s -> s.getType() == TypeSalle.SalleStockage)
                .toList();
        return BatimentResponseDto.builder()
                .id(batiment.getId())
                .numBatiment(batiment.getNumBatiment())
                .sallesId(batiment.getSalles().stream().map(Salle::getId).toList())
                .nbSalles(batiment.getSalles().size() - stockages.size())
                .stockages(stockages.stream()
                        .map(s -> BatimentResponseDto.StockageDto.builder()
                                .id(s.getId())
                                .numSalle(s.getNumSalle())
                                .nbMateriels(s.getMateriels().size())
                                .build())
                        .toList())
                .comptes(comptes(batiment))
                .build();
    }

    private SalleResponseDto.ComptesDto comptes(Batiment batiment) {
        SalleResponseDto.ComptesDto total = new SalleResponseDto.ComptesDto();
        for (Salle salle : batiment.getSalles()) {
            SalleResponseDto.ComptesDto c = salleAssembler.comptes(salle);
            total.setUse(total.getUse() + c.getUse());
            total.setDispo(total.getDispo() + c.getDispo());
            total.setChg(total.getChg() + c.getChg());
            total.setInc(total.getInc() + c.getInc());
            total.setDef(total.getDef() + c.getDef());
        }
        return total;
    }

    public List<BatimentResponseDto> toDtos(List<Batiment> batiments) {
        return batiments.stream()
                .map(this::toDto)
                .toList();
    }

    public Batiment toEntity(BatimentRequestDto batimentDto) {
        Batiment batiment = new Batiment();
        batiment.setNumBatiment(batimentDto.getNumBatiment());
        return batiment;
    }

}