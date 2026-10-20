package net.ent.etnc.orbit.dtos.services.impl;

import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.services.JournalisationService;
import net.ent.etnc.orbit.services.PersonnelService;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import net.ent.etnc.orbit.services.impl.AffecterFormateurServiceImpl;
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
class AffecterFormateurServiceImplTest {

    @Mock
    private SalleService salleService;

    @Mock
    private PersonnelService personnelService;

    /** Mock : les tests de ce service ne vérifient pas le journal. */
    @Mock
    private JournalisationService journalisationService;

    @InjectMocks
    private AffecterFormateurServiceImpl service;

    private static Salle salle(Long id, TypeSalle type) {
        Salle s = new Salle();
        s.setId(id);
        s.setNumSalle("S" + id);
        s.setType(type);
        return s;
    }

    private static Personnel personne(Long id, Role role) {
        Personnel p = new Personnel();
        p.setId(id);
        p.setNid(String.format("%010d", id));
        p.setNom("Nom" + id);
        p.setPrenom("Prenom" + id);
        p.setRole(role);
        return p;
    }

    // ---------- affecter ----------

    @Test
    void affecter_doitAjouterLeFormateurALaSalle() {
        Salle salle = salle(6L, TypeSalle.SalleDeCours);
        Personnel kevin = personne(4L, Role.FORMATEUR);
        when(salleService.findById(6L)).thenReturn(Optional.of(salle));
        when(personnelService.findById(4L)).thenReturn(Optional.of(kevin));

        assertThat(service.affecter(6L, 4L).getPersonnels()).containsExactly(kevin);
    }

    @Test
    void affecter_doitPermettreDeuxFormateursSurLaMemeSalle() {
        Salle salle = salle(6L, TypeSalle.SalleDeCours);
        Personnel kevin = personne(4L, Role.FORMATEUR);
        Personnel sophie = personne(5L, Role.FORMATEUR);
        salle.addPersonnel(kevin);
        when(salleService.findById(6L)).thenReturn(Optional.of(salle));
        when(personnelService.findById(5L)).thenReturn(Optional.of(sophie));

        assertThat(service.affecter(6L, 5L).getPersonnels()).containsExactlyInAnyOrder(kevin, sophie);
    }

    @Test
    void affecter_doitLeverServiceException_quandDejaAffecte() {
        Salle salle = salle(6L, TypeSalle.SalleDeCours);
        Personnel kevin = personne(4L, Role.FORMATEUR);
        salle.addPersonnel(kevin);
        when(salleService.findById(6L)).thenReturn(Optional.of(salle));
        when(personnelService.findById(4L)).thenReturn(Optional.of(kevin));

        assertThatThrownBy(() -> service.affecter(6L, 4L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("déjà affecté");
    }

    @Test
    void affecter_doitLeverServiceException_quandPasFormateur() {
        when(salleService.findById(6L)).thenReturn(Optional.of(salle(6L, TypeSalle.SalleDeCours)));
        when(personnelService.findById(7L)).thenReturn(Optional.of(personne(7L, Role.STAGIAIRE)));

        assertThatThrownBy(() -> service.affecter(6L, 7L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("n'est pas formateur");
    }

    @Test
    void affecter_doitLeverServiceException_quandSalleDeStockage() {
        when(salleService.findById(9L)).thenReturn(Optional.of(salle(9L, TypeSalle.SalleStockage)));
        when(personnelService.findById(4L)).thenReturn(Optional.of(personne(4L, Role.FORMATEUR)));

        assertThatThrownBy(() -> service.affecter(9L, 4L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("salle de cours");
    }

    @Test
    void affecter_doitLeverServiceException_quandSalleIntrouvable() {
        when(salleService.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.affecter(99L, 4L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("introuvable");
    }

    // ---------- retirer ----------

    @Test
    void retirer_doitRetirerLeFormateurEtGarderLesAutres() {
        Salle salle = salle(6L, TypeSalle.SalleDeCours);
        Personnel kevin = personne(4L, Role.FORMATEUR);
        Personnel sophie = personne(5L, Role.FORMATEUR);
        salle.addPersonnel(kevin);
        salle.addPersonnel(sophie);
        when(salleService.findById(6L)).thenReturn(Optional.of(salle));

        assertThat(service.retirer(6L, 4L).getPersonnels()).containsExactly(sophie);
    }

    @Test
    void retirer_doitLeverServiceException_quandPasAffecte() {
        when(salleService.findById(6L)).thenReturn(Optional.of(salle(6L, TypeSalle.SalleDeCours)));

        assertThatThrownBy(() -> service.retirer(6L, 4L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("n'est pas affecté");
    }
}