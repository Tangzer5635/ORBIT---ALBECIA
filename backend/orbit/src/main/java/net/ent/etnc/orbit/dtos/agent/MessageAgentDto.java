package net.ent.etnc.orbit.dtos.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.ent.etnc.orbit.dtos.agent.ReponseAgentDto.ProcessusAgent;

import java.util.List;

/**
 * Message JSON échangé sur le WebSocket de l'Agent ORBIT, dans les deux sens (JSON de orbit_agent_windows.py).
 * Usage interne au back : n'est jamais renvoyé au front. Le champ {@code type} dit quels champs sont remplis :
 * <ul>
 *   <li>{@code hello} (agent → back, à la connexion) : mac, nomMachine, ip, os, ram_go</li>
 *   <li>{@code processus} (agent → back, quand la liste change et toutes les 20 s) : processes</li>
 *   <li>{@code kill} (back → agent) : id, pid</li>
 *   <li>{@code resultat} (agent → back, réponse à un kill) : id, ok, statut (200 arrêté, 403 refusé, 404 déjà fermé), erreur</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MessageAgentDto(String type, String id, String mac, String nomMachine, String ip, String os,
                              @JsonProperty("ram_go") Double ramGo, List<ProcessusAgent> processes, Long pid,
                              Boolean ok, Integer statut, String erreur) {

    /** Ordre d'arrêter un processus, identifié par {@code id} pour retrouver la réponse. */
    public static MessageAgentDto kill(String id, long pid) {
        return new MessageAgentDto("kill", id, null, null, null, null, null, null, pid, null, null, null);
    }
}