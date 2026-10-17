package net.ent.etnc.orbit.services;

import net.ent.etnc.orbit.models.entities.Journalisation;
import net.ent.etnc.orbit.models.enums.TypeAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * Journal des actions. Volontairement sans update ni delete : le journal est en lecture seule.
 * N'étend pas {@code Service<T, ID>} pour ne pas hériter du CRUD complet.
 */
public interface JournalisationService {

    /**
     * Journalise une action faite par l'utilisateur connecté.
     * Sans utilisateur connecté (ex. jeu de données créé par Init au démarrage), rien n'est journalisé.
     */
    void journaliser(TypeAction action, String detail);

    /** Journalise une action dont l'auteur est donné (connexion, quand l'utilisateur n'est pas encore dans le contexte). */
    void journaliser(Long auteurId, TypeAction action, String detail);

    /**
     * Logs sur une période, filtrés en plus par la recherche.
     *
     * @param filtre filtre de la recherche (RSQL et type d'action), null = pas de filtre
     * @param debut  facultatif
     * @param fin    facultatif (maintenant par défaut)
     */
    Page<Journalisation> rechercher(Specification<Journalisation> filtre, LocalDateTime debut, LocalDateTime fin,
                                    Pageable pageable);
}