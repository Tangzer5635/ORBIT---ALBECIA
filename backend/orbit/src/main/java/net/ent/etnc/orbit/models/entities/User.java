package net.ent.etnc.orbit.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import net.ent.etnc.orbit.models.commons.AbstractPersistableWithIdSetter;
import net.ent.etnc.orbit.models.enums.Role;
import org.hibernate.validator.constraints.Length;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Entity
@Table(name = "USERS",
        uniqueConstraints = @UniqueConstraint(name = "uk_USER_username", columnNames = {"username"}))
@EqualsAndHashCode(callSuper = false, of = {"username"})
@ToString(callSuper = true, of = {"username", "role"})
public class User extends AbstractPersistableWithIdSetter<Long> implements UserDetails {

    @Getter
    @Setter
    @NotNull(message = "username ne doit pas être null")
    @NotEmpty(message = "username ne doit pas être vide")
    @NotBlank(message = "username doit contenir des caractères lisibles")
    @Length(min = 3, max = 50, message = "username doit avoir entre 3 et 50 caractères")
    @Column(name = "username", length = 50, nullable = false)
    private String username;

    @Getter
    @Setter
    @Column(name = "password", nullable = false)
    private String password;

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

    /** Le username est utilisé comme identifiant de connexion. */
    @Override
    public String getUsername() {
        return username;
    }

    /** Un compte inactif est considéré comme désactivé par Spring Security. */
    @Override
    public boolean isEnabled() {
        return active;
    }



}