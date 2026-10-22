package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.commons.JsonPatchUtils;
import net.ent.etnc.orbit.dtos.MasterDto;
import net.ent.etnc.orbit.models.entities.Master;
import net.ent.etnc.orbit.dtos.assemblers.MasterAssembler;
import net.ent.etnc.orbit.services.MasterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/masters")
public class MasterController {

    private final MasterService masterService;
    private final MasterAssembler masterAssembler;
    private final JsonPatchUtils jsonPatchUtils;

    static final Set<String> PATCHABLES = Set.of("nom", "versionMaster", "processusIds");

    @Autowired
    public MasterController(MasterService masterService, MasterAssembler masterAssembler, JsonPatchUtils jsonPatchUtils) {
        this.masterService = masterService;
        this.masterAssembler = masterAssembler;
        this.jsonPatchUtils = jsonPatchUtils;
    }

    @GetMapping("/")
    @PreAuthorize("hasRole('TECHNIQUE')")
    public ResponseEntity<Page<MasterDto>> getAll(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(masterService.findAll(pageable)
                .map(masterAssembler::toDto));
    }

    @GetMapping("/{id}/")
    @PreAuthorize("hasRole('TECHNIQUE')")
    public ResponseEntity<MasterDto> getById(@PathVariable Long id) {
        return masterService.findById(id)
                .map(master -> ResponseEntity.ok(masterAssembler.toDto(master)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<MasterDto> post(@RequestBody MasterDto masterDto) {
        return ResponseEntity.ok(masterAssembler.toDto(
                masterService.creer(masterAssembler.toEntity(masterDto), masterDto.getProcessusIds())));
    }

    /** JSON Patch (opérations « replace »). Ex. [{"op":"replace","path":"/processusIds","value":[1,4,7]}] */
    @PatchMapping("/{id}/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<MasterDto> patch(@PathVariable Long id, @RequestBody String patch) {
        Master existant = masterService.findById(id).orElse(null);
        if (existant == null) return ResponseEntity.notFound().build();
        MasterDto modifie = jsonPatchUtils.apply(patch, masterAssembler.toDto(existant), MasterDto.class, PATCHABLES);
        return ResponseEntity.ok(masterAssembler.toDto(
                masterService.modifier(id, masterAssembler.toEntity(modifie), modifie.getProcessusIds())));
    }

    @DeleteMapping("/{id}/")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!masterService.existsById(id)) return ResponseEntity.notFound().build();
        masterService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}