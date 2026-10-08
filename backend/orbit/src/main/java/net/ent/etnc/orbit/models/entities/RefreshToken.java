package net.ent.etnc.orbit.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.ent.etnc.orbit.models.commons.AbstractPersistableWithIdSetter;

import java.time.LocalDateTime;

/**
 * Refresh token persisté en base de données pour la rotation sécurisée des tokens JWT.
 *
 * <p>Seul le hash SHA-256 du token brut est stocké (jamais la valeur en clair).
 * Les tokens appartiennent à une famille ({@code family}) identifiée par un UUID :
 * si un token révoqué est présenté, toute sa famille est invalidée pour détecter
 * les tentatives de vol (family-based theft detection).
 */
@Entity
@Table(name = "refresh_tokens")
@EqualsAndHashCode(callSuper = false, of = {"tokenHash"})
@ToString(callSuper = true, of = {"username", "revoked"})
public class RefreshToken extends AbstractPersistableWithIdSetter<Long> {

    /** Hash SHA-256 du token brut (stocké en hexadécimal, 64 caractères). */
    @Getter
    @Setter
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    /** Username de l'utilisateur propriétaire du token, dénormalisé pour éviter une jointure. */
    @Getter
    @Setter
    @Column(name = "username", nullable = false, length = 100)
    private String username;

    /** Identifiant de famille (UUID) permettant la révocation groupée en cas de vol. */
    @Getter
    @Setter
    @Column(name = "family", nullable = false, length = 36)
    private String family;

    /** Date et heure d'expiration du token. */
    @Getter
    @Setter
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** {@code true} si le token a été utilisé ou révoqué manuellement. */
    @Getter
    @Setter
    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;
}
