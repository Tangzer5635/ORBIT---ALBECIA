package net.ent.etnc.orbit.dtos;

import lombok.*;
import net.ent.etnc.orbit.dtos.responseDTO.ProcessusResponseDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Master (création, modification et lecture).
 * {@code dateCreation} est renseignée automatiquement si elle est absente.
 * En écriture on envoie {@code processusIds} ; en lecture on reçoit {@code processus}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterDto {

    private Long id;
    private String nom;
    private String versionMaster;
    private LocalDateTime dateCreation;
    @Builder.Default
    private List<Long> processusIds = new ArrayList<>();
    private List<ProcessusResponseDto> processus;
}