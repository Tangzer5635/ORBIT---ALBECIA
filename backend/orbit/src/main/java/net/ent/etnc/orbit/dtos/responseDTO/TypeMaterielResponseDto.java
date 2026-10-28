package net.ent.etnc.orbit.dtos.responseDTO;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypeMaterielResponseDto {
    /** null pour un type de base. */
    private Long id;
    /** Code pour un type de base ("UC", "Ecran"...), libellé saisi pour un type ajouté. */
    private String libelle;
    /** true = type de base, non supprimable. */
    private boolean deBase;
}