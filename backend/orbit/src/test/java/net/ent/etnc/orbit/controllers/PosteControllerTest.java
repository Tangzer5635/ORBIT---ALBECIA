package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.commons.JsonPatchUtils;
import net.ent.etnc.orbit.dtos.assemblers.PosteAssembler;
import net.ent.etnc.orbit.dtos.requestDTO.PosteRequestDto;
import net.ent.etnc.orbit.dtos.requestDTO.PosteUpdateRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.MonPosteResponseDto;
import net.ent.etnc.orbit.dtos.responseDTO.PosteResponseDto;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.repositories.projections.MonPosteLigne;
import net.ent.etnc.orbit.security.WebSecurityConfig;
import net.ent.etnc.orbit.services.PosteService;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PosteController.class)
@Import({WebSecurityConfig.class, JsonPatchUtils.class}) // JsonPatchUtils : composant réel, utilisé par patch
class PosteControllerTest extends ControllerTestSupport {

    @MockitoBean
    private PosteService posteService;

    @MockitoBean
    private SalleService salleService;

    @MockitoBean
    private PosteAssembler posteAssembler;

    private Poste poste() {
        Poste p = new Poste();
        p.setId(1L);
        p.setNumPoste("P101-01");
        return p;
    }

    private void reponse() {
        when(posteAssembler.toDto(any())).thenReturn(PosteResponseDto.builder()
                .id(1L).numPoste("P101-01").occupantId(6L).materielsId(List.of(1L, 2L)).build());
    }

    // ---------- getAll ----------

