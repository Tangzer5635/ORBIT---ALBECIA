package net.ent.etnc.orbit.dtos.responseDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import net.ent.etnc.orbit.models.enums.Role;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {

    private Long id;
    @NotBlank(message = "login doit contenir des caractères lisibles")
    private String login;
    @NotNull(message = "Le rôle est obligatoire")
    private Role role;
    private boolean active;
}