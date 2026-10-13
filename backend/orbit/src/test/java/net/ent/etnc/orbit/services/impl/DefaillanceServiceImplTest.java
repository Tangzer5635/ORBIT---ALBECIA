package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.models.entities.Defaillance;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.models.enums.StatutDefaillance;
import net.ent.etnc.orbit.repositories.DefaillanceRepository;
import net.ent.etnc.orbit.services.MaterielService;
import net.ent.etnc.orbit.services.PosteService;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.commons.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaillanceServiceImplTest {

    @Mock
    private DefaillanceRepository repository;

    @Mock
    private MaterielService materielService;

    @Mock
    private PosteService posteService;

    @Mock
    private SalleService salleService;

    @InjectMocks
    private DefaillanceServiceImpl service;

    private static final Long ID_MATERIEL = 1L;
    private static final Long ID_POSTE = 10L;

    private Personnel personnel(Long id, Role role) {
        Personnel p = new Personnel();
        p.setId(id);
        p.setNid(String.format("%010d", id));
        p.setLogin("user" + id);
        p.setRole(role);
        return p;
    }

    private Materiel materiel() {
        Materiel m = new Materiel();
        m.setId(ID_MATERIEL);
        m.setNumSerie("UC-0001");
        return m;
    }

    private Poste posteOccupePar(Personnel occupant) {
        Poste p = new Poste();
        p.setId(ID_POSTE);
        p.setNumPoste("P101-01");
        p.setOccupant(occupant);
        return p;
    }

    private Salle salleAvecFormateur(Personnel formateur) {
        Salle s = new Salle();
        s.setId(1L);
        s.setNumSalle("101");
        s.addPersonnel(formateur);
        return s;
    }

    private Defaillance defaillanceSignalee() {
        Defaillance d = new Defaillance();
        d.setId(5L);
        d.setMateriel(materiel());
        d.setStatut(StatutDefaillance.SIGNALEE);
        return d;
    }

    private void sauvegardeRenvoieLArgument() {
        when(repository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }

    // ---------- signaler ----------

    @Test
    void signaler_doitCreerUneDefaillanceSignalee_quandStagiaireSurSonPoste() {
        Personnel stagiaire = personnel(6L, Role.STAGIAIRE);
        when(materielService.findById(ID_MATERIEL)).thenReturn(Optional.of(materiel()));
        when(posteService.posteDuMateriel(ID_MATERIEL)).thenReturn(Optional.of(posteOccupePar(stagiaire)));
        when(repository.existsByMateriel_IdAndStatut(ID_MATERIEL, StatutDefaillance.SIGNALEE)).thenReturn(false);
        sauvegardeRenvoieLArgument();

        Defaillance d = service.signaler(ID_MATERIEL, "Ne démarre plus", stagiaire);

        assertThat(d.getStatut()).isEqualTo(StatutDefaillance.SIGNALEE);
        assertThat(d.getSignalePar()).isSameAs(stagiaire);
        assertThat(d.getMateriel().getId()).isEqualTo(ID_MATERIEL);
        assertThat(d.getDescription()).isEqualTo("Ne démarre plus");
        assertThat(d.getDateSignalement()).isNotNull();
    }

    @Test
    void signaler_doitLeverAccessDenied_quandStagiaireSurLePosteDUnAutre() {
        Personnel stagiaire = personnel(6L, Role.STAGIAIRE);
        when(materielService.findById(ID_MATERIEL)).thenReturn(Optional.of(materiel()));
        when(posteService.posteDuMateriel(ID_MATERIEL)).thenReturn(Optional.of(posteOccupePar(personnel(7L, Role.STAGIAIRE))));

        assertThatThrownBy(() -> service.signaler(ID_MATERIEL, "Pas mon poste", stagiaire))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void signaler_doitCreer_quandFormateurSurUnPosteDeSesSalles() {
        Personnel formateur = personnel(4L, Role.FORMATEUR);
        when(materielService.findById(ID_MATERIEL)).thenReturn(Optional.of(materiel()));
        when(posteService.posteDuMateriel(ID_MATERIEL)).thenReturn(Optional.of(posteOccupePar(personnel(7L, Role.STAGIAIRE))));
        when(salleService.salleDuPoste(ID_POSTE)).thenReturn(Optional.of(salleAvecFormateur(formateur)));
        when(repository.existsByMateriel_IdAndStatut(ID_MATERIEL, StatutDefaillance.SIGNALEE)).thenReturn(false);
        sauvegardeRenvoieLArgument();

        Defaillance d = service.signaler(ID_MATERIEL, "Écran bleu", formateur);

        assertThat(d.getSignalePar()).isSameAs(formateur);
    }

    @Test
    void signaler_doitCreer_quandFormateurSurSonProprePoste() {
        Personnel formateur = personnel(4L, Role.FORMATEUR);
        when(materielService.findById(ID_MATERIEL)).thenReturn(Optional.of(materiel()));
        when(posteService.posteDuMateriel(ID_MATERIEL)).thenReturn(Optional.of(posteOccupePar(formateur)));
        when(repository.existsByMateriel_IdAndStatut(ID_MATERIEL, StatutDefaillance.SIGNALEE)).thenReturn(false);
        sauvegardeRenvoieLArgument();

        assertThat(service.signaler(ID_MATERIEL, "Clavier HS", formateur)).isNotNull();
        verify(salleService, never()).salleDuPoste(any());
    }

    @Test
    void signaler_doitLeverAccessDenied_quandFormateurHorsDeSesSalles() {
        Personnel formateur = personnel(4L, Role.FORMATEUR);
        when(materielService.findById(ID_MATERIEL)).thenReturn(Optional.of(materiel()));
        when(posteService.posteDuMateriel(ID_MATERIEL)).thenReturn(Optional.of(posteOccupePar(personnel(7L, Role.STAGIAIRE))));
        when(salleService.salleDuPoste(ID_POSTE)).thenReturn(Optional.of(salleAvecFormateur(personnel(5L, Role.FORMATEUR))));

        assertThatThrownBy(() -> service.signaler(ID_MATERIEL, "Pas ma salle", formateur))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void signaler_doitLeverAccessDenied_quandMaterielHorsPoste() {
        // matériel de salle (TBI, vidéoprojecteur) ou matériel en stock
        when(materielService.findById(ID_MATERIEL)).thenReturn(Optional.of(materiel()));
        when(posteService.posteDuMateriel(ID_MATERIEL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.signaler(ID_MATERIEL, "Tableau HS", personnel(4L, Role.FORMATEUR)))
                .isInstanceOf(AccessDeniedException.class);
        verify(salleService, never()).salleDuMateriel(any());
    }

    @Test
    void signaler_doitLeverServiceException_RGD03_quandDejaSignalee() {
        Personnel stagiaire = personnel(6L, Role.STAGIAIRE);
        when(materielService.findById(ID_MATERIEL)).thenReturn(Optional.of(materiel()));
        when(posteService.posteDuMateriel(ID_MATERIEL)).thenReturn(Optional.of(posteOccupePar(stagiaire)));
        when(repository.existsByMateriel_IdAndStatut(ID_MATERIEL, StatutDefaillance.SIGNALEE)).thenReturn(true);

        assertThatThrownBy(() -> service.signaler(ID_MATERIEL, "Toujours en panne", stagiaire))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Ce matériel a déjà une défaillance signalée");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void signaler_doitLeverNotFound_quandMaterielIntrouvable() {
        when(materielService.findById(ID_MATERIEL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.signaler(ID_MATERIEL, "Fantôme", personnel(6L, Role.STAGIAIRE)))
                .isInstanceOf(NotFoundException.class);
        verify(posteService, never()).posteDuMateriel(any());
    }

    // ---------- resoudre ----------

    @Test
    void resoudre_doitPasserLaDefaillanceAResolue_quandGestionnaire() {
        Personnel gestionnaire = personnel(2L, Role.GESTIONNAIRE);
        when(repository.findById(5L)).thenReturn(Optional.of(defaillanceSignalee()));
        sauvegardeRenvoieLArgument();

        Defaillance d = service.resoudre(5L, "Alimentation remplacée", gestionnaire);

        assertThat(d.getStatut()).isEqualTo(StatutDefaillance.RESOLUE);
        assertThat(d.getTraitePar()).isSameAs(gestionnaire);
        assertThat(d.getNoteSolution()).isEqualTo("Alimentation remplacée");
        assertThat(d.getDateResolution()).isNotNull();
        verify(posteService, never()).posteDuMateriel(any());
    }

    @Test
    void resoudre_doitPasserAResolue_quandAdministrateur() {
        when(repository.findById(5L)).thenReturn(Optional.of(defaillanceSignalee()));
        sauvegardeRenvoieLArgument();

        assertThat(service.resoudre(5L, "OK", personnel(1L, Role.ADMINISTRATEUR)).isResolue()).isTrue();
    }

    @Test
    void resoudre_doitPasserAResolue_quandFormateurDansSaSalle() {
        Personnel formateur = personnel(4L, Role.FORMATEUR);
        when(repository.findById(5L)).thenReturn(Optional.of(defaillanceSignalee()));
        when(posteService.posteDuMateriel(ID_MATERIEL)).thenReturn(Optional.of(posteOccupePar(null)));
        when(salleService.salleDuPoste(ID_POSTE)).thenReturn(Optional.of(salleAvecFormateur(formateur)));
        sauvegardeRenvoieLArgument();

        assertThat(service.resoudre(5L, "Pilote réinstallé", formateur).isResolue()).isTrue();
    }

    @Test
    void resoudre_doitPasserAResolue_quandFormateurEtMaterielDeSaSalle() {
        Personnel formateur = personnel(4L, Role.FORMATEUR);
        when(repository.findById(5L)).thenReturn(Optional.of(defaillanceSignalee()));
        when(posteService.posteDuMateriel(ID_MATERIEL)).thenReturn(Optional.empty());
        when(salleService.salleDuMateriel(ID_MATERIEL)).thenReturn(Optional.of(salleAvecFormateur(formateur)));
        sauvegardeRenvoieLArgument();

        assertThat(service.resoudre(5L, "Lampe changée", formateur).isResolue()).isTrue();
    }

    @Test
    void resoudre_doitLeverAccessDenied_quandFormateurHorsDeSesSalles() {
        Personnel formateur = personnel(4L, Role.FORMATEUR);
        when(repository.findById(5L)).thenReturn(Optional.of(defaillanceSignalee()));
        when(posteService.posteDuMateriel(ID_MATERIEL)).thenReturn(Optional.of(posteOccupePar(null)));
        when(salleService.salleDuPoste(ID_POSTE)).thenReturn(Optional.of(salleAvecFormateur(personnel(5L, Role.FORMATEUR))));

        assertThatThrownBy(() -> service.resoudre(5L, "Pas ma salle", formateur))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void resoudre_doitLeverAccessDenied_quandStagiaire() {
        when(repository.findById(5L)).thenReturn(Optional.of(defaillanceSignalee()));

        assertThatThrownBy(() -> service.resoudre(5L, "Je répare", personnel(6L, Role.STAGIAIRE)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void resoudre_doitLeverServiceException_RGD05_quandDejaResolue() {
        Defaillance d = defaillanceSignalee();
        d.resoudre(personnel(2L, Role.GESTIONNAIRE), "Déjà fait");
        when(repository.findById(5L)).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> service.resoudre(5L, "Encore", personnel(2L, Role.GESTIONNAIRE)))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Défaillance déjà résolue");
    }

    @Test
    void resoudre_doitLeverNotFound_quandIntrouvable() {
        when(repository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resoudre(5L, "Fantôme", personnel(2L, Role.GESTIONNAIRE)))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- requêtes ----------

    @Test
    void aUneDefaillanceOuverte_doitInterrogerLesDefaillancesSignalees() {
        when(repository.existsByMateriel_IdAndStatut(ID_MATERIEL, StatutDefaillance.SIGNALEE)).thenReturn(true);

        assertThat(service.aUneDefaillanceOuverte(ID_MATERIEL)).isTrue();
    }

    @Test
    void aDesDefaillances_doitInterrogerToutLHistorique() {
        when(repository.existsByMateriel_Id(ID_MATERIEL)).thenReturn(false);

        assertThat(service.aDesDefaillances(ID_MATERIEL)).isFalse();
    }

    // ---------- create / update interdits ----------

    @Test
    void create_doitLeverServiceException_RGD06() {
        assertThatThrownBy(() -> service.create(new Defaillance()))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Utiliser signaler()");
    }

    @Test
    void update_doitLeverServiceException_RGD07() {
        assertThatThrownBy(() -> service.update(new Defaillance()))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Une défaillance ne se modifie pas, elle se résout");
    }
}
