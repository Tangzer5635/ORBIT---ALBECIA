package net.ent.etnc.orbit.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Corps de la requête d'authentification ({@code POST /api/v1/auth/login/}).
 */
@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "username doit contenir des caractères lisibles")
    private String username;
    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;
}
