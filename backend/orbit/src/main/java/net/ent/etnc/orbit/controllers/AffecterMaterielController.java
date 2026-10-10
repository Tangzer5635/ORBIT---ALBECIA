package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.dtos.assemblers.MaterielAssembler;
import net.ent.etnc.orbit.dtos.responseDTO.MaterielResponseDto;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.services.AffecterMaterielService;
import net.ent.etnc.orbit.services.MaterielService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/affectermateriel")
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

    @PostMapping("/salles/{idSalle}/materiels/{idMateriel}/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<MaterielResponseDto> affecterASalle(@PathVariable Long idSalle, @PathVariable Long idMateriel) {
        return ResponseEntity.ok(dto(affecterMaterielService.affecterASalle(idSalle, idMateriel)));
    }

    @DeleteMapping("/salles/{idSalle}/materiels/{idMateriel}/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<MaterielResponseDto> retirerDeSalle(@PathVariable Long idSalle, @PathVariable Long idMateriel) {
        return ResponseEntity.ok(dto(affecterMaterielService.retirerDeSalle(idSalle, idMateriel)));
    }

    @PostMapping("/postes/{idPoste}/materiels/{idMateriel}/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<MaterielResponseDto> affecterAPoste(@PathVariable Long idPoste, @PathVariable Long idMateriel) {
        return ResponseEntity.ok(dto(affecterMaterielService.affecterAPoste(idPoste, idMateriel)));
    }

    @DeleteMapping("/postes/{idPoste}/materiels/{idMateriel}/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<MaterielResponseDto> retirerDuPoste(@PathVariable Long idPoste, @PathVariable Long idMateriel) {
        return ResponseEntity.ok(dto(affecterMaterielService.retirerDuPoste(idPoste, idMateriel)));
    }

    private MaterielResponseDto dto(Materiel m) {
        return materielAssembler.toDto(m, materielService.etatDe(m));
    }
}
