package net.ent.etnc.orbit.dtos.responseDTO;

import lombok.*;
import net.ent.etnc.orbit.models.enums.TypeMateriel;
import net.ent.etnc.orbit.models.enums.TypeSalle;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalleResponseDto {
    private Long id;
    private String numSalle;
    private String etage;
    private TypeSalle type;
    private List<Long> materielsId;
    private int nbPostes;
    private ComptesDto comptes;
    private Map<TypeMateriel, Long> materielsParType;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComptesDto {
        private int use;    // poste occupé, matériel en ordre
        private int dispo;  // poste libre, matériel en ordre
        private int chg;    // un matériel à remplacer bientôt (garantie)
        private int inc;    // poste sans matériel
        private int def;    // un matériel défectueux
    }
}
