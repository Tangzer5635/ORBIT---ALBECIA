package net.ent.etnc.orbit.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import net.ent.etnc.orbit.models.commons.AbstractPersistableWithIdSetter;
import net.ent.etnc.orbit.models.enums.Role;
import org.hibernate.validator.constraints.Length;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Entity
@Table(name = "USERS",
        uniqueConstraints = @UniqueConstraint(name = "uk_USER_nid", columnNames = {"nid"}))
@EqualsAndHashCode(callSuper = false, of = {"nid"})
@ToString(callSuper = true, of = {"nid","login", "role"})
public class Personnel extends AbstractPersistableWithIdSetter<Long> implements UserDetails {

    @Getter
    @Setter
    @NotNull(message = "nid ne doit pas être null")
    @NotEmpty(message = "nid ne doit pas être vide")
    @NotBlank(message = "nid doit contenir des caractères lisibles")
    @Pattern(regexp = "^[0-9]{10}$", message = "nid doit contenir exactement 10 chiffres")
    @Column(name = "nid", length = 10, nullable = false)
    private String nid;

    @Getter
    @Setter
    @NotNull(message = "nom ne doit pas être null")
    @NotEmpty(message = "nom ne doit pas être vide")
    @NotBlank(message = "nom doit contenir des caractères lisibles")
    @Length(min = 1, max = 50, message = "nom doit avoir entre 3 et 50 caractères")
    @Column(name = "nom", length = 50, nullable = false)
    private String nom;

    @Getter
    @Setter
    @NotNull(message = "prenom ne doit pas être null")
    @NotEmpty(message = "prenom ne doit pas être vide")
    @NotBlank(message = "prenom doit contenir des caractères lisibles")
    @Length(min = 1, max = 30, message = "prenom doit avoir entre 3 et 30 caractères")
    @Column(name = "prenom", length = 50, nullable = false)
    private String prenom;

    @Getter
    @Setter
    @NotNull(message = "login ne doit pas être null")
    @NotEmpty(message = "login ne doit pas être vide")
    @NotBlank(message = "login doit contenir des caractères lisibles")
    @Length(min = 3, max = 50, message = "login doit avoir entre 3 et 50 caractères")
    @Column(name = "login", length = 50, nullable = false)
    private String login;

    @Getter
    @Setter
    @Column(name = "motDePasse", nullable = false)
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
    private boolean active = true;

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



}