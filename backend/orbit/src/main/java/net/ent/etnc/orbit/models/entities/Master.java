package net.ent.etnc.orbit.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;
import net.ent.etnc.orbit.models.commons.AbstractPersistableWithIdSetter;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "master",
        uniqueConstraints = @UniqueConstraint(name = "uk_MASTER_nom", columnNames = {"nom"}))
@EqualsAndHashCode(callSuper = false, of = {"nom"})
@ToString(callSuper = true, of = {"nom", "versionMaster", "dateCreation"})
public class Master extends AbstractPersistableWithIdSetter<Long> {
    @Getter
    @Setter
    @NotNull(message = "nom ne doit pas être null")
    @NotEmpty(message = "nom ne doit pas être vide")
    @NotBlank(message = "nom doit contenir des caractères lisibles")
    @Length(min = 3, max = 50, message = "nom doit avoir entre 3 et 50 caractères")
    @Column(name = "nom", length = 50, nullable = false)
    private String nom;

    @Getter
    @Setter
    @NotNull(message = "version ne doit pas être null")
    @NotEmpty(message = "version ne doit pas être vide")
    @NotBlank(message = "version doit contenir des caractères lisibles")
    @Length(min = 3, max = 50, message = "version doit avoir entre 3 et 50 caractères")
    @Column(name = "version_master", length = 50, nullable = false)
    private String versionMaster;

    @Getter
    @Setter
    @NotNull(message = "dateCreation ne doit pas être null")
    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;



}