package net.ent.etnc.orbit.services.impl;

import lombok.extern.slf4j.Slf4j;

import net.ent.etnc.orbit.models.entities.RefreshToken;
import net.ent.etnc.orbit.repositories.RefreshTokenRepository;
import net.ent.etnc.orbit.services.RefreshTokenService;
import net.ent.etnc.orbit.services.commons.AbstractService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Implémentation de {@link RefreshTokenService} avec rotation et détection de vol.
 *
 * <p>Stratégie de sécurité :
 * <ul>
 *   <li>Le token brut (32 octets aléatoires encodés en Base64-URL, 256 bits d'entropie)
 *       n'est jamais persisté — seul son hash SHA-256 est stocké.</li>
 *   <li>Chaque utilisation d'un token génère un nouveau token dans la même famille (rotation).</li>
 *   <li>Si un token révoqué est présenté, toute sa famille est invalidée (family-based theft detection).</li>
 * </ul>
 *
 * <p>Le token brut est retourné directement par {@link #createRefreshToken} et
 * {@link #rotateRefreshToken} — l'appelant doit l'utiliser immédiatement
 * (transmission au client via cookie HttpOnly).
 */
@Slf4j
@Service
@Transactional
public class RefreshTokenServiceImpl extends AbstractService<RefreshToken, RefreshTokenRepository>
        implements RefreshTokenService {

    /** Instance partagée — {@link SecureRandom} est thread-safe et coûteux à instancier. */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final long refreshExpirationMs;

    public RefreshTokenServiceImpl(RefreshTokenRepository repository,
                                   @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        super(repository);
        this.refreshExpirationMs = refreshExpirationMs;
    }

    /**
     * Crée un nouveau refresh token (nouvelle famille) pour l'email donné.
     *
     * @param username de l'utilisateur
     * @return le token brut à transmettre au client
     */
    @Override
    public String createRefreshToken(String username) {
        String rawToken = generateRawToken();
        repository.save(buildToken(username, rawToken, UUID.randomUUID().toString()));
        return rawToken;
    }

    /**
     * Effectue la rotation : révoque l'ancien token et crée un nouveau dans la même famille.
     * Si le token présenté est déjà révoqué, toute la famille est invalidée.
     *
     * @param rawToken token brut reçu du client
     * @return nouveau token brut + email du propriétaire (pour générer un nouvel access token)
     * @throws ServiceException (RG-SEC01) token inconnu,
     *                          (RG-SEC02) token révoqué — vol détecté,
     *                          (RG-SEC03) token expiré
     */
    @Override
    @Transactional(noRollbackFor = ServiceException.class)
    public RotateResult rotateRefreshToken(String rawToken) {
        RefreshToken existing = repository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new ServiceException("RG-SEC01", "Refresh token invalide"));

        if (existing.isRevoked()) {
            revokeFamily(existing.getFamily());
            log.warn("VOL DÉTECTÉ — token révoqué réutilisé, famille {} invalidée. username={}",
                    existing.getFamily(), existing.getUsername());
            throw new ServiceException("RG-SEC02", "Refresh token révoqué — toutes vos sessions ont été fermées");
        }

        if (existing.getExpiresAt().isBefore(LocalDateTime.now())) {
            existing.setRevoked(true);
            repository.save(existing);
            throw new ServiceException("RG-SEC03", "Refresh token expiré");
        }

        existing.setRevoked(true);
        repository.save(existing);

        String newRaw = generateRawToken();
        repository.save(buildToken(existing.getUsername(), newRaw, existing.getFamily()));

        log.debug("Rotation OK — email={} famille={}", existing.getUsername(), existing.getFamily());
        return new RotateResult(newRaw, existing.getUsername());
    }

    /**
     * Révoque le token courant (déconnexion de la session active).
     * Silencieux si le token est inconnu — l'opération est idempotente.
     */
    @Override
    public void revokeToken(String rawToken) {
        repository.findByTokenHash(hash(rawToken)).ifPresent(t -> {
            t.setRevoked(true);
            repository.save(t);
        });
    }

    /**
     * Révoque tous les refresh tokens d'un utilisateur (déconnexion totale).
     */
    @Override
    public void revokeAllForUser(String username) {
        repository.deleteAllByUsername(username);
        log.info("Toutes les sessions révoquées pour username={}", username);
    }

    // ── Utilitaires privés ────────────────────────────────────────────────────

    /**
     * Construit et initialise un {@link RefreshToken} prêt à être persisté.
     * Factorisé pour éviter la duplication entre création et rotation.
     */
    private RefreshToken buildToken(String username, String rawToken, String family) {
        LocalDateTime now = LocalDateTime.now();
        RefreshToken token = new RefreshToken();
        token.setTokenHash(hash(rawToken));
        token.setUsername(username);
        token.setFamily(family);
        token.setExpiresAt(now.plus(Duration.ofMillis(refreshExpirationMs)));
        token.setRevoked(false);
        return token;
    }

    private void revokeFamily(String family) {
        List<RefreshToken> familyTokens = repository.findAllByFamily(family);
        familyTokens.forEach(t -> t.setRevoked(true));
        repository.saveAll(familyTokens);
    }

    /** Génère 32 octets cryptographiquement sûrs (256 bits) encodés en Base64-URL sans padding. */
    private static String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Calcule le hash SHA-256 du token brut et le retourne en hexadécimal minuscule. */
    private static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 non disponible", e);
        }
    }
}
