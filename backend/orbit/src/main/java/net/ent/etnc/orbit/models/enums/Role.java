package net.ent.etnc.orbit.models.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public enum Role {
    STAGIAIRE("Stagiaire"),
    FORMATEUR("Formateur"),
    TECHNIQUE("Technicien", true),
    GESTIONNAIRE("Gestionnaire", TECHNIQUE),
    ADMINISTRATEUR("Administrateur", GESTIONNAIRE);

    @Getter
    private final String libelle;
    @Getter
    private final Set<Role> roles;
    @Getter
    private boolean isTechnical = false;

    Role(String libelle, Role... roles) {
        this.libelle = libelle;
        this.roles = Set.of(roles);
    }

    Role(String libelle, boolean isTechnical, Role... roles) {
        this.libelle = libelle;
        this.isTechnical = isTechnical;
        this.roles = Set.of(roles);
    }

    private static Set<Role> getAllRoles(Role role) {
        Set<Role> roles = new LinkedHashSet<>();
        roles.add(role);
        for (Role r : role.getRoles()) {
            roles.addAll(getAllRoles(r));
        }
        return roles;
    }

    /**
     * Retourne tous les rôles non techniques (exposés à l'utilisateur final).
     */
    public static Role[] valuesNotTechnical() {
        return Arrays.stream(values()).filter(r -> !r.isTechnical()).toArray(Role[]::new);
    }

    /**
     * Vérifie si ce rôle possède (directement ou par héritage) le rôle donné.
     *
     * @param role le rôle à tester
     * @return {@code true} si ce rôle englobe le rôle demandé
     */
    public boolean hasRole(Role role) {
        return this.getAllRoles().contains(role);
    }

    /**
     * Retourne l'ensemble transitif de tous les rôles couverts par ce rôle.
     */
    public Set<Role> getAllRoles() {
        return Role.getAllRoles(this);
    }
}
