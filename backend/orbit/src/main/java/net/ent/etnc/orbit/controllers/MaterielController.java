package net.ent.etnc.orbit.controllers;

import jakarta.validation.Valid;
import net.ent.etnc.orbit.commons.RsqlFilterUtils;
import net.ent.etnc.orbit.dtos.requestDTO.MaterielRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.calendrier.CalendrierGarantiesDto;
import net.ent.etnc.orbit.dtos.responseDTO.commons.PageResponse;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.dtos.responseDTO.MaterielResponseDto;
import net.ent.etnc.orbit.dtos.assemblers.MaterielAssembler;
import net.ent.etnc.orbit.models.enums.EtatMateriel;
import net.ent.etnc.orbit.models.enums.LocalisationMateriel;
import net.ent.etnc.orbit.repositories.specifications.MaterielSpecifications;
import net.ent.etnc.orbit.services.MaterielService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/materiels")
public class MaterielController {

    private static final Map<String, String> FILTERABLE = Map.of(
            "modele", "modele",
            "numSerie", "numSerie",
            "dateAcquisition", "dateAcquisition",
            "dateFinGarantie", "dateFinGarantie",
            "archive", "archive");


    private final MaterielService materielService;
    private final MaterielAssembler materielAssembler;
    private final RsqlFilterUtils rsqlFilterUtils;

    @Autowired
    public MaterielController(MaterielService materielService, MaterielAssembler materielAssembler, RsqlFilterUtils rsqlFilterUtils) {
        this.materielService = materielService;
        this.materielAssembler = materielAssembler;
        this.rsqlFilterUtils = rsqlFilterUtils;
    }

    private MaterielResponseDto dto(Materiel m) {
        return materielAssembler.toDto(m, materielService.etatDe(m), materielService.salleDe(m).orElse(null));
    }

    @GetMapping("/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<PageResponse<MaterielResponseDto>> getAll(
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) EtatMateriel etat,
            @RequestParam(required = false) LocalisationMateriel localisation,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {

        List<Specification<Materiel>> specs = new ArrayList<>();
        Specification<Materiel> rsql = rsqlFilterUtils.build(filter, FILTERABLE);
        if (rsql != null) specs.add(rsql);
        if (type != null) specs.add(MaterielSpecifications.type(type));
        if (etat != null) specs.add(MaterielSpecifications.etat(etat));
        if (localisation != null) specs.add(MaterielSpecifications.localisation(localisation));

        // allOf d'une liste vide = aucun filtre
        Page<Materiel> page = materielService.findAll(Specification.allOf(specs), pageable);
        return ResponseEntity.ok(PageResponse.of(page, this::dto));
    }

    /** Fiche d'un matériel. Ouverte au formateur pour le matériel de ses salles (« Remonter une défaillance »). */
    @GetMapping("/{id}/")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE','FORMATEUR')")
    public ResponseEntity<MaterielResponseDto> getById(@PathVariable Long id, @AuthenticationPrincipal Personnel user) {
        return materielService.findPourUtilisateur(id, user)
                .map(m -> ResponseEntity.ok(dto(m)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/garanties/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<CalendrierGarantiesDto> calendrierGaranties(@RequestParam Integer annee) {
        return ResponseEntity.ok(materielService.calendrierGaranties(annee));
    }

    @PostMapping("/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<MaterielResponseDto> post(@Valid @RequestBody MaterielRequestDto materielRequestDto) {
        return ResponseEntity.ok(dto(materielService.creerEnStock(materielAssembler.toEntity(materielRequestDto), materielRequestDto.getIdStockage())));
    }

    //TODO A CHANGER LORS QUE LA FICHE MODIFIER SERA FAITE EN FRONT
    @PutMapping("/{id}/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<MaterielResponseDto> put(@PathVariable Long id, @Valid @RequestBody MaterielRequestDto materielRequestDto) {
        if (!materielService.existsById(id)) return ResponseEntity.notFound().build();
        Materiel materiel = materielAssembler.toEntity(materielRequestDto);
        materiel.setId(id);
        return ResponseEntity.ok(dto(materielService.update(materiel)));
    }

    @DeleteMapping("/{id}/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!materielService.existsById(id)) return ResponseEntity.notFound().build();
        materielService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/archiver/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<MaterielResponseDto> archiver(@PathVariable Long id) {
        return ResponseEntity.ok(dto(materielService.archiver(id)));
    }

}