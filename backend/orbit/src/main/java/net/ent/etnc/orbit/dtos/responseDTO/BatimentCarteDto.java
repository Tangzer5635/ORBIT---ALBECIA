package net.ent.etnc.orbit.dtos.responseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatimentCarteDto {
    private Long id;
    private String numBatiment;
    private long nbSalles;
    private List<BatimentResponseDto.StockageDto> stockages;
    private AnneauDto anneau;
}