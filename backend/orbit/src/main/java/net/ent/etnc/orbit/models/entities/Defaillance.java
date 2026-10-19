package net.ent.etnc.orbit.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.ent.etnc.orbit.models.commons.AbstractPersistableWithIdSetter;
import net.ent.etnc.orbit.models.enums.StatutDefaillance;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;

/**
 * Défaillance remontée sur un matériel (DCLAM : Defaillance — concerne Materiel,
 * signale Personnel, traite Personnel).
 */
@Entity
@Table(name = "defaillance")
@ToString(callSuper = true, of = {"description", "dateSignalement", "statut","noteSolution", "dateResolution"})
@EqualsAndHashCode(callSuper = false)
public class Defaillance extends AbstractPersistableWithIdSetter<Long> {

    @Getter @Setter
    @NotBlank(message = "description doit contenir des caractères lisibles")
    @Length(min = 5, max = 500, message = "description doit avoir entre 5 et 500 caractères")
    @Column(name = "description", length = 500, nullable = false)
    private String description;

    @Getter @Setter
    @NotNull(message = "dateSignalement ne doit pas être null")
    @Column(name = "date_signalement", nullable = false)
    private LocalDateTime dateSignalement;

    @Getter @Setter
    @NotNull(message = "statut ne doit pas être null")
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", length = 20, nullable = false)
    private StatutDefaillance statut = StatutDefaillance.SIGNALEE;

    /** concerne (1) */
    @Getter @Setter
    @NotNull(message = "materiel ne doit pas être null")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "materiel_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_defaillance_materiel"))
    private Materiel materiel;

    /** signale (1) */
    @Getter @Setter
    @NotNull(message = "signalePar ne doit pas être null")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "signale_par_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_defaillance_signale_par"))
    private Personnel signalePar;

    /** traite (0..1) : renseigné à la résolution. */
    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traite_par_id",
            foreignKey = @ForeignKey(name = "fk_defaillance_traite_par"))
    private Personnel traitePar;

    /** DCU « Traiter une défaillance » : note de solution saisie à la résolution. */
    @Getter
    @Length(max = 500, message = "noteSolution doit avoir au plus 500 caractères")
    @Column(name = "note_solution", length = 500)
    private String noteSolution;

    @Getter
    @Column(name = "date_resolution")
    private LocalDateTime dateResolution;

    public boolean isResolue() {
        return statut == StatutDefaillance.RESOLUE;
    }

    public void resoudre(Personnel technicien, String noteSolution) {
        this.statut = StatutDefaillance.RESOLUE;
        this.traitePar = technicien;
        this.noteSolution = noteSolution;
        this.dateResolution = LocalDateTime.now();
    }
}
