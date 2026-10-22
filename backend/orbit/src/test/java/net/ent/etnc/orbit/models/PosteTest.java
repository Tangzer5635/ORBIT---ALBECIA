package net.ent.etnc.orbit.models;

import net.ent.etnc.orbit.models.entities.Ecran;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.entities.UC;
import net.ent.etnc.orbit.models.entities.VideoProjecteur;
import net.ent.etnc.orbit.models.enums.EtatPoste;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PosteTest {

    private long suivant = 1;

    private <T extends Materiel> T materiel(T m) {
        m.setId(suivant);
        m.setNumSerie("MAT-" + suivant++);
        m.setModele("Modèle");
        m.setDateAcquisition(LocalDate.now().minusMonths(3));
        m.setDateFinGarantie(LocalDate.now().plusYears(2));
        return m;
    }

    private Poste poste(Materiel... materiels) {
        Poste p = new Poste();
        p.setNumPoste("B1-S101-P01");
        for (Materiel m : materiels) p.addMateriel(m);
        return p;
    }

    @Test
    void calculerEtat_doitEtreDisponible_quandUneUcEtUnEcran() {
        assertThat(poste(materiel(new UC()), materiel(new Ecran())).calculerEtat(Set.of()))
                .isEqualTo(EtatPoste.DISPONIBLE);
    }

    @Test
    void calculerEtat_doitEtreACompleter_quandPasDEcran() {
        assertThat(poste(materiel(new UC())).calculerEtat(Set.of())).isEqualTo(EtatPoste.A_COMPLETER);
    }

    @Test
    void calculerEtat_doitEtreACompleter_quandPasDUc() {
        assertThat(poste(materiel(new Ecran()), materiel(new Ecran())).calculerEtat(Set.of()))
                .isEqualTo(EtatPoste.A_COMPLETER);
    }

    @Test
    void calculerEtat_doitEtreACompleter_quandVide() {
        assertThat(poste().calculerEtat(Set.of())).isEqualTo(EtatPoste.A_COMPLETER);
    }

    @Test
    void calculerEtat_doitEtreDefectueux_avantACompleter() {
        UC uc = materiel(new UC());
        assertThat(poste(uc).calculerEtat(Set.of(uc.getId()))).isEqualTo(EtatPoste.DEFECTUEUX);
    }

    @Test
    void calculerEtat_doitResterDefectueux_quandUnStagiaireYEstAssis() {
        UC uc = materiel(new UC());
        Poste poste = poste(uc, materiel(new Ecran()));
        poste.setOccupant(new Personnel());
        assertThat(poste.calculerEtat(Set.of(uc.getId()))).isEqualTo(EtatPoste.DEFECTUEUX);
    }

    @Test
    void anomalies_doitEtreVide_quandCompositionAutorisee() {
        assertThat(poste(materiel(new UC()), materiel(new Ecran()), materiel(new Ecran())).anomalies()).isEmpty();
    }

    @Test
    void anomalies_doitSignalerDeuxUcTroisEcransEtUnVideoprojecteur() {
        Poste poste = poste(materiel(new UC()), materiel(new UC()),
                materiel(new Ecran()), materiel(new Ecran()), materiel(new Ecran()),
                materiel(new VideoProjecteur()));
        assertThat(poste.anomalies()).containsExactly(
                "2 unités centrales (1 au maximum)",
                "3 écrans (2 au maximum)",
                "1 vidéoprojecteur(s) : c'est du matériel de salle, pas de poste");
    }
}
