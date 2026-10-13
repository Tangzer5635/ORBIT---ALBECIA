package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.EtatMateriel;
import net.ent.etnc.orbit.models.enums.TypeMateriel;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.repositories.MaterielRepository;
import net.ent.etnc.orbit.services.DefaillanceService;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.commons.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterielServiceImplTest {

    @Mock
    private MaterielRepository repository;

    @Mock
    private SalleService salleService;

    @Mock
    private DefaillanceService defaillanceService;

    @InjectMocks
    private MaterielServiceImpl service;

    private Materiel materiel(Long id, LocalDate finGarantie) {
        Materiel m = new Materiel();
        m.setId(id);
        m.setNumSerie("UC-" + id);
        m.setModele("Dell OptiPlex 7010");
        m.setType(TypeMateriel.UC);
        m.setDateAcquisition(finGarantie.minusYears(3));
        m.setDateFinGarantie(finGarantie);
        return m;
    }

    private Materiel materielSousGarantie(Long id) {
        return materiel(id, LocalDate.now().plusYears(2));
    }

    private Salle salle(Long id, TypeSalle type) {
        Salle s = new Salle();
        s.setId(id);
        s.setNumSalle("S" + id);
        s.setType(type);
        return s;
    }

    // ---------- creerEnStock ----------

    @Test
    void creerEnStock_doitEnregistrerEtRangerDansLaReserve() {
        Materiel m = materielSousGarantie(null);
        m.setArchive(true);
        Salle reserve = salle(3L, TypeSalle.SalleStockage);
        when(repository.existsByNumSerie(m.getNumSerie())).thenReturn(false);
        when(salleService.findById(3L)).thenReturn(Optional.of(reserve));
        when(repository.save(m)).thenReturn(m);

        Materiel resultat = service.creerEnStock(m, 3L);

        assertThat(resultat).isSameAs(m);
        assertThat(resultat.isArchive()).isFalse();
        assertThat(reserve.getMateriels()).containsExactly(m);
    }

    @Test
    void creerEnStock_doitLeverServiceException_quandReserveNonFournie() {
        assertThatThrownBy(() -> service.creerEnStock(materielSousGarantie(null), null))
                .isInstanceOf(ServiceException.class)
                .hasMessage("La salle de stockage est obligatoire");
        verifyNoInteractions(repository);
    }

    @Test
    void creerEnStock_doitLeverServiceException_quandNumeroDeSerieDejaPris() {
        Materiel m = materielSousGarantie(null);
        when(repository.existsByNumSerie(m.getNumSerie())).thenReturn(true);

        assertThatThrownBy(() -> service.creerEnStock(m, 3L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("existe déjà");
        verify(repository, never()).save(any());
    }

    @Test
    void creerEnStock_doitLeverNotFound_quandReserveIntrouvable() {
        Materiel m = materielSousGarantie(null);
        when(repository.existsByNumSerie(m.getNumSerie())).thenReturn(false);
        when(salleService.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.creerEnStock(m, 3L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void creerEnStock_doitLeverServiceException_quandSalleDeCours() {
        Materiel m = materielSousGarantie(null);
        when(repository.existsByNumSerie(m.getNumSerie())).thenReturn(false);
        when(salleService.findById(1L)).thenReturn(Optional.of(salle(1L, TypeSalle.SalleDeCours)));

        assertThatThrownBy(() -> service.creerEnStock(m, 1L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Un matériel se crée dans une salle de stockage");
        verify(repository, never()).save(any());
    }

    // ---------- update ----------

    @Test
    void update_doitCopierLesChampsSurLExistant() {
        Materiel existant = materielSousGarantie(1L);
        Materiel modifie = materielSousGarantie(1L);
        modifie.setModele("HP EliteDesk 805");
        when(repository.findById(1L)).thenReturn(Optional.of(existant));
        when(repository.existsByNumSerieAndIdNot(modifie.getNumSerie(), 1L)).thenReturn(false);
        when(repository.save(existant)).thenReturn(existant);

        Materiel resultat = service.update(modifie);

        assertThat(resultat).isSameAs(existant);
        assertThat(existant.getModele()).isEqualTo("HP EliteDesk 805");
    }

    @Test
    void update_doitLeverServiceException_quandArchive() {
        Materiel existant = materielSousGarantie(1L);
        existant.setArchive(true);
        when(repository.findById(1L)).thenReturn(Optional.of(existant));

        assertThatThrownBy(() -> service.update(materielSousGarantie(1L)))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Un matériel archivé ne peut pas être modifié");
    }

    @Test
    void update_doitLeverServiceException_quandNumeroDeSerieDUnAutreMateriel() {
        Materiel modifie = materielSousGarantie(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(materielSousGarantie(1L)));
        when(repository.existsByNumSerieAndIdNot(modifie.getNumSerie(), 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(modifie))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("existe déjà");
        verify(repository, never()).save(any());
    }

    @Test
    void update_doitLeverNotFound_quandIntrouvable() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(materielSousGarantie(1L)))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- deleteById ----------

    @Test
    void deleteById_doitSupprimerEtSortirDeLaReserve_quandEnStock() {
        Materiel m = materielSousGarantie(9L);
        Salle reserve = salle(3L, TypeSalle.SalleStockage);
        reserve.addMateriel(m);
        when(repository.findById(9L)).thenReturn(Optional.of(m));
        when(defaillanceService.aDesDefaillances(9L)).thenReturn(false);
        when(salleService.estEnStock(9L)).thenReturn(true);
        when(salleService.salleDuMateriel(9L)).thenReturn(Optional.of(reserve));

        service.deleteById(9L);

        assertThat(reserve.getMateriels()).isEmpty();
        verify(repository).delete(m);
    }

    @Test
    void deleteById_doitSupprimer_quandArchive() {
        Materiel m = materielSousGarantie(12L);
        m.setArchive(true);
        when(repository.findById(12L)).thenReturn(Optional.of(m));
        when(defaillanceService.aDesDefaillances(12L)).thenReturn(false);
        when(salleService.salleDuMateriel(12L)).thenReturn(Optional.empty());

        service.deleteById(12L);

        verify(repository).delete(m);
        verify(salleService, never()).estEnStock(any());
    }

    @Test
    void deleteById_doitLeverServiceException_quandHistoriqueDeDefaillances() {
        when(repository.findById(1L)).thenReturn(Optional.of(materielSousGarantie(1L)));
        when(defaillanceService.aDesDefaillances(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteById(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("archive-le");
        verify(repository, never()).delete(any());
    }

    @Test
    void deleteById_doitLeverServiceException_quandAffecte() {
        when(repository.findById(1L)).thenReturn(Optional.of(materielSousGarantie(1L)));
        when(defaillanceService.aDesDefaillances(1L)).thenReturn(false);
        when(salleService.estEnStock(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteById(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("remets-le en stock");
        verify(repository, never()).delete(any());
    }

    @Test
    void deleteById_doitLeverNotFound_quandIntrouvable() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteById(1L))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- archiver ----------

    @Test
    void archiver_doitArchiverEtSortirDeLaReserve() {
        Materiel m = materielSousGarantie(9L);
        Salle reserve = salle(3L, TypeSalle.SalleStockage);
        reserve.addMateriel(m);
        when(repository.findById(9L)).thenReturn(Optional.of(m));
        when(salleService.salleDuMateriel(9L)).thenReturn(Optional.of(reserve));

        Materiel resultat = service.archiver(9L);

        assertThat(resultat.isArchive()).isTrue();
        assertThat(resultat.getDateArchivage()).isEqualTo(LocalDate.now());
        assertThat(reserve.getMateriels()).isEmpty();
    }

    @Test
    void archiver_doitLeverServiceException_quandDejaArchive() {
        Materiel m = materielSousGarantie(12L);
        m.setArchive(true);
        when(repository.findById(12L)).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> service.archiver(12L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Ce matériel est déjà archivé");
    }

    @Test
    void archiver_doitLeverServiceException_quandDansUneSalleDeCours() {
        Materiel m = materielSousGarantie(6L);
        when(repository.findById(6L)).thenReturn(Optional.of(m));
        when(salleService.salleDuMateriel(6L)).thenReturn(Optional.of(salle(1L, TypeSalle.SalleDeCours)));

        assertThatThrownBy(() -> service.archiver(6L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("remets-le en stock");
        assertThat(m.isArchive()).isFalse();
    }

    @Test
    void archiver_doitLeverServiceException_quandSurUnPoste() {
        when(repository.findById(1L)).thenReturn(Optional.of(materielSousGarantie(1L)));
        when(salleService.salleDuMateriel(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.archiver(1L))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void archiver_doitLeverNotFound_quandIntrouvable() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.archiver(1L))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- etatDe ----------

    @Test
    void etatDe_doitRenvoyerArchive_sansInterrogerStockNiDefaillances() {
        Materiel m = materielSousGarantie(12L);
        m.setArchive(true);

        assertThat(service.etatDe(m)).isEqualTo(EtatMateriel.ARCHIVE);
        verifyNoInteractions(salleService, defaillanceService);
    }

    @Test
    void etatDe_doitRenvoyerDefectueux_quandDefaillanceOuverte() {
        // prioritaire sur A_REMPLACER : garantie expirée ET défaillance ouverte
        Materiel m = materiel(5L, LocalDate.now().minusYears(1));
        when(salleService.estEnStock(5L)).thenReturn(false);
        when(defaillanceService.aUneDefaillanceOuverte(5L)).thenReturn(true);

        assertThat(service.etatDe(m)).isEqualTo(EtatMateriel.DEFECTUEUX);
    }

    @Test
    void etatDe_doitRenvoyerARemplacer_quandAcquisIlYaPlusDe2Ans() {
        Materiel m = materielSousGarantie(5L);
        m.setDateAcquisition(LocalDate.now().minusYears(2).minusDays(1));
        when(salleService.estEnStock(5L)).thenReturn(false);
        when(defaillanceService.aUneDefaillanceOuverte(5L)).thenReturn(false);

        assertThat(service.etatDe(m)).isEqualTo(EtatMateriel.A_REMPLACER);
    }

    @Test
    void etatDe_doitRenvoyerNormal_quandAcquisIlYaPile2Ans() {
        Materiel m = materielSousGarantie(5L);
        m.setDateAcquisition(LocalDate.now().minusYears(2));
        when(salleService.estEnStock(5L)).thenReturn(false);
        when(defaillanceService.aUneDefaillanceOuverte(5L)).thenReturn(false);

        assertThat(service.etatDe(m)).isEqualTo(EtatMateriel.NORMAL);
    }

    @Test
    void etatDe_doitRenvoyerDisponible_quandEnStock() {
        Materiel m = materielSousGarantie(9L);
        when(salleService.estEnStock(9L)).thenReturn(true);
        when(defaillanceService.aUneDefaillanceOuverte(9L)).thenReturn(false);

        assertThat(service.etatDe(m)).isEqualTo(EtatMateriel.DISPONIBLE);
    }

    @Test
    void etatDe_doitRenvoyerNormal_quandAffecteSousGarantie() {
        Materiel m = materielSousGarantie(1L);
        when(salleService.estEnStock(1L)).thenReturn(false);
        when(defaillanceService.aUneDefaillanceOuverte(1L)).thenReturn(false);

        assertThat(service.etatDe(m)).isEqualTo(EtatMateriel.NORMAL);
    }
}
