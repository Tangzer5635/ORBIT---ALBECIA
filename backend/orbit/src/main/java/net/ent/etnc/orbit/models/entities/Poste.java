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

    /**
     * Nombre maximum de matériels d'un type sur un poste : 1 UC, 2 écrans, ni vidéoprojecteur ni tableau
     * (matériel de salle), sans limite pour un type ajouté par l'admin.
     */
    public static int maximum(String type) {
        return switch (type) {
            case UC.TYPE -> 1;
            case Ecran.TYPE -> 2;
            case VideoProjecteur.TYPE, TableauInteractif.TYPE -> 0;
            default -> Integer.MAX_VALUE;
        };
    }

    /** Matériels non archivés de ce type posés sur le poste. */
    public long nombre(String type) {
        return materiels.stream().filter(m -> !m.isArchive() && type.equals(m.getType())).count();
    }

    /** Un poste est complet (donc peut être libre) avec au moins une UC et un écran. */
    public boolean estComplet() {
        return nombre(UC.TYPE) > 0 && nombre(Ecran.TYPE) > 0;
    }

    /**
     * Ce qui dépasse la composition autorisée (postes équipés avant la règle) : vide si tout va bien.
     * Ex. « 2 unités centrales (1 au maximum) », « 1 vidéoprojecteur (matériel de salle, pas de poste) ».
     */
    public List<String> anomalies() {
        List<String> anomalies = new ArrayList<>();
        long uc = nombre(UC.TYPE);
        long ecrans = nombre(Ecran.TYPE);
        long vp = nombre(VideoProjecteur.TYPE);
        long tbi = nombre(TableauInteractif.TYPE);
        if (uc > maximum(UC.TYPE))
            anomalies.add(uc + " unités centrales (" + maximum(UC.TYPE) + " au maximum)");
        if (ecrans > maximum(Ecran.TYPE))
            anomalies.add(ecrans + " écrans (" + maximum(Ecran.TYPE) + " au maximum)");
        if (vp > 0)
            anomalies.add(vp + " vidéoprojecteur(s) : c'est du matériel de salle, pas de poste");
        if (tbi > 0)
            anomalies.add(tbi + " tableau(x) interactif(s) : c'est du matériel de salle, pas de poste");
        return anomalies;
    }

    /**
     * Par ordre de priorité : défectueux, à compléter (pas d'UC ou pas d'écran), à changer,
     * puis utilisé ou disponible (« libre »).
     */
    public EtatPoste calculerEtat(Set<Long> materielsDefectueux) {
        List<EtatMateriel> etats = materiels.stream()
                .map(m -> m.calculerEtat(false, materielsDefectueux.contains(m.getId())))
                .filter(e -> e != EtatMateriel.ARCHIVE)
                .toList();

        if (etats.contains(EtatMateriel.DEFECTUEUX)) return EtatPoste.DEFECTUEUX;
        if (!estComplet()) return EtatPoste.A_COMPLETER;
        if (etats.contains(EtatMateriel.A_REMPLACER)) return EtatPoste.A_CHANGER;
        return occupant != null ? EtatPoste.UTILISE : EtatPoste.DISPONIBLE;
    }
}