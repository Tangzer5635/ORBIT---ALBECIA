package net.ent.etnc.orbit.dtos.assemblers;

import net.ent.etnc.orbit.dtos.responseDTO.SalleResponseDto;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.TypeMateriel;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.services.DefaillanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalleAssemblerTest {

    @Mock
    private DefaillanceService defaillanceService;

    @InjectMocks
    private SalleAssembler assembler;

    private Materiel materiel(Long id, TypeMateriel type, LocalDate finGarantie) {
        Materiel m = new Materiel();
        m.setId(id);
        m.setNumSerie("MAT-" + id);
        m.setType(type);
        m.setDateFinGarantie(finGarantie);
        return m;
    }

    private Poste poste(Long id, Personnel occupant, Materiel... materiels) {
        Poste p = new Poste();
        p.setId(id);
        p.setNumPoste("P-" + id);
        p.setOccupant(occupant);
        for (Materiel m : materiels) p.addMateriel(m);
        return p;
    }

    @Test
    void toDto_doitCompterLesPostesParEtat() {
        LocalDate garantieOk = LocalDate.now().plusYears(2);
        Salle salle = new Salle();
        salle.setId(1L);
        salle.setNumSalle("101");
        salle.setEtage("1");
        salle.setType(TypeSalle.SalleDeCours);

        salle.addPoste(poste(1L, new Personnel(), materiel(1L, TypeMateriel.UC, garantieOk)));         // utilisé
        salle.addPoste(poste(2L, null, materiel(2L, TypeMateriel.UC, garantieOk)));                    // disponible
        salle.addPoste(poste(3L, null, materiel(3L, TypeMateriel.UC, LocalDate.now().plusDays(10)))); // à changer
        salle.addPoste(poste(4L, null));                                                               // à compléter
        salle.addPoste(poste(5L, new Personnel(), materiel(5L, TypeMateriel.Ecran, garantieOk)));      // défectueux
        // seul le matériel 5 a une défaillance ouverte
        when(defaillanceService.aUneDefaillanceOuverte(anyLong())).thenAnswer(i -> i.getArgument(0).equals(5L));

        SalleResponseDto dto = assembler.toDto(salle);

        assertThat(dto.getNbPostes()).isEqualTo(5);
        assertThat(dto.getComptes().getUse()).isEqualTo(1);
        assertThat(dto.getComptes().getDispo()).isEqualTo(1);
        assertThat(dto.getComptes().getChg()).isEqualTo(1);
        assertThat(dto.getComptes().getInc()).isEqualTo(1);
        assertThat(dto.getComptes().getDef()).isEqualTo(1);
    }

    @Test
    void toDto_doitCompterLeMaterielDeLaSalleParType() {
        Salle reserve = new Salle();
        reserve.setId(2L);
        reserve.setNumSalle("199");
        reserve.setEtage("0");
        reserve.setType(TypeSalle.SalleStockage);
        LocalDate garantie = LocalDate.now().plusYears(2);
        reserve.addMateriel(materiel(1L, TypeMateriel.Ecran, garantie));
        reserve.addMateriel(materiel(2L, TypeMateriel.Ecran, garantie));

        SalleResponseDto dto = assembler.toDto(reserve);

        assertThat(dto.getMaterielsParType())
                .containsEntry(TypeMateriel.Ecran, 2L);
        assertThat(dto.getNbPostes()).isZero();
    }
}
