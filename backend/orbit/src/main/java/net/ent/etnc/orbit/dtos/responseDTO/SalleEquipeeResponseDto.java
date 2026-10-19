package net.ent.etnc.orbit.dtos.responseDTO;

import java.util.Map;

public record SalleEquipeeResponseDto(Long idSalle, Map<String, Integer> manquants) {}