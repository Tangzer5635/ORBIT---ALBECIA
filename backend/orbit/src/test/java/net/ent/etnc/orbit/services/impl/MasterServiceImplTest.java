package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Master;
import net.ent.etnc.orbit.repositories.MasterRepository;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MasterServiceImpl n'ajoute aucune règle : ces tests couvrent le CRUD commun d'AbstractService,
 * dont héritent tous les services.
 */
@ExtendWith(MockitoExtension.class)
class MasterServiceImplTest {

    @Mock
    private MasterRepository repository;

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

    @Test
    void create_doitEnregistrer() {
        Master m = master(null);
        when(repository.saveAndFlush(m)).thenReturn(m);

        assertThat(service.create(m)).isSameAs(m);
    }

    @Test
    void create_doitLeverServiceException_quandContrainteDeBaseViolee() {
        Master m = master(null);
        when(repository.saveAndFlush(m)).thenThrow(new DataIntegrityViolationException("uk_MASTER_nom"));

        assertThatThrownBy(() -> service.create(m))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Erreur lors de la sauvegarde");
    }

    @Test
    void update_doitReprendreLaVersionDeLExistant() {
        Master existant = master(1L);
        existant.setVersion(3L);
        Master modifie = master(1L);
        modifie.setVersionMaster("2.0");
        when(repository.findById(1L)).thenReturn(Optional.of(existant));
        when(repository.saveAndFlush(modifie)).thenReturn(modifie);

        Master resultat = service.update(modifie);

        assertThat(resultat.getVersion()).isEqualTo(3L);
        assertThat(resultat.getVersionMaster()).isEqualTo("2.0");
    }

    @Test
    void update_doitLeverNotFound_quandIntrouvable() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(master(1L)))
                .isInstanceOf(ServiceException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void update_doitLeverServiceException_quandIdNul() {
        assertThatThrownBy(() -> service.update(master(null)))
                .isInstanceOf(ServiceException.class)
                .hasMessage("L'ID de l'entité ne doit pas être nul pour une mise à jour");
    }

    @Test
    void findById_doitDeleguerAuRepository() {
        Master m = master(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(m));

        assertThat(service.findById(1L)).contains(m);
    }

    @Test
    void existsById_doitDeleguerAuRepository() {
        when(repository.existsById(1L)).thenReturn(true);

        assertThat(service.existsById(1L)).isTrue();
    }

    @Test
    void deleteById_doitDeleguerAuRepository() {
        service.deleteById(1L);

        verify(repository).deleteById(1L);
    }
}
