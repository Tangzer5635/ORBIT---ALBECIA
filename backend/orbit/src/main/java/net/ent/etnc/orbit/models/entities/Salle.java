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
@Table(name = "SALLE",
        uniqueConstraints = @UniqueConstraint(name = "uk_SALLE_BATIMENT_ID_numSalle", columnNames = {"BATIMENT_ID, numSalle"}))
@EqualsAndHashCode(callSuper = false, of = {"BATIMENT_ID, numSalle"})
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "personnel_id", nullable = false, foreignKey = @ForeignKey(name = "fk_SALLE_gestionnaire"))
    private Personnel personnel;

    @Valid
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "SALLE_PERSONNEL",
            joinColumns = @JoinColumn(name = "salle_id",
                    foreignKey = @ForeignKey(name = "fk_SALLE_PERSONNEL_salle")),
            inverseJoinColumns = @JoinColumn(name = "personnel_id",
                    foreignKey = @ForeignKey(name = "fk_SALLE_PERSONNEL_personnel")))
    private List<Personnel> personnels =  new ArrayList<>();

    @Getter @Setter
    @Column(length = 50, nullable = false)
    private boolean suspendu;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "master_id", nullable = false, foreignKey = @ForeignKey(name = "fk_SALLE_master"))
    private Master master;

    @Valid
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "SALLE_id",
            foreignKey = @ForeignKey(name = "fk_POSTE_SALLE"))
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