package net.ent.etnc.orbit.models.entities;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import net.ent.etnc.orbit.models.commons.AbstractPersistableWithIdSetter;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import org.hibernate.validator.constraints.Length;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "salle",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_salle_batiment_id_num_salle",
                columnNames = {"batiment_id", "num_salle"}
        ))
@EqualsAndHashCode(callSuper = false, of = {"numSalle"})
@ToString(callSuper = true, of = {"numSalle", "etage", "type"})
public class Salle extends AbstractPersistableWithIdSetter<Long> {

    @Getter
    @Setter
    @NotNull(message = "numSalle ne doit pas être null")
    @NotEmpty(message = "numSalle ne doit pas être vide")
    @NotBlank(message = "numSalle doit contenir des caractères lisibles")
    @Length(min = 1, max = 10, message = "numSalle doit avoir entre 1 et 10 caractères")
    @Column(name = "num_salle", length = 10, nullable = false)
    private String numSalle;

    @Getter
    @Setter
    @NotNull(message = "etage ne doit pas être null")
    @NotEmpty(message = "etage ne doit pas être vide")
    @NotBlank(message = "etage doit contenir des caractères lisibles")
    @Length(min = 1, max = 5, message = "etage doit avoir entre 1 et 5 caractères")
    @Column(name = "etage", length = 5, nullable = false)
    private String etage;

    @Getter
    @Setter
    @NotNull(message = "type ne doit pas être null")
    @Enumerated(EnumType.STRING)
    @Column(name = "type",length = 20, nullable = false)
    private TypeSalle type;

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gestionnaire_id", foreignKey = @ForeignKey(name = "fk_salle_gestionnaire"))
    private Personnel gestionnaire;

    @Valid
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "salle_personnel",
            joinColumns = @JoinColumn(name = "salle_id",
                    foreignKey = @ForeignKey(name = "fk_salle_personnel_salle")),
            inverseJoinColumns = @JoinColumn(name = "personnel_id",
                    foreignKey = @ForeignKey(name = "fk_salle_personnel_personnel")))
    private List<Personnel> personnels =  new ArrayList<>();

    @Getter @Setter
    @Column(name = "suspendu", nullable = false)
    private boolean suspendu;

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_id", foreignKey = @ForeignKey(name = "fk_salle_master"))
    private Master master;

    @Valid
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "salle_id",
            foreignKey = @ForeignKey(name = "fk_poste_salle"))
    private List<Poste> postes = new ArrayList<>();

    public List<Personnel> getPersonnels() {
        return Collections.unmodifiableList(personnels);
    }

    public void addPersonnel(Personnel personnel) {
        personnels.add(personnel);
    }

    public void removePersonnel(Personnel personnel) {
        personnels.remove(personnel);
    }

    public List<Poste> getPostes() {
        return Collections.unmodifiableList(postes);
    }

    public void addPoste(Poste poste) {
        postes.add(poste);
    }
    public void removePoste(Poste poste) {
        postes.remove(poste);
    }

}