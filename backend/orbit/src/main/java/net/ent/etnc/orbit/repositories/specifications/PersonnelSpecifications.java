package net.ent.etnc.orbit.repositories.specifications;

import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.enums.Role;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtres de la page Utilisateurs, sur le même principe que MaterielSpecifications.
 * La recherche texte (nom, prénom, login) passe par le filtre RSQL du contrôleur.
 */
public final class PersonnelSpecifications {

    private PersonnelSpecifications() {}

    /** Rôle exact (STAGIAIRE, FORMATEUR...). */
    public static Specification<Personnel> role(Role role) {
        return (personnel, query, cb) -> cb.equal(personnel.get("role"), role);
    }

    /** true = comptes actifs, false = comptes désactivés. */
    public static Specification<Personnel> active(boolean active) {
        return (personnel, query, cb) -> cb.equal(personnel.get("active"), active);
    }

    /** true = comptes verrouillés, false = comptes non verrouillés. */
    public static Specification<Personnel> verrouille(boolean verrouille) {
        return (personnel, query, cb) -> cb.equal(personnel.get("verrouille"), verrouille);
    }
}
