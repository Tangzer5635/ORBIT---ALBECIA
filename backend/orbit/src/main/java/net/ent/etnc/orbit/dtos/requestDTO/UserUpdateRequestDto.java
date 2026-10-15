package net.ent.etnc.orbit.dtos.requestDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Modification d'un compte. Le mot de passe est facultatif ; rôle et activation ont leurs propres endpoints. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequestDto {

    // Facultatif en modification : le formulaire ne demande pas le NID.
    // S’il est fourni, il doit contenir exactement 10 chiffres.
    @Pattern(regexp = "^[0-9]{10}$", message = "nid doit contenir exactement 10 chiffres")
    private String nid;
    @NotBlank(message = "nom doit contenir des caractères lisibles")
    @Size(max = 50, message = "nom de max 50 caractères")
    private String nom;
    @NotBlank(message = "prenom doit contenir des caractères lisibles")
    @Size(max = 30, message = "prenom de max 30 caractères")
    private String prenom;
    @NotBlank(message = "login doit contenir des caractères lisibles")
    @Size(min = 3, max = 50, message = "login doit avoir entre 3 et 50 caractères")
    private String login;
    @Size(max = 72, message = "motDePasse de max 72 caractères")
    private String motDePasse;
}