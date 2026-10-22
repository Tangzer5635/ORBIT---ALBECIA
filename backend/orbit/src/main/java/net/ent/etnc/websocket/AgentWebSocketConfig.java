package net.ent.etnc.orbit.websocket;

import net.ent.etnc.orbit.services.AgentService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Ouvre le WebSocket des agents : wss://serveur:8444/api/v1/agents/ws (Nginx relaie vers le back).
 * L'agent s'authentifie avec son en-tête X-Token dès l'ouverture : sans bon token, la connexion est refusée (401).
 */
@Configuration
@EnableWebSocket
public class AgentWebSocketConfig implements WebSocketConfigurer {

    public static final String CHEMIN = "/api/v1/agents/ws";

    private final net.ent.etnc.orbit.websocket.AgentWebSocketHandler handler;
    private final AgentService agentService;

    public AgentWebSocketConfig(net.ent.etnc.orbit.websocket.AgentWebSocketHandler handler, AgentService agentService) {
        this.handler = handler;
        this.agentService = agentService;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, CHEMIN)
                .addInterceptors(new TokenAgentInterceptor(agentService))
                // l'agent n'est pas un navigateur (pas de contrôle d'origine) : le token fait l'authentification
                .setAllowedOriginPatterns("*");
    }

    /** Refuse la connexion si le token X-Token n'est pas celui de agent.token. */
    private static class TokenAgentInterceptor implements HandshakeInterceptor {

        private final AgentService agentService;

        private TokenAgentInterceptor(AgentService agentService) {
            this.agentService = agentService;
        }

        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Map<String, Object> attributes) {
            try {
                agentService.verifierToken(request.getHeaders().getFirst("X-Token"));
                return true;
            } catch (ServiceException e) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Exception exception) {
        }
    }
}