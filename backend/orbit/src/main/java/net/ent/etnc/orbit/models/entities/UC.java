package net.ent.etnc.orbit.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Unité centrale : seul matériel qui porte un nom de machine et des processus installés. */
@Entity
@Table(name = "uc")
@PrimaryKeyJoinColumn(name = "id", foreignKey = @ForeignKey(name = "fk_uc_materiel"))
public class UC extends Materiel {

    public static final String TYPE = "UC";

    @Getter
    @Setter
    @Length(max = 50, message = "nomMachine doit avoir au plus 50 caractères")
    @Column(name = "nom_machine", length = 50)
    private String nomMachine;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "uc_processus",
            joinColumns = @JoinColumn(name = "uc_id",
                    foreignKey = @ForeignKey(name = "fk_uc_processus_uc")),
            inverseJoinColumns = @JoinColumn(name = "processus_id",
                    foreignKey = @ForeignKey(name = "fk_uc_processus_processus")))
    private List<Processus> processus = new ArrayList<>();

    public String getType() {
        return TYPE;
    }

    public List<Processus> getProcessus() {
        return Collections.unmodifiableList(processus);
    }

    public void addProcessus(Processus p) {
        if (!processus.contains(p)) processus.add(p);
    }

    public void removeProcessus(Processus p) {
        processus.remove(p);
    }
}