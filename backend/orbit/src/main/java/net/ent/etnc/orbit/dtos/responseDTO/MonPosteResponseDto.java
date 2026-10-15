package net.ent.etnc.orbit.dtos.responseDTO;

import lombok.*;
import net.ent.etnc.orbit.models.enums.EtatPoste;

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
    private EtatPoste etat;

    private Long idSalle;
    private String numSalle;
    private String etage;

    private Long idBatiment;
    private String numBatiment;
    private String stageNom; // stage accueilli par la salle, absent s'il n'y en a pas

    // mini-plan « Où se trouve mon poste » : postes stagiaires de la salle, dans l'ordre
    private List<PlanPosteDto> plan;

    private List<MaterielResponseDto> materiels;
}