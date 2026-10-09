package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.dtos.requestDTO.BatimentRequestDto;
import net.ent.etnc.orbit.models.entities.Batiment;
import net.ent.etnc.orbit.dtos.responseDTO.BatimentResponseDto;
import net.ent.etnc.orbit.dtos.assemblers.BatimentAssembler;
import net.ent.etnc.orbit.services.BatimentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/batiments")
public class BatimentController {

    private final BatimentService batimentService;
    private final BatimentAssembler batimentAssembler;

    @Autowired
    public BatimentController(BatimentService batimentService, BatimentAssembler batimentAssembler) {
        this.batimentService = batimentService;
        this.batimentAssembler = batimentAssembler;
    }

    @GetMapping("/")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<Page<BatimentResponseDto>> getAll(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(batimentService.findAll(pageable)
                .map(batimentAssembler::toDto));
    }

    @GetMapping("/{id}/")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<BatimentResponseDto> getById(@PathVariable Long id) {
        return batimentService.findById(id)
                .map(batiment -> ResponseEntity.ok(batimentAssembler.toDto(batiment)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<BatimentResponseDto> post(@RequestBody BatimentRequestDto batimentDto) {
        return ResponseEntity.ok(
                batimentAssembler.toDto(
                        batimentService.create(
                                batimentAssembler.toEntity(batimentDto))));
    }

    @PutMapping("/{id}/")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<BatimentResponseDto> put(@PathVariable Long id, @RequestBody BatimentRequestDto batimentDto) {
        if (!batimentService.existsById(id)) return ResponseEntity.notFound().build();
        Batiment batiment = batimentAssembler.toEntity(batimentDto);
        batiment.setId(id);
        return ResponseEntity.ok(
                batimentAssembler.toDto(
                        batimentService.update(
                                batiment)));
    }

    @DeleteMapping("/{id}/")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!batimentService.existsById(id)) return ResponseEntity.notFound().build();
        batimentService.deleteById(id);
        return ResponseEntity.noContent().build();
    }


}