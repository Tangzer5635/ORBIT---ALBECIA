package net.ent.etnc.orbit.dtos.services.impl;

import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.UC;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.services.MaterielService;
import net.ent.etnc.orbit.services.PosteService;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import net.ent.etnc.orbit.services.impl.AffecterMaterielServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AffecterMaterielServiceImplTest {

    @Mock
    private MaterielService materielService;

    @Mock
    private SalleService salleService;

    @Mock
    private PosteService posteService;

    @InjectMocks
    private AffecterMaterielServiceImpl service;

    private Salle salle(Long id, TypeSalle type) {
        Salle s = new Salle();
        s.setId(id);
        s.setNumSalle("S" + id);
        s.setType(type);
        return s;
    }

    private Materiel materiel(Long id) {
        Materiel m = new UC();
        m.setId(id);
        m.setNumSerie("MAT-" + id);
        return m;
    }

    private Poste poste(Long id) {
        Poste p = new Poste();
        p.setId(id);
        p.setNumPoste("P-" + id);
        return p;
    }

    /** Matériel rangé dans une réserve, comme après creerEnStock. */
    private Salle reserveAvec(Materiel m) {
        Salle reserve = salle(3L, TypeSalle.SalleStockage);
        reserve.addMateriel(m);
        when(salleService.salleDuMateriel(m.getId())).thenReturn(Optional.of(reserve));
        return reserve;
    }

    // ---------- affecterASalle ----------

    @Test
    void affecterASalle_doitSortirLeMaterielDuStockEtLePlacerDansLaSalle() {
        Salle salle = salle(2L, TypeSalle.SalleDeCours);
        Materiel m = materiel(11L);
        Salle reserve = reserveAvec(m);
        when(salleService.findById(2L)).thenReturn(Optional.of(salle));
        when(materielService.findById(11L)).thenReturn(Optional.of(m));

        Materiel resultat = service.affecterASalle(2L, 11L);

        assertThat(resultat).isSameAs(m);
        assertThat(salle.getMateriels()).containsExactly(m);
        assertThat(reserve.getMateriels()).isEmpty();
    }

    @Test
    void affecterASalle_doitLeverNotFound_quandSalleIntrouvable() {
        when(salleService.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.affecterASalle(2L, 11L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Salle 2 introuvable");
    }

    @Test
    void affecterASalle_doitLeverServiceException_quandSalleDeStockage() {
        when(salleService.findById(3L)).thenReturn(Optional.of(salle(3L, TypeSalle.SalleStockage)));

        assertThatThrownBy(() -> service.affecterASalle(3L, 11L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("On n'affecte pas un matériel à une salle de stockage");
        verify(materielService, never()).findById(any());
    }

    @Test
    void affecterASalle_doitLeverNotFound_quandMaterielIntrouvable() {
        when(salleService.findById(2L)).thenReturn(Optional.of(salle(2L, TypeSalle.SalleDeCours)));
        when(materielService.findById(11L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.affecterASalle(2L, 11L))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void affecterASalle_doitLeverServiceException_quandMaterielArchive() {
        Materiel m = materiel(12L);
        m.setArchive(true);
        when(salleService.findById(2L)).thenReturn(Optional.of(salle(2L, TypeSalle.SalleDeCours)));
        when(materielService.findById(12L)).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> service.affecterASalle(2L, 12L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Un matériel archivé ne peut pas être affecté");
    }

    @Test
    void affecterASalle_doitLeverServiceException_quandMaterielDejaAffecte() {
        Materiel m = materiel(11L);
        Salle autreSalle = salle(1L, TypeSalle.SalleDeCours);
        autreSalle.addMateriel(m);
        when(salleService.findById(2L)).thenReturn(Optional.of(salle(2L, TypeSalle.SalleDeCours)));
        when(materielService.findById(11L)).thenReturn(Optional.of(m));
        when(salleService.salleDuMateriel(11L)).thenReturn(Optional.of(autreSalle));

        assertThatThrownBy(() -> service.affecterASalle(2L, 11L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Seul un matériel en stock peut être affecté");
        assertThat(autreSalle.getMateriels()).containsExactly(m);
    }

    @Test
    void affecterASalle_doitLeverServiceException_quandMaterielSurUnPoste() {
        Materiel m = materiel(11L);
        when(salleService.findById(2L)).thenReturn(Optional.of(salle(2L, TypeSalle.SalleDeCours)));
        when(materielService.findById(11L)).thenReturn(Optional.of(m));
        when(salleService.salleDuMateriel(11L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.affecterASalle(2L, 11L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Seul un matériel en stock peut être affecté");
    }

    // ---------- affecterAPoste ----------

    @Test
    void affecterAPoste_doitSortirLeMaterielDuStockEtLePlacerSurLePoste() {
        Poste poste = poste(1L);
        Materiel m = materiel(8L);
        Salle reserve = reserveAvec(m);
        when(posteService.findById(1L)).thenReturn(Optional.of(poste));
        when(materielService.findById(8L)).thenReturn(Optional.of(m));

        Materiel resultat = service.affecterAPoste(1L, 8L);

        assertThat(resultat).isSameAs(m);
        assertThat(poste.getMateriels()).containsExactly(m);
        assertThat(reserve.getMateriels()).isEmpty();
    }

    @Test
    void affecterAPoste_doitLeverNotFound_quandPosteIntrouvable() {
        when(posteService.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.affecterAPoste(1L, 8L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Poste 1 introuvable");
        verify(materielService, never()).findById(any());
    }

    @Test
    void affecterAPoste_doitLeverServiceException_quandMaterielArchive() {
        Materiel m = materiel(12L);
        m.setArchive(true);
        when(posteService.findById(1L)).thenReturn(Optional.of(poste(1L)));
        when(materielService.findById(12L)).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> service.affecterAPoste(1L, 12L))
                .isInstanceOf(ServiceException.class);
    }

    // ---------- retirerDeSalle ----------

    @Test
    void retirerDeSalle_doitRemettreLeMaterielDansLaReserveDuBatiment() {
        Salle salle = salle(2L, TypeSalle.SalleDeCours);
        Materiel m = materiel(11L);
        salle.addMateriel(m);
        Salle reserve = salle(3L, TypeSalle.SalleStockage);
        when(salleService.findById(2L)).thenReturn(Optional.of(salle));
        when(salleService.trouverStockage(2L)).thenReturn(reserve);

        Materiel resultat = service.retirerDeSalle(2L, 11L);

        assertThat(resultat).isSameAs(m);
        assertThat(salle.getMateriels()).isEmpty();
        assertThat(reserve.getMateriels()).containsExactly(m);
    }

    @Test
    void retirerDeSalle_doitLeverServiceException_quandSalleDeStockage() {
        when(salleService.findById(3L)).thenReturn(Optional.of(salle(3L, TypeSalle.SalleStockage)));

        assertThatThrownBy(() -> service.retirerDeSalle(3L, 11L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Ce matériel est déjà en stock");
    }

    @Test
    void retirerDeSalle_doitLeverServiceException_quandMaterielPasDansLaSalle() {
        when(salleService.findById(2L)).thenReturn(Optional.of(salle(2L, TypeSalle.SalleDeCours)));

        assertThatThrownBy(() -> service.retirerDeSalle(2L, 11L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Ce matériel n'est pas dans cette salle");
        verify(salleService, never()).trouverStockage(any());
    }

    @Test
    void retirerDeSalle_doitLeverNotFound_quandSalleIntrouvable() {
        when(salleService.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.retirerDeSalle(2L, 11L))
                .isInstanceOf(ServiceException.class);
    }

    // ---------- retirerDuPoste ----------

    @Test
    void retirerDuPoste_doitRemettreLeMaterielDansLaReserveDuBatiment() {
        Poste poste = poste(1L);
        Materiel m = materiel(8L);
        poste.addMateriel(m);
        Salle reserve = salle(3L, TypeSalle.SalleStockage);
        when(posteService.findById(1L)).thenReturn(Optional.of(poste));
        when(salleService.trouverStockageDuPoste(1L)).thenReturn(reserve);

        Materiel resultat = service.retirerDuPoste(1L, 8L);

        assertThat(resultat).isSameAs(m);
        assertThat(poste.getMateriels()).isEmpty();
        assertThat(reserve.getMateriels()).containsExactly(m);
    }

    @Test
    void retirerDuPoste_doitLeverServiceException_quandMaterielPasSurLePoste() {
        when(posteService.findById(1L)).thenReturn(Optional.of(poste(1L)));

        assertThatThrownBy(() -> service.retirerDuPoste(1L, 8L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Ce matériel n'est pas sur ce poste");
    }

    @Test
    void retirerDuPoste_doitLeverNotFound_quandPosteIntrouvable() {
        when(posteService.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.retirerDuPoste(1L, 8L))
                .isInstanceOf(ServiceException.class);
    }
}