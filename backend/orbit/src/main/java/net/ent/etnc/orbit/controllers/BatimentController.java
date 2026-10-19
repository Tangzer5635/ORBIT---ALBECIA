package net.ent.etnc.orbit.controllers;

import jakarta.validation.Valid;
import net.ent.etnc.orbit.dtos.assemblers.BatimentAssembler;
import net.ent.etnc.orbit.dtos.requestDTO.BatimentRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.BatimentCarteDto;
import net.ent.etnc.orbit.dtos.responseDTO.BatimentResponseDto;
import net.ent.etnc.orbit.services.BatimentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    @PreAuthorize("hasRole('TECHNIQUE')")
    public ResponseEntity<Page<BatimentCarteDto>> getAll(
            @PageableDefault(size = 50, sort = "numBatiment") Pageable pageable
    ) {
        return ResponseEntity.ok(batimentService.findCartes(pageable));
    }

    @GetMapping("/{id}/")
    @PreAuthorize("hasRole('TECHNIQUE')")
    public ResponseEntity<BatimentResponseDto> getById(@PathVariable Long id) {
        return batimentService.findById(id)
                .map(batiment -> ResponseEntity.ok(batimentAssembler.toDto(batiment)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<BatimentResponseDto> post(@Valid @RequestBody BatimentRequestDto batimentDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                batimentAssembler.toDto(
                        batimentService.create(
                                batimentAssembler.toEntity(batimentDto),
                                batimentDto.getNumSalleStockage(),
                                batimentDto.getEtageStockage())));
    }

    @DeleteMapping("/{id}/")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestParam(required = false) Long stockageCible) {
        if (!batimentService.existsById(id)) return ResponseEntity.notFound().build();
        batimentService.supprimer(id, stockageCible);
        return ResponseEntity.noContent().build();
    }


}