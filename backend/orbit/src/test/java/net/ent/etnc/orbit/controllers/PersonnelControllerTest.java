package net.ent.etnc.orbit.controllers;

import net.ent.etnc.orbit.dtos.assemblers.UserAssembler;
import net.ent.etnc.orbit.dtos.requestDTO.UserCreateRequestDto;
import net.ent.etnc.orbit.dtos.requestDTO.UserUpdateRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.UserResponseDto;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.security.WebSecurityConfig;
import net.ent.etnc.orbit.services.PersonnelService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    private static final String JSON_MODIFICATION = """
            {"nid":"5000000001","nom":"Test","prenom":"Jean","login":"j.test"}""";

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
    void getAll_doitTrierParNomPrenomId_etPaginerPar20() throws Exception {
        when(personnelService.findAll(any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/personnels/").with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(personnelService).findAll(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by("nom", "prenom", "id"));
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
    void post_doitRenvoyer201_quandValide() throws Exception {
        when(userAssembler.toEntity(any(UserCreateRequestDto.class))).thenReturn(personnel(Role.STAGIAIRE));
        when(personnelService.create(any())).thenReturn(personnel(Role.STAGIAIRE));
        reponse(false);

        mockMvc.perform(post("/api/v1/personnels/").with(connecte(Role.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verrouille").value(false));
    }

    @Test
    void post_doitRenvoyer400_quandChampsManquants() throws Exception {
        mockMvc.perform(post("/api/v1/personnels/").with(connecte(Role.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"login\":\"vide\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.nid").exists())
                .andExpect(jsonPath("$.role").exists());
        verify(personnelService, never()).create(any());
    }

    @Test
    void post_doitRenvoyer400_quandNidInvalide() throws Exception {
        mockMvc.perform(post("/api/v1/personnels/").with(connecte(Role.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VALIDE.replace("5000000001", "12AB")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.nid").value("nid doit contenir exactement 10 chiffres"));
        verify(personnelService, never()).create(any());
    }

    @Test
    void post_doitRenvoyer400_quandMotDePasseManquant() throws Exception {
        mockMvc.perform(post("/api/v1/personnels/").with(connecte(Role.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VALIDE.replace("\"motDePasse\":\"test1234\",", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.motDePasse").exists());
        verify(personnelService, never()).create(any());
    }

    @Test
    void post_doitRenvoyer400_quandLoginTropCourt() throws Exception {
        mockMvc.perform(post("/api/v1/personnels/").with(connecte(Role.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VALIDE.replace("j.test", "ab")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.login").exists());
    }

    @Test
    void post_doitRenvoyer400_quandRoleInconnu() throws Exception {
        mockMvc.perform(post("/api/v1/personnels/").with(connecte(Role.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VALIDE.replace("STAGIAIRE", "PILOTE")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_doitRenvoyer400_quandLeServiceRefuse() throws Exception {
        when(userAssembler.toEntity(any(UserCreateRequestDto.class))).thenReturn(personnel(Role.STAGIAIRE));
        when(personnelService.create(any())).thenThrow(new ServiceException("RG-U03", "Le mot de passe est obligatoire"));

        mockMvc.perform(post("/api/v1/personnels/").with(connecte(Role.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDE))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Le mot de passe est obligatoire"));
    }

    @Test
    void post_doitRenvoyer403_quandGestionnaire() throws Exception {
        mockMvc.perform(post("/api/v1/personnels/").with(connecte(Role.GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDE))
                .andExpect(status().isForbidden());
    }

    // ---------- put ----------

    @Test
    void put_doitRenvoyer200_quandValide() throws Exception {
        when(userAssembler.toEntity(any(UserUpdateRequestDto.class))).thenReturn(personnel(Role.STAGIAIRE));
        when(personnelService.update(any())).thenReturn(personnel(Role.STAGIAIRE));
        reponse(false);

        mockMvc.perform(put("/api/v1/personnels/{id}/", 7L).with(connecte(Role.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_MODIFICATION))
                .andExpect(status().isOk());
    }

    @Test
    void put_doitRenvoyer400_quandIntrouvable() throws Exception {
        when(userAssembler.toEntity(any(UserUpdateRequestDto.class))).thenReturn(personnel(Role.STAGIAIRE));
        when(personnelService.update(any())).thenThrow(new ServiceException("RG-U02", "Utilisateur introuvable"));

        mockMvc.perform(put("/api/v1/personnels/{id}/", 7L).with(connecte(Role.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_MODIFICATION))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Utilisateur introuvable"));
    }

    @Test
    void put_doitRenvoyer400_quandNidInvalide() throws Exception {
        mockMvc.perform(put("/api/v1/personnels/{id}/", 7L).with(connecte(Role.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_MODIFICATION.replace("5000000001", "123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.nid").exists());
        verify(personnelService, never()).update(any());
    }

    @Test
    void put_doitRenvoyer403_quandGestionnaire() throws Exception {
        mockMvc.perform(put("/api/v1/personnels/{id}/", 7L).with(connecte(Role.GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_MODIFICATION))
                .andExpect(status().isForbidden());
    }

    // ---------- role ----------

    @Test
    void changerRole_doitRenvoyer200_quandValide() throws Exception {
        when(personnelService.changeRole(7L, Role.FORMATEUR)).thenReturn(personnel(Role.FORMATEUR));
        reponse(false);

        mockMvc.perform(patch("/api/v1/personnels/{id}/role/", 7L).with(connecte(Role.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"FORMATEUR\"}"))
                .andExpect(status().isOk());
        verify(personnelService).changeRole(7L, Role.FORMATEUR);
    }

    @Test
    void changerRole_doitRenvoyer400_quandRoleManquant() throws Exception {
        mockMvc.perform(patch("/api/v1/personnels/{id}/role/", 7L).with(connecte(Role.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.role").exists());
        verify(personnelService, never()).changeRole(any(), any());
    }

    @Test
    void changerRole_doitRenvoyer400_quandRoleInconnu() throws Exception {
        mockMvc.perform(patch("/api/v1/personnels/{id}/role/", 7L).with(connecte(Role.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"PILOTE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changerRole_doitRenvoyer400_quandLeServiceRefuse() throws Exception {
        when(personnelService.changeRole(eq(1L), any()))
                .thenThrow(new ServiceException("RG-U05", "Impossible de changer son propre rôle"));

        mockMvc.perform(patch("/api/v1/personnels/{id}/role/", 1L).with(connecte(Role.ADMINISTRATEUR))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"STAGIAIRE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Impossible de changer son propre rôle"));
    }

    @Test
    void changerRole_doitRenvoyer403_quandGestionnaire() throws Exception {
        mockMvc.perform(patch("/api/v1/personnels/{id}/role/", 7L).with(connecte(Role.GESTIONNAIRE))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"FORMATEUR\"}"))
                .andExpect(status().isForbidden());
        verify(personnelService, never()).changeRole(any(), any());
    }

    // ---------- activer / desactiver ----------

    @Test
    void activer_doitRenvoyer200_quandValide() throws Exception {
        when(personnelService.activer(7L)).thenReturn(personnel(Role.STAGIAIRE));
        reponse(false);

        mockMvc.perform(patch("/api/v1/personnels/{id}/activer/", 7L).with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void activer_doitRenvoyer400_quandDejaActif() throws Exception {
        when(personnelService.activer(7L)).thenThrow(new ServiceException("RG-U12", "Ce compte est déjà actif."));

        mockMvc.perform(patch("/api/v1/personnels/{id}/activer/", 7L).with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Ce compte est déjà actif."));
    }

    @Test
    void activer_doitRenvoyer403_quandGestionnaire() throws Exception {
        mockMvc.perform(patch("/api/v1/personnels/{id}/activer/", 7L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isForbidden());
        verify(personnelService, never()).activer(any());
    }

    @Test
    void desactiver_doitRenvoyer200_quandValide() throws Exception {
        Personnel inactif = personnel(Role.STAGIAIRE);
        inactif.setActive(false);
        when(personnelService.desactiver(7L)).thenReturn(inactif);
        when(userAssembler.toDto(any())).thenReturn(UserResponseDto.builder()
                .id(7L).login("j.test").role(Role.STAGIAIRE).active(false).build());

        mockMvc.perform(patch("/api/v1/personnels/{id}/desactiver/", 7L).with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void desactiver_doitRenvoyer400_quandAdministrateur() throws Exception {
        when(personnelService.desactiver(1L))
                .thenThrow(new ServiceException("RG-U05", "Impossible de désactiver un administrateur."));

        mockMvc.perform(patch("/api/v1/personnels/{id}/desactiver/", 1L).with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void desactiver_doitRenvoyer403_quandGestionnaire() throws Exception {
        mockMvc.perform(patch("/api/v1/personnels/{id}/desactiver/", 7L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isForbidden());
        verify(personnelService, never()).desactiver(any());
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
    void verrouiller_doitRenvoyer400_quandIntrouvable() throws Exception {
        when(personnelService.verrouiller(999L)).thenThrow(new ServiceException("RG-U02", "Utilisateur introuvable"));

        mockMvc.perform(patch("/api/v1/personnels/{id}/verrouiller/", 999L).with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isBadRequest());
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
    void delete_doitRenvoyer400_quandIntrouvable() throws Exception {
        doThrow(new ServiceException("Utilisateur introuvable")).when(personnelService).deleteById(999L);

        mockMvc.perform(delete("/api/v1/personnels/{id}/", 999L).with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_doitRenvoyer400_quandDernierAdministrateur() throws Exception {
        doThrow(new ServiceException("Impossible de supprimer le dernier administrateur.")).when(personnelService).deleteById(1L);

        mockMvc.perform(delete("/api/v1/personnels/{id}/", 1L).with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_doitRenvoyer400_quandPropreCompte() throws Exception {
        doThrow(new ServiceException("RG-U09", "Impossible de supprimer son propre compte.")).when(personnelService).deleteById(1L);

        mockMvc.perform(delete("/api/v1/personnels/{id}/", 1L).with(connecte(Role.ADMINISTRATEUR)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Impossible de supprimer son propre compte."));
    }

    @Test
    void delete_doitRenvoyer403_quandGestionnaire() throws Exception {
        mockMvc.perform(delete("/api/v1/personnels/{id}/", 7L).with(connecte(Role.GESTIONNAIRE)))
                .andExpect(status().isForbidden());
    }
}
