package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.repositories.SalleRepository;
import net.ent.etnc.orbit.services.PosteService;
import net.ent.etnc.orbit.services.commons.NotFoundException;
import net.ent.etnc.orbit.services.commons.ServiceException;
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
class SalleServiceImplTest {

    @Mock
    private SalleRepository repository;

    @Mock
    private PosteService posteService;

    @InjectMocks
    private SalleServiceImpl service;

    private Salle salle(Long id, TypeSalle type) {
        Salle s = new Salle();
        s.setId(id);
        s.setNumSalle("S" + id);
        s.setEtage("1");
        s.setType(type);
        return s;
    }

    private Poste poste(Long id) {
        Poste p = new Poste();
        p.setId(id);
        p.setNumPoste("P-" + id);
        return p;
    }

    private Materiel materiel(Long id) {
        Materiel m = new Materiel();
        m.setId(id);
        m.setNumSerie("MAT-" + id);
        return m;
    }

    // ---------- creerPoste ----------

    @Test
    void creerPoste_doitCreerLePosteEtLAjouterALaSalle() {
        Salle salle = salle(1L, TypeSalle.SalleDeCours);
        Poste nouveau = poste(null);
        Poste cree = poste(5L);
        when(repository.findById(1L)).thenReturn(Optional.of(salle));
        when(posteService.create(nouveau)).thenReturn(cree);

        Poste resultat = service.creerPoste(1L, nouveau);

        assertThat(resultat).isSameAs(cree);
        assertThat(salle.getPostes()).containsExactly(cree);
    }

    @Test
    void creerPoste_doitLeverServiceException_quandSalleDeStockage() {
        when(repository.findById(3L)).thenReturn(Optional.of(salle(3L, TypeSalle.SalleStockage)));

        assertThatThrownBy(() -> service.creerPoste(3L, poste(null)))
                .isInstanceOf(ServiceException.class)
                .hasMessage("On ne crée pas de poste dans une salle de stockage");
        verify(posteService, never()).create(any());
    }

