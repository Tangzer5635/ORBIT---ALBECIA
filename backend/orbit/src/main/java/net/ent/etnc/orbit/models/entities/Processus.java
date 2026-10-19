package net.ent.etnc.orbit.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.ent.etnc.orbit.models.commons.AbstractPersistableWithIdSetter;
import net.ent.etnc.orbit.models.enums.ListeProcessus;
import org.hibernate.validator.constraints.Length;

@Entity
@Table(name = "processus",
        uniqueConstraints = @UniqueConstraint(name = "uk_processus_nom", columnNames = {"nom"}))
@EqualsAndHashCode(callSuper = false, of = {"nom"})
@ToString(callSuper = true, of = {"nom", "liste"})
public class Processus extends AbstractPersistableWithIdSetter<Long> {

    @Getter
    @NotBlank(message = "nom doit contenir des caractères lisibles")
    @Length(max = 100, message = "nom doit avoir au plus 100 caractères")
    @Column(name = "nom", length = 100, nullable = false)
    private String nom;

    @Getter
    @Setter
    @NotNull(message = "liste ne doit pas être null")
    @Enumerated(EnumType.STRING)
    @Column(name = "liste", length = 10, nullable = false)
    private ListeProcessus liste;

    public void setNom(String nom) {
        this.nom = nom == null ? null : nom.trim();
    }
}