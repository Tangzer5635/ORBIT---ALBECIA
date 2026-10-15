package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.dtos.assemblers.PosteAssembler;
import net.ent.etnc.orbit.dtos.responseDTO.PosteResponseDto;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.security.WebSecurityConfig;
import net.ent.etnc.orbit.services.AffecterStagiaireService;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AffecterStagiaireController.class)
@Import(WebSecurityConfig.class)
class AffecterStagiaireControllerTest extends ControllerTestSupport {

    @MockitoBean
    private AffecterStagiaireService affecterStagiaireService;

    @MockitoBean
    private SalleService salleService;

    @MockitoBean
    private PosteAssembler posteAssembler;

    // ---------- affecterStagiaire : POST /postes/{idPoste}/?stagiaireId= ----------

    @Test
    void affecterStagiaire_doitRenvoyer200_quandValide() throws Exception {
        when(affecterStagiaireService.affecterAPoste(10L, 7L)).thenReturn(new Poste());
        when(posteAssembler.toDto(any(Poste.class))).thenReturn(
                PosteResponseDto.builder().id(10L).occupantId(7L).occupant("Théo Dubois").build());

        mockMvc.perform(post("/api/v1/postes/{idPoste}/", 10L).param("stagiaireId", "7")
                        .with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occupant").value("Théo Dubois"));
    }

    @Test
    void affecterStagiaire_doitRenvoyer400_quandAutreStage() throws Exception {
        when(affecterStagiaireService.affecterAPoste(10L, 7L))
                .thenThrow(new ServiceException("Théo Dubois n'est pas dans le stage WB47"));

        mockMvc.perform(post("/api/v1/postes/{idPoste}/", 10L).param("stagiaireId", "7")
                        .with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Théo Dubois n'est pas dans le stage WB47"));
    }

    @Test
    void affecterStagiaire_doitRenvoyer200_quandFormateurDeLaSalle() throws Exception {
        when(affecterStagiaireService.affecterAPoste(10L, 7L)).thenReturn(new Poste());
        when(posteAssembler.toDto(any(Poste.class))).thenReturn(PosteResponseDto.builder().id(10L).build());

        mockMvc.perform(post("/api/v1/postes/{idPoste}/", 10L).param("stagiaireId", "7")
                        .with(connecte(Role.FORMATEUR)))
                .andExpect(status().isOk());
    }

    @Test
    void affecterStagiaire_doitRenvoyer403_quandFormateurPasDeCetteSalle() throws Exception {
        Salle salle = new Salle();
        salle.setId(5L);
        when(salleService.salleDuPoste(10L)).thenReturn(Optional.of(salle));
        doThrow(new AccessDeniedException("Salle 5 non affectée à ce formateur"))
                .when(salleService).verifierAcces(any(), any());

        mockMvc.perform(post("/api/v1/postes/{idPoste}/", 10L).param("stagiaireId", "7")
                        .with(connecte(Role.FORMATEUR)))
                .andExpect(status().isForbidden());
        verify(affecterStagiaireService, never()).affecterAPoste(any(), any());
    }

    @Test
    void affecterStagiaire_doitRenvoyer403_quandStagiaire() throws Exception {
        mockMvc.perform(post("/api/v1/postes/{idPoste}/", 10L).param("stagiaireId", "7")
                        .with(connecte(Role.STAGIAIRE)))
                .andExpect(status().isForbidden());
        verify(affecterStagiaireService, never()).affecterAPoste(any(), any());
    }

    @Test
    void affecterStagiaire_doitRenvoyer401_quandNonAuthentifie() throws Exception {
        mockMvc.perform(post("/api/v1/postes/{idPoste}/", 10L).param("stagiaireId", "7"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- retirerStagiaire : DELETE /postes/{idPoste}/?stagiaireId= ----------

    @Test
    void retirerStagiaire_doitRenvoyer200_quandValide() throws Exception {
        when(affecterStagiaireService.retirerDePoste(10L, 7L)).thenReturn(new Poste());
        when(posteAssembler.toDto(any(Poste.class))).thenReturn(PosteResponseDto.builder().id(10L).build());

        mockMvc.perform(delete("/api/v1/postes/{idPoste}/", 10L).param("stagiaireId", "7")
                        .with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void retirerStagiaire_doitRenvoyer400_quandPasSurCePoste() throws Exception {
        when(affecterStagiaireService.retirerDePoste(10L, 7L))
                .thenThrow(new ServiceException("Ce stagiaire n'est pas sur le poste B351-S105-P01"));

        mockMvc.perform(delete("/api/v1/postes/{idPoste}/", 10L).param("stagiaireId", "7")
                        .with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void retirerStagiaire_doitRenvoyer403_quandStagiaire() throws Exception {
        mockMvc.perform(delete("/api/v1/postes/{idPoste}/", 10L).param("stagiaireId", "7")
                        .with(connecte(Role.STAGIAIRE)))
                .andExpect(status().isForbidden());
        verify(affecterStagiaireService, never()).retirerDePoste(any(), any());
    }
}