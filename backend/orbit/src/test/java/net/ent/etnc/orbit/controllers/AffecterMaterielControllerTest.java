package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.dtos.assemblers.MaterielAssembler;
import net.ent.etnc.orbit.dtos.responseDTO.MaterielResponseDto;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.UC;
import net.ent.etnc.orbit.models.enums.EtatMateriel;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.security.WebSecurityConfig;
import net.ent.etnc.orbit.services.AffecterMaterielService;
import net.ent.etnc.orbit.services.MaterielService;
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

@WebMvcTest(AffecterMaterielController.class)
@Import(WebSecurityConfig.class)
class AffecterMaterielControllerTest extends ControllerTestSupport {

    @MockitoBean
    private AffecterMaterielService affecterMaterielService;

    @MockitoBean
    private MaterielAssembler materielAssembler;

    @MockitoBean
    private MaterielService materielService;

    private void reponseMateriel() {
        when(materielAssembler.toDto(any(), any())).thenReturn(
                MaterielResponseDto.builder().id(11L).numSerie("VP-0002").etat(EtatMateriel.NORMAL).build());
    }

    // ---------- affecterASalle : POST /materiels/{id}/?salleId= ----------

    @Test
    void affecterASalle_doitRenvoyer200_quandValide() throws Exception {
        when(affecterMaterielService.affecterASalle(2L, 11L)).thenReturn(new UC());
        reponseMateriel();

        mockMvc.perform(post("/api/v1/materiels/{id}/", 11L).param("salleId", "2")
                        .with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numSerie").value("VP-0002"));
    }

    @Test
    void affecterASalle_doitRenvoyer200_quandAdmin() throws Exception {
        when(affecterMaterielService.affecterASalle(2L, 11L)).thenReturn(new UC());
        reponseMateriel();

        mockMvc.perform(post("/api/v1/materiels/{id}/", 11L).param("salleId", "2")
                        .with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isOk());
    }

    @Test
    void affecterASalle_doitRenvoyer400_quandSalleIntrouvable() throws Exception {
        when(affecterMaterielService.affecterASalle(999L, 11L)).thenThrow(new ServiceException("Salle 999 introuvable"));

        mockMvc.perform(post("/api/v1/materiels/{id}/", 11L).param("salleId", "999")
                        .with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Salle 999 introuvable"));
    }

    @Test
    void affecterASalle_doitRenvoyer400_quandMaterielPasEnStock() throws Exception {
        when(affecterMaterielService.affecterASalle(2L, 11L)).thenThrow(new ServiceException("Seul un matériel en stock peut être affecté"));

        mockMvc.perform(post("/api/v1/materiels/{id}/", 11L).param("salleId", "2")
                        .with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Seul un matériel en stock peut être affecté"));
    }

    @Test
    void affecterASalle_doitRenvoyer403_quandFormateur() throws Exception {
        mockMvc.perform(post("/api/v1/materiels/{id}/", 11L).param("salleId", "2")
                        .with(connecte(Role.FORMATEUR)))
                .andExpect(status().isForbidden());
        verify(affecterMaterielService, never()).affecterASalle(any(), any());
    }

    @Test
    void affecterASalle_doitRenvoyer401_quandNonAuthentifie() throws Exception {
        mockMvc.perform(post("/api/v1/materiels/{id}/", 11L).param("salleId", "2"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- retirerDeSalle : DELETE /materiels/{id}/?salleId= ----------

    @Test
    void retirerDeSalle_doitRenvoyer200_quandValide() throws Exception {
        when(affecterMaterielService.retirerDeSalle(2L, 11L)).thenReturn(new UC());
        reponseMateriel();

        mockMvc.perform(delete("/api/v1/materiels/{id}/", 11L).param("salleId", "2")
                        .with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isOk());
    }

    @Test
    void retirerDeSalle_doitRenvoyer400_quandMaterielPasDansLaSalle() throws Exception {
        when(affecterMaterielService.retirerDeSalle(2L, 11L)).thenThrow(new ServiceException("Ce matériel n'est pas dans cette salle"));

        mockMvc.perform(delete("/api/v1/materiels/{id}/", 11L).param("salleId", "2")
                        .with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void retirerDeSalle_doitRenvoyer403_quandFormateur() throws Exception {
        mockMvc.perform(delete("/api/v1/materiels/{id}/", 11L).param("salleId", "2")
                        .with(connecte(Role.FORMATEUR)))
                .andExpect(status().isForbidden());
    }

    // ---------- affecterAPoste : POST /materiels/{id}/?posteId= ----------

    @Test
    void affecterAPoste_doitRenvoyer200_quandValide() throws Exception {
        when(affecterMaterielService.affecterAPoste(1L, 8L)).thenReturn(new UC());
        reponseMateriel();

        mockMvc.perform(post("/api/v1/materiels/{id}/", 8L).param("posteId", "1")
                        .with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isOk());
        verify(affecterMaterielService).affecterAPoste(1L, 8L);
    }

    @Test
    void affecterAPoste_doitRenvoyer400_quandPosteIntrouvable() throws Exception {
        when(affecterMaterielService.affecterAPoste(999L, 8L)).thenThrow(new ServiceException("Poste 999 introuvable"));

        mockMvc.perform(post("/api/v1/materiels/{id}/", 8L).param("posteId", "999")
                        .with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void affecterAPoste_doitRenvoyer400_quandMaterielArchive() throws Exception {
        when(affecterMaterielService.affecterAPoste(1L, 12L)).thenThrow(new ServiceException("Un matériel archivé ne peut pas être affecté"));

        mockMvc.perform(post("/api/v1/materiels/{id}/", 12L).param("posteId", "1")
                        .with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void affecterAPoste_doitRenvoyer403_quandStagiaire() throws Exception {
        mockMvc.perform(post("/api/v1/materiels/{id}/", 8L).param("posteId", "1")
                        .with(connecte(Role.STAGIAIRE)))
                .andExpect(status().isForbidden());
    }

    // ---------- retirerDuPoste : DELETE /materiels/{id}/?posteId= ----------

    @Test
    void retirerDuPoste_doitRenvoyer200_quandValide() throws Exception {
        when(affecterMaterielService.retirerDuPoste(1L, 8L)).thenReturn(new UC());
        reponseMateriel();

        mockMvc.perform(delete("/api/v1/materiels/{id}/", 8L).param("posteId", "1")
                        .with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isOk());
    }

    @Test
    void retirerDuPoste_doitRenvoyer400_quandMaterielPasSurLePoste() throws Exception {
        when(affecterMaterielService.retirerDuPoste(1L, 8L)).thenThrow(new ServiceException("Ce matériel n'est pas sur ce poste"));

        mockMvc.perform(delete("/api/v1/materiels/{id}/", 8L).param("posteId", "1")
                        .with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void retirerDuPoste_doitRenvoyer403_quandFormateur() throws Exception {
        mockMvc.perform(delete("/api/v1/materiels/{id}/", 8L).param("posteId", "1")
                        .with(connecte(Role.FORMATEUR)))
                .andExpect(status().isForbidden());
    }

    // ---------- routage ----------

    @Test
    void sansParametre_doitRenvoyer4xx_etNeRienAffecter() throws Exception {
        // sans salleId ni posteId, aucune méthode d'affectation ne correspond
        mockMvc.perform(post("/api/v1/materiels/{id}/", 8L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().is4xxClientError());
        verify(affecterMaterielService, never()).affecterAPoste(any(), any());
        verify(affecterMaterielService, never()).affecterASalle(any(), any());
    }
}