package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.dtos.assemblers.PosteAssembler;
import net.ent.etnc.orbit.dtos.assemblers.SalleAssembler;
import net.ent.etnc.orbit.dtos.responseDTO.PosteResponseDto;
import net.ent.etnc.orbit.dtos.responseDTO.SalleResponseDto;
import net.ent.etnc.orbit.services.PosteService;
import net.ent.etnc.orbit.services.SalleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/affectermateriel")
public class AffecterMaterielController {

    private final SalleService salleService;
    private final SalleAssembler salleAssembler;
    private final PosteService posteService;
    private final PosteAssembler posteAssembler;

    @Autowired
    public AffecterMaterielController(SalleService salleService, SalleAssembler salleAssembler, PosteService posteService, PosteAssembler posteAssembler) {
        this.salleService = salleService;
        this.salleAssembler = salleAssembler;
        this.posteService = posteService;
        this.posteAssembler = posteAssembler;
    }

    @PostMapping("/salles/{idSalle}/materiels/{idMateriel}/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<SalleResponseDto> affecterASalle(@PathVariable Long idSalle, @PathVariable Long idMateriel) {
        return ResponseEntity.ok(salleAssembler.toDto(salleService.affecterMateriel(idSalle, idMateriel)));
    }

    @DeleteMapping("/salles/{idSalle}/materiels/{idMateriel}/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<Void> remettreEnStock(@PathVariable Long idSalle, @PathVariable Long idMateriel) {
        salleService.remettreEnStock(idSalle, idMateriel);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/postes/{idPoste}/materiels/{idMateriel}/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<PosteResponseDto> affecterAPoste(@PathVariable Long idPoste, @PathVariable Long idMateriel) {
        return ResponseEntity.ok(posteAssembler.toDto(posteService.affecterMateriel(idPoste, idMateriel)));
    }

    @DeleteMapping("/postes/{idPoste}/materiels/{idMateriel}/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<Void> retirerDuPoste(@PathVariable Long idPoste, @PathVariable Long idMateriel) {
        posteService.remettreEnStock(idPoste, idMateriel);
        return ResponseEntity.noContent().build();
    }
}
