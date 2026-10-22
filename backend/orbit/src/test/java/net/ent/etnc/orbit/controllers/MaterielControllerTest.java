package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.dtos.assemblers.MaterielAssembler;
import net.ent.etnc.orbit.dtos.requestDTO.MaterielRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.MaterielResponseDto;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.UC;
import net.ent.etnc.orbit.models.enums.EtatMateriel;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.commons.JsonPatchUtils;
import net.ent.etnc.orbit.commons.RsqlFilterUtils;
import net.ent.etnc.orbit.security.WebSecurityConfig;
import net.ent.etnc.orbit.services.MaterielService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
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

@WebMvcTest(MaterielController.class)
@Import({WebSecurityConfig.class, RsqlFilterUtils.class, JsonPatchUtils.class}) // composants réels, utilisés par getAll et patch
class MaterielControllerTest extends ControllerTestSupport {

    private static final String JSON_VALIDE = """
            {"numSerie":"UC-0100","modele":"Dell OptiPlex 7020","dateAcquisition":"2026-09-01",
             "dateFinGarantie":"2029-09-01","type":"UC","idStockage":3}""";

    @MockitoBean
    private MaterielService materielService;

    @MockitoBean
    private MaterielAssembler materielAssembler;

    private Materiel materiel() {
        Materiel m = new UC();
        m.setId(1L);
        m.setNumSerie("UC-0100");
        m.setModele("Dell OptiPlex 7020");
        m.setDateAcquisition(LocalDate.of(2026, 9, 1));
        m.setDateFinGarantie(LocalDate.of(2029, 9, 1));
        return m;
    }

    private void reponse(EtatMateriel etat) {
        when(materielAssembler.toDto(any(), any(), any())).thenReturn(
                MaterielResponseDto.builder().id(1L).numSerie("UC-0100").etat(etat).build());
    }

    // ---------- getAll ----------

    @Test
    void getAll_doitRenvoyer200_quandGestionnaire() throws Exception {
        when(materielService.findAll(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(materiel()), PageRequest.of(0, 20), 1));
        reponse(EtatMateriel.DISPONIBLE);

        mockMvc.perform(get("/api/v1/materiels/").with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].numSerie").value("UC-0100"));
    }

