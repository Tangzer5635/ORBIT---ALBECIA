package net.ent.etnc.orbit.services;


import net.ent.etnc.orbit.models.entities.RefreshToken;
import net.ent.etnc.orbit.services.commons.Service;

/**
 * Service de gestion des refresh tokens JWT.
 *
 * <p>Implémente une stratégie de rotation avec détection de vol (family-based theft detection) :
 * chaque utilisation d'un refresh token en génère un nouveau dans la même famille ;
 * si un token révoqué est réutilisé, toute la famille est invalidée.
 *
 * <p>Les tokens bruts (256 bits, Base64-URL) ne sont jamais persistés — seul leur hash SHA-256
 * est stocké en base. Les méthodes de mutation retournent directement le token brut à transmettre
 * au client, éliminant tout besoin de {@code ThreadLocal}.
 */
public interface RefreshTokenService extends Service<RefreshToken, Long> {

    /**
     * Résultat d'une rotation : nouveau token brut et email du propriétaire
     * (pour permettre à l'appelant de générer un nouvel access token sans requête supplémentaire).
     */
    record RotateResult(String rawToken, String username) {}

    /**
     * Crée un nouveau refresh token pour l'email donné et démarre une nouvelle famille.
     *
     * @param email email de l'utilisateur propriétaire
     * @return le token brut à transmettre dans le cookie HTTP-only
     */
    String createRefreshToken(String email);

    /**
     * Effectue la rotation d'un refresh token : révoque l'ancien et crée un nouveau dans la même famille.
     * Si le token est déjà révoqué, toute la famille est invalidée (détection de vol).
     *
     * @param rawToken le token brut reçu dans le cookie HTTP-only
     * @return le nouveau token brut et l'email du propriétaire
     * @throws net.ent.etnc.certificationcenter.services.commons.ServiceException
     *         (RG-SEC01) token inconnu, (RG-SEC02) vol détecté, (RG-SEC03) token expiré
     */
    RotateResult rotateRefreshToken(String rawToken);

    /**
     * Révoque le refresh token courant (déconnexion de la session active uniquement).
     * Si le token est inconnu, l'opération est silencieusement ignorée.
     *
     * @param rawToken token brut à révoquer
     */
    void revokeToken(String rawToken);

    /**
     * Révoque tous les refresh tokens d'un utilisateur (déconnexion totale, reset de mot de passe…).
     *
     * @param email email de l'utilisateur
     */
    void revokeAllForUser(String email);
}
