package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.UC;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.repositories.PosteRepository;
import net.ent.etnc.orbit.repositories.projections.MonPosteLigne;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PosteServiceImplTest {

    @Mock
    private PosteRepository repository;

    @InjectMocks
    private PosteServiceImpl service;

    private Poste poste(Long id, String numero) {
        Poste p = new Poste();
        p.setId(id);
        p.setNumPoste(numero);
        return p;
    }

    // ---------- create ----------

    @Test
    void create_doitEnregistrer_quandNumeroLibre() {
        Poste p = poste(null, "P101-99");
        when(repository.existsByNumPoste("P101-99")).thenReturn(false);
        when(repository.saveAndFlush(p)).thenReturn(p);

        assertThat(service.create(p)).isSameAs(p);
    }

    @Test
    void create_doitLeverServiceException_quandNumeroDejaPris() {
        when(repository.existsByNumPoste("P101-01")).thenReturn(true);

        assertThatThrownBy(() -> service.create(poste(null, "P101-01")))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Le poste P101-01 existe déjà");
        verify(repository, never()).saveAndFlush(any());
    }

    // ---------- update ----------

    @Test
    void update_doitRenommerSansPerdreLeMateriel() {
        Poste existant = poste(1L, "P101-01");
        Materiel m = new UC();
        m.setId(1L);
        m.setNumSerie("UC-0001");
        existant.addMateriel(m);
        when(repository.findById(1L)).thenReturn(Optional.of(existant));
        when(repository.existsByNumPosteAndIdNot("P101-50", 1L)).thenReturn(false);

        Poste resultat = service.update(poste(1L, "P101-50"));

        assertThat(resultat).isSameAs(existant);
        assertThat(resultat.getNumPoste()).isEqualTo("P101-50");
        assertThat(resultat.getMateriels()).containsExactly(m);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void update_doitLeverServiceException_quandNumeroDUnAutrePoste() {
        when(repository.findById(1L)).thenReturn(Optional.of(poste(1L, "P101-01")));
        when(repository.existsByNumPosteAndIdNot("P101-02", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(poste(1L, "P101-02")))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Le poste P101-02 existe déjà");
    }

    @Test
    void update_doitLeverNotFound_quandIntrouvable() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(poste(1L, "P101-01")))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Poste 1 introuvable");
    }

    // ---------- posteDuMateriel ----------

    @Test
    void posteDuMateriel_doitDeleguerAuRepository() {
        Poste p = poste(1L, "P101-01");
        when(repository.findByMateriels_Id(8L)).thenReturn(Optional.of(p));

        assertThat(service.posteDuMateriel(8L)).contains(p);
    }

    // ---------- numéro ----------

    @Test
    void setNumPoste_doitRetirerLesEspacesAutourDuNumero() {
        assertThat(poste(null, "  P101-99 ").getNumPoste()).isEqualTo("P101-99");
    }

    // ---------- findAllAvecDetails / findByIdAvecDetails ----------

    @Test
    void findAllAvecDetails_doitChargerLesMaterielsEnUneRequete() {
        PageRequest pageable = PageRequest.of(0, 50);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(
                List.of(poste(1L, "P101-01"), poste(2L, "P101-02")), pageable, 2));

        service.findAllAvecDetails(pageable);

        verify(repository).chargerMateriels(List.of(1L, 2L));
    }

    @Test
    void findAllAvecDetails_neChargeRien_quandPageVide() {
        PageRequest pageable = PageRequest.of(0, 50);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.findAllAvecDetails(pageable);

        verify(repository, never()).chargerMateriels(any());
    }

    @Test
    void findByIdAvecDetails_doitChargerLesMateriels() {
        when(repository.findById(1L)).thenReturn(Optional.of(poste(1L, "P101-01")));

        assertThat(service.findByIdAvecDetails(1L)).isPresent();

        verify(repository).chargerMateriels(List.of(1L));
    }

    @Test
    void findByIdAvecDetails_neChargeRien_quandIntrouvable() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThat(service.findByIdAvecDetails(1L)).isEmpty();

        verify(repository, never()).chargerMateriels(any());
    }

    // ---------- lignesMonPoste ----------

    @Test
    void lignesMonPoste_doitDeleguerAuRepository() {
        List<MonPosteLigne> lignes = List.of(new MonPosteLigne(1L, "P101-01", 2L, "101", "1", 3L, "B201", null, false));
        when(repository.lignesMonPoste(6L)).thenReturn(lignes);

        assertThat(service.lignesMonPoste(6L)).isSameAs(lignes);
    }
}