    @Test
    void creerPoste_doitLeverNotFound_quandSalleIntrouvable() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.creerPoste(1L, poste(null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Salle 1 introuvable");
    }

    // ---------- supprimerPoste ----------

    @Test
    void supprimerPoste_doitRemettreLeMaterielEnReserveEtSupprimer() {
        Poste poste = poste(5L);
        Materiel m = materiel(8L);
        poste.addMateriel(m);
        Salle salle = salle(1L, TypeSalle.SalleDeCours);
        salle.addPoste(poste);
        Salle reserve = salle(3L, TypeSalle.SalleStockage);
        when(posteService.findById(5L)).thenReturn(Optional.of(poste));
        when(repository.findSallesDuBatimentDuPoste(5L, TypeSalle.SalleStockage)).thenReturn(List.of(reserve));
        when(repository.findByPostes_Id(5L)).thenReturn(Optional.of(salle));

        service.supprimerPoste(5L);

        assertThat(poste.getMateriels()).isEmpty();
        assertThat(reserve.getMateriels()).containsExactly(m);
        assertThat(salle.getPostes()).isEmpty();
        verify(posteService).delete(poste);
    }

    @Test
    void supprimerPoste_doitSupprimerSansChercherDeReserve_quandPosteVide() {
        Poste poste = poste(5L);
        when(posteService.findById(5L)).thenReturn(Optional.of(poste));
        when(repository.findByPostes_Id(5L)).thenReturn(Optional.empty());

        service.supprimerPoste(5L);

        verify(repository, never()).findSallesDuBatimentDuPoste(any(), any());
        verify(posteService).delete(poste);
    }

    @Test
    void supprimerPoste_doitLeverNotFound_quandIntrouvable() {
        when(posteService.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.supprimerPoste(5L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Poste 5 introuvable");
        verify(posteService, never()).delete(any());
    }

    // ---------- deleteById ----------

    @Test
    void deleteById_doitRangerTousLesMaterielsEtSupprimerLesPostes() {
        Salle salle = salle(1L, TypeSalle.SalleDeCours);
        Materiel tbi = materiel(6L);
        salle.addMateriel(tbi);
        Poste poste = poste(5L);
        Materiel uc = materiel(1L);
        poste.addMateriel(uc);
        salle.addPoste(poste);
        Salle reserve = salle(3L, TypeSalle.SalleStockage);
        when(repository.findById(1L)).thenReturn(Optional.of(salle));
        when(repository.findSallesDuBatiment(1L, TypeSalle.SalleStockage)).thenReturn(List.of(reserve));

        service.deleteById(1L);

        assertThat(reserve.getMateriels()).containsExactlyInAnyOrder(tbi, uc);
        assertThat(salle.getMateriels()).isEmpty();
        assertThat(salle.getPostes()).isEmpty();
        verify(posteService).delete(poste);
        verify(repository).delete(salle);
    }

    @Test
    void deleteById_doitSupprimerSansChercherDeReserve_quandSalleVide() {
        Salle salle = salle(1L, TypeSalle.SalleDeCours);
        when(repository.findById(1L)).thenReturn(Optional.of(salle));

        service.deleteById(1L);

        verify(repository, never()).findSallesDuBatiment(any(), any());
        verify(repository).delete(salle);
    }

    @Test
    void deleteById_doitLeverServiceException_quandReserveNonVide() {
        Salle reserve = salle(3L, TypeSalle.SalleStockage);
        reserve.addMateriel(materiel(9L));
        when(repository.findById(3L)).thenReturn(Optional.of(reserve));

        assertThatThrownBy(() -> service.deleteById(3L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Vide la salle de stockage avant de la supprimer");
        verify(repository, never()).delete(any());
    }

    @Test
    void deleteById_doitLeverNotFound_quandIntrouvable() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteById(1L))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- trouverStockage ----------

    @Test
    void trouverStockage_doitPrendreLaReserveDuBatiment() {
        Salle reserve = salle(3L, TypeSalle.SalleStockage);
        when(repository.findSallesDuBatiment(1L, TypeSalle.SalleStockage)).thenReturn(List.of(reserve, salle(5L, TypeSalle.SalleStockage)));

        assertThat(service.trouverStockage(1L)).isSameAs(reserve);
        verify(repository, never()).findFirstByTypeOrderByIdAsc(any());
    }

    @Test
    void trouverStockage_doitPrendreLaPremiereReserve_quandAucuneDansLeBatiment() {
        Salle reserveGlobale = salle(6L, TypeSalle.SalleStockage);
        when(repository.findSallesDuBatiment(1L, TypeSalle.SalleStockage)).thenReturn(List.of());
        when(repository.findFirstByTypeOrderByIdAsc(TypeSalle.SalleStockage)).thenReturn(Optional.of(reserveGlobale));

        assertThat(service.trouverStockage(1L)).isSameAs(reserveGlobale);
    }

    @Test
    void trouverStockage_doitLeverServiceException_quandAucuneReserve() {
        when(repository.findSallesDuBatiment(1L, TypeSalle.SalleStockage)).thenReturn(List.of());
        when(repository.findFirstByTypeOrderByIdAsc(TypeSalle.SalleStockage)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.trouverStockage(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Aucune salle de stockage n'existe");
    }

    @Test
    void trouverStockageDuPoste_doitPrendreLaReserveDuBatimentDuPoste() {
        Salle reserve = salle(3L, TypeSalle.SalleStockage);
        when(repository.findSallesDuBatimentDuPoste(5L, TypeSalle.SalleStockage)).thenReturn(List.of(reserve));

        assertThat(service.trouverStockageDuPoste(5L)).isSameAs(reserve);
    }

    // ---------- requêtes ----------

    @Test
    void estEnStock_doitChercherDansLesSallesDeStockage() {
        when(repository.existsByTypeAndMateriels_Id(TypeSalle.SalleStockage, 9L)).thenReturn(true);

        assertThat(service.estEnStock(9L)).isTrue();
    }
}
