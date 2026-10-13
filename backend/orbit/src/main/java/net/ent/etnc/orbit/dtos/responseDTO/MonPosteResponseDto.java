package net.ent.etnc.orbit.dtos.responseDTO;

import lombok.*;

import java.util.List;

/**
 * Poste de l'utilisateur connecté (UC « Consulter son poste ») :
 * le poste, sa salle, son bâtiment et le détail de son matériel.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonPosteResponseDto {

    private Long id;
    private String numPoste;

    private Long idSalle;
    private String numSalle;
    private String etage;

    private Long idBatiment;
    private String numBatiment;

    private List<MaterielResponseDto> materiels;
}
