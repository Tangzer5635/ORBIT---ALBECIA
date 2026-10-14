package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.entities.Stage;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.repositories.projections.SalleDuStage;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.StageService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AffecterStageServiceImplTest {

    @Mock
    private StageService stageService;

    @Mock
    private SalleService salleService;

    @InjectMocks
    private AffecterStageServiceImpl service;

    private static Salle salle(Long id, TypeSalle type) {
        Salle s = new Salle();
        s.setId(id);
        s.setNumSalle("S" + id);
        s.setType(type);
        return s;
    }

    private static Stage stage(Long id, String nom, LocalDate fin) {
        Stage s = new Stage();
        s.setId(id);
        s.setNom(nom);
        s.setDateDebut(LocalDate.now().minusMonths(1));
        s.setDateFin(fin);
        return s;
    }

    // ---------- affecterASalle ----------

    @Test
    void affecterASalle_doitAffecterLeStage_quandSalleDeCoursLibre() {
        Salle salle = salle(6L, TypeSalle.SalleDeCours);
        Stage wb47 = stage(3L, "WB47", LocalDate.now().plusMonths(3));
        when(salleService.findById(6L)).thenReturn(Optional.of(salle));
        when(stageService.findById(3L)).thenReturn(Optional.of(wb47));
        when(stageService.salleDuStage(3L)).thenReturn(Optional.empty());

        assertThat(service.affecterASalle(6L, 3L).getStage()).isEqualTo(wb47);
    }

    @Test
    void affecterASalle_doitLeverServiceException_quandSalleDeStockage() {
        when(salleService.findById(9L)).thenReturn(Optional.of(salle(9L, TypeSalle.SalleStockage)));
        when(stageService.findById(3L)).thenReturn(Optional.of(stage(3L, "WB47", LocalDate.now().plusMonths(3))));

        assertThatThrownBy(() -> service.affecterASalle(9L, 3L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("salle de cours");
    }

    @Test
    void affecterASalle_doitLeverServiceException_quandSalleAccueilleDejaUnStage() {
        Salle salle = salle(6L, TypeSalle.SalleDeCours);
        salle.setStage(stage(1L, "VU82", LocalDate.now().plusMonths(3)));
        when(salleService.findById(6L)).thenReturn(Optional.of(salle));
        when(stageService.findById(3L)).thenReturn(Optional.of(stage(3L, "WB47", LocalDate.now().plusMonths(3))));

        assertThatThrownBy(() -> service.affecterASalle(6L, 3L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("VU82");
        assertThat(salle.getStage().getNom()).isEqualTo("VU82");
    }

    @Test
    void affecterASalle_doitLeverServiceException_quandStageDejaDansUneSalle() {
        when(salleService.findById(6L)).thenReturn(Optional.of(salle(6L, TypeSalle.SalleDeCours)));
        when(stageService.findById(3L)).thenReturn(Optional.of(stage(3L, "WB47", LocalDate.now().plusMonths(3))));
        when(stageService.salleDuStage(3L)).thenReturn(Optional.of(new SalleDuStage(3L, 7L, "106", "351")));

        assertThatThrownBy(() -> service.affecterASalle(6L, 3L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("déjà affecté");
    }

    @Test
    void affecterASalle_doitLeverServiceException_quandStageTermine() {
        when(salleService.findById(6L)).thenReturn(Optional.of(salle(6L, TypeSalle.SalleDeCours)));
        when(stageService.findById(3L)).thenReturn(Optional.of(stage(3L, "VU79", LocalDate.now().minusDays(1))));
        when(stageService.salleDuStage(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.affecterASalle(6L, 3L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("terminé");
    }

    // ---------- retirerDeSalle ----------

    @Test
    void retirerDeSalle_doitRetirerLeStageEtLibererLesPostes() {
        Salle salle = salle(6L, TypeSalle.SalleDeCours);
        salle.setStage(stage(3L, "WB47", LocalDate.now().plusMonths(3)));
        Poste poste = new Poste();
        poste.setNumPoste("P01");
        poste.setOccupant(new Personnel());
        salle.addPoste(poste);
        when(salleService.findById(6L)).thenReturn(Optional.of(salle));

        Salle resultat = service.retirerDeSalle(6L, 3L);

        assertThat(resultat.getStage()).isNull();
        assertThat(poste.getOccupant()).isNull();
    }

    @Test
    void retirerDeSalle_doitLeverServiceException_quandAutreStage() {
        Salle salle = salle(6L, TypeSalle.SalleDeCours);
        salle.setStage(stage(1L, "VU82", LocalDate.now().plusMonths(3)));
        when(salleService.findById(6L)).thenReturn(Optional.of(salle));

        assertThatThrownBy(() -> service.retirerDeSalle(6L, 3L))
                .isInstanceOf(ServiceException.class);
        assertThat(salle.getStage()).isNotNull();
    }
}
