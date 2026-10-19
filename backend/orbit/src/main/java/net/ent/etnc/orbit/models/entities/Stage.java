package net.ent.etnc.orbit.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import net.ent.etnc.orbit.models.commons.AbstractPersistableWithIdSetter;
import net.ent.etnc.orbit.models.enums.TypeFormation;

import java.time.LocalDate;

@Entity
@Table(name = "stage",
        uniqueConstraints = @UniqueConstraint(name = "uk_stage_nom", columnNames = {"nom"}))
@EqualsAndHashCode(callSuper = false, of = {"nom"})
@ToString(callSuper = true, of = {"nom", "type", "dateDebut", "dateFin"})
public class Stage extends AbstractPersistableWithIdSetter<Long> {

    /** Ex. WB47 : 2 lettres majuscules puis 2 chiffres. */
    @Getter
    @NotNull
    @Pattern(regexp = "^[A-Z]{2}[0-9]{2}$", message = "nom : 2 lettres majuscules puis 2 chiffres (ex. WB47)")
    @Column(name = "nom", length = 4, nullable = false)
    private String nom;

    @Getter
    @Setter
    @NotNull(message = "type ne doit pas être null")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 10, nullable = false)
    private TypeFormation type;

    @Getter
    @Setter
    @NotNull(message = "dateDebut ne doit pas être null")
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Getter
    @Setter
    @NotNull(message = "dateFin ne doit pas être null")
    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    /** Formateur qui gère le stage (CDSF) */
    @Getter
    @Setter
    @NotNull(message = "formateur ne doit pas être null")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "formateur_id", nullable = false, foreignKey = @ForeignKey(name = "fk_stage_formateur"))
    private Personnel formateur;

    public void setNom(String nom) {
        this.nom = nom == null ? null : nom.trim();
    }
}