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
    private final SalleAssembler assembler = new SalleAssembler();

    private Materiel materiel(Long id, TypeMateriel type, LocalDate finGarantie) {
        Materiel m = new Materiel();
        m.setId(id);
        m.setNumSerie("MAT-" + id);
        m.setType(type);
        m.setDateFinGarantie(finGarantie);
        return m;
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
