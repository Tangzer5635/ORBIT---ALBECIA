package net.ent.etnc.orbit.controllers;

import jakarta.validation.Valid;
import net.ent.etnc.orbit.dtos.assemblers.StageAssembler;
import net.ent.etnc.orbit.dtos.requestDTO.StageRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.StageResponseDto;
import net.ent.etnc.orbit.models.entities.Stage;
import net.ent.etnc.orbit.repositories.projections.SalleDuStage;
import net.ent.etnc.orbit.services.StageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/stages")
public class StageController {

    private final StageService stageService;
    private final StageAssembler stageAssembler;

    @Autowired
    public StageController(StageService stageService, StageAssembler stageAssembler) {
        this.stageService = stageService;
        this.stageAssembler = stageAssembler;
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

    @PutMapping("/{id}/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<StageResponseDto> put(@PathVariable Long id, @Valid @RequestBody StageRequestDto dto) {
        return ResponseEntity.ok(dto(stageService.modifier(id, stageAssembler.toEntity(dto), dto.getFormateurId())));
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
