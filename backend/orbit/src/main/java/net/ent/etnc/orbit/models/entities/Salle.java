package net.ent.etnc.orbit.models.entities;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import net.ent.etnc.orbit.models.commons.AbstractPersistableWithIdSetter;
import net.ent.etnc.orbit.models.enums.MotifGel;
import net.ent.etnc.orbit.models.enums.TypeSalle;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "salle",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_salle_batiment_id_num_salle", columnNames = {"batiment_id", "num_salle"}),
                // un stage n'est que dans une salle (stage_id vide = pas de stage)
                @UniqueConstraint(name = "uk_salle_stage", columnNames = {"stage_id"})
        })

@ToString(callSuper = true, of = {"numSalle", "etage", "type"})
public class Salle extends AbstractPersistableWithIdSetter<Long> {

    @Getter
    @NotBlank
    @Size(max = 10, message = "numSalle de max 10 caractères")
    @Column(name = "num_salle", length = 10, nullable = false)
    private String numSalle;

    @Getter
    @Setter
    @NotBlank
    @Size(max = 5, message = "etage de max 5 caractères")
    @Column(name = "etage", length = 5, nullable = false)
    private String etage;

    @Getter
    @Setter
    @NotNull(message = "type ne doit pas être null")
    @Enumerated(EnumType.STRING)
    @Column(name = "type",length = 20, nullable = false)
    private TypeSalle type;

    /**
     * Formateurs attribués à la salle, avec leur dernier jour d'accès (inclus).
     * Valeur null : attribution faite avant la date limite, sans fin.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "salle_personnel",
            joinColumns = @JoinColumn(name = "salle_id"),
            foreignKey = @ForeignKey(name = "fk_salle_personnel_salle"))
    @MapKeyJoinColumn(name = "personnel_id", foreignKey = @ForeignKey(name = "fk_salle_personnel_personnel"))
    @Column(name = "jusqu")
    private Map<Personnel, LocalDate> personnels = new HashMap<>();

    @Valid
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "salle_id",
            foreignKey = @ForeignKey(name = "fk_materiel_salle"))
    private List<Materiel> materiels = new ArrayList<>();

    /** « Prénom Nom » du gestionnaire qui a créé la salle (copié : la suppression de son compte ne bloque rien). */
    @Getter @Setter
    @Size(max = 101, message = "creePar de max 101 caractères")
    @Column(name = "cree_par", length = 101)
    private String creePar;

    /** Salle gelée : visible, mais plus d'affectation de stage, de stagiaire ni de master. */
    @Getter @Setter
    @Column(name = "suspendu", nullable = false)
    private boolean suspendu;

    @Getter @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "gel_motif", length = 20)
    private MotifGel gelMotif;

    @Getter @Setter
    @Size(max = 200, message = "gelNote de max 200 caractères")
    @Column(name = "gel_note", length = 200)
    private String gelNote;

    @Getter @Setter
    @Column(name = "gel_depuis")
    private LocalDate gelDepuis;

    @Getter @Setter
    @Column(name = "gel_jusqu")
    private LocalDate gelJusqu;

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_id", foreignKey = @ForeignKey(name = "fk_salle_master"))
    private Master master;

    /** Stage accueilli par la salle (salle de cours seulement, un seul à la fois). */
    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", foreignKey = @ForeignKey(name = "fk_salle_stage"))
    private Stage stage;

    @Valid
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "salle_id",
            foreignKey = @ForeignKey(name = "fk_poste_salle"))
    private List<Poste> postes = new ArrayList<>();

    /** Le numéro est toujours enregistré sans espaces autour (" 101" et "101" sont la même salle). */
    public void setNumSalle(String numSalle) {
        this.numSalle = numSalle == null ? null : numSalle.trim();
    }

    /** Formateurs attribués et leur date limite d'accès, accès expiré compris. */
    public Map<Personnel, LocalDate> getAcces() {
        return Collections.unmodifiableMap(personnels);
    }

    /** Tous les formateurs attribués, accès expiré compris. */
    public List<Personnel> getPersonnels() {
        return List.copyOf(personnels.keySet());
    }

    /** Formateurs dont l'accès est en cours. */
    public List<Personnel> getPersonnelsActifs() {
        return personnels.entrySet().stream()
                .filter(e -> accesActif(e.getValue()))
                .map(Map.Entry::getKey)
                .toList();
    }

    /** Le formateur attribué à la salle avec cet id, s'il l'est (accès expiré compris). */
    public Optional<Personnel> personnel(Long idPersonnel) {
        return personnels.keySet().stream().filter(p -> Objects.equals(p.getId(), idPersonnel)).findFirst();
    }

    /** Attribution sans date limite (données de démonstration, tests). */
    public void addPersonnel(Personnel personnel) {
        addPersonnel(personnel, null);
    }

    /** Attribue la salle jusqu'à cette date ; déjà attribué : la date est remplacée. */
    public void addPersonnel(Personnel personnel, LocalDate jusqu) {
        personnels.put(personnel, jusqu);
    }

    public void removePersonnel(Personnel personnel) {
        personnels.keySet().removeIf(p -> p == personnel
                || (personnel.getId() != null && personnel.getId().equals(p.getId())));
    }

    /** Accès en cours : pas de date limite, ou date limite pas encore dépassée (le jour même compte). */
    public static boolean accesActif(LocalDate jusqu) {
        return jusqu == null || !jusqu.isBefore(LocalDate.now());
    }

    public List<Poste> getPostes() {
        return Collections.unmodifiableList(postes);
    }

    public void addPoste(Poste poste) {
        postes.add(poste);
    }

    public void removePoste(Poste poste) {
        postes.remove(poste);
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

}