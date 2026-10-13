package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.dtos.assemblers.UserAssembler;
import net.ent.etnc.orbit.dtos.requestDTO.UserRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.UserResponseDto;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.security.WebSecurityConfig;
import net.ent.etnc.orbit.services.PersonnelService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PersonnelController.class)
@Import(WebSecurityConfig.class)
class PersonnelControllerTest extends ControllerTestSupport {

    private static final String JSON_VALIDE = """
            {"nid":"5000000001","nom":"Test","prenom":"Jean","login":"j.test",
             "motDePasse":"test1234","role":"STAGIAIRE"}""";

    @MockitoBean
    private PersonnelService personnelService;

    @MockitoBean
    private UserAssembler userAssembler;

    private void reponse(boolean verrouille) {
        when(userAssembler.toDto(any())).thenReturn(UserResponseDto.builder()
                .id(7L).login("j.test").nom("Test").prenom("Jean").role(Role.STAGIAIRE)
                .active(true).verrouille(verrouille).build());
    }

    // ---------- getAll ----------

    @Test
    void getAll_doitRenvoyer200_quandAdmin() throws Exception {
        when(personnelService.findAll(any())).thenReturn(
                new PageImpl<>(List.of(personnel(Role.STAGIAIRE)), PageRequest.of(0, 20), 1));
        reponse(false);

        mockMvc.perform(get("/api/v1/personnels/").with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].login").value("j.test"))
                .andExpect(jsonPath("$.content[0].motDePasse").doesNotExist());
    }

