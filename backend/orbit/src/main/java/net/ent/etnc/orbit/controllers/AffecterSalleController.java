package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.dtos.assemblers.BatimentAssembler;
import net.ent.etnc.orbit.dtos.responseDTO.BatimentResponseDto;
import net.ent.etnc.orbit.services.BatimentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/affectersalle")
public class AffecterSalleController {

    private final BatimentService batimentService;
    private final BatimentAssembler batimentAssembler;

    @Autowired
    public AffecterSalleController(BatimentService batimentService, BatimentAssembler batimentAssembler) {
        this.batimentService = batimentService;
        this.batimentAssembler = batimentAssembler;
    }

    @PostMapping("/{idBatiment}/salles/{idSalle}/")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<BatimentResponseDto> affecterSalle(@PathVariable Long idBatiment, @PathVariable Long idSalle) {
        return ResponseEntity.ok(batimentAssembler.toDto(batimentService.affecterSalle(idBatiment, idSalle)));
    }

    @DeleteMapping("/{idBatiment}/salles/{idSalle}/")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<Void> retirerSalle(@PathVariable Long idBatiment, @PathVariable Long idSalle) {
        batimentService.retirerSalle(idBatiment, idSalle);
        return ResponseEntity.noContent().build();
    }
}