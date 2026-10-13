package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.dtos.assemblers.DefaillanceAssembler;
import net.ent.etnc.orbit.dtos.responseDTO.DefaillanceResponseDto;
import net.ent.etnc.orbit.models.entities.Defaillance;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.models.enums.StatutDefaillance;
import net.ent.etnc.orbit.security.WebSecurityConfig;
import net.ent.etnc.orbit.services.DefaillanceService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DefaillanceController.class)
@Import(WebSecurityConfig.class)
class DefaillanceControllerTest extends ControllerTestSupport {

    private static final String JSON_SIGNALER = "{\"idMateriel\":1,\"description\":\"L'unité centrale ne démarre plus\"}";
    private static final String JSON_RESOUDRE = "{\"noteSolution\":\"Alimentation remplacée\"}";

    @MockitoBean
    private DefaillanceService defaillanceService;

    @MockitoBean
    private DefaillanceAssembler defaillanceAssembler;

    private void reponse(StatutDefaillance statut) {
        when(defaillanceAssembler.toDto(any())).thenReturn(DefaillanceResponseDto.builder()
                .id(1L).idMateriel(1L).numSerieMateriel("UC-0001").statut(statut).build());
    }

    // ---------- getAll ----------

    @Test
    void getAll_doitRenvoyer200_sansFiltre() throws Exception {
        when(defaillanceService.findAll(any())).thenReturn(new PageImpl<>(List.of(new Defaillance()), PageRequest.of(0, 20), 1));
        reponse(StatutDefaillance.SIGNALEE);

        mockMvc.perform(get("/api/v1/defaillances/").with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].numSerieMateriel").value("UC-0001"));
        verify(defaillanceService, never()).findByStatut(any(), any());
    }

    @Test
    void getAll_doitFiltrerParStatut() throws Exception {
        when(defaillanceService.findByStatut(eq(StatutDefaillance.SIGNALEE), any()))
                .thenReturn(new PageImpl<>(List.of(new Defaillance()), PageRequest.of(0, 20), 1));
        reponse(StatutDefaillance.SIGNALEE);

        mockMvc.perform(get("/api/v1/defaillances/").param("statut", "SIGNALEE").with(connecte(Role.FORMATEUR)))
                .andExpect(status().isOk());
        verify(defaillanceService, never()).findAll(any());
    }

