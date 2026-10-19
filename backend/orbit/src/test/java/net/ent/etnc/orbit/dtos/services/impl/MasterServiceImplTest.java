package net.ent.etnc.orbit.dtos.services.impl;

import net.ent.etnc.orbit.models.entities.Master;
import net.ent.etnc.orbit.models.entities.Processus;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.ListeProcessus;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.repositories.MasterRepository;
import net.ent.etnc.orbit.services.JournalisationService;
import net.ent.etnc.orbit.services.MaterielService;
import net.ent.etnc.orbit.services.PosteService;
import net.ent.etnc.orbit.services.ProcessusService;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import net.ent.etnc.orbit.services.impl.MasterServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MasterServiceImplTest {

    @Mock
    private MasterRepository repository;

    @Mock
    private ProcessusService processusService;

    @Mock
    private SalleService salleService;

    @Mock
    private MaterielService materielService;

    @Mock
    private PosteService posteService;

    /** Mock : les tests de ce service ne vérifient pas le journal. */
    @Mock
    private JournalisationService journalisationService;

    @InjectMocks
    private MasterServiceImpl service;

    private Master master(Long id) {
        Master m = new Master();
        m.setId(id);
        m.setNom("Master Dev");
        m.setVersionMaster("1.0");
        m.setDateCreation(LocalDateTime.now());
        return m;
    }

    private Processus processus(Long id, String nom, ListeProcessus liste) {
        Processus p = new Processus();
        p.setId(id);
        p.setNom(nom);
        p.setListe(liste);
        return p;
    }

    private Salle salle(Long id, boolean gelee) {
        Salle s = new Salle();
        s.setId(id);
        s.setNumSalle("101");
        s.setEtage("1");
        s.setType(TypeSalle.SalleDeCours);
        s.setSuspendu(gelee);
        return s;
    }

    // ---------- CRUD commun (AbstractService) ----------

    @Test
    void create_doitLeverServiceException_quandContrainteDeBaseViolee() {
        Master m = master(null);
        when(repository.saveAndFlush(m)).thenThrow(new DataIntegrityViolationException("uk_MASTER_nom"));

        assertThatThrownBy(() -> service.create(m))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Erreur lors de la sauvegarde");
    }

    @Test
    void findById_doitDeleguerAuRepository() {
        Master m = master(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(m));

        assertThat(service.findById(1L)).contains(m);
    }

    // ---------- creer ----------

    @Test
    void creer_doitAjouterLesProcessusEnListeBlanche() {
        Master m = master(null);
        Processus chrome = processus(1L, "chrome.exe", ListeProcessus.BLANCHE);
        when(processusService.findById(1L)).thenReturn(Optional.of(chrome));
        when(repository.saveAndFlush(m)).thenReturn(m);

        Master cree = service.creer(m, List.of(1L));

        assertThat(cree.getProcessus()).containsExactly(chrome);
    }

    @Test
    void creer_doitRefuserUnProcessusEnListeNoire() {
        when(processusService.findById(2L)).thenReturn(Optional.of(processus(2L, "steam.exe", ListeProcessus.NOIRE)));

        assertThatThrownBy(() -> service.creer(master(null), List.of(2L)))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("liste noire");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void creer_doitRefuserUnNomDejaPris() {
        when(repository.existsByNom("Master Dev")).thenReturn(true);

        assertThatThrownBy(() -> service.creer(master(null), List.of()))
                .isInstanceOf(ServiceException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    // ---------- plaquer ----------

    @Test
    void plaquer_doitPoserLeMasterEtInstallerSesProcessus() {
        Master m = master(1L);
        Salle s = salle(5L, false);
        when(repository.findById(1L)).thenReturn(Optional.of(m));
        when(salleService.findById(5L)).thenReturn(Optional.of(s));

        service.plaquer(5L, 1L);

        assertThat(s.getMaster()).isSameAs(m);
        verify(materielService).installerImage(5L, m.getProcessus());
    }

    @Test
    void plaquer_doitRefuserUneSalleGelee() {
        when(repository.findById(1L)).thenReturn(Optional.of(master(1L)));
        when(salleService.findById(5L)).thenReturn(Optional.of(salle(5L, true)));

        assertThatThrownBy(() -> service.plaquer(5L, 1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("gelée");
        verify(materielService, never()).installerImage(anyLong(), any());
    }

    // ---------- deleteById ----------

    @Test
    void deleteById_doitRetirerLeMasterEtSesProcessusDesSalles() {
        Master m = master(1L);
        Salle s = salle(5L, false);
        s.setMaster(m);
        when(repository.findById(1L)).thenReturn(Optional.of(m));
        when(salleService.sallesDuMaster(1L)).thenReturn(List.of(s));

        service.deleteById(1L);

        assertThat(s.getMaster()).isNull();
        verify(materielService).installerImage(5L, List.of());
        verify(repository).delete(m);
    }

    @Test
    void deleteById_doitLeverServiceException_quandIntrouvable() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteById(1L)).isInstanceOf(ServiceException.class);
    }
}