package net.ent.etnc.orbit.dtos.services.impl;

import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.entities.Stage;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.repositories.StageRepository;
import net.ent.etnc.orbit.services.JournalisationService;
import net.ent.etnc.orbit.services.PersonnelService;
import net.ent.etnc.orbit.services.PosteService;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import net.ent.etnc.orbit.services.impl.StageServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StageServiceImplTest {

    @Mock
    private StageRepository repository;

    @Mock
    private PersonnelService personnelService;

    @Mock
    private PosteService posteService;

    @Mock
    private SalleService salleService;

    /** Mock : les tests de ce service ne vérifient pas le journal. */
    @Mock
    private JournalisationService journalisationService;

    @InjectMocks
    private StageServiceImpl service;

    private static Stage stage(LocalDate fin) {
        Stage s = new Stage();
        s.setId(3L);
        s.setNom("WB47");
        s.setDateDebut(fin.minusMonths(5));
        s.setDateFin(fin);
        return s;
    }

    private static Personnel stagiaire(Long id, Stage stage) {
        Personnel p = new Personnel();
        p.setId(id);
        p.setRole(Role.STAGIAIRE);
        p.setStage(stage);
        return p;
    }

    // ---------- deleteById ----------

    @Test
    void deleteById_doitRetirerLesStagiairesDuStageEtDeLeurPosteEtLeStageDeSaSalle() {
        Stage wb47 = stage(LocalDate.now().minusDays(1));
        Personnel theo = stagiaire(7L, wb47);
        Personnel lea = stagiaire(8L, wb47);
        Poste poste = new Poste();
        poste.setNumPoste("B1-S101-P01");
        poste.setOccupant(theo);
        Salle salle = new Salle();
        salle.setNumSalle("101");
        salle.setStage(wb47);
        salle.addPoste(poste);
        when(repository.findById(3L)).thenReturn(Optional.of(wb47));
        when(personnelService.stagiairesDuStage(3L)).thenReturn(List.of(theo, lea));
        when(salleService.sallesDuStage(3L)).thenReturn(List.of(salle));

        service.deleteById(3L);

        assertThat(theo.getStage()).isNull();
        assertThat(lea.getStage()).isNull();
        verify(posteService).libererPosteDe(7L);
        verify(posteService).libererPosteDe(8L);
        assertThat(salle.getStage()).isNull();
        assertThat(poste.getOccupant()).isNull();
        verify(repository).delete(wb47);
    }

    @Test
    void deleteById_doitLeverServiceException_quandStagePasTermine() {
        when(repository.findById(3L)).thenReturn(Optional.of(stage(LocalDate.now().plusDays(10))));

        assertThatThrownBy(() -> service.deleteById(3L))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Le stage WB47 n'est pas terminé : on ne supprime qu'un stage terminé");
        verify(personnelService, never()).stagiairesDuStage(any());
        verify(repository, never()).delete((Stage) any());
    }
}
