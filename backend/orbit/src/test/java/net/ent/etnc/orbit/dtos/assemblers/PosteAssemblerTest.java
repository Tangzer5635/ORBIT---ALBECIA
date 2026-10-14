package net.ent.etnc.orbit.dtos.assemblers;

import net.ent.etnc.orbit.dtos.responseDTO.MonPosteResponseDto;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Ecran;
import net.ent.etnc.orbit.models.enums.EtatMateriel;
import net.ent.etnc.orbit.repositories.projections.MonPosteLigne;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PosteAssemblerTest {

    private final PosteAssembler assembler = new PosteAssembler(new MaterielAssembler(new ProcessusAssembler()));

    private Materiel materiel(Long id, LocalDate acquisition) {
        Materiel m = new Ecran();
        m.setId(id);
        m.setNumSerie("MAT-" + id);
        m.setModele("Modele");
        m.setDateAcquisition(acquisition);
        m.setDateFinGarantie(LocalDate.now().plusYears(2));
        return m;
    }

    private MonPosteLigne ligne(Materiel m, boolean defaillance) {
        return new MonPosteLigne(1L, "P101-01", 2L, "101", "1", 3L, "B201", m, defaillance);
    }

    @Test
    void toDtoMonPoste_doitRemplirPosteSalleEtBatiment() {
        MonPosteResponseDto dto = assembler.toDtoMonPoste(List.of(ligne(null, false)));

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getNumPoste()).isEqualTo("P101-01");
        assertThat(dto.getIdSalle()).isEqualTo(2L);
        assertThat(dto.getNumSalle()).isEqualTo("101");
        assertThat(dto.getEtage()).isEqualTo("1");
        assertThat(dto.getIdBatiment()).isEqualTo(3L);
        assertThat(dto.getNumBatiment()).isEqualTo("B201");
    }

    @Test
    void toDtoMonPoste_doitRenvoyerUneListeVide_quandLePosteNAPasDeMateriel() {
        assertThat(assembler.toDtoMonPoste(List.of(ligne(null, false))).getMateriels()).isEmpty();
    }

    @Test
    void toDtoMonPoste_doitCalculerLEtatDeChaqueMateriel() {
        Materiel ok = materiel(1L, LocalDate.now().minusMonths(6));
        Materiel panne = materiel(2L, LocalDate.now().minusMonths(6));
        Materiel vieux = materiel(3L, LocalDate.now().minusYears(3));

        MonPosteResponseDto dto = assembler.toDtoMonPoste(List.of(ligne(ok, false), ligne(panne, true), ligne(vieux, false)));

        assertThat(dto.getMateriels()).extracting("etat")
                .containsExactly(EtatMateriel.NORMAL, EtatMateriel.DEFECTUEUX, EtatMateriel.A_REMPLACER);
    }
}