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
@Table(name = "BATIMENT",
        uniqueConstraints = @UniqueConstraint(name = "uk_BATIMENT_numBatiment", columnNames = {"numBatiment"}))
@EqualsAndHashCode(callSuper = false, of = {"numBatiment"})
@ToString(callSuper = true, of = {"numBatiment"})
public class Batiment extends AbstractPersistableWithIdSetter<Long> {
    @Getter
    @Setter
    @NotNull(message = "numBatiment ne doit pas être null")
    @NotEmpty(message = "numBatiment ne doit pas être vide")
    @NotBlank(message = "numBatiment doit contenir des caractères lisibles")
    @Length(min = 1, max = 10, message = "numBatiment doit avoir entre 1 et 10 caractères")
    @Column(name = "num_batiment", length = 10, nullable = false)
    private String numBatiment;

    @Valid
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "BATIMENT_id",
            foreignKey = @ForeignKey(name = "fk_SALLE_BATIMENT"))
    private List<Salle> salles = new ArrayList<>();

    public List<Salle> getSalles() {
        return Collections.unmodifiableList(salles);
    }

    public void addSalle(Salle salle) {
        salles.add(salle);
    }
    public void removeSalle(Salle salle) {
        salles.remove(salle);
    }
}