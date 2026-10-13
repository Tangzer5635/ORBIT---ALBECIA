package net.ent.etnc.orbit.dtos.responseDTO;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatimentResponseDto {

    private Long id;
    private String numBatiment;
    private List<Long> sallesId;
    private int nbSalles;
    private List<StockageDto> stockages;
    private SalleResponseDto.ComptesDto comptes;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockageDto {
        private Long id;
        private String numSalle;
        private int nbMateriels;
    }


}