package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.dtos.agent.MessageAgentDto;
import net.ent.etnc.orbit.dtos.agent.ReponseAgentDto;
import net.ent.etnc.orbit.dtos.agent.ReponseAgentDto.ProcessusAgent;
import net.ent.etnc.orbit.models.entities.UC;
import net.ent.etnc.orbit.services.AgentService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Service
public class AgentServiceImpl implements AgentService {

    /** L'agent envoie un message au moins toutes les 20 s : sans nouvelle depuis 60 s, il est considéré arrêté. */
    private static final Duration DELAI_HORS_LIGNE = Duration.ofSeconds(60);

    /** Temps laissé à l'agent pour répondre à un ordre d'arrêt. */
    private static final long DELAI_REPONSE_SECONDES = 5;

    /** Un agent connu : sa présentation, comment lui écrire, sa dernière liste de processus et sa dernière nouvelle. */
    private static final class AgentConnu {
        private final String nomMachine;
        private final String ip;
        private final String os;
        private final Double ramGo;
        private final Consumer<MessageAgentDto> envoi;
        private volatile List<ProcessusAgent> processus = List.of();
        private volatile Instant derniereNouvelle = Instant.now();

        private AgentConnu(String nomMachine, String ip, String os, Double ramGo, Consumer<MessageAgentDto> envoi) {
            this.nomMachine = nomMachine;
            this.ip = ip;
            this.os = os;
            this.ramGo = ramGo;
            this.envoi = envoi;
        }
    }

    /** Clé : adresse MAC normalisée (00:0C:29:AB:CD:EF). */
    private final Map<String, AgentConnu> agents = new ConcurrentHashMap<>();
    /** Ordres d'arrêt envoyés et pas encore répondus, par identifiant d'ordre. */
    private final Map<String, CompletableFuture<ResultatArret>> attentes = new ConcurrentHashMap<>();
    private final String tokenAgent;

    public AgentServiceImpl(@Value("${agent.token}") String tokenAgent) {
        this.tokenAgent = tokenAgent;
    }

    @Override
    public void verifierToken(String token) throws ServiceException {
        // comparaison à temps constant, comme hmac.compare_digest côté agent
        if (token == null || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                tokenAgent.getBytes(StandardCharsets.UTF_8)))
            throw new ServiceException("Token de l'agent invalide");
    }

    @Override
    public void connecter(String mac, String nomMachine, String ip, String os, Double ramGo,
                          Consumer<MessageAgentDto> envoi) throws ServiceException {
        if (mac == null || !mac.matches(UC.MAC))
            throw new ServiceException("Adresse MAC de l'agent invalide : " + mac);
        agents.put(UC.normaliserMac(mac), new AgentConnu(nomMachine, ip, os, ramGo, envoi));
    }

    @Override
    public void deconnecter(String mac, Consumer<MessageAgentDto> envoi) {
        // si la machine s'est déjà reconnectée, l'ancienne connexion qui se ferme ne doit pas effacer la nouvelle
        agents.computeIfPresent(UC.normaliserMac(mac), (cle, agent) -> agent.envoi == envoi ? null : agent);
    }

    @Override
    public void recevoirProcessus(String mac, List<ProcessusAgent> processus) {
        AgentConnu agent = agents.get(UC.normaliserMac(mac));
        if (agent == null) return;
        agent.processus = processus == null ? List.of() : processus;
        agent.derniereNouvelle = Instant.now();
    }

    @Override
    public void recevoirResultat(String id, ResultatArret resultat) {
        CompletableFuture<ResultatArret> attente = id == null ? null : attentes.get(id);
        if (attente != null) attente.complete(resultat);
    }

    @Override
    public ReponseAgentDto etat(String mac) throws ServiceException {
        AgentConnu agent = agentEnLigne(mac);
        return new ReponseAgentDto(agent.nomMachine, UC.normaliserMac(mac), agent.os, agent.ramGo, agent.processus);
    }

    @Override
    public String adresseIp(String mac) throws ServiceException {
        return agentEnLigne(mac).ip;
    }

    @Override
    public ResultatArret tuer(String mac, long pid) throws ServiceException {
        AgentConnu agent = agentEnLigne(mac);
        String id = UUID.randomUUID().toString();
        CompletableFuture<ResultatArret> attente = new CompletableFuture<>();
        attentes.put(id, attente);
        try {
            agent.envoi.accept(MessageAgentDto.kill(id, pid));
            return attente.get(DELAI_REPONSE_SECONDES, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new ServiceException("L'agent de " + agent.nomMachine + " n'a pas répondu à l'ordre d'arrêt");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("Arrêt interrompu", e);
        } catch (ExecutionException e) {
            throw new ServiceException("Arrêt impossible : " + e.getMessage(), e);
        } finally {
            attentes.remove(id);
        }
    }

    @Override
    public void ordonnerArret(String mac, long pid) throws ServiceException {
        // identifiant que personne n'attend : la réponse de l'agent sera simplement ignorée
        agentEnLigne(mac).envoi.accept(MessageAgentDto.kill("auto-" + UUID.randomUUID(), pid));
    }

    /** L'agent de cette machine, s'il est connecté et donne encore des nouvelles. */
    private AgentConnu agentEnLigne(String mac) throws ServiceException {
        AgentConnu agent = agents.get(UC.normaliserMac(mac));
        if (agent == null)
            throw new ServiceException("Aucun Agent ORBIT n'est connecté avec l'adresse MAC " + mac
                    + " : agent arrêté sur la machine, mauvaise adresse MAC, ou serveur ORBIT injoignable depuis la machine");
        if (agent.derniereNouvelle.isBefore(Instant.now().minus(DELAI_HORS_LIGNE))) {
            LocalTime heure = LocalTime.ofInstant(agent.derniereNouvelle, ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS);
            throw new ServiceException("L'Agent ORBIT de " + agent.nomMachine + " (" + mac
                    + ") ne donne plus de nouvelles depuis " + heure + " : machine éteinte ou agent arrêté");
        }
        return agent;
    }
}