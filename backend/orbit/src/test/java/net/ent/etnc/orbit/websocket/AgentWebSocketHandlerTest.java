package net.ent.etnc.orbit.websocket;

import net.ent.etnc.orbit.dtos.agent.MessageAgentDto;
import net.ent.etnc.orbit.services.AgentService;
import net.ent.etnc.orbit.services.ProcessusLanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentWebSocketHandlerTest {

    @Mock
    private AgentService agentService;

    @Mock
    private ProcessusLanceService processusLanceService;

    @Mock
    private WebSocketSession session;

    @Captor
    private ArgumentCaptor<Consumer<MessageAgentDto>> envoi;

    @Captor
    private ArgumentCaptor<TextMessage> envoye;

    private final Map<String, Object> attributs = new HashMap<>();

    private AgentWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new AgentWebSocketHandler(agentService, processusLanceService, JsonMapper.builder().build());
        when(session.getAttributes()).thenReturn(attributs);
    }

    @Test
    void hello_doitConnecterLAgentPuisLuiEnvoyerLesOrdres() throws Exception {
        handler.handleTextMessage(session, new TextMessage("""
                {"type":"hello","mac":"00:0C:29:AB:CD:EF","nomMachine":"PC-00001"}"""));

        verify(agentService).connecter(eq("00:0C:29:AB:CD:EF"), eq("PC-00001"), any(), any(), any(), envoi.capture());
        assertThat(attributs).containsEntry("mac", "00:0C:29:AB:CD:EF");

        // un ordre d'arrêt passe par la session décorée jusqu'à la vraie session
        envoi.getValue().accept(MessageAgentDto.kill("ordre-1", 1234L));

        verify(session).sendMessage(envoye.capture());
        assertThat(envoye.getValue().getPayload())
                .contains("\"type\":\"kill\"")
                .contains("\"id\":\"ordre-1\"")
                .contains("\"pid\":1234");
    }
}