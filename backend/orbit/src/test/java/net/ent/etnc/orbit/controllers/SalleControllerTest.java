package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.dtos.assemblers.SalleAssembler;
import net.ent.etnc.orbit.dtos.requestDTO.SalleRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.AnneauDto;
import net.ent.etnc.orbit.dtos.responseDTO.SalleCarteDto;
import net.ent.etnc.orbit.dtos.responseDTO.SalleEquipeeResponseDto;
import net.ent.etnc.orbit.dtos.responseDTO.SalleResponseDto;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.security.WebSecurityConfig;
import net.ent.etnc.orbit.services.BatimentService;
import net.ent.etnc.orbit.services.SalleService;
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
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SalleController.class)
@Import(WebSecurityConfig.class)
class SalleControllerTest extends ControllerTestSupport {

    private static final String JSON_VALIDE =
            "{\"numSalle\":\"101\",\"etage\":\"1\",\"type\":\"SalleDeCours\",\"idBatiment\":1}";

    @MockitoBean
    private SalleService salleService;

    @MockitoBean
    private BatimentService batimentService;

    @MockitoBean
    private SalleAssembler salleAssembler;

    private Salle salle() {
        Salle s = new Salle();
        s.setId(1L);
        s.setNumSalle("101");
        s.setEtage("1");
        s.setType(TypeSalle.SalleDeCours);
        return s;
    }

    private void reponse() {
        when(salleAssembler.toDto(any())).thenReturn(SalleResponseDto.builder()
                .id(1L).numSalle("101").etage("1").type(TypeSalle.SalleDeCours).materielsId(List.of()).build());
    }

    // ---------- getAll ----------

    @Test
    void getAll_doitRenvoyer200_quandGestionnaire() throws Exception {
        when(salleService.findAllAvecDetails(any())).thenReturn(new PageImpl<>(List.of(salle()), PageRequest.of(0, 20), 1));
        reponse();

        mockMvc.perform(get("/api/v1/salles/").with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].numSalle").value("101"));
    }

