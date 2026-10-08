package net.ent.etnc.orbit.repositories;

import net.ent.etnc.orbit.models.entities.RefreshToken;
import net.ent.etnc.orbit.repositories.commons.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository JPA pour l'entité {@link RefreshToken}.
 */
@Repository
public interface RefreshTokenRepository extends BaseRepository<RefreshToken> {

    /**
     * Recherche un refresh token par son hash SHA-256.
     *
     * @param tokenHash le hash hexadécimal du token brut
     * @return le token correspondant, ou {@link Optional#empty()} s'il n'existe pas
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Retourne tous les tokens appartenant à une famille donnée.
     * Utilisé pour la révocation en cascade lors d'une détection de vol.
     *
     * @param family l'UUID de la famille
     */
    List<RefreshToken> findAllByFamily(String family);

    /**
     * Supprime tous les refresh tokens d'un utilisateur (lors de la déconnexion totale).
     *
     * @param username de l'utilisateur
     */
    void deleteAllByUsername(String username);
}