    @Test
    void getAll_doitRenvoyer403_quandFormateur() throws Exception {
        mockMvc.perform(get("/api/v1/materiels/").with(connecte(Role.FORMATEUR)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_doitRenvoyer401_quandNonAuthentifie() throws Exception {
        mockMvc.perform(get("/api/v1/materiels/"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- getById ----------

    @Test
    void getById_doitRenvoyer200_avecEtatCalcule() throws Exception {
        when(materielService.findPourUtilisateur(eq(1L), any())).thenReturn(Optional.of(materiel()));
        when(materielService.etatDe(any())).thenReturn(EtatMateriel.DEFECTUEUX);
        reponse(EtatMateriel.DEFECTUEUX);

        mockMvc.perform(get("/api/v1/materiels/{id}/", 1L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etat").value("DEFECTUEUX"));
        verify(materielAssembler).toDto(any(), eq(EtatMateriel.DEFECTUEUX), any());
    }

    @Test
    void getById_doitRenvoyer404_quandIntrouvable() throws Exception {
        when(materielService.findPourUtilisateur(eq(1L), any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/materiels/{id}/", 1L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getById_doitRenvoyer400_quandIdNonNumerique() throws Exception {
        mockMvc.perform(get("/api/v1/materiels/{id}/", "abc").with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isBadRequest());
    }

    // ---------- post ----------

    @Test
    void post_doitRenvoyer200_quandValide() throws Exception {
        when(materielAssembler.toEntity(any(MaterielRequestDto.class))).thenReturn(materiel());
        when(materielService.creerEnStock(any(), eq(3L))).thenReturn(materiel());
        reponse(EtatMateriel.DISPONIBLE);

        mockMvc.perform(post("/api/v1/materiels/").with(connecte(Role.GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etat").value("DISPONIBLE"));
    }

    @Test
    void post_doitRenvoyer400_quandChampsManquants() throws Exception {
        mockMvc.perform(post("/api/v1/materiels/").with(connecte(Role.GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"numSerie\":\"UC-0100\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.modele").exists())
                .andExpect(jsonPath("$.type").exists());
        verify(materielService, never()).creerEnStock(any(), any());
    }

    @Test
    void post_doitRenvoyer400_quandTypeInconnu() throws Exception {
        mockMvc.perform(post("/api/v1/materiels/").with(connecte(Role.GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VALIDE.replace("\"UC\"", "\"GRILLE_PAIN\"")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_doitRenvoyer400_quandNumeroDeSerieDejaPris() throws Exception {
        when(materielAssembler.toEntity(any(MaterielRequestDto.class))).thenReturn(materiel());
        when(materielService.creerEnStock(any(), eq(3L))).thenThrow(new ServiceException("Le numéro de série UC-0100 existe déjà"));

        mockMvc.perform(post("/api/v1/materiels/").with(connecte(Role.GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDE))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Le numéro de série UC-0100 existe déjà"));
    }

    @Test
    void post_doitRenvoyer400_quandReserveIntrouvable() throws Exception {
        when(materielAssembler.toEntity(any(MaterielRequestDto.class))).thenReturn(materiel());
        when(materielService.creerEnStock(any(), eq(3L))).thenThrow(new ServiceException("Salle 3 introuvable"));

        mockMvc.perform(post("/api/v1/materiels/").with(connecte(Role.GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDE))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_doitRenvoyer403_quandFormateur() throws Exception {
        mockMvc.perform(post("/api/v1/materiels/").with(connecte(Role.FORMATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDE))
                .andExpect(status().isForbidden());
    }

    // ---------- patch ----------

    private static final String PATCH_MODELE = "[{\"op\":\"replace\",\"path\":\"/modele\",\"value\":\"Dell OptiPlex 7090\"}]";

    /** État courant renvoyé par l'assembler (mocké) : base sur laquelle le patch s'applique. */
    private void etatCourant() {
        when(materielService.findById(1L)).thenReturn(Optional.of(materiel()));
        when(materielAssembler.toRequestDto(any())).thenReturn(MaterielRequestDto.builder()
                .numSerie("UC-0100").modele("Dell OptiPlex 7020").type("UC")
                .dateAcquisition(LocalDate.of(2026, 9, 1)).dateFinGarantie(LocalDate.of(2029, 9, 1)).build());
    }

    @Test
    void patch_doitRenvoyer200_etNeModifierQueLeModele() throws Exception {
        etatCourant();
        when(materielAssembler.toEntity(any(MaterielRequestDto.class))).thenReturn(materiel());
        when(materielService.update(any())).thenReturn(materiel());
        reponse(EtatMateriel.NORMAL);

        mockMvc.perform(patch("/api/v1/materiels/{id}/", 1L).with(connecte(Role.GESTIONNAIRE))
                        .contentType("application/json-patch+json").content(PATCH_MODELE))
                .andExpect(status().isOk());

        ArgumentCaptor<MaterielRequestDto> captor = ArgumentCaptor.forClass(MaterielRequestDto.class);
        verify(materielAssembler).toEntity(captor.capture());
        assertThat(captor.getValue().getModele()).isEqualTo("Dell OptiPlex 7090");
        assertThat(captor.getValue().getNumSerie()).isEqualTo("UC-0100");
    }

    @Test
    void patch_doitRenvoyer400_quandNumeroDeSerie() throws Exception {
        etatCourant();

        mockMvc.perform(patch("/api/v1/materiels/{id}/", 1L).with(connecte(Role.GESTIONNAIRE))
                        .contentType("application/json-patch+json")
                        .content("[{\"op\":\"replace\",\"path\":\"/numSerie\",\"value\":\"UC-9999\"}]"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Champ non patchable : numSerie"));
        verify(materielService, never()).update(any());
    }

    @Test
    void patch_doitRenvoyer400_quandValeurInvalide() throws Exception {
        etatCourant();

        mockMvc.perform(patch("/api/v1/materiels/{id}/", 1L).with(connecte(Role.GESTIONNAIRE))
                        .contentType("application/json-patch+json")
                        .content("[{\"op\":\"replace\",\"path\":\"/modele\",\"value\":\"\"}]"))
                .andExpect(status().isBadRequest());
        verify(materielService, never()).update(any());
    }

    @Test
    void patch_doitRenvoyer400_quandOperationAutreQueReplace() throws Exception {
        etatCourant();

        mockMvc.perform(patch("/api/v1/materiels/{id}/", 1L).with(connecte(Role.GESTIONNAIRE))
                        .contentType("application/json-patch+json")
                        .content("[{\"op\":\"remove\",\"path\":\"/modele\"}]"))
                .andExpect(status().isBadRequest());
        verify(materielService, never()).update(any());
    }

    @Test
    void patch_doitRenvoyer400_quandMaterielArchive() throws Exception {
        etatCourant();
        when(materielAssembler.toEntity(any(MaterielRequestDto.class))).thenReturn(materiel());
        when(materielService.update(any())).thenThrow(new ServiceException("Un matériel archivé ne peut pas être modifié"));

        mockMvc.perform(patch("/api/v1/materiels/{id}/", 1L).with(connecte(Role.GESTIONNAIRE))
                        .contentType("application/json-patch+json").content(PATCH_MODELE))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patch_doitRenvoyer404_quandIntrouvable() throws Exception {
        when(materielService.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/v1/materiels/{id}/", 1L).with(connecte(Role.GESTIONNAIRE))
                        .contentType("application/json-patch+json").content(PATCH_MODELE))
                .andExpect(status().isNotFound());
        verify(materielService, never()).update(any());
    }

    // ---------- delete ----------

    @Test
    void delete_doitRenvoyer204_quandTrouve() throws Exception {
        when(materielService.existsById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/materiels/{id}/", 1L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isNoContent());
        verify(materielService).deleteById(1L);
    }

    @Test
    void delete_doitRenvoyer404_quandIntrouvable() throws Exception {
        when(materielService.existsById(1L)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/materiels/{id}/", 1L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_doitRenvoyer400_quandHistoriqueDeDefaillances() throws Exception {
        when(materielService.existsById(1L)).thenReturn(true);
        doThrow(new ServiceException("Ce matériel a un historique de défaillances : archive-le au lieu de le supprimer"))
                .when(materielService).deleteById(1L);

        mockMvc.perform(delete("/api/v1/materiels/{id}/", 1L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_doitRenvoyer403_quandFormateur() throws Exception {
        mockMvc.perform(delete("/api/v1/materiels/{id}/", 1L).with(connecte(Role.FORMATEUR)))
                .andExpect(status().isForbidden());
        verify(materielService, never()).deleteById(any());
    }

    // ---------- archiver ----------

    @Test
    void archiver_doitRenvoyer200_quandEnStock() throws Exception {
        when(materielService.archiver(1L)).thenReturn(materiel());
        reponse(EtatMateriel.ARCHIVE);

        mockMvc.perform(patch("/api/v1/materiels/{id}/archiver/", 1L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etat").value("ARCHIVE"));
    }

    @Test
    void archiver_doitRenvoyer400_quandAffecte() throws Exception {
        when(materielService.archiver(1L)).thenThrow(new ServiceException("Un matériel affecté ne peut pas être archivé, remets-le en stock d'abord"));

        mockMvc.perform(patch("/api/v1/materiels/{id}/archiver/", 1L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void archiver_doitRenvoyer400_quandIntrouvable() throws Exception {
        when(materielService.archiver(1L)).thenThrow(new ServiceException("Matériel 1 introuvable"));

        mockMvc.perform(patch("/api/v1/materiels/{id}/archiver/", 1L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void archiver_doitRenvoyer403_quandFormateur() throws Exception {
        mockMvc.perform(patch("/api/v1/materiels/{id}/archiver/", 1L).with(connecte(Role.FORMATEUR)))
                .andExpect(status().isForbidden());
    }
}