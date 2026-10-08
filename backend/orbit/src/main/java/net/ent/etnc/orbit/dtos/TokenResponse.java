package net.ent.etnc.orbit.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Réponse d'authentification contenant l'access token JWT.
 *
 * <p>Le refresh token n'est pas inclus ici — il est transmis exclusivement
 * via un cookie HttpOnly pour ne pas être lisible en JavaScript.
 */
@Getter
@Setter
@AllArgsConstructor
public class TokenResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private Long expiresIn;

    public TokenResponse(String accessToken, Long expiresIn) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
    }
}
