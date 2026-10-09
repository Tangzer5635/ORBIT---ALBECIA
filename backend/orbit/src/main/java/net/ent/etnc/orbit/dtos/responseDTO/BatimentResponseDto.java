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
}