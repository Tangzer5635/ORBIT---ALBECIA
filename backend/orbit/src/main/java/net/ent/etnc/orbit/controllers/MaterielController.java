package net.ent.etnc.orbit.controllers;

import jakarta.validation.Valid;
import net.ent.etnc.orbit.dtos.requestDTO.MaterielRequestDto;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.dtos.responseDTO.MaterielResponseDto;
import net.ent.etnc.orbit.dtos.assemblers.MaterielAssembler;
import net.ent.etnc.orbit.services.MaterielService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/materiels")
public class MaterielController {

    private final MaterielService materielService;
    private final MaterielAssembler materielAssembler;

    @Autowired
    public MaterielController(MaterielService materielService, MaterielAssembler materielAssembler) {
        this.materielService = materielService;
        this.materielAssembler = materielAssembler;
    }

    @GetMapping("/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<Page<MaterielResponseDto>> getAll(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(materielService.findAll(pageable)
                .map(materielAssembler::toDto));
    }

    @GetMapping("/{id}/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<MaterielResponseDto> getById(@PathVariable Long id) {
        return materielService.findById(id)
                .map(materiel -> ResponseEntity.ok(materielAssembler.toDto(materiel)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<MaterielResponseDto> post(@RequestBody MaterielRequestDto materielRequestDto) {
        return ResponseEntity.ok(
                materielAssembler.toDto(
                        materielService.create(
                                materielAssembler.toEntity(materielRequestDto))));
    }

    @PutMapping("/{id}/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<MaterielResponseDto> put(@PathVariable Long id, @RequestBody MaterielRequestDto materielRequestDto) {
        if (!materielService.existsById(id)) return ResponseEntity.notFound().build();
        Materiel materiel = materielAssembler.toEntity(materielRequestDto);
        materiel.setId(id);
        return ResponseEntity.ok(
                materielAssembler.toDto(
                        materielService.update(
                                materiel)));
    }

    @DeleteMapping("/{id}/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!materielService.existsById(id)) return ResponseEntity.notFound().build();
        materielService.deleteById(id);
        return ResponseEntity.noContent().build();
    }


}