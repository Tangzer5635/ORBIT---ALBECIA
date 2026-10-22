package net.ent.etnc.orbit.dtos.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Réponse de l'Agent ORBIT à GET /processes (JSON de orbit_agent_windows.py).
 * Usage interne au back : n'est jamais renvoyée au front.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReponseAgentDto(String host, String mac, String os, @JsonProperty("ram_go") Double ramGo,
                              List<ProcessusAgent> processes) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProcessusAgent(long pid, String name,
                                 @JsonProperty("cpu_percent") double cpuPercent,
                                 @JsonProperty("memory_percent") double memoryPercent) {}
}