    @Test
    void getAll_doitRenvoyer403_quandGestionnaire() throws Exception {
        mockMvc.perform(get("/api/v1/personnels/").with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_doitRenvoyer401_quandNonAuthentifie() throws Exception {
        mockMvc.perform(get("/api/v1/personnels/"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- getById ----------

    @Test
    void getById_doitRenvoyer200_quandTrouve() throws Exception {
        when(personnelService.findById(7L)).thenReturn(Optional.of(personnel(Role.STAGIAIRE)));
        reponse(false);

        mockMvc.perform(get("/api/v1/personnels/{id}/", 7L).with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STAGIAIRE"));
    }

    @Test
    void getById_doitRenvoyer404_quandIntrouvable() throws Exception {
        when(personnelService.findById(7L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/personnels/{id}/", 7L).with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isNotFound());
    }

    // ---------- post ----------

    @Test
    void post_doitRenvoyer200_quandValide() throws Exception {
        when(userAssembler.toEntity(any(UserRequestDto.class))).thenReturn(personnel(Role.STAGIAIRE));
        when(personnelService.create(any())).thenReturn(personnel(Role.STAGIAIRE));
        reponse(false);

        mockMvc.perform(post("/api/v1/personnels").with(connecte(Role.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verrouille").value(false));
    }

    @Test
    void post_doitRenvoyer400_quandChampsManquants() throws Exception {
        mockMvc.perform(post("/api/v1/personnels").with(connecte(Role.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"login\":\"vide\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.nid").exists())
                .andExpect(jsonPath("$.role").exists());
        verify(personnelService, never()).create(any());
    }

    @Test
    void post_doitRenvoyer400_quandRoleInconnu() throws Exception {
        mockMvc.perform(post("/api/v1/personnels").with(connecte(Role.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VALIDE.replace("STAGIAIRE", "PILOTE")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_doitRenvoyer400_quandMotDePasseManquant() throws Exception {
        when(userAssembler.toEntity(any(UserRequestDto.class))).thenReturn(personnel(Role.STAGIAIRE));
        when(personnelService.create(any())).thenThrow(new ServiceException("RG-U03", "Le mot de passe est obligatoire"));

        mockMvc.perform(post("/api/v1/personnels").with(connecte(Role.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDE))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Le mot de passe est obligatoire"));
    }

    @Test
    void post_doitRenvoyer403_quandGestionnaire() throws Exception {
        mockMvc.perform(post("/api/v1/personnels").with(connecte(Role.GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDE))
                .andExpect(status().isForbidden());
    }

    // ---------- put ----------

    @Test
    void put_doitRenvoyer200_quandValide() throws Exception {
        when(personnelService.existsById(7L)).thenReturn(true);
        when(userAssembler.toEntity(any(UserRequestDto.class))).thenReturn(personnel(Role.STAGIAIRE));
        when(personnelService.update(any())).thenReturn(personnel(Role.STAGIAIRE));
        reponse(false);

        mockMvc.perform(put("/api/v1/personnels/{id}/", 7L).with(connecte(Role.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDE))
                .andExpect(status().isOk());
    }

    @Test
    void put_doitRenvoyer404_quandIntrouvable() throws Exception {
        when(personnelService.existsById(7L)).thenReturn(false);

        mockMvc.perform(put("/api/v1/personnels/{id}/", 7L).with(connecte(Role.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDE))
                .andExpect(status().isNotFound());
        verify(personnelService, never()).update(any());
    }

    @Test
    void put_doitRenvoyer403_quandGestionnaire() throws Exception {
        mockMvc.perform(put("/api/v1/personnels/{id}/", 7L).with(connecte(Role.GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDE))
                .andExpect(status().isForbidden());
    }

    // ---------- verrouiller ----------

    @Test
    void verrouiller_doitRenvoyer200_quandValide() throws Exception {
        when(personnelService.verrouiller(7L)).thenReturn(personnel(Role.STAGIAIRE));
        reponse(true);

        mockMvc.perform(patch("/api/v1/personnels/{id}/verrouiller/", 7L).with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verrouille").value(true));
    }

    @Test
    void verrouiller_doitRenvoyer400_quandAdministrateur() throws Exception {
        when(personnelService.verrouiller(1L)).thenThrow(new ServiceException("RG-U06", "Impossible de verrouiller un administrateur."));

        mockMvc.perform(patch("/api/v1/personnels/{id}/verrouiller/", 1L).with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Impossible de verrouiller un administrateur."));
    }

    @Test
    void verrouiller_doitRenvoyer404_quandIntrouvable() throws Exception {
        when(personnelService.verrouiller(999L)).thenThrow(new ServiceException("RG-U02", "Utilisateur introuvable"));

        mockMvc.perform(patch("/api/v1/personnels/{id}/verrouiller/", 999L).with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isNotFound());
    }

    @Test
    void verrouiller_doitRenvoyer403_quandGestionnaire() throws Exception {
        mockMvc.perform(patch("/api/v1/personnels/{id}/verrouiller/", 7L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isForbidden());
        verify(personnelService, never()).verrouiller(any());
    }

    // ---------- deverrouiller ----------

    @Test
    void deverrouiller_doitRenvoyer200_quandValide() throws Exception {
        when(personnelService.deverrouiller(7L)).thenReturn(personnel(Role.STAGIAIRE));
        reponse(false);

        mockMvc.perform(patch("/api/v1/personnels/{id}/deverrouiller/", 7L).with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verrouille").value(false));
    }

    @Test
    void deverrouiller_doitRenvoyer400_quandPasVerrouille() throws Exception {
        when(personnelService.deverrouiller(7L)).thenThrow(new ServiceException("RG-U08", "Ce compte n'est pas verrouillé."));

        mockMvc.perform(patch("/api/v1/personnels/{id}/deverrouiller/", 7L).with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deverrouiller_doitRenvoyer403_quandGestionnaire() throws Exception {
        mockMvc.perform(patch("/api/v1/personnels/{id}/deverrouiller/", 7L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isForbidden());
    }

    // ---------- delete ----------

    @Test
    void delete_doitRenvoyer204_quandTrouve() throws Exception {
        mockMvc.perform(delete("/api/v1/personnels/{id}/", 7L).with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isNoContent());
        verify(personnelService).deleteById(7L);
    }

    @Test
    void delete_doitRenvoyer404_quandIntrouvable() throws Exception {
        doThrow(new ServiceException("Utilisateur introuvable")).when(personnelService).deleteById(999L);

        mockMvc.perform(delete("/api/v1/personnels/{id}/", 999L).with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_doitRenvoyer400_quandDernierAdministrateur() throws Exception {
        doThrow(new ServiceException("Impossible de supprimer le dernier administrateur.")).when(personnelService).deleteById(1L);

        mockMvc.perform(delete("/api/v1/personnels/{id}/", 1L).with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_doitRenvoyer403_quandGestionnaire() throws Exception {
        mockMvc.perform(delete("/api/v1/personnels/{id}/", 7L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isForbidden());
    }
}
