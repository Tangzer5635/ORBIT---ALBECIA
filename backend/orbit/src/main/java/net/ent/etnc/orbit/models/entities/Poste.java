package net.ent.etnc.orbit.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import net.ent.etnc.orbit.models.commons.AbstractPersistableWithIdSetter;
import org.hibernate.validator.constraints.Length;

@Entity
@Table(name = "POSTE",
        uniqueConstraints = @UniqueConstraint(name = "uk_POSTE_nom", columnNames = {"numPoste"}))
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

}