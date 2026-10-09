package net.ent.etnc.orbit.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;
import net.ent.etnc.orbit.models.commons.AbstractPersistableWithIdSetter;
import net.ent.etnc.orbit.models.enums.EtatMateriel;
import net.ent.etnc.orbit.models.enums.TypeMateriel;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;

@Entity
@Table(name = "materiel",
        uniqueConstraints = @UniqueConstraint(name = "uk_materiel_num_serie", columnNames = {"num_serie"}))
@EqualsAndHashCode(callSuper = false, of = {"numSerie"})
@ToString(callSuper = true, of = {"numSerie", "modele", "dateFinGarantie", "dateAcquisition", "type", "etat"})
public class Materiel extends AbstractPersistableWithIdSetter<Long> {

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

    @Getter
    @Setter
    @NotNull(message = "type ne doit pas être null")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 15, nullable = false)
    private TypeMateriel type;


    @Getter
    @Setter
    @NotNull(message = "etat ne doit pas être null")
    @Enumerated(EnumType.STRING)
    @Column(name = "etat", length = 15, nullable = false)
    private EtatMateriel etat = EtatMateriel.DISPONIBLE;

}