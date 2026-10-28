package net.ent.etnc.orbit.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.ent.etnc.orbit.models.commons.AbstractPersistableWithIdSetter;
import org.hibernate.validator.constraints.Length;

import java.util.List;

/**
 * Type de matériel ajouté par l'administrateur (clavier, souris, imprimante...).
 * Les 4 types de base (UC, écran, vidéoprojecteur, tableau interactif) ne sont pas dans cette table :
 * ce sont des classes, car des règles métier en dépendent. Ils ne peuvent donc pas être supprimés.
 */
@Entity
@Table(name = "type_materiel",
        uniqueConstraints = @UniqueConstraint(name = "uk_type_materiel_libelle", columnNames = {"libelle"}))
@EqualsAndHashCode(callSuper = false, of = {"libelle"})
@ToString(callSuper = true, of = {"libelle"})
public class TypeMateriel extends AbstractPersistableWithIdSetter<Long> {

    /** Les 4 types de base, protégés (hors table). */
    public static final List<String> DE_BASE =
            List.of(UC.TYPE, Ecran.TYPE, VideoProjecteur.TYPE, TableauInteractif.TYPE);

    @Getter
    @NotBlank(message = "libelle doit contenir des caractères lisibles")
    @Length(max = 50, message = "libelle doit avoir au plus 50 caractères")
    @Column(name = "libelle", length = 50, nullable = false)
    private String libelle;

    public void setLibelle(String libelle) {
        this.libelle = libelle == null ? null : libelle.trim();
    }
}