package net.ent.etnc.orbit.dtos.requestDTO;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.ent.etnc.orbit.models.enums.Role;

/** Changement du rôle d'un compte. */
@Getter
@Setter
@NoArgsConstructor
public class RoleRequestDto {

    @NotNull(message = "Le rôle est obligatoire")
    private Role role;
}