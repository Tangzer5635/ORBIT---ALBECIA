package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.dtos.assemblers.BatimentAssembler;
import net.ent.etnc.orbit.dtos.responseDTO.BatimentResponseDto;
import net.ent.etnc.orbit.services.BatimentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/salles/{idSalle}")
public class AffecterSalleController {

    private final BatimentService batimentService;
    private final BatimentAssembler batimentAssembler;

    @Autowired
    public AffecterSalleController(BatimentService batimentService, BatimentAssembler batimentAssembler) {
        this.batimentService = batimentService;
        this.batimentAssembler = batimentAssembler;
    }

    @PostMapping(value = "/", params = "batimentId")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<BatimentResponseDto> affecterSalle(@PathVariable Long idSalle, @RequestParam Long batimentId) {
        return ResponseEntity.ok(batimentAssembler.toDto(batimentService.affecterSalle(batimentId, idSalle)));
    }

    @DeleteMapping(value = "/", params = "batimentId")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<Void> retirerSalle(@PathVariable Long idSalle, @RequestParam Long batimentId) {
        batimentService.retirerSalle(batimentId, idSalle);
        return ResponseEntity.noContent().build();
    }
}