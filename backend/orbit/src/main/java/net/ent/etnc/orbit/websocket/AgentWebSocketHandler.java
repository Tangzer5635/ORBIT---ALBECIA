package net.ent.etnc.orbit.websocket;

import net.ent.etnc.orbit.dtos.agent.MessageAgentDto;
import net.ent.etnc.orbit.services.AgentService;
import net.ent.etnc.orbit.services.AgentService.ResultatArret;
import net.ent.etnc.orbit.services.ProcessusLanceService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * WebSocket de l'Agent ORBIT (ouvert par l'agent, relayé par Nginx) : reçoit sa présentation et ses listes de
 * processus, et lui envoie les ordres d'arrêt (demandés depuis le panneau d'un poste, ou automatiques pour la liste
 * noire). Aucune règle métier ici : tout est dans {@link AgentService} et {@link ProcessusLanceService}.
 * Le format des messages est décrit dans {@link MessageAgentDto}.
 */
@Component
public class AgentWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(AgentWebSocketHandler.class);

    /** Attributs de la session : machine de l'agent et comment lui écrire (renseignés à son « hello »). */
    private static final String ATTRIBUT_MAC = "mac";
    private static final String ATTRIBUT_ENVOI = "envoi";

    /** Une liste de processus dépasse vite les 8 Ko par défaut de Tomcat. */
    private static final int TAILLE_MAX_MESSAGE = 1024 * 1024;

    /** Envois vers l'agent : délai max d'un envoi et taille max des ordres en attente avant de couper la session. */
    private static final int DELAI_ENVOI_MS = 5_000;
    private static final int TAILLE_MAX_EN_ATTENTE = 64 * 1024;

    private final AgentService agentService;
    private final ProcessusLanceService processusLanceService;
    private final ObjectMapper objectMapper;

    @Autowired
    public AgentWebSocketHandler(AgentService agentService, ProcessusLanceService processusLanceService,
                                 ObjectMapper objectMapper) {
        this.agentService = agentService;
        this.processusLanceService = processusLanceService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        session.setTextMessageSizeLimit(TAILLE_MAX_MESSAGE);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage texte) throws IOException {
        try {
            traiter(session, objectMapper.readValue(texte.getPayload(), MessageAgentDto.class));
        } catch (RuntimeException e) {
            // message illisible ou refusé : on coupe, l'agent se reconnectera et se présentera de nouveau
            log.warn("Message d'agent refusé, connexion fermée : {}", e.getMessage());
            session.close(CloseStatus.POLICY_VIOLATION);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object mac = session.getAttributes().get(ATTRIBUT_MAC);
        Object envoi = session.getAttributes().get(ATTRIBUT_ENVOI);
        if (mac != null && envoi != null)
            agentService.deconnecter((String) mac, (Consumer<MessageAgentDto>) envoi);
    }

    private void traiter(WebSocketSession session, MessageAgentDto message) {
        String type = message.type() == null ? "" : message.type();
        switch (type) {
            case "hello" -> {
                // le décorateur de Spring envoie les ordres un par un, même si plusieurs requêtes écrivent en même temps
                WebSocketSession sortie = new ConcurrentWebSocketSessionDecorator(session, DELAI_ENVOI_MS, TAILLE_MAX_EN_ATTENTE);
                Consumer<MessageAgentDto> envoi = ordre -> envoyer(sortie, ordre);
                agentService.connecter(message.mac(), message.nomMachine(), message.ip(), message.os(),
                        message.ramGo(), envoi);
                session.getAttributes().put(ATTRIBUT_MAC, message.mac());
                session.getAttributes().put(ATTRIBUT_ENVOI, envoi);
            }
            case "processus" -> {
                String mac = mac(session);
                agentService.recevoirProcessus(mac, message.processes());
                // les processus en liste noire dans la salle de la machine sont arrêtés tout de suite
                processusLanceService.appliquerListeNoire(mac);
            }
            case "resultat" -> agentService.recevoirResultat(message.id(), new ResultatArret(
                    Boolean.TRUE.equals(message.ok()), message.statut() == null ? 0 : message.statut(),
                    message.erreur()));
            default -> throw new ServiceException("Type de message inconnu : " + type);
        }
    }

    /** La machine de cet agent ; un message avant le « hello » est refusé. */
    private String mac(WebSocketSession session) {
        Object mac = session.getAttributes().get(ATTRIBUT_MAC);
        if (mac == null) throw new ServiceException("Message reçu avant le hello de l'agent");
        return (String) mac;
    }

    /** Écrit à l'agent, via la session décorée (envois concurrents sérialisés par Spring). */
    private void envoyer(WebSocketSession sortie, MessageAgentDto ordre) {
        try {
            sortie.sendMessage(new TextMessage(objectMapper.writeValueAsString(ordre)));
        } catch (IOException e) {
            throw new ServiceException("Agent injoignable : la connexion est fermée", e);
        }
    }
}