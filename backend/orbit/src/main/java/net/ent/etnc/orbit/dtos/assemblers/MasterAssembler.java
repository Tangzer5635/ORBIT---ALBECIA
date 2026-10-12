package net.ent.etnc.orbit.dtos.assemblers;

import net.ent.etnc.orbit.dtos.MasterDto;
import net.ent.etnc.orbit.models.entities.Master;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class MasterAssembler {

    public MasterDto toDto(Master master) {
        return MasterDto.builder()
                .id(master.getId())
                .nom(master.getNom())
                .versionMaster(master.getVersionMaster())
                .dateCreation(master.getDateCreation())
                .build();
    }

    public List<MasterDto> toDtos(List<Master> masters) {
        return masters.stream()
                .map(this::toDto)
                .toList();
    }

    public Master toEntity(MasterDto masterDto) {
        Master master = new Master();
        master.setNom(masterDto.getNom());
        master.setVersionMaster(masterDto.getVersionMaster());
        master.setDateCreation(masterDto.getDateCreation() != null ? masterDto.getDateCreation() : LocalDateTime.now());
        return master;
    }
}
