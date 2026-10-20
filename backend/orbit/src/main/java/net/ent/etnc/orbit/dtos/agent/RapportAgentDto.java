package net.ent.etnc.orbit.dtos.agent;

import java.util.List;

public record RapportAgentDto(String machine, double timestamp, List<ProcessusLanceDto> processus) {}