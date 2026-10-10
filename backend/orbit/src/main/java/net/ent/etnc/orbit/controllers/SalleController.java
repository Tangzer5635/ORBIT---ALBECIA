package net.ent.etnc.orbit.controllers;

import jakarta.validation.Valid;
import net.ent.etnc.orbit.dtos.assemblers.SalleAssembler;
import net.ent.etnc.orbit.dtos.requestDTO.SalleRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.SalleResponseDto;
import net.ent.etnc.orbit.services.BatimentService;
import net.ent.etnc.orbit.services.SalleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/salles")
public class SalleController {

    private final SalleService salleService;
    private final BatimentService batimentService;
    private final SalleAssembler salleAssembler;

    @Autowired
    public SalleController(SalleService salleService, BatimentService batimentService, SalleAssembler salleAssembler) {
        this.salleService = salleService;
        this.batimentService = batimentService;
        this.salleAssembler = salleAssembler;
    }

    @GetMapping("/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<Page<SalleResponseDto>> getAll(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(salleService.findAll(pageable)
                .map(salleAssembler::toDto));
    }

    @GetMapping("/{id}/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<SalleResponseDto> getById(@PathVariable Long id) {
        return salleService.findById(id)
                .map(salle -> ResponseEntity.ok(salleAssembler.toDto(salle)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<SalleResponseDto> post(@Valid @RequestBody SalleRequestDto salleDto) {
        return ResponseEntity.ok(salleAssembler.toDto(
                batimentService.creerSalle(salleDto.getIdBatiment(), salleAssembler.toEntity(salleDto))));
    }

    @PatchMapping("/{id}/gestionnaire/{idGestionnaire}/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<SalleResponseDto> changerGestionnaire(@PathVariable Long id, @PathVariable Long idGestionnaire) {
        return ResponseEntity.ok(salleAssembler.toDto(salleService.changerGestionnaire(id, idGestionnaire)));
    }

    @DeleteMapping("/{id}/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!salleService.existsById(id)) return ResponseEntity.notFound().build();
        salleService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}