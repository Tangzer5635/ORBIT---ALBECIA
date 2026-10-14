package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.repositories.*;
import net.ent.etnc.orbit.services.RefreshTokenService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonnelServiceImplTest {

    @Mock
    private PersonnelRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private PosteRepository posteRepository;

    @Mock
    private SalleRepository salleRepository;

    @Mock
    private DefaillanceRepository defaillanceRepository;

    @InjectMocks
    private PersonnelServiceImpl service;

    @Mock
    private StageRepository stageRepository;

    @AfterEach
    void viderLeContexteDeSecurite() {
        SecurityContextHolder.clearContext();
    }

    private Personnel personnel(Long id, Role role) {
        Personnel p = new Personnel();
        p.setId(id);
        p.setNid(String.format("%010d", id == null ? 0 : id));
        p.setNom("Test");
        p.setPrenom("Jean");
        p.setLogin("user" + id);
        p.setRole(role);
        p.setActive(true);
        return p;
    }

    private void connecteEnTantQue(Personnel courant) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(courant, null, courant.getAuthorities()));
    }

    private void sauvegardeRenvoieLArgument() {
        when(repository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }

    // ---------- create ----------

    @Test
    void create_doitHacherLeMotDePasse() {
        Personnel p = personnel(null, Role.STAGIAIRE);
        p.setMotDePasse("secret");
        when(passwordEncoder.encode("secret")).thenReturn("hash");
        sauvegardeRenvoieLArgument();

        Personnel resultat = service.create(p);

        assertThat(resultat.getPassword()).isEqualTo("hash");
    }

    @Test
    void create_doitLeverServiceException_RGU03_quandMotDePasseVide() {
        Personnel p = personnel(null, Role.STAGIAIRE);
        p.setMotDePasse("  ");

        assertThatThrownBy(() -> service.create(p))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Le mot de passe est obligatoire");
        verify(repository, never()).saveAndFlush(any());
    }

    // ---------- update ----------

    @Test
    void update_doitConserverLeMotDePasse_quandNonFourni() {
        Personnel existant = personnel(7L, Role.STAGIAIRE);
        existant.setMotDePasse("ancien-hash");
        when(repository.findById(7L)).thenReturn(Optional.of(existant));
        sauvegardeRenvoieLArgument();

        Personnel resultat = service.update(personnel(7L, Role.STAGIAIRE));

        assertThat(resultat.getPassword()).isEqualTo("ancien-hash");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void update_doitConserverLeRoleEtLActivation() {
        Personnel existant = personnel(7L, Role.FORMATEUR);
        existant.setActive(false);
        existant.setMotDePasse("hash");
        Personnel modifie = personnel(7L, Role.ADMINISTRATEUR);
        modifie.setActive(true);
        when(repository.findById(7L)).thenReturn(Optional.of(existant));
        sauvegardeRenvoieLArgument();

        Personnel resultat = service.update(modifie);

        assertThat(resultat.getRole()).isEqualTo(Role.FORMATEUR);
        assertThat(resultat.isActive()).isFalse();
    }

    @Test
    void update_doitHacherLeNouveauMotDePasse() {
        Personnel modifie = personnel(7L, Role.STAGIAIRE);
        modifie.setMotDePasse("nouveau");
        when(repository.findById(7L)).thenReturn(Optional.of(personnel(7L, Role.STAGIAIRE)));
        when(passwordEncoder.encode("nouveau")).thenReturn("nouveau-hash");
        sauvegardeRenvoieLArgument();

        assertThat(service.update(modifie).getPassword()).isEqualTo("nouveau-hash");
    }

    @Test
    void update_doitConserverLeVerrouillage() {
        Personnel existant = personnel(7L, Role.STAGIAIRE);
        existant.setVerrouille(true);
        existant.setMotDePasse("hash");
        when(repository.findById(7L)).thenReturn(Optional.of(existant));
        sauvegardeRenvoieLArgument();

        Personnel resultat = service.update(personnel(7L, Role.STAGIAIRE));

        assertThat(resultat.isVerrouille()).isTrue();
    }

    @Test
    void update_doitLeverNotFound_quandIntrouvable() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(personnel(7L, Role.STAGIAIRE)))
                .isInstanceOf(ServiceException.class);
    }

    // ---------- deleteById ----------

    @Test
    void deleteById_doitSupprimer_quandStagiaire() {
        when(repository.findById(7L)).thenReturn(Optional.of(personnel(7L, Role.STAGIAIRE)));

        service.deleteById(7L);

        verify(refreshTokenService).revokeAllForUser("user7");
        verify(repository).deleteById(7L);
    }

    @Test
    void deleteById_doitLeRetirerDePartout() {
        Personnel p = personnel(7L, Role.FORMATEUR);
        Salle salle = new Salle();
        salle.addPersonnel(p);
        when(repository.findById(7L)).thenReturn(Optional.of(p));
        when(salleRepository.findByPersonnels_Id(7L)).thenReturn(List.of(salle));

        service.deleteById(7L);

        verify(posteRepository).liberer(7L);
        assertThat(salle.getPersonnels()).isEmpty();
        verify(defaillanceRepository).retirerTechnicien(7L);
        verify(repository).deleteById(7L);
    }

    @Test
    void deleteById_doitLeverServiceException_quandDefaillancesSignalees() {
        when(repository.findById(7L)).thenReturn(Optional.of(personnel(7L, Role.STAGIAIRE)));
        when(defaillanceRepository.existsBySignalePar_Id(7L)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteById(7L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Impossible de supprimer un compte ayant signalé des défaillances.");
        verify(posteRepository, never()).liberer(any());
        verify(repository, never()).deleteById(any());
    }

    @Test
    void deleteById_doitLeverServiceException_quandPropreCompte() {
        Personnel admin = personnel(1L, Role.ADMINISTRATEUR);
        connecteEnTantQue(admin);
        when(repository.findById(1L)).thenReturn(Optional.of(personnel(1L, Role.ADMINISTRATEUR)));

        assertThatThrownBy(() -> service.deleteById(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Impossible de supprimer son propre compte.");
        verify(posteRepository, never()).liberer(any());
        verify(repository, never()).deleteById(any());
    }

    @Test
    void deleteById_doitLeverServiceException_quandDernierAdministrateur() {
        when(repository.findById(1L)).thenReturn(Optional.of(personnel(1L, Role.ADMINISTRATEUR)));
        when(repository.countByRole(Role.ADMINISTRATEUR)).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteById(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Impossible de supprimer le dernier administrateur.");
        verify(repository, never()).deleteById(any());
    }

    @Test
    void deleteById_doitSupprimer_quandIlResteUnAutreAdministrateur() {
        connecteEnTantQue(personnel(2L, Role.ADMINISTRATEUR));
        when(repository.findById(1L)).thenReturn(Optional.of(personnel(1L, Role.ADMINISTRATEUR)));
        when(repository.countByRole(Role.ADMINISTRATEUR)).thenReturn(2L);

        service.deleteById(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void deleteById_doitLeverNotFound_quandIntrouvable() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteById(7L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Utilisateur introuvable");
    }

    // ---------- desactiver / activer ----------

    @Test
    void desactiver_doitDesactiverLeCompteEtRevoquerLesSessions() {
        Personnel p = personnel(9L, Role.STAGIAIRE);
        when(repository.findById(9L)).thenReturn(Optional.of(p));
        when(repository.save(p)).thenReturn(p);

        assertThat(service.desactiver(9L).isActive()).isFalse();
        verify(refreshTokenService).revokeAllForUser("user9");
    }

    @Test
    void desactiver_doitLeverServiceException_quandAdministrateur() {
        when(repository.findById(1L)).thenReturn(Optional.of(personnel(1L, Role.ADMINISTRATEUR)));

        assertThatThrownBy(() -> service.desactiver(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Impossible de désactiver un administrateur.");
    }

    @Test
    void desactiver_doitLeverServiceException_quandDejaInactif() {
        Personnel p = personnel(9L, Role.STAGIAIRE);
        p.setActive(false);
        when(repository.findById(9L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.desactiver(9L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Ce compte est déjà désactivé.");
        verify(repository, never()).save(any());
    }

    @Test
    void activer_doitActiverLeCompte() {
        Personnel p = personnel(9L, Role.STAGIAIRE);
        p.setActive(false);
        when(repository.findById(9L)).thenReturn(Optional.of(p));
        when(repository.save(p)).thenReturn(p);

        Personnel resultat = service.activer(9L);

        assertThat(resultat.isActive()).isTrue();
        assertThat(resultat.isEnabled()).isTrue();
    }

    @Test
    void activer_doitLeverServiceException_quandDejaActif() {
        when(repository.findById(9L)).thenReturn(Optional.of(personnel(9L, Role.STAGIAIRE)));

        assertThatThrownBy(() -> service.activer(9L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Ce compte est déjà actif.");
        verify(repository, never()).save(any());
    }

    @Test
    void activer_doitLeverNotFound_quandIntrouvable() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activer(9L)).isInstanceOf(ServiceException.class);
    }

    // ---------- verrouiller ----------

    @Test
    void verrouiller_doitVerrouillerEtRevoquerLesSessions() {
        Personnel p = personnel(7L, Role.STAGIAIRE);
        when(repository.findById(7L)).thenReturn(Optional.of(p));
        when(repository.save(p)).thenReturn(p);

        Personnel resultat = service.verrouiller(7L);

        assertThat(resultat.isVerrouille()).isTrue();
        assertThat(resultat.isAccountNonLocked()).isFalse();
        verify(refreshTokenService).revokeAllForUser("user7");
    }

    @Test
    void verrouiller_doitLeverServiceException_quandAdministrateur() {
        when(repository.findById(1L)).thenReturn(Optional.of(personnel(1L, Role.ADMINISTRATEUR)));

        assertThatThrownBy(() -> service.verrouiller(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Impossible de verrouiller un administrateur.");
        verify(refreshTokenService, never()).revokeAllForUser(any());
    }

    @Test
    void verrouiller_doitLeverServiceException_quandDejaVerrouille() {
        Personnel p = personnel(7L, Role.STAGIAIRE);
        p.setVerrouille(true);
        when(repository.findById(7L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.verrouiller(7L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Ce compte est déjà verrouillé.");
    }

    @Test
    void verrouiller_doitLeverNotFound_quandIntrouvable() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verrouiller(7L))
                .isInstanceOf(ServiceException.class);
    }

    // ---------- deverrouiller ----------

    @Test
    void deverrouiller_doitDeverrouiller() {
        Personnel p = personnel(7L, Role.STAGIAIRE);
        p.setVerrouille(true);
        when(repository.findById(7L)).thenReturn(Optional.of(p));
        when(repository.save(p)).thenReturn(p);

        assertThat(service.deverrouiller(7L).isVerrouille()).isFalse();
    }

    @Test
    void deverrouiller_doitLeverServiceException_quandPasVerrouille() {
        when(repository.findById(7L)).thenReturn(Optional.of(personnel(7L, Role.STAGIAIRE)));

        assertThatThrownBy(() -> service.deverrouiller(7L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Ce compte n'est pas verrouillé.");
    }

    // ---------- changeRole ----------

    @Test
    void changeRole_doitChangerLeRoleEtRevoquerLesSessions() {
        Personnel p = personnel(7L, Role.STAGIAIRE);
        when(repository.findById(7L)).thenReturn(Optional.of(p));
        when(repository.save(p)).thenReturn(p);

        assertThat(service.changeRole(7L, Role.FORMATEUR).getRole()).isEqualTo(Role.FORMATEUR);
        verify(refreshTokenService).revokeAllForUser("user7");
    }

    @Test
    void changeRole_doitLeverServiceException_quandOnChangeSonPropreRole() {
        connecteEnTantQue(personnel(1L, Role.ADMINISTRATEUR));
        when(repository.findById(1L)).thenReturn(Optional.of(personnel(1L, Role.ADMINISTRATEUR)));

        assertThatThrownBy(() -> service.changeRole(1L, Role.STAGIAIRE))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Impossible de changer son propre rôle");
        verify(repository, never()).save(any());
        verify(refreshTokenService, never()).revokeAllForUser(any());
    }

    @Test
    void changeRole_doitLeverServiceException_quandDernierAdministrateur() {
        when(repository.findById(1L)).thenReturn(Optional.of(personnel(1L, Role.ADMINISTRATEUR)));
        when(repository.countByRole(Role.ADMINISTRATEUR)).thenReturn(1L);

        assertThatThrownBy(() -> service.changeRole(1L, Role.GESTIONNAIRE))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Impossible de changer le rôle du dernier administrateur.");
        verify(repository, never()).save(any());
    }

    @Test
    void changeRole_doitChanger_quandIlResteUnAutreAdministrateur() {
        connecteEnTantQue(personnel(2L, Role.ADMINISTRATEUR));
        Personnel p = personnel(1L, Role.ADMINISTRATEUR);
        when(repository.findById(1L)).thenReturn(Optional.of(p));
        when(repository.countByRole(Role.ADMINISTRATEUR)).thenReturn(2L);
        when(repository.save(p)).thenReturn(p);

        assertThat(service.changeRole(1L, Role.GESTIONNAIRE).getRole()).isEqualTo(Role.GESTIONNAIRE);
    }

    @Test
    void changeRole_doitLeverNotFound_quandIntrouvable() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeRole(7L, Role.FORMATEUR)).isInstanceOf(ServiceException.class);
    }

    // ---------- findByUsername ----------

    @Test
    void findByUsername_doitDeleguerAuRepository() {
        Personnel p = personnel(6L, Role.STAGIAIRE);
        when(repository.findByUsername("user6")).thenReturn(Optional.of(p));

        assertThat(service.findByUsername("user6")).contains(p);
    }
}
