package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.dtos.assemblers.PosteAssembler;
import net.ent.etnc.orbit.dtos.responseDTO.PosteResponseDto;
import net.ent.etnc.orbit.services.AffecterStagiaireService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/postes/{idPoste}")
public class AffecterStagiaireController {

    private final AffecterStagiaireService affecterStagiaireService;
    private final PosteAssembler posteAssembler;

    @Autowired
    public AffecterStagiaireController(AffecterStagiaireService affecterStagiaireService, PosteAssembler posteAssembler) {
        this.affecterStagiaireService = affecterStagiaireService;
        this.posteAssembler = posteAssembler;
    }

    @PostMapping(value = "/", params = "stagiaireId")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<PosteResponseDto> affecterStagiaire(@PathVariable Long idPoste, @RequestParam Long stagiaireId) {
        return ResponseEntity.ok(posteAssembler.toDto(affecterStagiaireService.affecterAPoste(idPoste, stagiaireId)));
    }

    @DeleteMapping(value = "/", params = "stagiaireId")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<PosteResponseDto> retirerStagiaire(@PathVariable Long idPoste, @RequestParam Long stagiaireId) {
        return ResponseEntity.ok(posteAssembler.toDto(affecterStagiaireService.retirerDePoste(idPoste, stagiaireId)));
    }
}
