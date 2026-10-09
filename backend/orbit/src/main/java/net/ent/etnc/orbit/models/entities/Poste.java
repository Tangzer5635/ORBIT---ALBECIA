package net.ent.etnc.orbit.models.entities;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import net.ent.etnc.orbit.models.commons.AbstractPersistableWithIdSetter;
import org.hibernate.validator.constraints.Length;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "poste",
        uniqueConstraints = @UniqueConstraint(name = "uk_poste_num_poste", columnNames = {"num_poste"}))
@EqualsAndHashCode(callSuper = false, of = {"numPoste"})
@ToString(callSuper = true, of = {"numPoste"})
public class Poste extends AbstractPersistableWithIdSetter<Long> {
    @Getter
    @Setter
    @NotNull(message = "numPoste ne doit pas être null")
    @NotEmpty(message = "numPoste ne doit pas être vide")
    @NotBlank(message = "numPoste doit contenir des caractères lisibles")
    @Length(min = 1, max = 50, message = "numPoste doit avoir entre 1 et 50 caractères")
    @Column(name = "num_poste", length = 50, nullable = false)
    private String numPoste;

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "occupant_id", foreignKey = @ForeignKey(name = "fk_poste_occupant"))
    private Personnel occupant;

    @Valid
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "poste_id",
            foreignKey = @ForeignKey(name = "fk_materiel_poste"))
    private List<Materiel> materiels = new ArrayList<>();

    public List<Materiel> getMateriels() {
        return Collections.unmodifiableList(materiels);
    }

    public void addMateriel(Materiel materiel) {
        materiels.add(materiel);
    }

    public void removeMateriel(Materiel materiel) {
        materiels.remove(materiel);
    }
}