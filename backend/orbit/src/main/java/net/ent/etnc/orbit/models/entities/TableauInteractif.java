package net.ent.etnc.orbit.models.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "tableau_interactif")
@PrimaryKeyJoinColumn(name = "id", foreignKey = @ForeignKey(name = "fk_tableau_interactif_materiel"))
public class TableauInteractif extends Materiel {

    public static final String TYPE = "TableauInteractif";

    public String getType() {
        return TYPE;
    }
}