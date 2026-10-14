package net.ent.etnc.orbit.dtos.assemblers;

import net.ent.etnc.orbit.dtos.requestDTO.UserCreateRequestDto;
import net.ent.etnc.orbit.dtos.requestDTO.UserUpdateRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.UserResponseDto;
import net.ent.etnc.orbit.models.entities.Personnel;
import org.springframework.stereotype.Component;

@Component
public class UserAssembler {

    public UserResponseDto toDto(Personnel personnel) {
        return UserResponseDto.builder()
                .id(personnel.getId())
                .login(personnel.getUsername())
                .role(personnel.getRole())
                .nom(personnel.getNom())
                .prenom(personnel.getPrenom())
                .active(personnel.isActive())
                .verrouille(personnel.isVerrouille())
                .stageId(personnel.getStage() != null ? personnel.getStage().getId() : null)
                .build();
    }

    public Personnel toEntity(UserCreateRequestDto userDto) {
        Personnel user = new Personnel();
        user.setNid(userDto.getNid());
        user.setNom(userDto.getNom());
        user.setPrenom(userDto.getPrenom());
        user.setLogin(userDto.getLogin());
        user.setMotDePasse(userDto.getMotDePasse());
        user.setRole(userDto.getRole());
        user.setActive(userDto.getActive() == null || userDto.getActive());
        return user;
    }

    /** Rôle et activation ne viennent pas de ce DTO : le service conserve ceux du compte existant. */
    public Personnel toEntity(UserUpdateRequestDto userDto) {
        Personnel user = new Personnel();
        user.setNid(userDto.getNid());
        user.setNom(userDto.getNom());
        user.setPrenom(userDto.getPrenom());
        user.setLogin(userDto.getLogin());
        user.setMotDePasse(userDto.getMotDePasse());
        return user;
    }

}