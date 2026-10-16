package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.dtos.assemblers.PosteAssembler;
import net.ent.etnc.orbit.dtos.responseDTO.PosteResponseDto;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.services.AffecterStagiaireService;
import net.ent.etnc.orbit.services.SalleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Affecter / retirer un stagiaire d'un poste : ouvert au rôle technique (gestionnaire, administrateur et formateur).
 * Un formateur n'agit que dans les salles qui lui sont affectées.
 */
@RestController
@RequestMapping("/api/v1/postes/{idPoste}")
public class AffecterStagiaireController {

    private final AffecterStagiaireService affecterStagiaireService;
    private final SalleService salleService;
    private final PosteAssembler posteAssembler;

    @Autowired
    public AffecterStagiaireController(AffecterStagiaireService affecterStagiaireService, SalleService salleService,
                                       PosteAssembler posteAssembler) {
        this.affecterStagiaireService = affecterStagiaireService;
        this.salleService = salleService;
        this.posteAssembler = posteAssembler;
    }

    @PostMapping(value = "/", params = "stagiaireId")
    @PreAuthorize("hasRole('TECHNIQUE')")
    public ResponseEntity<PosteResponseDto> affecterStagiaire(@PathVariable Long idPoste, @RequestParam Long stagiaireId,
                                                              @AuthenticationPrincipal Personnel utilisateur) {
        verifierAcces(idPoste, utilisateur);
        return ResponseEntity.ok(posteAssembler.toDto(affecterStagiaireService.affecterAPoste(idPoste, stagiaireId)));
    }

    @DeleteMapping(value = "/", params = "stagiaireId")
    @PreAuthorize("hasRole('TECHNIQUE')")
    public ResponseEntity<PosteResponseDto> retirerStagiaire(@PathVariable Long idPoste, @RequestParam Long stagiaireId,
                                                             @AuthenticationPrincipal Personnel utilisateur) {
        verifierAcces(idPoste, utilisateur);
        return ResponseEntity.ok(posteAssembler.toDto(affecterStagiaireService.retirerDePoste(idPoste, stagiaireId)));
    }

    /** Un formateur ne touche qu'aux postes des salles qui lui sont affectées. */
    private void verifierAcces(Long idPoste, Personnel utilisateur) {
        salleService.salleDuPoste(idPoste).ifPresent(salle -> salleService.verifierAcces(salle.getId(), utilisateur));
    }
}