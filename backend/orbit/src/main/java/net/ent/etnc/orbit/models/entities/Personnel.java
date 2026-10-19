package net.ent.etnc.orbit.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import net.ent.etnc.orbit.models.commons.AbstractPersistableWithIdSetter;
import net.ent.etnc.orbit.models.enums.Role;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Entity
@Table(name = "personnel",
        uniqueConstraints = @UniqueConstraint(name = "uk_USER_nid", columnNames = {"nid"}))
@EqualsAndHashCode(callSuper = false, of = {"nid"})
@ToString(callSuper = true, of = {"nid","nom","prenom","login", "role", "active", "verrouille"})
public class Personnel extends AbstractPersistableWithIdSetter<Long> implements UserDetails {

    @Getter
    @NotBlank
    @Pattern(regexp = "^[0-9]{10}$", message = "nid doit contenir exactement 10 chiffres")
    @Column(name = "nid", length = 10, nullable = false)
    private String nid;

    @Getter
    @Setter
    @NotBlank
    @Size(max = 50, message = "nom de max 50 caractères")
    @Column(name = "nom", length = 50, nullable = false)
    private String nom;

    @Getter
    @Setter
    @NotBlank
    @Size(max = 30, message = "prenom de max 30 caractères")
    @Column(name = "prenom", length = 30, nullable = false)
    private String prenom;

    @Getter
    @Size(min = 3, max = 50, message = "login doit avoir entre 3 et 50 caractères")
    @Column(name = "login", length = 50, nullable = false)
    private String login;

    @Getter
    @Setter
    @Column(name = "mot_de_passe", nullable = false) //pas de length pour le hash
    private String motDePasse;

    @Getter
    @Setter
    @NotNull(message = "Le rôle est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Getter
    @Setter
    @Column(name = "active", nullable = false)
    private boolean active = false;

    @Getter
    @Setter
    @Column(name = "verrouille", nullable = false)
    private boolean verrouille = false;

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", foreignKey = @ForeignKey(name = "fk_personnel_stage"))
    private Stage stage;

    /** Le nid est toujours enregistré sans espaces autour. */
    public void setNid(String nid) {
        this.nid = nid == null ? null : nid.trim();
    }

    /** Le login est toujours enregistré sans espaces autour. */
    public void setLogin(String login) {
        this.login = login == null ? null : login.trim();
    }

    /**
     * Retourne les autorités Spring Security de cet utilisateur.
     * Inclut le rôle propre et tous les rôles hérités (hiérarchie cumulative).
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return role.getAllRoles().stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name()))
                .toList();
    }

    @Override
    public @Nullable String getPassword() {
        return motDePasse;
    }

    /** Le username est utilisé comme identifiant de connexion. */
    @Override
    public String getUsername() {
        return login;
    }

    /** Un compte inactif est considéré comme désactivé par Spring Security. */
    @Override
    public boolean isEnabled() {
        return active;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !verrouille;
    }

}