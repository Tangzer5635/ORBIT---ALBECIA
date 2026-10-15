package net.ent.etnc.orbit.dtos.services.impl;

import net.ent.etnc.orbit.models.entities.Batiment;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.repositories.BatimentRepository;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import net.ent.etnc.orbit.services.impl.BatimentServiceImpl;
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
class BatimentServiceImplTest {

    @Mock
    private BatimentRepository repository;

    @Mock
    private SalleService salleService;

    @InjectMocks
    private BatimentServiceImpl service;

    private Batiment batiment() {
        Batiment b = new Batiment();
        b.setId(1L);
        b.setNumBatiment("B201");
        return b;
    }

    private Salle salle(Long id, String numero) {
        Salle s = new Salle();
        s.setId(id);
        s.setNumSalle(numero);
        s.setEtage("1");
        s.setType(TypeSalle.SalleDeCours);
        return s;
    }

    // ---------- create(batiment) ----------

    @Test
    void create_doitCreerUneSalleDeStockageAvecLeBatiment() {
        Batiment b = batiment();
        when(repository.saveAndFlush(b)).thenReturn(b);
        when(salleService.create(any(Salle.class))).thenAnswer(i -> i.getArgument(0));

        Batiment resultat = service.create(b);

        assertThat(resultat.getSalles()).hasSize(1);
        Salle stockage = resultat.getSalles().get(0);
        assertThat(stockage.getType()).isEqualTo(TypeSalle.SalleStockage);
        assertThat(stockage.getNumSalle()).isEqualTo("199");
    }

    // ---------- affecterSalle ----------

    @Test
    void affecterSalle_doitAjouterLaSalleAuBatiment() {
        Batiment b = batiment();
        Salle s = salle(6L, "999");
        when(repository.findById(1L)).thenReturn(Optional.of(b));
        when(salleService.findById(6L)).thenReturn(Optional.of(s));
        when(repository.existsBySalles_Id(6L)).thenReturn(false);

        Batiment resultat = service.affecterSalle(1L, 6L);

        assertThat(resultat.getSalles()).containsExactly(s);
    }

    @Test
    void affecterSalle_doitLeverServiceException_quandSalleDejaDansUnBatiment() {
        when(repository.findById(1L)).thenReturn(Optional.of(batiment()));
        when(salleService.findById(6L)).thenReturn(Optional.of(salle(6L, "999")));
        when(repository.existsBySalles_Id(6L)).thenReturn(true);

        assertThatThrownBy(() -> service.affecterSalle(1L, 6L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("La salle est déjà affectée à un bâtiment");
    }

    @Test
    void affecterSalle_doitLeverServiceException_quandNumeroDejaPrisDansLeBatiment() {
        Batiment b = batiment();
        b.addSalle(salle(1L, "101"));
        when(repository.findById(1L)).thenReturn(Optional.of(b));
        when(salleService.findById(6L)).thenReturn(Optional.of(salle(6L, "101")));
        when(repository.existsBySalles_Id(6L)).thenReturn(false);

        assertThatThrownBy(() -> service.affecterSalle(1L, 6L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Une salle 101 existe déjà dans ce bâtiment");
    }

    @Test
    void affecterSalle_doitLeverNotFound_quandSalleIntrouvable() {
        when(repository.findById(1L)).thenReturn(Optional.of(batiment()));
        when(salleService.findById(6L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.affecterSalle(1L, 6L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Salle 6 introuvable");
    }

    @Test
    void affecterSalle_doitLeverNotFound_quandBatimentIntrouvable() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.affecterSalle(1L, 6L))
                .isInstanceOf(ServiceException.class);
        verify(salleService, never()).findById(any());
    }

    // ---------- retirerSalle ----------

    @Test
    void retirerSalle_doitRetirerLaSalleDuBatiment() {
        Batiment b = batiment();
        Salle s = salle(6L, "999");
        b.addSalle(s);
        when(repository.findById(1L)).thenReturn(Optional.of(b));

        service.retirerSalle(1L, 6L);

        assertThat(b.getSalles()).isEmpty();
    }

    @Test
    void retirerSalle_doitLeverServiceException_quandSalleHorsDuBatiment() {
        Batiment b = batiment();
        b.addSalle(salle(1L, "101"));
        when(repository.findById(1L)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.retirerSalle(1L, 6L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Cette salle n'appartient pas à ce bâtiment");
        assertThat(b.getSalles()).hasSize(1);
    }

    @Test
    void retirerSalle_doitRefuser_quandSalleDeStockage() {
        Batiment b = batiment();
        Salle stockage = salle(6L, "199");
        stockage.setType(TypeSalle.SalleStockage);
        b.addSalle(stockage);
        when(repository.findById(1L)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.retirerSalle(1L, 6L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Une salle de stockage ne peut disparaître qu'avec la suppression de son bâtiment");
        assertThat(b.getSalles()).hasSize(1);
    }

    @Test
    void retirerSalle_doitLeverNotFound_quandBatimentIntrouvable() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.retirerSalle(1L, 6L))
                .isInstanceOf(ServiceException.class);
    }
}