package net.ent.etnc.orbit.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import net.ent.etnc.orbit.models.entities.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utilitaire de génération et validation des access tokens JWT (JJWT 0.13.x).
 *
 * <p>Le token contient le sujet (username), le rôle ({@code claim("role")}), et une expiration.
 * La clé est dérivée de {@code app.jwt.secret} via HMAC-SHA. Le token n'est jamais persisté —
 * il est stateless et revalidé à chaque requête par {@link AuthJwtFilter}.
 */
@Component
public class JwtUtils {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    /**
     * Génère un access token signé contenant l'email et le rôle de l'utilisateur.
     */
    public String generateJwtToken(User user) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("role", user.getRole().name())
                .claim("username",  user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key)
                .compact();
    }

    /** Extrait le usernam (subject) d'un token valide. */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Retourne {@code true} si le token est correctement signé et non expiré.
     * Avale silencieusement toute {@link JwtException} — le filter ignorera simplement ce token.
     */
    public boolean validateJwtToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
