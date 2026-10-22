package net.ent.etnc.orbit.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.ent.etnc.orbit.models.commons.AbstractPersistableWithIdSetter;
import net.ent.etnc.orbit.models.enums.ListeProcessus;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * Liste (blanche ou noire) choisie par un formateur pour un processus, à la place de la liste globale,
 * dans toutes ses salles. Si plusieurs formateurs d'une même salle ont surchargé le même processus,
 * c'est la surcharge la plus récente qui s'applique dans cette salle.
 *
 * <p>Supprimée avec le processus ou le formateur (ON DELETE CASCADE en base) : ni Processus ni Personnel
 * ne connaissent cette entité (pas de relation bidirectionnelle).
 */
@Entity
@Table(name = "surcharge_processus",
        uniqueConstraints = @UniqueConstraint(name = "uk_surcharge_processus_formateur_processus",
                columnNames = {"formateur_id", "processus_id"}))
@EqualsAndHashCode(callSuper = false, of = {"formateur", "processus"})
@ToString(callSuper = true, of = {"liste", "dateModification"})
public class SurchargeProcessus extends AbstractPersistableWithIdSetter<Long> {

    @Getter
    @Setter
    @NotNull(message = "formateur ne doit pas être null")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "formateur_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_surcharge_processus_formateur"))
    private Personnel formateur;

    @Getter
    @Setter
    @NotNull(message = "processus ne doit pas être null")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "processus_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_surcharge_processus_processus"))
    private Processus processus;

    @Getter
    @Setter
    @NotNull(message = "liste ne doit pas être null")
    @Enumerated(EnumType.STRING)
    @Column(name = "liste", length = 10, nullable = false)
    private ListeProcessus liste;

    /** Date du dernier changement : départage deux formateurs d'une même salle (le plus récent l'emporte). */
    @Getter
    @Setter
    @NotNull(message = "dateModification ne doit pas être null")
    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification;
}
