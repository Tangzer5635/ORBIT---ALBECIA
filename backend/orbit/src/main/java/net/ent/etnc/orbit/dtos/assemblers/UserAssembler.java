package net.ent.etnc.orbit.dtos.assemblers;

import net.ent.etnc.orbit.dtos.UserRequestDto;
import net.ent.etnc.orbit.dtos.UserResponseDto;
import net.ent.etnc.orbit.models.entities.Personnel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserAssembler {

    public UserResponseDto toDto(Personnel personnel) {
        return UserResponseDto.builder()
                .id(personnel.getId())
                .login(personnel.getUsername())
                .role(personnel.getRole())
                .active(personnel.isActive())
                .build();
    }

    public List<UserResponseDto> toDtos(List<Personnel> users) {
        return users.stream()
                .map(this::toDto)
                .toList();
    }

    public Personnel toEntity(UserRequestDto userDto) {
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

}