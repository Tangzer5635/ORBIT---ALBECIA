package net.ent.etnc.orbit.controllers;

import jakarta.validation.Valid;
import net.ent.etnc.orbit.dtos.assemblers.TypeMaterielAssembler;
import net.ent.etnc.orbit.dtos.requestDTO.TypeMaterielRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.TypeMaterielResponseDto;
import net.ent.etnc.orbit.services.TypeMaterielService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/types-materiel")
public class TypeMaterielController {

    private final TypeMaterielService typeMaterielService;
    private final TypeMaterielAssembler typeMaterielAssembler;

    @Autowired
    public TypeMaterielController(TypeMaterielService typeMaterielService, TypeMaterielAssembler typeMaterielAssembler) {
        this.typeMaterielService = typeMaterielService;
        this.typeMaterielAssembler = typeMaterielAssembler;
    }

    /** Tous les types (de base + ajoutés) : liste déroulante de création et filtre de la page Matériel. */
    @GetMapping("/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<List<TypeMaterielResponseDto>> getAll() {
        return ResponseEntity.ok(typeMaterielAssembler.toListe(typeMaterielService.findAllTries()));
    }

    @PostMapping("/")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<TypeMaterielResponseDto> post(@Valid @RequestBody TypeMaterielRequestDto dto) {
        return ResponseEntity.ok(typeMaterielAssembler.toDto(typeMaterielService.create(typeMaterielAssembler.toEntity(dto))));
    }

    @DeleteMapping("/{id}/")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!typeMaterielService.existsById(id)) return ResponseEntity.notFound().build();
        typeMaterielService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}