    @Test
    void getAll_doitRenvoyer400_quandStatutInconnu() throws Exception {
        mockMvc.perform(get("/api/v1/defaillances/").param("statut", "PERDUE").with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAll_doitRenvoyer403_quandStagiaire() throws Exception {
        mockMvc.perform(get("/api/v1/defaillances/").with(connecte(Role.STAGIAIRE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_doitRenvoyer401_quandNonAuthentifie() throws Exception {
        mockMvc.perform(get("/api/v1/defaillances/"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- getById ----------

    @Test
    void getById_doitRenvoyer200_quandTrouvee() throws Exception {
        when(defaillanceService.findById(1L)).thenReturn(Optional.of(new Defaillance()));
        reponse(StatutDefaillance.SIGNALEE);

        mockMvc.perform(get("/api/v1/defaillances/{id}/", 1L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("SIGNALEE"));
    }

    @Test
    void getById_doitRenvoyer404_quandIntrouvable() throws Exception {
        when(defaillanceService.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/defaillances/{id}/", 1L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getById_doitRenvoyer403_quandStagiaire() throws Exception {
        mockMvc.perform(get("/api/v1/defaillances/{id}/", 1L).with(connecte(Role.STAGIAIRE)))
                .andExpect(status().isForbidden());
    }

    // ---------- getByMateriel ----------

    @Test
    void getByMateriel_doitRenvoyer200_quandStagiaire() throws Exception {
        when(defaillanceService.findByMateriel(1L)).thenReturn(List.of(new Defaillance()));
        reponse(StatutDefaillance.RESOLUE);

        mockMvc.perform(get("/api/v1/defaillances/materiel/{idMateriel}/", 1L).with(connecte(Role.STAGIAIRE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].statut").value("RESOLUE"));
    }

    @Test
    void getByMateriel_doitRenvoyerListeVide_quandAucuneDefaillance() throws Exception {
        when(defaillanceService.findByMateriel(9L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/defaillances/materiel/{idMateriel}/", 9L).with(connecte(Role.STAGIAIRE)))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getByMateriel_doitRenvoyer401_quandNonAuthentifie() throws Exception {
        mockMvc.perform(get("/api/v1/defaillances/materiel/{idMateriel}/", 1L))
                .andExpect(status().isUnauthorized());
    }

    // ---------- signaler ----------

    @Test
    void signaler_doitRenvoyer201_etTransmettreLAuteurConnecte() throws Exception {
        when(defaillanceService.signaler(eq(1L), anyString(), any(Personnel.class))).thenReturn(new Defaillance());
        reponse(StatutDefaillance.SIGNALEE);

        mockMvc.perform(post("/api/v1/defaillances/").with(connecte(Role.STAGIAIRE))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_SIGNALER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("SIGNALEE"));
        verify(defaillanceService).signaler(eq(1L), eq("L'unité centrale ne démarre plus"),
                argThat(p -> p.getRole() == Role.STAGIAIRE));
    }

    @Test
    void signaler_doitRenvoyer201_quandFormateur() throws Exception {
        when(defaillanceService.signaler(eq(1L), anyString(), any(Personnel.class))).thenReturn(new Defaillance());
        reponse(StatutDefaillance.SIGNALEE);

        mockMvc.perform(post("/api/v1/defaillances/").with(connecte(Role.FORMATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_SIGNALER))
                .andExpect(status().isCreated());
    }

    @Test
    void signaler_doitRenvoyer403_quandHorsDeSonPosteOuDeSesSalles() throws Exception {
        when(defaillanceService.signaler(eq(1L), anyString(), any(Personnel.class)))
                .thenThrow(new AccessDeniedException("Ce matériel n'est ni sur votre poste ni sur un poste de vos salles"));

        mockMvc.perform(post("/api/v1/defaillances/").with(connecte(Role.STAGIAIRE))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_SIGNALER))
                .andExpect(status().isForbidden());
    }

    @Test
    void signaler_doitRenvoyer400_quandDejaSignalee() throws Exception {
        when(defaillanceService.signaler(eq(1L), anyString(), any(Personnel.class)))
                .thenThrow(new ServiceException("RG-D03", "Ce matériel a déjà une défaillance signalée"));

        mockMvc.perform(post("/api/v1/defaillances/").with(connecte(Role.STAGIAIRE))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_SIGNALER))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Ce matériel a déjà une défaillance signalée"));
    }

    @Test
    void signaler_doitRenvoyer404_quandMaterielIntrouvable() throws Exception {
        when(defaillanceService.signaler(eq(1L), anyString(), any(Personnel.class)))
                .thenThrow(new ServiceException("RG-D01", "Matériel 1 introuvable"));

        mockMvc.perform(post("/api/v1/defaillances/").with(connecte(Role.STAGIAIRE))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_SIGNALER))
                .andExpect(status().isNotFound());
    }

    @Test
    void signaler_doitRenvoyer400_quandDescriptionTropCourte() throws Exception {
        mockMvc.perform(post("/api/v1/defaillances/").with(connecte(Role.STAGIAIRE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idMateriel\":1,\"description\":\"HS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.description").exists());
        verify(defaillanceService, never()).signaler(any(), any(), any());
    }

    @Test
    void signaler_doitRenvoyer400_quandMaterielManquant() throws Exception {
        mockMvc.perform(post("/api/v1/defaillances/").with(connecte(Role.STAGIAIRE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Aucun matériel indiqué\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.idMateriel").exists());
    }

    @Test
    void signaler_doitRenvoyer403_quandGestionnaire() throws Exception {
        mockMvc.perform(post("/api/v1/defaillances/").with(connecte(Role.GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_SIGNALER))
                .andExpect(status().isForbidden());
    }

    @Test
    void signaler_doitRenvoyer403_quandAdministrateur() throws Exception {
        // DCU : l'administrateur hérite du gestionnaire, pas du formateur
        mockMvc.perform(post("/api/v1/defaillances/").with(connecte(Role.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_SIGNALER))
                .andExpect(status().isForbidden());
    }

    // ---------- resoudre ----------

    @Test
    void resoudre_doitRenvoyer200_quandGestionnaire() throws Exception {
        when(defaillanceService.resoudre(eq(1L), eq("Alimentation remplacée"), any(Personnel.class))).thenReturn(new Defaillance());
        reponse(StatutDefaillance.RESOLUE);

        mockMvc.perform(patch("/api/v1/defaillances/{id}/resoudre/", 1L).with(connecte(Role.GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_RESOUDRE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("RESOLUE"));
    }

    @Test
    void resoudre_doitRenvoyer403_quandFormateurHorsDeSesSalles() throws Exception {
        when(defaillanceService.resoudre(eq(1L), anyString(), any(Personnel.class)))
                .thenThrow(new AccessDeniedException("Cette défaillance n'est pas dans vos salles"));

        mockMvc.perform(patch("/api/v1/defaillances/{id}/resoudre/", 1L).with(connecte(Role.FORMATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_RESOUDRE))
                .andExpect(status().isForbidden());
    }

    @Test
    void resoudre_doitRenvoyer400_quandDejaResolue() throws Exception {
        when(defaillanceService.resoudre(eq(1L), anyString(), any(Personnel.class)))
                .thenThrow(new ServiceException("RG-D05", "Défaillance déjà résolue"));

        mockMvc.perform(patch("/api/v1/defaillances/{id}/resoudre/", 1L).with(connecte(Role.GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_RESOUDRE))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resoudre_doitRenvoyer404_quandIntrouvable() throws Exception {
        when(defaillanceService.resoudre(eq(1L), anyString(), any(Personnel.class)))
                .thenThrow(new ServiceException("RG-D04", "Défaillance 1 introuvable"));

        mockMvc.perform(patch("/api/v1/defaillances/{id}/resoudre/", 1L).with(connecte(Role.GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_RESOUDRE))
                .andExpect(status().isNotFound());
    }

    @Test
    void resoudre_doitRenvoyer400_quandNoteManquante() throws Exception {
        mockMvc.perform(patch("/api/v1/defaillances/{id}/resoudre/", 1L).with(connecte(Role.GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verify(defaillanceService, never()).resoudre(any(), any(), any());
    }

    @Test
    void resoudre_doitRenvoyer403_quandStagiaire() throws Exception {
        mockMvc.perform(patch("/api/v1/defaillances/{id}/resoudre/", 1L).with(connecte(Role.STAGIAIRE))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_RESOUDRE))
                .andExpect(status().isForbidden());
    }
}
