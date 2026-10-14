package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Batiment;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.repositories.BatimentRepository;
import net.ent.etnc.orbit.repositories.SalleRepository;
import net.ent.etnc.orbit.services.PosteService;
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
class SalleServiceImplTest {

    @Mock
    private SalleRepository repository;

    @Mock
    private PosteService posteService;

    @Mock
    private BatimentRepository batimentRepository;

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
                .isInstanceOf(ServiceException.class)
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
                .isInstanceOf(ServiceException.class)
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
    void deleteById_doitRefuser_quandSalleDeStockage() {
        Salle reserve = salle(3L, TypeSalle.SalleStockage);
        when(repository.findById(3L)).thenReturn(Optional.of(reserve));

        assertThatThrownBy(() -> service.deleteById(3L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Une salle de stockage ne peut disparaître qu'avec la suppression de son bâtiment");
        verify(repository, never()).delete(any());
    }

    // ---------- supprimerAvecBatiment ----------

    @Test
    void supprimerAvecBatiment_doitSupprimerLaReserveVide() {
        Salle reserve = salle(3L, TypeSalle.SalleStockage);
        when(repository.findById(3L)).thenReturn(Optional.of(reserve));

        service.supprimerAvecBatiment(3L);

        verify(repository).delete(reserve);
    }

    @Test
    void supprimerAvecBatiment_doitLeverServiceException_quandReserveNonVide() {
        Salle reserve = salle(3L, TypeSalle.SalleStockage);
        reserve.addMateriel(materiel(9L));
        when(repository.findById(3L)).thenReturn(Optional.of(reserve));

        assertThatThrownBy(() -> service.supprimerAvecBatiment(3L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Vide la salle de stockage avant de la supprimer");
        verify(repository, never()).delete(any());
    }

    @Test
    void deleteById_doitLeverNotFound_quandIntrouvable() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteById(1L))
                .isInstanceOf(ServiceException.class);
    }

    // ---------- create(idBatiment, salle) ----------

    private Batiment batiment() {
        Batiment b = new Batiment();
        b.setId(1L);
        b.setNumBatiment("B201");
        return b;
    }

    @Test
    void create_doitCreerLaSalleEtLAjouterAuBatiment() {
        Batiment b = batiment();
        b.addSalle(salle(1L, TypeSalle.SalleDeCours));
        Salle nouvelle = salle(null, TypeSalle.SalleDeCours);
        nouvelle.setNumSalle("102");
        Salle creee = salle(2L, TypeSalle.SalleDeCours);
        creee.setNumSalle("102");
        when(batimentRepository.findById(1L)).thenReturn(Optional.of(b));
        when(repository.saveAndFlush(nouvelle)).thenReturn(creee);

        Salle resultat = service.create(1L, nouvelle);

        assertThat(resultat).isSameAs(creee);
        assertThat(b.getSalles()).contains(creee).hasSize(2);
    }

    @Test
    void create_doitLeverServiceException_quandNumeroDejaPrisDansLeBatiment() {
        Batiment b = batiment();
        Salle existante = salle(1L, TypeSalle.SalleDeCours);
        existante.setNumSalle("101");
        b.addSalle(existante);
        Salle doublon = salle(null, TypeSalle.SalleDeCours);
        doublon.setNumSalle("101");
        when(batimentRepository.findById(1L)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.create(1L, doublon))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Une salle 101 existe déjà dans ce bâtiment");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void create_doitLeverServiceException_quandBatimentIntrouvable() {
        when(batimentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(1L, salle(null, TypeSalle.SalleDeCours)))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Bâtiment 1 introuvable");
    }

    // ---------- findAllAvecDetails / findByIdAvecDetails ----------

    @Test
    void findAllAvecDetails_doitChargerMaterielsEtPostesEnUneRequeteChacun() {
        PageRequest pageable = PageRequest.of(0, 30);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(
                List.of(salle(1L, TypeSalle.SalleDeCours), salle(2L, TypeSalle.SalleStockage)), pageable, 2));

        service.findAllAvecDetails(pageable);

        verify(repository).chargerMateriels(List.of(1L, 2L));
        verify(repository).chargerPostes(List.of(1L, 2L));
    }

    @Test
    void findAllAvecDetails_neChargeRien_quandPageVide() {
        PageRequest pageable = PageRequest.of(0, 30);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.findAllAvecDetails(pageable);

        verify(repository, never()).chargerMateriels(any());
        verify(repository, never()).chargerPostes(any());
    }

    @Test
    void findByIdAvecDetails_doitChargerMaterielsEtPostes() {
        when(repository.findById(1L)).thenReturn(Optional.of(salle(1L, TypeSalle.SalleDeCours)));

        assertThat(service.findByIdAvecDetails(1L)).isPresent();

        verify(repository).chargerMateriels(List.of(1L));
        verify(repository).chargerPostes(List.of(1L));
    }

    @Test
    void findByIdAvecDetails_neChargeRien_quandIntrouvable() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThat(service.findByIdAvecDetails(1L)).isEmpty();

        verify(repository, never()).chargerMateriels(any());
    }

    // ---------- trouverStockage ----------

    @Test
    void trouverStockage_doitPrendreLaReserveDuBatiment() {
        Salle reserve = salle(3L, TypeSalle.SalleStockage);
        when(repository.findSallesDuBatiment(1L, TypeSalle.SalleStockage)).thenReturn(List.of(reserve, salle(5L, TypeSalle.SalleStockage)));

        assertThat(service.trouverStockage(1L)).isSameAs(reserve);
    }

    @Test
    void trouverStockage_doitLeverServiceException_quandAucuneReserve() {
        when(repository.findSallesDuBatiment(1L, TypeSalle.SalleStockage)).thenReturn(List.of());

        assertThatThrownBy(() -> service.trouverStockage(1L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Aucune salle de stockage dans le bâtiment");
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
