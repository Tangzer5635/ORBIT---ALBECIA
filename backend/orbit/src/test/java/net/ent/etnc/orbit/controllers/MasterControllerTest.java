package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.commons.JsonPatchUtils;
import net.ent.etnc.orbit.dtos.MasterDto;
import net.ent.etnc.orbit.dtos.assemblers.MasterAssembler;
import net.ent.etnc.orbit.models.entities.Master;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.security.WebSecurityConfig;
import net.ent.etnc.orbit.services.MasterService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MasterController.class)
@Import({WebSecurityConfig.class, JsonPatchUtils.class}) // JsonPatchUtils : composant réel, utilisé par patch
class MasterControllerTest extends ControllerTestSupport {

    private static final String JSON_VALIDE = "{\"nom\":\"Master Dev\",\"versionMaster\":\"1.0\"}";

    @MockitoBean
    private MasterService masterService;

    @MockitoBean
    private MasterAssembler masterAssembler;

    private Master master() {
        Master m = new Master();
        m.setId(1L);
        m.setNom("Master Dev");
        m.setVersionMaster("1.0");
        m.setDateCreation(LocalDateTime.of(2026, 10, 12, 9, 0));
        return m;
    }

    private void reponse() {
        when(masterAssembler.toDto(any())).thenReturn(MasterDto.builder()
                .id(1L).nom("Master Dev").versionMaster("1.0").dateCreation(LocalDateTime.of(2026, 10, 12, 9, 0)).build());
    }

    // ---------- getAll ----------

    @Test
    void getAll_doitRenvoyer200_quandFormateur() throws Exception {
        when(masterService.findAll(any())).thenReturn(new PageImpl<>(List.of(master()), PageRequest.of(0, 20), 1));
        reponse();

        mockMvc.perform(get("/api/v1/masters/").with(connecte(Role.FORMATEUR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nom").value("Master Dev"));
    }

    @Test
    void getAll_doitRenvoyer200_quandGestionnaire() throws Exception {
        when(masterService.findAll(any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/masters/").with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isOk());
    }

    @Test
    void getAll_doitRenvoyer403_quandStagiaire() throws Exception {
        mockMvc.perform(get("/api/v1/masters/").with(connecte(Role.STAGIAIRE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_doitRenvoyer401_quandNonAuthentifie() throws Exception {
        mockMvc.perform(get("/api/v1/masters/"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- getById ----------

    @Test
    void getById_doitRenvoyer200_quandTrouve() throws Exception {
        when(masterService.findById(1L)).thenReturn(Optional.of(master()));
        reponse();

        mockMvc.perform(get("/api/v1/masters/{id}/", 1L).with(connecte(Role.FORMATEUR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionMaster").value("1.0"));
    }

    @Test
    void getById_doitRenvoyer404_quandIntrouvable() throws Exception {
        when(masterService.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/masters/{id}/", 1L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isNotFound());
    }

    // ---------- post ----------

    @Test
    void post_doitRenvoyer200_quandValide() throws Exception {
        when(masterAssembler.toEntity(any(MasterDto.class))).thenReturn(master());
        when(masterService.creer(any(), any())).thenReturn(master());
        reponse();

        mockMvc.perform(post("/api/v1/masters/").with(connecte(Role.GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Master Dev"));
    }

    @Test
    void post_doitRenvoyer400_quandNomDejaPris() throws Exception {
        when(masterAssembler.toEntity(any(MasterDto.class))).thenReturn(master());
        when(masterService.creer(any(), any())).thenThrow(new ServiceException("Erreur lors de la sauvegarde"));

        mockMvc.perform(post("/api/v1/masters/").with(connecte(Role.GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDE))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Erreur lors de la sauvegarde"));
    }

    @Test
    void post_doitRenvoyer403_quandFormateur() throws Exception {
        mockMvc.perform(post("/api/v1/masters/").with(connecte(Role.FORMATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDE))
                .andExpect(status().isForbidden());
        verify(masterService, never()).creer(any(), any());
    }

    // ---------- patch ----------

    private static final String PATCH_NOM = "[{\"op\":\"replace\",\"path\":\"/nom\",\"value\":\"Master Web\"}]";

    @Test
    void patch_doitRenvoyer200_etNeModifierQueLesChampsEnvoyes() throws Exception {
        when(masterService.findById(1L)).thenReturn(Optional.of(master()));
        when(masterAssembler.toEntity(any(MasterDto.class))).thenReturn(master());
        when(masterService.modifier(any(), any(), any())).thenReturn(master());
        reponse();

        mockMvc.perform(patch("/api/v1/masters/{id}/", 1L).with(connecte(Role.GESTIONNAIRE))
                        .contentType("application/json-patch+json").content(PATCH_NOM))
                .andExpect(status().isOk());

        ArgumentCaptor<MasterDto> captor = ArgumentCaptor.forClass(MasterDto.class);
        verify(masterAssembler).toEntity(captor.capture());
        assertThat(captor.getValue().getNom()).isEqualTo("Master Web");
        assertThat(captor.getValue().getVersionMaster()).isEqualTo("1.0");
    }

    @Test
    void patch_doitRenvoyer400_quandChampNonModifiable() throws Exception {
        when(masterService.findById(1L)).thenReturn(Optional.of(master()));
        reponse();

        mockMvc.perform(patch("/api/v1/masters/{id}/", 1L).with(connecte(Role.GESTIONNAIRE))
                        .contentType("application/json-patch+json")
                        .content("[{\"op\":\"replace\",\"path\":\"/dateCreation\",\"value\":\"2020-01-01T00:00:00\"}]"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Champ non patchable : dateCreation"));
        verify(masterService, never()).modifier(any(), any(), any());
    }

    @Test
    void patch_doitRenvoyer404_quandIntrouvable() throws Exception {
        when(masterService.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/v1/masters/{id}/", 1L).with(connecte(Role.GESTIONNAIRE))
                        .contentType("application/json-patch+json").content(PATCH_NOM))
                .andExpect(status().isNotFound());
        verify(masterService, never()).modifier(any(), any(), any());
    }

    @Test
    void patch_doitRenvoyer403_quandFormateur() throws Exception {
        mockMvc.perform(patch("/api/v1/masters/{id}/", 1L).with(connecte(Role.FORMATEUR))
                        .contentType("application/json-patch+json").content(PATCH_NOM))
                .andExpect(status().isForbidden());
    }

    // ---------- delete ----------

    @Test
    void delete_doitRenvoyer204_quandTrouve() throws Exception {
        when(masterService.existsById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/masters/{id}/", 1L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isNoContent());
        verify(masterService).deleteById(1L);
    }

    @Test
    void delete_doitRenvoyer404_quandIntrouvable() throws Exception {
        when(masterService.existsById(1L)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/masters/{id}/", 1L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_doitRenvoyer403_quandFormateur() throws Exception {
        mockMvc.perform(delete("/api/v1/masters/{id}/", 1L).with(connecte(Role.FORMATEUR)))
                .andExpect(status().isForbidden());
    }
}