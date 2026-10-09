package net.ent.etnc.orbit.controllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/affectersalle")
public class AffecterSalleController {
    //TODO Ajouter une salle à un batiment
//    @PostMapping
//    @PreAuthorize("hasRole('ADMINISTRATEUR')")
//    public ResponseEntity<AjouterSalleABatiment> addSalleAUnBatiment(@RequestBody AjouterSalleABatiment batimentDto) {
//        return ResponseEntity.ok(
//              batimentService.addSalleAUnBatiment(batimentDto.idSalle, batimentDto.idBatiment)
//        )
//    }

    //TODO Supprimer une salle à un batiment
//    @PostMapping
//    @PreAuthorize("hasRole('ADMINISTRATEUR')")
//    public ResponseEntity<SupprimerSalleABatiment> deleteSalleAUnBatiment(@RequestBody SupprimerSalleABatiment batimentDto) {
//        return ResponseEntity.ok(
//
//        )
//    }

}
