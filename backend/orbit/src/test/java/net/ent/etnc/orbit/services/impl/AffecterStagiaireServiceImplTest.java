package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.entities.Stage;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.services.PersonnelService;
import net.ent.etnc.orbit.services.PosteService;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AffecterStagiaireServiceImplTest {

    @Mock
    private PosteService posteService;

    @Mock
    private SalleService salleService;

    @Mock
    private PersonnelService personnelService;

    @InjectMocks
    private AffecterStagiaireServiceImpl service;

    private static Stage stage(Long id, String nom) {
        Stage s = new Stage();
        s.setId(id);
        s.setNom(nom);
        return s;
    }

    private static Personnel stagiaire(Long id, Stage stage) {
        Personnel p = new Personnel();
        p.setId(id);
        p.setPrenom("Théo");
        p.setNom("Dubois");
        p.setRole(Role.STAGIAIRE);
        p.setActive(true);
        p.setStage(stage);
        return p;
    }

    private static Poste poste(Long id, String numPoste) {
        Poste p = new Poste();
        p.setId(id);
        p.setNumPoste(numPoste);
        return p;
    }

    private static Salle salle(Stage stage) {
        Salle s = new Salle();
        s.setNumSalle("105");
        s.setStage(stage);
        return s;
    }

    // ---------- affecterAPoste ----------

    @Test
    void affecterAPoste_doitAsseoirLeStagiaire_quandDuStageDeLaSalleEtSansPoste() {
        Stage wb47 = stage(3L, "WB47");
        Poste poste = poste(10L, "B351-S105-P01");
        Personnel theo = stagiaire(7L, wb47);
        when(posteService.findById(10L)).thenReturn(Optional.of(poste));
        when(personnelService.findById(7L)).thenReturn(Optional.of(theo));
        when(salleService.salleDuPoste(10L)).thenReturn(Optional.of(salle(wb47)));
        when(posteService.occupeUnPoste(7L)).thenReturn(false);

        assertThat(service.affecterAPoste(10L, 7L).getOccupant()).isEqualTo(theo);
    }

    @Test
    void affecterAPoste_doitLeverServiceException_quandAutreStage() {
        when(posteService.findById(10L)).thenReturn(Optional.of(poste(10L, "B351-S105-P01")));
        when(personnelService.findById(7L)).thenReturn(Optional.of(stagiaire(7L, stage(1L, "VU82"))));
        when(salleService.salleDuPoste(10L)).thenReturn(Optional.of(salle(stage(3L, "WB47"))));

        assertThatThrownBy(() -> service.affecterAPoste(10L, 7L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("WB47");
    }

    @Test
    void affecterAPoste_doitLeverServiceException_quandSalleSansStage() {
        when(posteService.findById(10L)).thenReturn(Optional.of(poste(10L, "B351-S105-P01")));
        when(personnelService.findById(7L)).thenReturn(Optional.of(stagiaire(7L, stage(3L, "WB47"))));
        when(salleService.salleDuPoste(10L)).thenReturn(Optional.of(salle(null)));

        assertThatThrownBy(() -> service.affecterAPoste(10L, 7L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("aucun stage");
    }

    @Test
    void affecterAPoste_doitLeverServiceException_quandStagiaireADejaUnPoste() {
        Stage wb47 = stage(3L, "WB47");
        when(posteService.findById(10L)).thenReturn(Optional.of(poste(10L, "B351-S105-P01")));
        when(personnelService.findById(7L)).thenReturn(Optional.of(stagiaire(7L, wb47)));
        when(salleService.salleDuPoste(10L)).thenReturn(Optional.of(salle(wb47)));
        when(posteService.occupeUnPoste(7L)).thenReturn(true);

        assertThatThrownBy(() -> service.affecterAPoste(10L, 7L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("déjà un poste");
    }

    @Test
    void affecterAPoste_doitLeverServiceException_quandPosteOccupe() {
        Poste poste = poste(10L, "B351-S105-P01");
        Personnel marc = stagiaire(8L, stage(3L, "WB47"));
        marc.setPrenom("Marc");
        poste.setOccupant(marc);
        when(posteService.findById(10L)).thenReturn(Optional.of(poste));
        when(personnelService.findById(7L)).thenReturn(Optional.of(stagiaire(7L, stage(3L, "WB47"))));

        assertThatThrownBy(() -> service.affecterAPoste(10L, 7L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Marc");
        assertThat(poste.getOccupant()).isEqualTo(marc);
    }

    @Test
    void affecterAPoste_doitLeverServiceException_quandPosteFormateur() {
        when(posteService.findById(10L)).thenReturn(Optional.of(poste(10L, "B351-S105-F")));
        when(personnelService.findById(7L)).thenReturn(Optional.of(stagiaire(7L, stage(3L, "WB47"))));

        assertThatThrownBy(() -> service.affecterAPoste(10L, 7L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("formateur");
    }

    @Test
    void affecterAPoste_doitLeverServiceException_quandPasStagiaire() {
        Personnel formateur = stagiaire(2L, null);
        formateur.setRole(Role.FORMATEUR);
        when(posteService.findById(10L)).thenReturn(Optional.of(poste(10L, "B351-S105-P01")));
        when(personnelService.findById(2L)).thenReturn(Optional.of(formateur));

        assertThatThrownBy(() -> service.affecterAPoste(10L, 2L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("n'est pas un stagiaire");
    }

    // ---------- retirerDePoste ----------

    @Test
    void retirerDePoste_doitLibererLePoste_quandCeStagiaireYEst() {
        Poste poste = poste(10L, "B351-S105-P01");
        poste.setOccupant(stagiaire(7L, stage(3L, "WB47")));
        when(posteService.findById(10L)).thenReturn(Optional.of(poste));

        assertThat(service.retirerDePoste(10L, 7L).getOccupant()).isNull();
    }

    @Test
    void retirerDePoste_doitLeverServiceException_quandAutreOccupant() {
        Poste poste = poste(10L, "B351-S105-P01");
        poste.setOccupant(stagiaire(8L, stage(3L, "WB47")));
        when(posteService.findById(10L)).thenReturn(Optional.of(poste));

        assertThatThrownBy(() -> service.retirerDePoste(10L, 7L))
                .isInstanceOf(ServiceException.class);
        assertThat(poste.getOccupant()).isNotNull();
    }
}
