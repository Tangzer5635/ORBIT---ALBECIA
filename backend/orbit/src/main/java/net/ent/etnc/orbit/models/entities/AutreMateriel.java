package net.ent.etnc.orbit.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Matériel d'un type ajouté par l'administrateur (voir {@link TypeMateriel}).
 * Il s'affecte à un poste ou à une salle, sans limite de nombre.
 */
@Entity
@Table(name = "autre_materiel")
@PrimaryKeyJoinColumn(name = "id", foreignKey = @ForeignKey(name = "fk_autre_materiel_materiel"))
@ToString(callSuper = true, of = {"typeMateriel"})
@EqualsAndHashCode(callSuper = true)
public class AutreMateriel extends Materiel {

    @Getter
    @Setter
    @NotNull(message = "typeMateriel ne doit pas être null")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "type_materiel_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_autre_materiel_type_materiel"))
    private TypeMateriel typeMateriel;

    /** Le libellé du type ajouté (ex. "Clavier"). */
    @Override
    public String getType() {
        return typeMateriel.getLibelle();
    }
}