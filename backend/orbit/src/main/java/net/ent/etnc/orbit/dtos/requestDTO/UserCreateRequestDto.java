package net.ent.etnc.orbit.dtos.requestDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import net.ent.etnc.orbit.models.enums.Role;

/** Création d'un compte. Un compte peut être créé en amont, inactif ({@code active = false}). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequestDto {

    @NotBlank(message = "nid doit contenir des caractères lisibles")
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
    @NotBlank(message = "motDePasse doit contenir des caractères lisibles")
    @Size(max = 72, message = "motDePasse de max 72 caractères")
    private String motDePasse;
    @NotNull(message = "Le rôle est obligatoire")
    private Role role;
    private Boolean active;
}