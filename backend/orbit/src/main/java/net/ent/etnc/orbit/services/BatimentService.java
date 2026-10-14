package net.ent.etnc.orbit.services;

import net.ent.etnc.orbit.dtos.responseDTO.BatimentCarteDto;
import net.ent.etnc.orbit.models.entities.Batiment;
import net.ent.etnc.orbit.services.commons.Service;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface BatimentService extends Service<Batiment, Long> {
    void supprimer(Long idBatiment, Long idStockageCible) throws ServiceException;
    Batiment affecterSalle(Long idBatiment, Long idSalle) throws ServiceException;
    void retirerSalle(Long idBatiment, Long idSalle) throws ServiceException;
    Page<BatimentCarteDto> findCartes(Pageable pageable);

    @Transactional
    Optional<Batiment> batimentDeLaSalle(Long idSalle);
}