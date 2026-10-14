package net.ent.etnc.orbit.models.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "ecran")
@PrimaryKeyJoinColumn(name = "id", foreignKey = @ForeignKey(name = "fk_ecran_materiel"))
public class Ecran extends Materiel {

    public static final String TYPE = "Ecran";

    @Override
    public String getType() {
        return TYPE;
    }
}