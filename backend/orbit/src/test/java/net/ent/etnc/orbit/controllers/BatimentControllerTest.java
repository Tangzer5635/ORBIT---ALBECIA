package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.dtos.assemblers.BatimentAssembler;
import net.ent.etnc.orbit.dtos.assemblers.SalleAssembler;
import net.ent.etnc.orbit.dtos.requestDTO.BatimentRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.AnneauDto;
import net.ent.etnc.orbit.dtos.responseDTO.BatimentCarteDto;
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

    private Batiment batiment() {
        Batiment b = new Batiment();
        b.setId(1L);
        b.setNumBatiment("B201");
        return b;
    }

    private BatimentResponseDto dto() {
        return BatimentResponseDto.builder().id(1L).numBatiment("B201").sallesId(List.of()).build();
    }

    // ---------- getAll ----------

    @Test
    void getAll_doitRenvoyer200_quandAdmin() throws Exception {
        when(batimentService.findCartes(any())).thenReturn(new PageImpl<>(List.of(BatimentCarteDto.builder()
                .id(1L).numBatiment("B201").nbSalles(3).stockages(List.of())
                .anneau(new AnneauDto(2, 5, 1, 0)).build()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/batiments/").with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].numBatiment").value("B201"));
    }

    @Test
    void getAll_doitRenvoyer403_quandStagiaire() throws Exception {
        mockMvc.perform(get("/api/v1/batiments/").with(connecte(Role.STAGIAIRE)))
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
    void post_doitRenvoyer201_quandValide() throws Exception {
        when(batimentAssembler.toEntity(any(BatimentRequestDto.class))).thenReturn(batiment());
        when(batimentService.create(any(Batiment.class), any(), any()));
        when(batimentAssembler.toDto(any())).thenReturn(dto());

        mockMvc.perform(post("/api/v1/batiments/").with(connecte(Role.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numBatiment\":\"B201\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numBatiment").value("B201"));
    }

    @Test
    void post_doitRenvoyer400_quandRegleDeGestionViolee() throws Exception {
        when(batimentAssembler.toEntity(any(BatimentRequestDto.class))).thenReturn(batiment());
        when(batimentService.create(any(Batiment.class), any(), any()));

        mockMvc.perform(post("/api/v1/batiments/").with(connecte(Role.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numBatiment\":\"B201\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Erreur lors de la sauvegarde"));
    }

    @Test
    void post_doitRenvoyer403_quandGestionnaire() throws Exception {
        mockMvc.perform(post("/api/v1/batiments/").with(connecte(Role.GESTIONNAIRE))
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
        verify(batimentService).supprimer(1L, null);
    }

    @Test
    void delete_doitTransmettreLaSalleDeStockageCible() throws Exception {
        when(batimentService.existsById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/batiments/{id}/", 1L).param("stockageCible", "7")
                        .with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isNoContent());
        verify(batimentService).supprimer(1L, 7L);
    }

    @Test
    void delete_doitRenvoyer404_quandIntrouvable() throws Exception {
        when(batimentService.existsById(1L)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/batiments/{id}/", 1L).with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isNotFound());
        verify(batimentService, never()).supprimer(any(), any());
    }

    @Test
    void delete_doitRenvoyer403_quandGestionnaire() throws Exception {
        mockMvc.perform(delete("/api/v1/batiments/{id}/", 1L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isForbidden());
    }
}