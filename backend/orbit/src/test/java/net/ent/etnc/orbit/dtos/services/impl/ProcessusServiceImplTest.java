package net.ent.etnc.orbit.dtos.services.impl;

import net.ent.etnc.orbit.models.entities.Master;
import net.ent.etnc.orbit.models.entities.Processus;
import net.ent.etnc.orbit.models.enums.ListeProcessus;
import net.ent.etnc.orbit.repositories.ProcessusRepository;
import net.ent.etnc.orbit.services.JournalisationService;
import net.ent.etnc.orbit.services.MasterService;
import net.ent.etnc.orbit.services.MaterielService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import net.ent.etnc.orbit.services.impl.ProcessusServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessusServiceImplTest {

    @Mock
    private ProcessusRepository repository;

    @Mock
    private MaterielService materielService;

    @Mock
    private MasterService masterService;

    /** Mock : les tests de ce service ne vérifient pas le journal. */
    @Mock
    private JournalisationService journalisationService;

    @InjectMocks
    private ProcessusServiceImpl service;

    private Processus processus(Long id, String nom) {
        Processus p = new Processus();
        p.setId(id);
        p.setNom(nom);
        p.setListe(ListeProcessus.BLANCHE);
        return p;
    }

    @Test
    void create_doitLeverServiceException_quandLeNomExisteDeja() {
        when(repository.existsByNom("chrome.exe")).thenReturn(true);

        assertThatThrownBy(() -> service.create(processus(null, "chrome.exe")))
                .isInstanceOf(ServiceException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deleteById_doitRetirerDesMastersEtDesinstallerDesUcAvantDeSupprimer() {
        Processus p = processus(4L, "steam.exe");
        when(repository.findById(4L)).thenReturn(Optional.of(p));

        service.deleteById(4L);

        verify(masterService).retirerProcessusPartout(p);
        verify(materielService).desinstallerPartout(p);
        verify(repository).delete(p);
    }

    @Test
    void changerListe_doitRefuserLaListeNoire_quandLeProcessusEstDansUnMaster() {
        Processus p = processus(1L, "chrome.exe");
        Master m = new Master();
        m.setNom("Dev 2026");
        when(repository.findById(1L)).thenReturn(Optional.of(p));
        when(masterService.mastersAvec(1L)).thenReturn(List.of(m));

        assertThatThrownBy(() -> service.changerListe(1L, ListeProcessus.NOIRE))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Dev 2026");
        assertThat(p.getListe()).isEqualTo(ListeProcessus.BLANCHE);
        verify(materielService, never()).desinstallerPartout(any());
    }

    @Test
    void changerListe_doitDesinstallerDesUc_quandPasseEnNoire() {
        Processus p = processus(1L, "steam.exe");
        when(repository.findById(1L)).thenReturn(Optional.of(p));
        when(masterService.mastersAvec(1L)).thenReturn(List.of());

        service.changerListe(1L, ListeProcessus.NOIRE);

        assertThat(p.getListe()).isEqualTo(ListeProcessus.NOIRE);
        verify(materielService).desinstallerPartout(p);
    }

    @Test
    void changerListe_doitPasserEnBlancheSansRienDesinstaller() {
        Processus p = processus(1L, "steam.exe");
        p.setListe(ListeProcessus.NOIRE);
        when(repository.findById(1L)).thenReturn(Optional.of(p));

        service.changerListe(1L, ListeProcessus.BLANCHE);

        assertThat(p.getListe()).isEqualTo(ListeProcessus.BLANCHE);
        verify(masterService, never()).mastersAvec(any());
        verify(materielService, never()).desinstallerPartout(any());
    }

    @Test
    void deleteById_doitLeverServiceException_quandIntrouvable() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteById(9L)).isInstanceOf(ServiceException.class);
    }
}