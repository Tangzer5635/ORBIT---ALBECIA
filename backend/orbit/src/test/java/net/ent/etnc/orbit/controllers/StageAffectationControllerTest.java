package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.dtos.assemblers.UserAssembler;
import net.ent.etnc.orbit.dtos.responseDTO.UserResponseDto;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.security.WebSecurityConfig;
import net.ent.etnc.orbit.services.StageAffectationService;
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

@WebMvcTest(StageAffectationController.class)
@Import(WebSecurityConfig.class)
class StageAffectationControllerTest extends ControllerTestSupport {

    @MockitoBean
    private StageAffectationService stageAffectationService;

    @MockitoBean
    private UserAssembler userAssembler;

    // ---------- ajouterAStage : POST /personnels/{idStagiaire}/?stageId= ----------

    @Test
    void ajouterAStage_doitRenvoyer200_quandValide() throws Exception {
        when(stageAffectationService.ajouterAStage(3L, 7L)).thenReturn(new Personnel());
        when(userAssembler.toDto(any())).thenReturn(
                UserResponseDto.builder().id(7L).role(Role.STAGIAIRE).stageId(3L).build());

        mockMvc.perform(post("/api/v1/personnels/{idStagiaire}/", 7L).param("stageId", "3")
                        .with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stageId").value(3));
    }

    @Test
    void ajouterAStage_doitRenvoyer400_quandDejaDansUnAutreStage() throws Exception {
        when(stageAffectationService.ajouterAStage(3L, 7L))
                .thenThrow(new ServiceException("Théo Dubois est déjà dans le stage WB47 : on ne change pas de stage"));

        mockMvc.perform(post("/api/v1/personnels/{idStagiaire}/", 7L).param("stageId", "3")
                        .with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Théo Dubois est déjà dans le stage WB47 : on ne change pas de stage"));
    }

    @Test
    void ajouterAStage_doitRenvoyer400_quandPasStagiaire() throws Exception {
        when(stageAffectationService.ajouterAStage(3L, 2L))
                .thenThrow(new ServiceException("Pierre Guillou n'est pas un stagiaire"));

        mockMvc.perform(post("/api/v1/personnels/{idStagiaire}/", 2L).param("stageId", "3")
                        .with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ajouterAStage_doitRenvoyer403_quandFormateur() throws Exception {
        mockMvc.perform(post("/api/v1/personnels/{idStagiaire}/", 7L).param("stageId", "3")
                        .with(connecte(Role.FORMATEUR)))
                .andExpect(status().isForbidden());
        verify(stageAffectationService, never()).ajouterAStage(any(), any());
    }

    @Test
    void ajouterAStage_doitRenvoyer401_quandNonAuthentifie() throws Exception {
        mockMvc.perform(post("/api/v1/personnels/{idStagiaire}/", 7L).param("stageId", "3"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- supprimerAStage : DELETE /personnels/{idStagiaire}/?stageId= ----------

    @Test
    void supprimerAStage_doitRenvoyer200_quandValide() throws Exception {
        when(stageAffectationService.supprimerAStage(3L, 7L)).thenReturn(new Personnel());
        when(userAssembler.toDto(any())).thenReturn(
                UserResponseDto.builder().id(7L).role(Role.STAGIAIRE).build());

        mockMvc.perform(delete("/api/v1/personnels/{idStagiaire}/", 7L).param("stageId", "3")
                        .with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void supprimerAStage_doitRenvoyer400_quandPasDansCeStage() throws Exception {
        when(stageAffectationService.supprimerAStage(3L, 7L))
                .thenThrow(new ServiceException("Théo Dubois n'est pas dans ce stage"));

        mockMvc.perform(delete("/api/v1/personnels/{idStagiaire}/", 7L).param("stageId", "3")
                        .with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void supprimerAStage_doitRenvoyer403_quandStagiaire() throws Exception {
        mockMvc.perform(delete("/api/v1/personnels/{idStagiaire}/", 7L).param("stageId", "3")
                        .with(connecte(Role.STAGIAIRE)))
                .andExpect(status().isForbidden());
        verify(stageAffectationService, never()).supprimerAStage(any(), any());
    }
}