    @Test
    void getAll_doitRenvoyer403_quandStagiaire() throws Exception {
        mockMvc.perform(get("/api/v1/salles/").with(connecte(Role.STAGIAIRE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_doitRenvoyer401_quandNonAuthentifie() throws Exception {
        mockMvc.perform(get("/api/v1/salles/"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- getById ----------

    @Test
    void getById_doitRenvoyer200_quandTrouve() throws Exception {
        when(salleService.findByIdAvecDetails(1L)).thenReturn(Optional.of(salle()));
        reponse();

        mockMvc.perform(get("/api/v1/salles/{id}/", 1L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numSalle").value("101"));
    }

    @Test
    void getById_doitRenvoyer404_quandIntrouvable() throws Exception {
        when(salleService.findByIdAvecDetails(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/salles/{id}/", 1L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isNotFound());
    }

    // ---------- post ----------

    @Test
    void post_doitRenvoyer201_quandValide() throws Exception {
        when(salleAssembler.toEntity(any(SalleRequestDto.class))).thenReturn(salle());
        when(salleService.create(eq(1L), any(Salle.class))).thenReturn(salle());
        reponse();

        mockMvc.perform(post("/api/v1/salles").with(connecte(Role.GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numSalle").value("101"));
    }

    @Test
    void post_doitRenvoyer400_quandNumeroDejaPrisDansLeBatiment() throws Exception {
        when(salleAssembler.toEntity(any(SalleRequestDto.class))).thenReturn(salle());
        when(salleService.create(eq(1L), any(Salle.class)))
                .thenThrow(new ServiceException("Une salle 101 existe déjà dans ce bâtiment"));

        mockMvc.perform(post("/api/v1/salles").with(connecte(Role.GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDE))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Une salle 101 existe déjà dans ce bâtiment"));
    }

    @Test
    void post_doitRenvoyer400_quandBatimentIntrouvable() throws Exception {
        when(salleAssembler.toEntity(any(SalleRequestDto.class))).thenReturn(salle());
        when(salleService.create(eq(1L), any(Salle.class))).thenThrow(new ServiceException("Bâtiment 1 introuvable"));

        mockMvc.perform(post("/api/v1/salles").with(connecte(Role.GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDE))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_doitRenvoyer400_quandChampsManquants() throws Exception {
        mockMvc.perform(post("/api/v1/salles").with(connecte(Role.GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"numSalle\":\"101\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.idBatiment").exists());
        verify(salleService, never()).create(any(), any());
    }

    // ---------- postEquipee ----------

    private static final String JSON_EQUIPEE_VALIDE =
            "{\"idBatiment\":1,\"numSalle\":\"101\",\"etage\":\"1\",\"nbPostes\":%d,\"ecransParPoste\":2,\"nbVideoProjecteurs\":1,\"nbTableaux\":1}";

    @Test
    void postEquipee_doitRenvoyer201_quandValide() throws Exception {
        when(salleService.creerSalleEquipee(any())).thenReturn(new SalleEquipeeResponseDto(1L, Map.of()));

        mockMvc.perform(post("/api/v1/salles/equipee/").with(connecte(Role.GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_EQUIPEE_VALIDE.formatted(10)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idSalle").value(1));
    }

    @Test
    void postEquipee_doitRenvoyer400_quandTropDePostes() throws Exception {
        mockMvc.perform(post("/api/v1/salles/equipee/").with(connecte(Role.GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_EQUIPEE_VALIDE.formatted(51)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.nbPostes").exists());
        verify(salleService, never()).creerSalleEquipee(any());
    }

    @Test
    void post_doitRenvoyer403_quandFormateur() throws Exception {
        mockMvc.perform(post("/api/v1/salles").with(connecte(Role.FORMATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDE))
                .andExpect(status().isForbidden());
    }

    // ---------- delete ----------

    @Test
    void delete_doitRenvoyer204_quandTrouvee() throws Exception {
        when(salleService.existsById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/salles/{id}/", 1L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isNoContent());
        verify(salleService).deleteById(1L);
    }

    @Test
    void delete_doitRenvoyer404_quandIntrouvable() throws Exception {
        when(salleService.existsById(1L)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/salles/{id}/", 1L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_doitRenvoyer400_quandReserveNonVide() throws Exception {
        when(salleService.existsById(3L)).thenReturn(true);
        doThrow(new ServiceException("Vide la salle de stockage avant de la supprimer")).when(salleService).deleteById(3L);

        mockMvc.perform(delete("/api/v1/salles/{id}/", 3L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_doitRenvoyer403_quandFormateur() throws Exception {
        mockMvc.perform(delete("/api/v1/salles/{id}/", 1L).with(connecte(Role.FORMATEUR)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getByBatiment_doitRenvoyerLesCartes_quandBatimentTrouve() throws Exception {
        when(batimentService.existsById(1L)).thenReturn(true);
        when(salleService.findCartesDuBatiment(1L)).thenReturn(List.of(SalleCarteDto.builder()
                .id(5L).numSalle("101").etage("1").type(TypeSalle.SalleDeCours).nbPostes(12)
                .anneau(new AnneauDto(0, 10, 1, 1)).build()));

        mockMvc.perform(get("/api/v1/salles/").param("batiment", "1").with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nbPostes").value(12))
                .andExpect(jsonPath("$[0].anneau.bonEtat").value(10));
    }

    @Test
    void getByBatiment_doitRenvoyer403_quandFormateur() throws Exception {
        mockMvc.perform(get("/api/v1/salles/").param("batiment", "1").with(connecte(Role.FORMATEUR)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_doitRenvoyer403_quandFormateur() throws Exception {
        mockMvc.perform(get("/api/v1/salles/").with(connecte(Role.FORMATEUR)))
                .andExpect(status().isForbidden());
    }

    // ---------- mes salles ----------

    @Test
    void getMesSalles_doitRenvoyerLesSallesDuFormateurConnecte() throws Exception {
        when(salleService.findCartesDuFormateur(1L)).thenReturn(List.of(SalleCarteDto.builder()
                .id(5L).numSalle("101").numBatiment("201").etage("1").type(TypeSalle.SalleDeCours).nbPostes(12)
                .anneau(new AnneauDto(0, 10, 1, 1)).build()));

        mockMvc.perform(get("/api/v1/salles/mes-salles/").with(connecte(Role.FORMATEUR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numSalle").value("101"))
                .andExpect(jsonPath("$[0].numBatiment").value("201"));
    }

    @Test
    void getMesSalles_doitRenvoyer403_quandGestionnaire() throws Exception {
        mockMvc.perform(get("/api/v1/salles/mes-salles/").with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMesSalles_doitRenvoyer401_quandNonAuthentifie() throws Exception {
        mockMvc.perform(get("/api/v1/salles/mes-salles/"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- accès d'un formateur à une salle ----------

    @Test
    void getById_doitRenvoyer403_quandSalleNonAffecteeAuFormateur() throws Exception {
        doThrow(new AccessDeniedException("non affectée")).when(salleService).verifierAcces(eq(1L), any());

        mockMvc.perform(get("/api/v1/salles/{id}/", 1L).with(connecte(Role.FORMATEUR)))
                .andExpect(status().isForbidden());
        verify(salleService, never()).findByIdAvecDetails(any());
    }
}
