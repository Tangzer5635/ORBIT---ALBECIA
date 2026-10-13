package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.dtos.assemblers.BatimentAssembler;
import net.ent.etnc.orbit.dtos.responseDTO.BatimentResponseDto;
import net.ent.etnc.orbit.models.entities.Batiment;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.security.WebSecurityConfig;
import net.ent.etnc.orbit.services.BatimentService;
import net.ent.etnc.orbit.services.commons.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

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

@WebMvcTest(AffecterSalleController.class)
@Import(WebSecurityConfig.class)
class AffecterSalleControllerTest extends ControllerTestSupport {

    @MockitoBean
    private BatimentService batimentService;

    @MockitoBean
    private BatimentAssembler batimentAssembler;

    // ---------- affecterSalle : POST /salles/{idSalle}/?batimentId= ----------

    @Test
    void affecterSalle_doitRenvoyer200_quandValide() throws Exception {
        when(batimentService.affecterSalle(1L, 6L)).thenReturn(new Batiment());
        when(batimentAssembler.toDto(any())).thenReturn(
                BatimentResponseDto.builder().id(1L).numBatiment("B201").sallesId(List.of(6L)).build());

        mockMvc.perform(post("/api/v1/salles/{idSalle}/", 6L).param("batimentId", "1")
                        .with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sallesId[0]").value(6));
    }

    @Test
    void affecterSalle_doitRenvoyer404_quandBatimentIntrouvable() throws Exception {
        when(batimentService.affecterSalle(999L, 6L)).thenThrow(new NotFoundException("Bâtiment 999 introuvable"));

        mockMvc.perform(post("/api/v1/salles/{idSalle}/", 6L).param("batimentId", "999")
                        .with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Bâtiment 999 introuvable"));
    }

    @Test
    void affecterSalle_doitRenvoyer400_quandSalleDejaAffectee() throws Exception {
        when(batimentService.affecterSalle(1L, 6L)).thenThrow(new ServiceException("La salle est déjà affectée à un bâtiment"));

        mockMvc.perform(post("/api/v1/salles/{idSalle}/", 6L).param("batimentId", "1")
                        .with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void affecterSalle_doitRenvoyer403_quandGestionnaire() throws Exception {
        mockMvc.perform(post("/api/v1/salles/{idSalle}/", 6L).param("batimentId", "1")
                        .with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void affecterSalle_doitRenvoyer401_quandNonAuthentifie() throws Exception {
        mockMvc.perform(post("/api/v1/salles/{idSalle}/", 6L).param("batimentId", "1"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- retirerSalle : DELETE /salles/{idSalle}/?batimentId= ----------

    @Test
    void retirerSalle_doitRenvoyer204_quandValide() throws Exception {
        mockMvc.perform(delete("/api/v1/salles/{idSalle}/", 6L).param("batimentId", "1")
                        .with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isNoContent());
        verify(batimentService).retirerSalle(1L, 6L);
    }

    @Test
    void retirerSalle_doitRenvoyer400_quandSalleHorsDuBatiment() throws Exception {
        doThrow(new ServiceException("Cette salle n'appartient pas à ce bâtiment"))
                .when(batimentService).retirerSalle(1L, 6L);

        mockMvc.perform(delete("/api/v1/salles/{idSalle}/", 6L).param("batimentId", "1")
                        .with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void retirerSalle_doitRenvoyer403_quandGestionnaire() throws Exception {
        mockMvc.perform(delete("/api/v1/salles/{idSalle}/", 6L).param("batimentId", "1")
                        .with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isForbidden());
        verify(batimentService, never()).retirerSalle(any(), any());
    }
}
