package net.ent.etnc.orbit.controllers;

import jakarta.validation.Valid;
import net.ent.etnc.orbit.commons.JsonPatchUtils;
import net.ent.etnc.orbit.dtos.assemblers.StageAssembler;
import net.ent.etnc.orbit.dtos.requestDTO.StageRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.BilanImportStagesDto;
import net.ent.etnc.orbit.dtos.responseDTO.StageResponseDto;
import net.ent.etnc.orbit.models.entities.Stage;
import net.ent.etnc.orbit.repositories.projections.SalleDuStage;
import net.ent.etnc.orbit.services.StageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/stages")
public class StageController {

    private final StageService stageService;
    private final StageAssembler stageAssembler;
    private final JsonPatchUtils jsonPatchUtils;

    /** Tout reste modifiable, même une fois le stage commencé. */
    static final Set<String> PATCHABLES = Set.of("nom", "type", "dateDebut", "dateFin", "formateurId");

    @Autowired
    public StageController(StageService stageService, StageAssembler stageAssembler, JsonPatchUtils jsonPatchUtils) {
        this.stageService = stageService;
        this.stageAssembler = stageAssembler;
        this.jsonPatchUtils = jsonPatchUtils;
    }

    @GetMapping("/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<Page<StageResponseDto>> getAll(@PageableDefault(size = 50, sort = "dateDebut") Pageable pageable) {
        Page<Stage> page = stageService.findAll(pageable);
        Map<Long, SalleDuStage> salles = stageService.sallesDesStages(page.map(Stage::getId).getContent());
        return ResponseEntity.ok(page.map(s -> stageAssembler.toDto(s, salles.get(s.getId()))));
    }

    @GetMapping("/{id}/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<StageResponseDto> getById(@PathVariable Long id) {
        return stageService.findById(id)
                .map(s -> ResponseEntity.ok(dto(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<StageResponseDto> post(@Valid @RequestBody StageRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                dto(stageService.creer(stageAssembler.toEntity(dto), dto.getFormateurId())));
    }

    /** Import d'un fichier CSV de stages (champ « fichier »), voir StageService.importer. */
    @PostMapping(value = "/import/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<BilanImportStagesDto> importer(@RequestParam("fichier") MultipartFile fichier) throws IOException {
        return ResponseEntity.ok(stageService.importer(new String(fichier.getBytes(), StandardCharsets.UTF_8)));
    }

    /** JSON Patch (opérations « replace »). Ex. [{"op":"replace","path":"/dateFin","value":"2027-02-05"}] */
    @PatchMapping("/{id}/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<StageResponseDto> patch(@PathVariable Long id, @RequestBody String patch) {
        Stage existant = stageService.findById(id).orElse(null);
        if (existant == null) return ResponseEntity.notFound().build();
        StageRequestDto modifie = jsonPatchUtils.apply(patch, stageAssembler.toRequestDto(existant),
                StageRequestDto.class, PATCHABLES);
        return ResponseEntity.ok(dto(stageService.modifier(id, stageAssembler.toEntity(modifie), modifie.getFormateurId())));
    }

    @DeleteMapping("/{id}/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        stageService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private StageResponseDto dto(Stage stage) {
        return stageAssembler.toDto(stage, stageService.salleDuStage(stage.getId()).orElse(null));
    }
}