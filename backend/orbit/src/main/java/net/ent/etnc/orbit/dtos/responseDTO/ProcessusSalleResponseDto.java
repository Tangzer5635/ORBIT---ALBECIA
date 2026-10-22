package net.ent.etnc.orbit.dtos.responseDTO;

import lombok.*;
import net.ent.etnc.orbit.models.enums.ListeProcessus;

/** Un processus vu depuis une salle : liste globale, liste qui s'applique dans la salle et surcharges. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessusSalleResponseDto {
    private Long id;
    private String nom;
    private ListeProcessus listeGlobale;
    private ListeProcessus liste;            // celle qui s'applique dans la salle
    private String surchargePar;             // "Prénom Nom" du formateur dont la surcharge s'applique, null = liste globale
    private ListeProcessus maSurcharge;      // surcharge du formateur connecté, null s'il n'en a pas
}
