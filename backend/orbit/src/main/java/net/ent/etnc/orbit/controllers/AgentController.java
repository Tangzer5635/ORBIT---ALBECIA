package net.ent.etnc.orbit.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

/**
 * Relais entre ORBIT et l'agent Python installé sur un poste.
 * Le front appelle ce controller ; lui seul connaît le token de l'agent.
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final RestClient client = RestClient.create();

    @Value("${agent.url}")     // ex. http://192.168.1.57:8777
    private String agentUrl;

    @Value("${agent.token}")   // le même token que l'agent Python
    private String agentToken;

    /** Liste les process du poste. */
    @GetMapping(value = "/processes", produces = "application/json")
    public String processes() {
        return client.get()
                .uri(agentUrl + "/processes")
                .header("X-Token", agentToken)
                .retrieve()
                .body(String.class);
    }

    /** Tue un process du poste par son PID. */
    @PostMapping(value = "/kill/{pid}", produces = "application/json")
    public String kill(@PathVariable int pid) {
        return client.post()
                .uri(agentUrl + "/kill/" + pid)
                .header("X-Token", agentToken)
                .retrieve()
                .body(String.class);
    }
}