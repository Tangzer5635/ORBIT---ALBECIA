package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.entities.Stage;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.services.PersonnelService;
import net.ent.etnc.orbit.services.PosteService;
import net.ent.etnc.orbit.services.StageService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StageAffectationServiceImplTest {

    @Mock
    private StageService stageService;

    @Mock
    private PersonnelService personnelService;

    @Mock
    private PosteService posteService;

    @InjectMocks
    private StageAffectationServiceImpl service;

    private static Stage stage(Long id, String nom) {
        Stage s = new Stage();
        s.setId(id);
        s.setNom(nom);
        return s;
    }

    private static Personnel personnel(Long id, Role role, Stage stage) {
        Personnel p = new Personnel();
        p.setId(id);
        p.setPrenom("Théo");
        p.setNom("Dubois");
        p.setRole(role);
        p.setStage(stage);
        return p;
    }

    @Test
    void ajouterAStage_doitAffecterLeStage_quandStagiaireSansStage() {
        Stage wb47 = stage(3L, "WB47");
        Personnel theo = personnel(7L, Role.STAGIAIRE, null);
        when(stageService.findById(3L)).thenReturn(Optional.of(wb47));
        when(personnelService.findById(7L)).thenReturn(Optional.of(theo));

        assertThat(service.ajouterAStage(3L, 7L).getStage()).isEqualTo(wb47);
    }

    @Test
    void ajouterAStage_doitLeverServiceException_quandDejaDansUnAutreStage() {
        Personnel theo = personnel(7L, Role.STAGIAIRE, stage(1L, "VU82"));
        when(stageService.findById(3L)).thenReturn(Optional.of(stage(3L, "WB47")));
        when(personnelService.findById(7L)).thenReturn(Optional.of(theo));

        assertThatThrownBy(() -> service.ajouterAStage(3L, 7L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("VU82");
        assertThat(theo.getStage().getNom()).isEqualTo("VU82");
    }

    @Test
    void ajouterAStage_doitLeverServiceException_quandFormateur() {
        // un formateur hérite des droits stagiaire, mais n'entre pas dans un stage
        when(stageService.findById(3L)).thenReturn(Optional.of(stage(3L, "WB47")));
        when(personnelService.findById(2L)).thenReturn(Optional.of(personnel(2L, Role.FORMATEUR, null)));

        assertThatThrownBy(() -> service.ajouterAStage(3L, 2L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("n'est pas un stagiaire");
    }

    @Test
    void ajouterAStage_doitLeverServiceException_quandStageIntrouvable() {
        when(stageService.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ajouterAStage(99L, 7L))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void supprimerAStage_doitRetirerLeStageEtLibererSonPoste_quandStagiaireDansCeStage() {
        Personnel theo = personnel(7L, Role.STAGIAIRE, stage(3L, "WB47"));
        when(personnelService.findById(7L)).thenReturn(Optional.of(theo));

        assertThat(service.supprimerAStage(3L, 7L).getStage()).isNull();
        verify(posteService).libererPosteDe(7L);
    }

    @Test
    void supprimerAStage_doitLeverServiceException_quandPasDansCeStage() {
        when(personnelService.findById(7L)).thenReturn(Optional.of(personnel(7L, Role.STAGIAIRE, stage(1L, "VU82"))));

        assertThatThrownBy(() -> service.supprimerAStage(3L, 7L))
                .isInstanceOf(ServiceException.class);
    }
}