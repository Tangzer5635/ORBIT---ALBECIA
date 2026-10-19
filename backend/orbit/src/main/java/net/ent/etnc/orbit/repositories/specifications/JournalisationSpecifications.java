package net.ent.etnc.orbit.repositories.specifications;

import net.ent.etnc.orbit.models.entities.Journalisation;
import net.ent.etnc.orbit.models.enums.TypeAction;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * Filtres du journal, sur le même principe que PersonnelSpecifications.
 * La recherche texte (détail, id de l'auteur) passe par le filtre RSQL du contrôleur.
 */
public final class JournalisationSpecifications {

    private JournalisationSpecifications() {}

    /** Lignes dont la date est entre debut et fin (bornes incluses). */
    public static Specification<Journalisation> periode(LocalDateTime debut, LocalDateTime fin) {
        return (journalisation, query, cb) -> cb.between(journalisation.get("dateHeure"), debut, fin);
    }

    /** Type d'action exact (CONNEXION, SIGNALEMENT...). */
    public static Specification<Journalisation> action(TypeAction action) {
        return (journalisation, query, cb) -> cb.equal(journalisation.get("action"), action);
    }
}