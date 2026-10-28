package net.ent.etnc.orbit.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.ent.etnc.orbit.models.commons.AbstractPersistableWithIdSetter;
import net.ent.etnc.orbit.models.enums.EtatMateriel;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "materiel",
        uniqueConstraints = @UniqueConstraint(name = "uk_materiel_num_serie", columnNames = {"num_serie"}))
@EqualsAndHashCode(callSuper = false, of = {"numSerie"})
@ToString(callSuper = true, of = {"numSerie", "modele", "dateFinGarantie", "dateAcquisition", "archive", "dateArchivage"})
public abstract class Materiel extends AbstractPersistableWithIdSetter<Long> {

    //TODO voir la gestion des numSerie avec le jeu de donnée
    @Getter
    @Setter
    @NotNull(message = "numSerie ne doit pas être null")
    @NotEmpty(message = "numSerie ne doit pas être vide")
    @NotBlank(message = "numSerie doit contenir des caractères lisibles")
    @Length(min = 3, max = 50, message = "numSerie doit avoir entre 3 et 50 caractères")
    @Column(name = "num_serie", length = 50, nullable = false)
    private String numSerie;

    @Getter
    @Setter
    @NotNull(message = "modele ne doit pas être null")
    @NotEmpty(message = "modele ne doit pas être vide")
    @NotBlank(message = "modele doit contenir des caractères lisibles")
    @Length(min = 1, max = 50, message = "modele doit avoir entre 1 et 50 caractères")
    @Column(name = "modele", length = 50, nullable = false)
    private String modele;

    @Getter
    @Setter
    @NotNull(message = "dateFinGarantie ne doit pas être null")
    @Column(name = "date_fin_garantie", nullable = false)
    private LocalDate dateFinGarantie;

    @Getter
    @Setter
    @NotNull(message = "dateAcquisition ne doit pas être null")
    @Column(name = "date_acquisition", nullable = false)
    private LocalDate dateAcquisition;

    @Getter @Setter
    @Column(name = "archive", nullable = false)
    private boolean archive;

    @Getter @Setter
    @Column(name = "date_archivage")
    private LocalDate dateArchivage;

    /**
     * Type du matériel tel qu'il est exposé au front ("UC", "Ecran"... ou le libellé d'un type ajouté).
     * Méthode et pas getClass() : un matériel chargé en LAZY est un proxy Hibernate.
     */
    public abstract String getType();

    /**
     * État calculé (non stocké), par ordre de priorité :
     * archivé, défectueux (défaillance signalée non résolue), à remplacer (acquis il y a
     * plus de 2 ans), puis disponible (en stock) ou normal (affecté).
     */
    public EtatMateriel calculerEtat(boolean enStock, boolean defaillanceOuverte) {
        if (archive) return EtatMateriel.ARCHIVE;
        if (defaillanceOuverte) return EtatMateriel.DEFECTUEUX;
        if (dateAcquisition.isBefore(seuilARemplacer())) return EtatMateriel.A_REMPLACER;
        return enStock ? EtatMateriel.DISPONIBLE : EtatMateriel.NORMAL;
    }

    /** Un matériel acquis avant cette date (il y a plus de 2 ans) est à remplacer. */
    public static LocalDate seuilARemplacer() {
        return LocalDate.now().minusYears(2);
    }

}