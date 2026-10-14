package net.ent.etnc.orbit.dtos.assemblers;

import net.ent.etnc.orbit.dtos.requestDTO.SalleRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.SalleResponseDto;
import net.ent.etnc.orbit.models.entities.Ecran;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.services.DefaillanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SalleAssemblerTest {

    @Mock
    private DefaillanceService defaillanceService;

    @InjectMocks
    private final SalleAssembler assembler = new SalleAssembler();

    private Materiel ecran(Long id, LocalDate finGarantie) {
        Materiel m = new Ecran();
        m.setId(id);
        m.setNumSerie("MAT-" + id);
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
        reserve.addMateriel(ecran(1L, garantie));
        reserve.addMateriel(ecran(2L, garantie));

        SalleResponseDto dto = assembler.toDto(reserve);

        assertThat(dto.getMaterielsParType())
                .containsEntry(Ecran.TYPE, 2L);
        assertThat(dto.getNbPostes()).isZero();
    }

    @Test
    void toEntity_doitRetirerLesEspacesAutourDuNumero() {
        SalleRequestDto dto = SalleRequestDto.builder()
                .numSalle("  101 ").etage("1").type(TypeSalle.SalleDeCours).idBatiment(1L).build();

        assertThat(assembler.toEntity(dto).getNumSalle()).isEqualTo("101");
    }
}