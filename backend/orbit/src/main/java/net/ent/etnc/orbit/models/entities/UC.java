package net.ent.etnc.orbit.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Length;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Unité centrale : seul matériel qui porte un nom de machine, une adresse MAC et des processus installés.
 * L'adresse MAC relie l'UC à l'Agent ORBIT qui tourne dessus (l'agent s'annonce au serveur avec sa MAC).
 */
@Entity
@Table(name = "uc")
@PrimaryKeyJoinColumn(name = "id", foreignKey = @ForeignKey(name = "fk_uc_materiel"))
@ToString(callSuper = true, of = {"nomMachine", "adresseMac"})
@EqualsAndHashCode(callSuper = true)
public class UC extends Materiel {

    public static final String TYPE = "UC";

    /** Adresse MAC saisie : 6 octets en hexadécimal séparés par « : » ou « - » (ex. 00-0C-29-AB-CD-EF). */
    public static final String MAC = "^([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}$";

    @Getter
    @Setter
    @Length(max = 50, message = "nomMachine doit avoir au plus 50 caractères")
    @Column(name = "nom_machine", length = 50)
    private String nomMachine;

    /** Adresse MAC de la machine, stockée au format 00:0C:29:AB:CD:EF. Facultative. */
    @Getter
    @Pattern(regexp = MAC, message = "adresseMac doit être une adresse MAC (ex. 00:0C:29:AB:CD:EF)")
    @Column(name = "adresse_mac", length = 17)
    private String adresseMac;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "uc_processus",
            joinColumns = @JoinColumn(name = "uc_id",
                    foreignKey = @ForeignKey(name = "fk_uc_processus_uc")),
            inverseJoinColumns = @JoinColumn(name = "processus_id",
                    foreignKey = @ForeignKey(name = "fk_uc_processus_processus")))
    private List<Processus> processus = new ArrayList<>();

    /** Vide = pas d'adresse. */
    public void setAdresseMac(String adresseMac) {
        this.adresseMac = normaliserMac(adresseMac);
    }

    /** "00-0c-29-ab-cd-ef" -> "00:0C:29:AB:CD:EF" (vide -> null) : une seule écriture pour comparer. */
    public static String normaliserMac(String mac) {
        return mac == null || mac.isBlank() ? null : mac.trim().toUpperCase().replace('-', ':');
    }

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