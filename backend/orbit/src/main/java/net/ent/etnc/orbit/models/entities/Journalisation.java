package net.ent.etnc.orbit.models.entities;

import jakarta.persistence.*;
import lombok.*;
import net.ent.etnc.orbit.models.commons.AbstractPersistableWithIdSetter;
import net.ent.etnc.orbit.models.enums.TypeAction;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * Une ligne du journal des actions.
 *
 * <p>Le journal est en lecture seule : {@link Immutable} interdit à Hibernate toute mise à jour,
 * et aucune méthode de modification ou de suppression n'est exposée par le service.
 * L'auteur est stocké par son id, sans clé étrangère vers Personnel : la ligne reste
 * lisible même si le compte est supprimé, et le journal ne contient ni nom ni prénom (CRE 3).
 */
@Entity
@Immutable
@Table(name = "journalisation",
        indexes = @Index(name = "idx_journalisation_date_heure", columnList = "date_heure"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(callSuper = true, of = {"dateHeure", "auteurId", "action", "detail"})
@EqualsAndHashCode(callSuper = false)
public class Journalisation extends AbstractPersistableWithIdSetter<Long> {

    @Column(name = "date_heure", nullable = false, updatable = false)
    private LocalDateTime dateHeure;

    /** Id du Personnel auteur de l'action ; null si inconnu (ex. échec de connexion sur un identifiant inexistant). */
    @Column(name = "auteur_id", updatable = false)
    private Long auteurId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 20, nullable = false, updatable = false)
    private TypeAction action;

    @Column(name = "detail", length = 500, nullable = false, updatable = false)
    private String detail;

    public Journalisation(Long auteurId, TypeAction action, String detail) {
        this.dateHeure = LocalDateTime.now();
        this.auteurId = auteurId;
        this.action = action;
        this.detail = detail == null ? "" : detail.length() > 500 ? detail.substring(0, 500) : detail;
    }
}