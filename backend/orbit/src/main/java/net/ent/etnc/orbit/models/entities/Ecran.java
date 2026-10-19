package net.ent.etnc.orbit.models.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "ecran")
@PrimaryKeyJoinColumn(name = "id", foreignKey = @ForeignKey(name = "fk_ecran_materiel"))
@ToString(callSuper = true, of = "TYPE")
@EqualsAndHashCode(callSuper = true)
public class Ecran extends Materiel {

    public static final String TYPE = "Ecran";

    @Override
    public String getType() {
        return TYPE;
    }
}