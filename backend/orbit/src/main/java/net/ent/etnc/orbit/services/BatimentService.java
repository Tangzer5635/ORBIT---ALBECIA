package net.ent.etnc.orbit.services;

import net.ent.etnc.orbit.dtos.requestDTO.SalleEquipeeRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.BatimentCarteDto;
import net.ent.etnc.orbit.dtos.responseDTO.SalleEquipeeResponseDto;
import net.ent.etnc.orbit.models.entities.Batiment;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.services.commons.Service;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BatimentService extends Service<Batiment, Long> {
    Salle create(Long idBatiment, Salle salle);
    void supprimer(Long idBatiment, Long idStockageCible) throws ServiceException;
    SalleEquipeeResponseDto creerSalleEquipee(SalleEquipeeRequestDto dto) throws ServiceException;
    Batiment affecterSalle(Long idBatiment, Long idSalle) throws ServiceException;
    void retirerSalle(Long idBatiment, Long idSalle) throws ServiceException;
    Page<BatimentCarteDto> findCartes(Pageable pageable);
}