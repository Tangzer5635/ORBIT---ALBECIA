package net.ent.etnc.orbit.dtos.services.impl;

import net.ent.etnc.orbit.models.entities.Processus;
import net.ent.etnc.orbit.models.enums.ListeProcessus;
import net.ent.etnc.orbit.repositories.ProcessusRepository;
import net.ent.etnc.orbit.services.JournalisationService;
import net.ent.etnc.orbit.services.MaterielService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import net.ent.etnc.orbit.services.impl.ProcessusServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

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
    void deleteById_doitDesinstallerDesUcAvantDeSupprimer() {
        Processus p = processus(4L, "steam.exe");
        when(repository.findById(4L)).thenReturn(Optional.of(p));

        service.deleteById(4L);

        verify(materielService).desinstallerPartout(p);
        verify(repository).delete(p);
    }

    @Test
    void deleteById_doitLeverServiceException_quandIntrouvable() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteById(9L)).isInstanceOf(ServiceException.class);
    }
}