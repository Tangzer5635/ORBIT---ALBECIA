package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.dtos.assemblers.SalleAssembler;
import net.ent.etnc.orbit.dtos.responseDTO.FormateurDto;
import net.ent.etnc.orbit.dtos.responseDTO.SalleResponseDto;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.security.WebSecurityConfig;
import net.ent.etnc.orbit.services.AffecterFormateurService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AffecterFormateurController.class)
@Import(WebSecurityConfig.class)
class AffecterFormateurControllerTest extends ControllerTestSupport {

    @MockitoBean
    private AffecterFormateurService affecterFormateurService;

    @MockitoBean
    private SalleAssembler salleAssembler;

    // ---------- affecter : POST /salles/{idSalle}/?formateurId= ----------

    @Test
    void affecterFormateur_doitRenvoyer200_quandValide() throws Exception {
        Salle salle = new Salle();
        when(affecterFormateurService.affecter(6L, 4L)).thenReturn(salle);
        when(salleAssembler.toDto(any())).thenReturn(SalleResponseDto.builder().id(6L).numSalle("105").build());
        when(salleAssembler.formateurs(salle)).thenReturn(List.of(new FormateurDto(4L, "Kevin", "Ledeme")));

        mockMvc.perform(post("/api/v1/salles/{idSalle}/", 6L).param("formateurId", "4")
                        .with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formateurs[0].nom").value("Ledeme"));
    }

    @Test
    void affecterFormateur_doitRenvoyer400_quandDejaAffecte() throws Exception {
        when(affecterFormateurService.affecter(6L, 4L))
                .thenThrow(new ServiceException("Ce formateur est déjà affecté à la salle 105"));

        mockMvc.perform(post("/api/v1/salles/{idSalle}/", 6L).param("formateurId", "4")
                        .with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Ce formateur est déjà affecté à la salle 105"));
    }

    @Test
    void affecterFormateur_doitRenvoyer403_quandFormateur() throws Exception {
        mockMvc.perform(post("/api/v1/salles/{idSalle}/", 6L).param("formateurId", "4")
                        .with(connecte(Role.FORMATEUR)))
                .andExpect(status().isForbidden());
        verify(affecterFormateurService, never()).affecter(any(), any());
    }

    @Test
    void affecterFormateur_doitRenvoyer401_quandNonAuthentifie() throws Exception {
        mockMvc.perform(post("/api/v1/salles/{idSalle}/", 6L).param("formateurId", "4"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- retirer : DELETE /salles/{idSalle}/?formateurId= ----------

    @Test
    void retirerFormateur_doitRenvoyer200_quandValide() throws Exception {
        when(affecterFormateurService.retirer(6L, 4L)).thenReturn(new Salle());
        when(salleAssembler.toDto(any())).thenReturn(SalleResponseDto.builder().id(6L).numSalle("105").build());

        mockMvc.perform(delete("/api/v1/salles/{idSalle}/", 6L).param("formateurId", "4")
                        .with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numSalle").value("105"));
    }

    @Test
    void retirerFormateur_doitRenvoyer400_quandPasAffecte() throws Exception {
        when(affecterFormateurService.retirer(6L, 4L))
                .thenThrow(new ServiceException("Ce formateur n'est pas affecté à la salle 105"));

        mockMvc.perform(delete("/api/v1/salles/{idSalle}/", 6L).param("formateurId", "4")
                        .with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void retirerFormateur_doitRenvoyer403_quandFormateur() throws Exception {
        mockMvc.perform(delete("/api/v1/salles/{idSalle}/", 6L).param("formateurId", "4")
                        .with(connecte(Role.FORMATEUR)))
                .andExpect(status().isForbidden());
        verify(affecterFormateurService, never()).retirer(any(), any());
    }
}
