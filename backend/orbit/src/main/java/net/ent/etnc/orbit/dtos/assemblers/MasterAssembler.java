package net.ent.etnc.orbit.dtos.assemblers;

import net.ent.etnc.orbit.dtos.MasterDto;
import net.ent.etnc.orbit.models.entities.Master;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class MasterAssembler {

    private final ProcessusAssembler processusAssembler;

    public MasterAssembler(ProcessusAssembler processusAssembler) {
        this.processusAssembler = processusAssembler;
    }

    public MasterDto toDto(Master master) {
        return MasterDto.builder()
                .id(master.getId())
                .nom(master.getNom())
                .versionMaster(master.getVersionMaster())
                .dateCreation(master.getDateCreation())
                .processusIds(master.getProcessus().stream().map(p -> p.getId()).toList())
                .processus(master.getProcessus().stream().map(processusAssembler::toDto).toList())
                .build();
    }

    public List<MasterDto> toDtos(List<Master> masters) {
        return masters.stream()
                .map(this::toDto)
                .toList();
    }

    /** Sans les processus : le service les retrouve à partir de {@code processusIds}. */
    public Master toEntity(MasterDto masterDto) {
        Master master = new Master();
        master.setNom(masterDto.getNom());
        master.setVersionMaster(masterDto.getVersionMaster());
        master.setDateCreation(masterDto.getDateCreation() != null ? masterDto.getDateCreation() : LocalDateTime.now());
        return master;
    }
}