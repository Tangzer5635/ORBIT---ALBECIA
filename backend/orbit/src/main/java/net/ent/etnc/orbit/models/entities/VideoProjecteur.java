package net.ent.etnc.orbit.models.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "video_projecteur")
@PrimaryKeyJoinColumn(name = "id", foreignKey = @ForeignKey(name = "fk_video_projecteur_materiel"))
public class VideoProjecteur extends Materiel {

    public static final String TYPE = "VideoProjecteur";

    public String getType() {
        return TYPE;
    }
}