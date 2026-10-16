package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.dtos.assemblers.SalleAssembler;
import net.ent.etnc.orbit.dtos.responseDTO.SalleResponseDto;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.security.WebSecurityConfig;
import net.ent.etnc.orbit.services.AffecterStageService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AffecterStageController.class)
@Import(WebSecurityConfig.class)
class AffecterStageControllerTest extends ControllerTestSupport {

    @MockitoBean
    private AffecterStageService affecterStageService;

    @MockitoBean
    private SalleAssembler salleAssembler;

    // ---------- affecterStage : POST /salles/{idSalle}/?stageId= ----------

    @Test
    void affecterStage_doitRenvoyer200_quandValide() throws Exception {
        when(affecterStageService.affecterASalle(6L, 3L)).thenReturn(new Salle());
        when(salleAssembler.toDto(any())).thenReturn(
                SalleResponseDto.builder().id(6L).numSalle("105").stageId(3L).stageNom("WB47").build());

        mockMvc.perform(post("/api/v1/salles/{idSalle}/", 6L).param("stageId", "3")
                        .with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stageNom").value("WB47"));
    }

    @Test
    void affecterStage_doitRenvoyer400_quandSalleDejaPrise() throws Exception {
        when(affecterStageService.affecterASalle(6L, 3L))
                .thenThrow(new ServiceException("La salle 105 accueille déjà le stage VU82"));

        mockMvc.perform(post("/api/v1/salles/{idSalle}/", 6L).param("stageId", "3")
                        .with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("La salle 105 accueille déjà le stage VU82"));
    }

    @Test
    void affecterStage_doitRenvoyer400_quandSalleDeStockage() throws Exception {
        when(affecterStageService.affecterASalle(9L, 3L))
                .thenThrow(new ServiceException("Seule une salle de cours accueille un stage"));

        mockMvc.perform(post("/api/v1/salles/{idSalle}/", 9L).param("stageId", "3")
                        .with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void affecterStage_doitRenvoyer403_quandFormateur() throws Exception {
        mockMvc.perform(post("/api/v1/salles/{idSalle}/", 6L).param("stageId", "3")
                        .with(connecte(Role.FORMATEUR)))
                .andExpect(status().isForbidden());
        verify(affecterStageService, never()).affecterASalle(any(), any());
    }

    @Test
    void affecterStage_doitRenvoyer401_quandNonAuthentifie() throws Exception {
        mockMvc.perform(post("/api/v1/salles/{idSalle}/", 6L).param("stageId", "3"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- retirerStage : DELETE /salles/{idSalle}/?stageId= ----------

    @Test
    void retirerStage_doitRenvoyer200_quandValide() throws Exception {
        when(affecterStageService.retirerDeSalle(6L, 3L)).thenReturn(new Salle());
        when(salleAssembler.toDto(any())).thenReturn(SalleResponseDto.builder().id(6L).numSalle("105").build());

        mockMvc.perform(delete("/api/v1/salles/{idSalle}/", 6L).param("stageId", "3")
                        .with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(6));
    }

    @Test
    void retirerStage_doitRenvoyer400_quandStagePasDansLaSalle() throws Exception {
        when(affecterStageService.retirerDeSalle(6L, 3L))
                .thenThrow(new ServiceException("Ce stage n'est pas dans cette salle"));

        mockMvc.perform(delete("/api/v1/salles/{idSalle}/", 6L).param("stageId", "3")
                        .with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void retirerStage_doitRenvoyer403_quandStagiaire() throws Exception {
        mockMvc.perform(delete("/api/v1/salles/{idSalle}/", 6L).param("stageId", "3")
                        .with(connecte(Role.STAGIAIRE)))
                .andExpect(status().isForbidden());
        verify(affecterStageService, never()).retirerDeSalle(any(), any());
    }
}
