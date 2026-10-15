package net.ent.etnc.orbit.repositories.specifications;


import jakarta.persistence.criteria.*;
import net.ent.etnc.orbit.models.entities.Defaillance;
import net.ent.etnc.orbit.models.entities.*;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.EtatMateriel;
import net.ent.etnc.orbit.models.enums.LocalisationMateriel;
import net.ent.etnc.orbit.models.enums.StatutDefaillance;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtre pour l'état et la localisation du matériel.
 *
 * ici l'état est calculé depuis la BDD
 */
public final class MaterielSpecifications {

    private MaterielSpecifications() {}

    /** EXISTS une défaillance SIGNALEE sur ce matériel (= DefaillanceService.aUneDefaillanceOuverte). */
    private static Predicate defaillanceOuverte(Root<Materiel> materiel, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Subquery<Long> sousRequete = query.subquery(Long.class);
        Root<Defaillance> defaillance = sousRequete.from(Defaillance.class);
        sousRequete.select(defaillance.get("id")).where(
                cb.equal(defaillance.get("materiel"), materiel),
                cb.equal(defaillance.get("statut"), StatutDefaillance.SIGNALEE));
        return cb.exists(sousRequete);
    }

    /** EXISTS une salle de stockage qui contient ce matériel (= SalleService.estEnStock). */
    private static Predicate enStock(Root<Materiel> materiel, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Subquery<Long> sousRequete = query.subquery(Long.class);
        Root<Salle> salle = sousRequete.from(Salle.class);
        Join<Salle, Materiel> contenu = salle.join("materiels");
        sousRequete.select(salle.get("id")).where(
                cb.equal(contenu, materiel),
                cb.equal(salle.get("type"), TypeSalle.SalleStockage));
        return cb.exists(sousRequete);
    }

    /** Filtre sur la classe du matériel (UC, Ecran, VideoProjecteur, TableauInteractif). */
    public static Specification<Materiel> type(String type) {
        Class<? extends Materiel> classe = switch (type) {
            case UC.TYPE -> UC.class;
            case Ecran.TYPE -> Ecran.class;
            case VideoProjecteur.TYPE -> VideoProjecteur.class;
            case TableauInteractif.TYPE -> TableauInteractif.class;
            default -> throw new IllegalArgumentException("Type de matériel inconnu : " + type);
        };
        return (materiel, query, cb) -> cb.equal(materiel.type(), classe);
    }

    public static Specification<Materiel> localisation(LocalisationMateriel localisation) {
        return (materiel, query, cb) -> {
            Predicate actif = cb.isFalse(materiel.get("archive"));
            return switch (localisation) {
                case PARTOUT -> actif;
                case STOCK -> cb.and(actif, enStock(materiel, query, cb));
                case SALLE -> cb.and(actif, cb.not(enStock(materiel, query, cb)));
                case ARCHIVE -> cb.isTrue(materiel.get("archive"));
            };
        };
    }

    /** Même ordre de priorité que Materiel.calculerEtat : archivé, défectueux, à remplacer, disponible / normal. */
    public static Specification<Materiel> etat(EtatMateriel etat) {
        return (materiel, query, cb) -> {
            Predicate actif = cb.isFalse(materiel.get("archive"));
            Predicate ancien = cb.lessThan(materiel.get("dateAcquisition"), Materiel.seuilARemplacer());
            return switch (etat) {
                case ARCHIVE -> cb.isTrue(materiel.get("archive"));
                case DEFECTUEUX -> cb.and(actif, defaillanceOuverte(materiel, query, cb));
                case A_REMPLACER -> cb.and(actif, cb.not(defaillanceOuverte(materiel, query, cb)), ancien);
                case DISPONIBLE -> cb.and(actif, cb.not(defaillanceOuverte(materiel, query, cb)), cb.not(ancien),
                        enStock(materiel, query, cb));
                case NORMAL -> cb.and(actif, cb.not(defaillanceOuverte(materiel, query, cb)), cb.not(ancien),
                        cb.not(enStock(materiel, query, cb)));
            };
        };
    }

}
