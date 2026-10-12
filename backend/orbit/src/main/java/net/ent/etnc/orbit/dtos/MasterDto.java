package net.ent.etnc.orbit.dtos;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Master (création, modification et lecture).
 * {@code dateCreation} est renseignée automatiquement si elle est absente.
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
}
