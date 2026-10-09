package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.services.MaterielService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/affectermateriel")
public class AffecterMaterielController {

    private MaterielService materielService;

    public AffecterMaterielController(MaterielService materielService) {
        this.materielService = materielService;
    }

    //TODO AFFECTER UN MATERIAL A UNE SALLE
//    @PostMapping
//    @PreAuthorize("hasRole('GESTIONNAIRE')")
//    public ResponseEntity<MaterielAffectDto> affecterAUneSalle(@Valid @RequestBody MaterielAffectDto materielAffectDto){
//        return ResponseEntity.ok(
//
//        )
//    }
}
