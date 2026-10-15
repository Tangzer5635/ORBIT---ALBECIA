package net.ent.etnc.orbit.repositories.specifications;

import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.enums.EtatCompte;
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

    /** Actif, inactif (désactivé) ou verrouillé. */
    public static Specification<Personnel> etat(EtatCompte etat) {
        return (personnel, query, cb) -> switch (etat) {
            case ACTIF -> cb.isTrue(personnel.get("active"));
            case INACTIF -> cb.isFalse(personnel.get("active"));
            case VERROUILLE -> cb.isTrue(personnel.get("verrouille"));
        };
    }
}
