package net.ent.etnc.orbit.controllers;

import jakarta.validation.Valid;
import net.ent.etnc.orbit.dtos.UserRequestDto;
import net.ent.etnc.orbit.dtos.UserResponseDto;
import net.ent.etnc.orbit.dtos.assemblers.UserAssembler;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.services.PersonnelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/personnels")
public class PersonnelController {

    private final PersonnelService personnelService;
    private final UserAssembler userAssembler;

    @Autowired
    public PersonnelController(PersonnelService personnelService, UserAssembler userAssembler) {
        this.personnelService = personnelService;
        this.userAssembler = userAssembler;
    }

    @GetMapping("/")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<Page<UserResponseDto>> getAll(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(personnelService.findAll(pageable)
                .map(userAssembler::toDto));
    }

    @GetMapping("/{id}/")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<UserResponseDto> getById(@PathVariable Long id) {
        return personnelService.findById(id)
                .map(user -> ResponseEntity.ok(userAssembler.toDto(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<UserResponseDto> post(@Valid @RequestBody UserRequestDto userDto) {
        return ResponseEntity.ok(
                userAssembler.toDto(personnelService.create(userAssembler.toEntity(userDto))));
    }

    @PutMapping("/{id}/")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<UserResponseDto> put(@PathVariable Long id, @Valid @RequestBody UserRequestDto userRequestDto) {
        if (!personnelService.existsById(id)) return ResponseEntity.notFound().build();
        Personnel user = userAssembler.toEntity(userRequestDto);
        user.setId(id);
        return ResponseEntity.ok(userAssembler.toDto(personnelService.update(user)));
    }

    @DeleteMapping("/{id}/")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        personnelService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}