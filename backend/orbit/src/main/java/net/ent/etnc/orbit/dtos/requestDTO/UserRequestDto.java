package net.ent.etnc.orbit.dtos.requestDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import net.ent.etnc.orbit.models.enums.Role;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDto {

    @NotBlank(message = "nid doit contenir des caractères lisibles")
    private String nid;
    @NotBlank(message = "nom doit contenir des caractères lisibles")
    private String nom;
    @NotBlank(message = "prenom doit contenir des caractères lisibles")
    private String prenom;
    @NotBlank(message = "login doit contenir des caractères lisibles")
    private String login;
    private String motDePasse;
    @NotNull(message = "Le rôle est obligatoire")
    private Role role;
    private Boolean active;
}