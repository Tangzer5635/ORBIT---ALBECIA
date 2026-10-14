package net.ent.etnc.orbit.models.entities;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import net.ent.etnc.orbit.models.commons.AbstractPersistableWithIdSetter;
import net.ent.etnc.orbit.models.enums.EtatMateriel;
import net.ent.etnc.orbit.models.enums.EtatPoste;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "poste",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_poste_num_poste", columnNames = {"num_poste"}),
                // un stagiaire n'occupe qu'un poste à la fois (occupant_id vide = poste libre)
                @UniqueConstraint(name = "uk_poste_occupant", columnNames = {"occupant_id"})
        })
@EqualsAndHashCode(callSuper = false, of = {"numPoste"})
@ToString(callSuper = true, of = {"numPoste"})
public class Poste extends AbstractPersistableWithIdSetter<Long> {
    @Getter
    @NotBlank
    @Size(max = 50, message = "numPoste de max 50 caractères")
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

    public void setNumPoste(String numPoste) {
        this.numPoste = numPoste == null ? null : numPoste.trim();
    }

    public List<Materiel> getMateriels() {
        return Collections.unmodifiableList(materiels);
    }

    public void addMateriel(Materiel materiel) {
        materiels.add(materiel);
    }

    public void removeMateriel(Materiel materiel) {
        materiels.remove(materiel);
    }

    public EtatPoste calculerEtat(Set<Long> materielsDefectueux) {
        List<EtatMateriel> etats = materiels.stream()
                .map(m -> m.calculerEtat(false, materielsDefectueux.contains(m.getId())))
                .filter(e -> e != EtatMateriel.ARCHIVE)
                .toList();

        if (etats.isEmpty()) return EtatPoste.A_COMPLETER;
        if (etats.contains(EtatMateriel.DEFECTUEUX)) return EtatPoste.DEFECTUEUX;
        if (etats.contains(EtatMateriel.A_REMPLACER)) return EtatPoste.A_CHANGER;
        return occupant != null ? EtatPoste.UTILISE : EtatPoste.DISPONIBLE;
    }
}