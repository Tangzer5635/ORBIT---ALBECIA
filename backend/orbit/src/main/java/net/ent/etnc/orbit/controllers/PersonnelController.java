package net.ent.etnc.orbit.controllers;

import jakarta.validation.Valid;
import net.ent.etnc.orbit.commons.RsqlFilterUtils;
import net.ent.etnc.orbit.dtos.assemblers.UserAssembler;
import net.ent.etnc.orbit.dtos.requestDTO.RoleRequestDto;
import net.ent.etnc.orbit.dtos.requestDTO.UserCreateRequestDto;
import net.ent.etnc.orbit.dtos.requestDTO.UserUpdateRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.UserResponseDto;
import net.ent.etnc.orbit.dtos.responseDTO.commons.PageResponse;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.repositories.specifications.PersonnelSpecifications;
import net.ent.etnc.orbit.services.PersonnelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/personnels")
public class PersonnelController {

    /** Champs utilisables dans le filtre RSQL (?filter=) : sert aussi de liste blanche. */
    private static final Map<String, String> FILTERABLE = Map.of(
            "nom", "nom",
            "prenom", "prenom",
            "login", "login");

    private final PersonnelService personnelService;
    private final UserAssembler userAssembler;
    private final RsqlFilterUtils rsqlFilterUtils;

    @Autowired
    public PersonnelController(PersonnelService personnelService, UserAssembler userAssembler,
                               RsqlFilterUtils rsqlFilterUtils) {
        this.personnelService = personnelService;
        this.userAssembler = userAssembler;
        this.rsqlFilterUtils = rsqlFilterUtils;
    }

    /**
     * Une page d'utilisateurs filtrée par la base (même principe que GET /materiels/).
     * Ex. : /personnels/?page=0&size=20&role=FORMATEUR&active=true&filter=(nom=ilike="dur",prenom=ilike="dur",login=ilike="dur")
     */
    @GetMapping("/")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<PageResponse<UserResponseDto>> getAll(
            @RequestParam(required = false) String filter,
            @RequestParam(name = "filtreRole", required = false) Role role,
            @RequestParam(required = false) Boolean active,       // absent = actifs et inactifs
            @RequestParam(required = false) Boolean verrouille,   // absent = verrouillés ou non
            @PageableDefault(size = 20, sort = {"nom", "prenom", "id"}) Pageable pageable
    ) {
        List<Specification<Personnel>> specs = new ArrayList<>();
        Specification<Personnel> rsql = rsqlFilterUtils.build(filter, FILTERABLE);
        if (rsql != null) specs.add(rsql);
        if (role != null) specs.add(PersonnelSpecifications.role(role));
        if (active != null) specs.add(PersonnelSpecifications.active(active));
        if (verrouille != null) specs.add(PersonnelSpecifications.verrouille(verrouille));

        // allOf d'une liste vide = aucun filtre
        Page<Personnel> page = personnelService.findAll(Specification.allOf(specs), pageable);
        return ResponseEntity.ok(PageResponse.of(page, userAssembler::toDto));
    }

    @GetMapping("/{id}/")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<UserResponseDto> getById(@PathVariable Long id) {
        return personnelService.findById(id)
                .map(user -> ResponseEntity.ok(userAssembler.toDto(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<UserResponseDto> post(@Valid @RequestBody UserCreateRequestDto userDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                userAssembler.toDto(personnelService.create(userAssembler.toEntity(userDto))));
    }

    @PutMapping("/{id}/")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<UserResponseDto> put(@PathVariable Long id, @Valid @RequestBody UserUpdateRequestDto userDto) {
        Personnel user = userAssembler.toEntity(userDto);
        user.setId(id);
        return ResponseEntity.ok(userAssembler.toDto(personnelService.update(user)));
    }

    @PatchMapping("/{id}/role/")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<UserResponseDto> changerRole(@PathVariable Long id, @Valid @RequestBody RoleRequestDto roleDto) {
        return ResponseEntity.ok(userAssembler.toDto(personnelService.changeRole(id, roleDto.getRole())));
    }

    @PatchMapping("/{id}/activer/")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<UserResponseDto> activer(@PathVariable Long id) {
        return ResponseEntity.ok(userAssembler.toDto(personnelService.activer(id)));
    }

    @PatchMapping("/{id}/desactiver/")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<UserResponseDto> desactiver(@PathVariable Long id) {
        return ResponseEntity.ok(userAssembler.toDto(personnelService.desactiver(id)));
    }

    @PatchMapping("/{id}/verrouiller/")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<UserResponseDto> verrouiller(@PathVariable Long id) {
        return ResponseEntity.ok(userAssembler.toDto(personnelService.verrouiller(id)));
    }

    @PatchMapping("/{id}/deverrouiller/")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<UserResponseDto> deverrouiller(@PathVariable Long id) {
        return ResponseEntity.ok(userAssembler.toDto(personnelService.deverrouiller(id)));
    }

    @DeleteMapping("/{id}/")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        personnelService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /** Liste par rôle (ex. ?role=STAGIAIRE ou ?role=FORMATEUR) : ouverte au gestionnaire pour les stages. */
    @GetMapping(value = "/", params = "role")
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<Page<UserResponseDto>> getByRole(
            @RequestParam Role role,
            @PageableDefault(size = 200, sort = {"nom", "prenom", "id"}) Pageable pageable
    ) {
        return ResponseEntity.ok(personnelService.findByRole(role, pageable).map(userAssembler::toDto));
    }

    /** Stagiaires du stage sans poste (ex. ?stage=3&sansPoste) : ceux qu'on peut asseoir dans la salle du stage. */
    @GetMapping(value = "/", params = {"stage", "sansPoste"})
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public ResponseEntity<List<UserResponseDto>> getSansPoste(@RequestParam("stage") Long idStage) {
        return ResponseEntity.ok(personnelService.stagiairesSansPoste(idStage).stream().map(userAssembler::toDto).toList());
    }
}