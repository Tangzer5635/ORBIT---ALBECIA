package net.ent.etnc.orbit.dtos.assemblers;

import net.ent.etnc.orbit.dtos.UserRequestDto;
import net.ent.etnc.orbit.dtos.UserResponseDto;
import net.ent.etnc.orbit.models.entities.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserAssembler {

    public UserResponseDto toDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .active(user.isActive())
                .build();
    }

    public List<UserResponseDto> toDtos(List<User> users) {
        return users.stream()
                .map(this::toDto)
                .toList();
    }

    public User toEntity(UserRequestDto userDto) {
        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setPassword(userDto.getPassword());
        user.setRole(userDto.getRole());
        user.setActive(userDto.getActive() == null || userDto.getActive());
        return user;
    }

}