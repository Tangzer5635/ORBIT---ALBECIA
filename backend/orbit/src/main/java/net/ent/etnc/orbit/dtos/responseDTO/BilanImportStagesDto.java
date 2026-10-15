package net.ent.etnc.orbit.dtos.responseDTO;

import java.util.List;

public record BilanImportStagesDto(List<String> crees, List<String> misAJour, List<String> erreurs) {}