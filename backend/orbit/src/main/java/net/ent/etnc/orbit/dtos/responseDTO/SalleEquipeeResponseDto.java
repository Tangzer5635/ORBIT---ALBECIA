package net.ent.etnc.orbit.dtos.responseDTO;

import net.ent.etnc.orbit.models.enums.TypeMateriel;

import java.util.Map;

public record SalleEquipeeResponseDto(Long idSalle, Map<TypeMateriel, Integer> manquants) {}