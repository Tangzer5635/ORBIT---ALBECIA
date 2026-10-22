package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.dtos.assemblers.MaterielAssembler;
import net.ent.etnc.orbit.dtos.responseDTO.AffectationMaterielDto;
import net.ent.etnc.orbit.dtos.responseDTO.MaterielResponseDto;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.services.AffecterMaterielService;
import net.ent.etnc.orbit.services.MaterielService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/materiels/{idMateriel}")
public class AffecterMaterielController {

    private final AffecterMaterielService affecterMaterielService;
    private final MaterielAssembler materielAssembler;
    private final MaterielService materielService;

    @Autowired
    public AffecterMaterielController(AffecterMaterielService affecterMaterielService, MaterielAssembler materielAssembler, MaterielService materielService) {
        this.affecterMaterielService = affecterMaterielService;
        this.materielAssembler = materielAssembler;
        this.materielService = materielService;
    }

    /** Où se trouve le matériel (fiche garantie du calendrier). */
    @GetMapping("/affectation/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<AffectationMaterielDto> affectation(@PathVariable Long idMateriel) {
        return ResponseEntity.ok(affecterMaterielService.affectation(idMateriel));
    }

    @PostMapping(value = "/", params = "salleId")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<MaterielResponseDto> affecterASalle(@PathVariable Long idMateriel, @RequestParam Long salleId) {
        return ResponseEntity.ok(dto(affecterMaterielService.affecterASalle(salleId, idMateriel)));
    }

    @DeleteMapping(value = "/", params = "salleId")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<MaterielResponseDto> retirerDeSalle(@PathVariable Long idMateriel, @RequestParam Long salleId) {
        return ResponseEntity.ok(dto(affecterMaterielService.retirerDeSalle(salleId, idMateriel)));
    }

    @PostMapping(value = "/", params = "posteId")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<MaterielResponseDto> affecterAPoste(@PathVariable Long idMateriel, @RequestParam Long posteId) {
        return ResponseEntity.ok(dto(affecterMaterielService.affecterAPoste(posteId, idMateriel)));
    }

    @DeleteMapping(value = "/", params = "posteId")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<MaterielResponseDto> retirerDuPoste(@PathVariable Long idMateriel, @RequestParam Long posteId) {
        return ResponseEntity.ok(dto(affecterMaterielService.retirerDuPoste(posteId, idMateriel)));
    }

    /** Changer d'emplacement : le matériel quitte sa salle ou son poste pour cette salle de cours. */
    @PatchMapping(value = "/", params = "salleId")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<MaterielResponseDto> deplacerVersSalle(@PathVariable Long idMateriel, @RequestParam Long salleId) {
        return ResponseEntity.ok(dto(affecterMaterielService.deplacerVersSalle(salleId, idMateriel)));
    }

    /** Changer d'emplacement : le matériel quitte sa salle ou son poste pour ce poste. */
    @PatchMapping(value = "/", params = "posteId")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<MaterielResponseDto> deplacerVersPoste(@PathVariable Long idMateriel, @RequestParam Long posteId) {
        return ResponseEntity.ok(dto(affecterMaterielService.deplacerVersPoste(posteId, idMateriel)));
    }

    /**
     * Archiver depuis la fiche garantie (?retirer=true) : un matériel encore affecté est d'abord remis en stock.
     * Sans ce paramètre, PATCH /materiels/{id}/archiver/ (MaterielController) n'archive qu'un matériel en stock.
     */
    @PatchMapping(value = "/archiver/", params = "retirer=true")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<MaterielResponseDto> retirerEtArchiver(@PathVariable Long idMateriel) {
        return ResponseEntity.ok(dto(affecterMaterielService.retirerEtArchiver(idMateriel)));
    }

    private MaterielResponseDto dto(Materiel m) {
        return materielAssembler.toDto(m, materielService.etatDe(m));
    }
}