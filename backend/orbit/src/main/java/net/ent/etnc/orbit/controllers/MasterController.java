package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.models.entities.Master;
import net.ent.etnc.orbit.dtos.MasterDto;
import net.ent.etnc.orbit.dtos.assemblers.MasterAssembler;
import net.ent.etnc.orbit.services.MasterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/masters")
public class MasterController {

    private final MasterService masterService;
    private final MasterAssembler masterAssembler;

    @Autowired
    public MasterController(MasterService masterService, MasterAssembler masterAssembler) {
        this.masterService = masterService;
        this.masterAssembler = masterAssembler;
    }

    @GetMapping
    @PreAuthorize("hasRole('')")
    public ResponseEntity<Page<MasterDto>> getAll(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(masterService.findAll(pageable)
                .map(masterAssembler::toDto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('')")
    public ResponseEntity<MasterDto> getById(@PathVariable Long id) {
        return masterService.findById(id)
                .map(master -> ResponseEntity.ok(masterAssembler.toDto(master)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('')")
    public ResponseEntity<MasterDto> post(@RequestBody MasterDto masterDto) {
        return ResponseEntity.ok(
                masterAssembler.toDto(
                        masterService.create(
                                masterAssembler.toEntity(masterDto))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('')")
    public ResponseEntity<MasterDto> put(@PathVariable Long id, @RequestBody MasterDto masterDto) {
        if (!masterService.existsById(id)) return ResponseEntity.notFound().build();
        Master master = masterAssembler.toEntity(masterDto);
        master.setId(id);
        return ResponseEntity.ok(
                masterAssembler.toDto(
                        masterService.update(
                                master)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!masterService.existsById(id)) return ResponseEntity.notFound().build();
        masterService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}