    @Test
    void getAll_doitRenvoyer200_quandFormateur() throws Exception {
        when(posteService.findAllAvecDetails(any())).thenReturn(new PageImpl<>(List.of(poste()), PageRequest.of(0, 20), 1));
        reponse();

        mockMvc.perform(get("/api/v1/postes/").with(connecte(Role.FORMATEUR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].numPoste").value("P101-01"));
    }

    @Test
    void getAll_doitRenvoyer200_quandGestionnaire() throws Exception {
        when(posteService.findAllAvecDetails(any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/postes/").with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isOk());
    }

    @Test
    void getAll_doitRenvoyer403_quandStagiaire() throws Exception {
        mockMvc.perform(get("/api/v1/postes/").with(connecte(Role.STAGIAIRE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_doitRenvoyer401_quandNonAuthentifie() throws Exception {
        mockMvc.perform(get("/api/v1/postes/"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- getById ----------

    @Test
    void getById_doitRenvoyer200_quandTrouve() throws Exception {
        when(posteService.findByIdAvecDetails(1L)).thenReturn(Optional.of(poste()));
        reponse();

        mockMvc.perform(get("/api/v1/postes/{id}/", 1L).with(connecte(Role.FORMATEUR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occupantId").value(6))
                .andExpect(jsonPath("$.materielsId.length()").value(2));
    }

    @Test
    void getById_doitRenvoyer404_quandIntrouvable() throws Exception {
        when(posteService.findByIdAvecDetails(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/postes/{id}/", 1L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isNotFound());
    }

    // ---------- getMyPoste ----------

    @Test
    void getMyPoste_doitRenvoyer200_quandLeStagiaireAUnPoste() throws Exception {
        when(posteService.lignesMonPoste(1L)).thenReturn(List.of(
                new MonPosteLigne(1L, "P101-01", 2L, "101", "1", 3L, "B201", null, false)));
        when(posteAssembler.toDtoMonPoste(any())).thenReturn(MonPosteResponseDto.builder()
                .id(1L).numPoste("P101-01").numSalle("101").numBatiment("B201").materiels(List.of()).build());

        mockMvc.perform(get("/api/v1/postes/me/").with(connecte(Role.STAGIAIRE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numPoste").value("P101-01"))
                .andExpect(jsonPath("$.numBatiment").value("B201"));
    }

    @Test
    void getMyPoste_doitRenvoyer404_quandAucunPoste() throws Exception {
        when(posteService.lignesMonPoste(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/postes/me/").with(connecte(Role.STAGIAIRE)))
                .andExpect(status().isNotFound());
        verify(posteAssembler, never()).toDtoMonPoste(any());
    }

    // ---------- post ----------

    @Test
    void post_doitRenvoyer201_quandValide() throws Exception {
        when(posteAssembler.toEntity(any(PosteRequestDto.class))).thenReturn(poste());
        when(salleService.creerPoste(eq(1L), any(Poste.class))).thenReturn(poste());
        reponse();

        mockMvc.perform(post("/api/v1/postes/").with(connecte(Role.GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numPoste\":\"P101-01\",\"idSalle\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numPoste").value("P101-01"));
    }

    @Test
    void post_doitRenvoyer400_quandSalleManquante() throws Exception {
        mockMvc.perform(post("/api/v1/postes/").with(connecte(Role.GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numPoste\":\"P101-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.idSalle").exists());
        verify(salleService, never()).creerPoste(any(), any());
    }

    @Test
    void post_doitRenvoyer400_quandNumeroManquant() throws Exception {
        mockMvc.perform(post("/api/v1/postes/").with(connecte(Role.GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idSalle\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.numPoste").exists());
    }

    @Test
    void post_doitRenvoyer400_quandSalleDeStockage() throws Exception {
        when(posteAssembler.toEntity(any(PosteRequestDto.class))).thenReturn(poste());
        when(salleService.creerPoste(eq(3L), any(Poste.class)))
                .thenThrow(new ServiceException("On ne crée pas de poste dans une salle de stockage"));

        mockMvc.perform(post("/api/v1/postes/").with(connecte(Role.GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numPoste\":\"R199-01\",\"idSalle\":3}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_doitRenvoyer400_quandSalleIntrouvable() throws Exception {
        when(posteAssembler.toEntity(any(PosteRequestDto.class))).thenReturn(poste());
        when(salleService.creerPoste(eq(999L), any(Poste.class))).thenThrow(new ServiceException("Salle 999 introuvable"));

        mockMvc.perform(post("/api/v1/postes/").with(connecte(Role.GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numPoste\":\"X-01\",\"idSalle\":999}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_doitRenvoyer400_quandJsonMalForme() throws Exception {
        mockMvc.perform(post("/api/v1/postes/").with(connecte(Role.GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numPoste\":\"P-01\",\"idSalle\":}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_doitRenvoyer403_quandFormateur() throws Exception {
        mockMvc.perform(post("/api/v1/postes/").with(connecte(Role.FORMATEUR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numPoste\":\"P101-01\",\"idSalle\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void methodeNonAutorisee_doitRenvoyer405() throws Exception {
        mockMvc.perform(patch("/api/v1/postes/").with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isMethodNotAllowed());
    }

    // ---------- patch ----------

    private static final String PATCH_NUMERO = "[{\"op\":\"replace\",\"path\":\"/numPoste\",\"value\":\"P101-02\"}]";

    private void etatCourant() {
        when(posteService.findById(1L)).thenReturn(Optional.of(poste()));
        when(posteAssembler.toUpdateDto(any())).thenReturn(PosteUpdateRequestDto.builder().numPoste("P101-01").build());
    }

    @Test
    void patch_doitRenvoyer200_etTransmettreLIdEtLeNouveauNumero() throws Exception {
        etatCourant();
        when(posteAssembler.toEntity(any(PosteUpdateRequestDto.class))).thenReturn(new Poste());
        when(posteService.update(any())).thenReturn(poste());
        reponse();

        mockMvc.perform(patch("/api/v1/postes/{id}/", 1L).with(connecte(Role.GESTIONNAIRE))
                        .contentType("application/json-patch+json").content(PATCH_NUMERO))
                .andExpect(status().isOk());

        ArgumentCaptor<PosteUpdateRequestDto> dto = ArgumentCaptor.forClass(PosteUpdateRequestDto.class);
        verify(posteAssembler).toEntity(dto.capture());
        assertThat(dto.getValue().getNumPoste()).isEqualTo("P101-02");
        ArgumentCaptor<Poste> captor = ArgumentCaptor.forClass(Poste.class);
        verify(posteService).update(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1L);
    }

    @Test
    void patch_doitRenvoyer400_quandNumeroDejaPris() throws Exception {
        etatCourant();
        when(posteAssembler.toEntity(any(PosteUpdateRequestDto.class))).thenReturn(new Poste());
        when(posteService.update(any())).thenThrow(new ServiceException("Le poste P101-02 existe déjà"));

        mockMvc.perform(patch("/api/v1/postes/{id}/", 1L).with(connecte(Role.GESTIONNAIRE))
                        .contentType("application/json-patch+json").content(PATCH_NUMERO))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patch_doitRenvoyer404_quandIntrouvable() throws Exception {
        when(posteService.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/v1/postes/{id}/", 1L).with(connecte(Role.GESTIONNAIRE))
                        .contentType("application/json-patch+json").content(PATCH_NUMERO))
                .andExpect(status().isNotFound());
        verify(posteService, never()).update(any());
    }

    @Test
    void patch_doitRenvoyer403_quandFormateur() throws Exception {
        mockMvc.perform(patch("/api/v1/postes/{id}/", 1L).with(connecte(Role.FORMATEUR))
                        .contentType("application/json-patch+json").content(PATCH_NUMERO))
                .andExpect(status().isForbidden());
    }

    // ---------- delete ----------

    @Test
    void delete_doitRenvoyer204_quandTrouve() throws Exception {
        mockMvc.perform(delete("/api/v1/postes/{id}/", 1L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isNoContent());
        verify(salleService).supprimerPoste(1L);
    }

    @Test
    void delete_doitRenvoyer400_quandIntrouvable() throws Exception {
        doThrow(new ServiceException("Poste 1 introuvable")).when(salleService).supprimerPoste(1L);

        mockMvc.perform(delete("/api/v1/postes/{id}/", 1L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_doitRenvoyer403_quandFormateur() throws Exception {
        mockMvc.perform(delete("/api/v1/postes/{id}/", 1L).with(connecte(Role.FORMATEUR)))
                .andExpect(status().isForbidden());
        verify(salleService, never()).supprimerPoste(any());
    }
}
