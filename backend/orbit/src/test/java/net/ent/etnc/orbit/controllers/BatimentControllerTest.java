package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.dtos.assemblers.BatimentAssembler;
import net.ent.etnc.orbit.dtos.assemblers.SalleAssembler;
import net.ent.etnc.orbit.dtos.requestDTO.BatimentRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.BatimentResponseDto;
import net.ent.etnc.orbit.dtos.responseDTO.SalleResponseDto;
import net.ent.etnc.orbit.models.entities.Batiment;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.security.WebSecurityConfig;
import net.ent.etnc.orbit.services.BatimentService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BatimentController.class)
@Import(WebSecurityConfig.class)
class BatimentControllerTest extends ControllerTestSupport {

    @MockitoBean
    private BatimentService batimentService;

    @MockitoBean
    private BatimentAssembler batimentAssembler;

    @MockitoBean
    private SalleAssembler salleAssembler;

    private Batiment batiment() {
        Batiment b = new Batiment();
        b.setId(1L);
        b.setNumBatiment("B201");
        return b;
    }

    private BatimentResponseDto dto() {
        return BatimentResponseDto.builder().id(1L).numBatiment("B201").sallesId(List.of()).build();
    }

    // ---------- getSalles ----------

    @Test
    void getSalles_doitRenvoyer200_quandBatimentTrouve() throws Exception {
        when(batimentService.findById(1L)).thenReturn(Optional.of(batiment()));
        when(salleAssembler.toDtos(any())).thenReturn(List.of(
                SalleResponseDto.builder().id(5L).numSalle("101").etage("1")
                        .type(TypeSalle.SalleDeCours).materielsId(List.of()).build()));

        mockMvc.perform(get("/api/v1/batiments/{id}/salles/", 1L).with(connecte(Role.FORMATEUR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numSalle").value("101"));
    }

    @Test
    void getSalles_doitRenvoyer404_quandBatimentIntrouvable() throws Exception {
        when(batimentService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/batiments/{id}/salles/", 99L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSalles_doitRenvoyer403_quandStagiaire() throws Exception {
        mockMvc.perform(get("/api/v1/batiments/{id}/salles/", 1L).with(connecte(Role.STAGIAIRE)))
                .andExpect(status().isForbidden());
    }

    // ---------- getAll ----------

    @Test
    void getAll_doitRenvoyer200_quandAdmin() throws Exception {
        when(batimentService.findAll(any())).thenReturn(new PageImpl<>(List.of(batiment()), PageRequest.of(0, 20), 1));
        when(batimentAssembler.toDto(any())).thenReturn(dto());

        mockMvc.perform(get("/api/v1/batiments/").with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].numBatiment").value("B201"));
    }

    @Test
    void getAll_doitRenvoyer403_quandFormateur() throws Exception {
        mockMvc.perform(get("/api/v1/batiments/").with(connecte(Role.FORMATEUR)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_doitRenvoyer401_quandNonAuthentifie() throws Exception {
        mockMvc.perform(get("/api/v1/batiments/"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- getById ----------

    @Test
    void getById_doitRenvoyer200_quandTrouve() throws Exception {
        when(batimentService.findById(1L)).thenReturn(Optional.of(batiment()));
        when(batimentAssembler.toDto(any())).thenReturn(dto());

        mockMvc.perform(get("/api/v1/batiments/{id}/", 1L).with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getById_doitRenvoyer404_quandIntrouvable() throws Exception {
        when(batimentService.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/batiments/{id}/", 1L).with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getById_doitRenvoyer400_quandIdNonNumerique() throws Exception {
        mockMvc.perform(get("/api/v1/batiments/{id}/", "abc").with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isBadRequest());
    }

    // ---------- post ----------

    @Test
    void post_doitRenvoyer200_quandValide() throws Exception {
        when(batimentAssembler.toEntity(any(BatimentRequestDto.class))).thenReturn(batiment());
        when(batimentService.create(any(Batiment.class))).thenReturn(batiment());
        when(batimentAssembler.toDto(any())).thenReturn(dto());

        mockMvc.perform(post("/api/v1/batiments").with(connecte(Role.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numBatiment\":\"B201\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numBatiment").value("B201"));
    }

    @Test
    void post_doitRenvoyer400_quandRegleDeGestionViolee() throws Exception {
        when(batimentAssembler.toEntity(any(BatimentRequestDto.class))).thenReturn(batiment());
        when(batimentService.create(any(Batiment.class))).thenThrow(new ServiceException("Erreur lors de la sauvegarde"));

        mockMvc.perform(post("/api/v1/batiments").with(connecte(Role.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numBatiment\":\"B201\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Erreur lors de la sauvegarde"));
    }

    @Test
    void post_doitRenvoyer403_quandGestionnaire() throws Exception {
        mockMvc.perform(post("/api/v1/batiments").with(connecte(Role.GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numBatiment\":\"B201\"}"))
                .andExpect(status().isForbidden());
    }

    // ---------- delete ----------

    @Test
    void delete_doitRenvoyer204_quandTrouve() throws Exception {
        when(batimentService.existsById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/batiments/{id}/", 1L).with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isNoContent());
        verify(batimentService).deleteById(1L);
    }

    @Test
    void delete_doitRenvoyer404_quandIntrouvable() throws Exception {
        when(batimentService.existsById(1L)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/batiments/{id}/", 1L).with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isNotFound());
        verify(batimentService, never()).deleteById(any());
    }

    @Test
    void delete_doitRenvoyer403_quandGestionnaire() throws Exception {
        mockMvc.perform(delete("/api/v1/batiments/{id}/", 1L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isForbidden());
    }
}