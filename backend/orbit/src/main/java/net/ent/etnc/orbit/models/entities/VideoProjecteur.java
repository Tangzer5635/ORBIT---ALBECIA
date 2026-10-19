package net.ent.etnc.orbit.models.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "video_projecteur")
@PrimaryKeyJoinColumn(name = "id", foreignKey = @ForeignKey(name = "fk_video_projecteur_materiel"))
@ToString(callSuper = true, of = {"TYPE"})
@EqualsAndHashCode(callSuper = true)
public class VideoProjecteur extends Materiel {

    public static final String TYPE = "VideoProjecteur";

    public String getType() {
        return TYPE;
    }
}