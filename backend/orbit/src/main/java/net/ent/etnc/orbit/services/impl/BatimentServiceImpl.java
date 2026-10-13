package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.dtos.responseDTO.AnneauDto;
import net.ent.etnc.orbit.dtos.responseDTO.BatimentCarteDto;
import net.ent.etnc.orbit.dtos.responseDTO.BatimentResponseDto;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.repositories.projections.IdCompte;
import net.ent.etnc.orbit.repositories.projections.StockageCompte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import net.ent.etnc.orbit.models.entities.Batiment;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.repositories.BatimentRepository;
import net.ent.etnc.orbit.services.BatimentService;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.commons.AbstractService;
import net.ent.etnc.orbit.services.commons.NotFoundException;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BatimentServiceImpl extends AbstractService<Batiment, BatimentRepository> implements BatimentService {

    private static final String NUM_SALLE_STOCKAGE = "199";
    private static final String ETAGE_SALLE_STOCKAGE = "0";

    private final SalleService salleService;

    @Autowired
    public BatimentServiceImpl(BatimentRepository batimentRepository, SalleService salleService) {
        super(batimentRepository);
        this.salleService = salleService;
    }

    /**
     * Crée le bâtiment et, dans la même transaction, sa salle de stockage :
     * si l'une des deux créations échoue, rien n'est enregistré.
     */
    @Override
    @Transactional
    public Batiment create(Batiment batiment) {
        Batiment cree = super.create(batiment);
        Salle stockage = new Salle();
        stockage.setNumSalle(NUM_SALLE_STOCKAGE);
        stockage.setEtage(ETAGE_SALLE_STOCKAGE);
        stockage.setType(TypeSalle.SalleStockage);
        cree.addSalle(salleService.create(stockage));
        return cree;
    }

    @Override
    @Transactional
    public Salle create(Long idBatiment, Salle salle) {
        Batiment batiment = repository.findById(idBatiment).orElseThrow(() -> new NotFoundException("Bâtiment " + idBatiment + " introuvable"));
        if (batiment.getSalles().stream().anyMatch(s -> s.getNumSalle().equals(salle.getNumSalle())))
            throw new ServiceException("Une salle " + salle.getNumSalle() + " existe déjà dans ce bâtiment");
        Salle creee = salleService.create(salle);
        batiment.addSalle(creee);
        return creee;
    }

    @Override
    @Transactional
    public Batiment affecterSalle(Long idBatiment, Long idSalle) {
        Batiment batiment = repository.findById(idBatiment).orElseThrow(() -> new NotFoundException("Bâtiment " + idBatiment + " introuvable"));
        Salle salle = salleService.findById(idSalle).orElseThrow(() -> new NotFoundException("Salle " + idSalle + " introuvable"));
        if (repository.existsBySalles_Id(idSalle))
            throw new ServiceException("La salle est déjà affectée à un bâtiment");
        if (batiment.getSalles().stream().anyMatch(s -> s.getNumSalle().equals(salle.getNumSalle())))
            throw new ServiceException("Une salle " + salle.getNumSalle() + " existe déjà dans ce bâtiment");
        batiment.addSalle(salle);
        return batiment;
    }

    @Override
    @Transactional
    public void retirerSalle(Long idBatiment, Long idSalle) {
        Batiment batiment = repository.findById(idBatiment).orElseThrow(() -> new NotFoundException("Bâtiment " + idBatiment + " introuvable"));
        Salle salle = batiment.getSalles().stream()
                .filter(s -> s.getId().equals(idSalle))
                .findFirst()
                .orElseThrow(() -> new ServiceException("Cette salle n'appartient pas à ce bâtiment"));
        batiment.removeSalle(salle);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BatimentCarteDto> findCartes(Pageable pageable) {
        Page<Batiment> page = repository.findAll(pageable);
        List<Long> ids = page.map(Batiment::getId).getContent();
        if (ids.isEmpty()) return Page.empty(pageable);

        LocalDate seuil = Materiel.seuilARemplacer();
        Map<Long, Long> nbSalles = repository.nbSallesDeCours(ids).stream()
                .collect(Collectors.toMap(IdCompte::id, IdCompte::nb));
        Map<Long, List<BatimentResponseDto.StockageDto>> stockages = repository.stockages(ids).stream()
                .collect(Collectors.groupingBy(StockageCompte::idBatiment, Collectors.mapping(
                        s -> new BatimentResponseDto.StockageDto(s.id(), s.numSalle(), s.nbMateriels()),
                        Collectors.toList())));
        Map<Long, AnneauDto> anneaux = AnneauDto.parId(
                repository.comptesEtatsEnSalle(ids, seuil), repository.comptesEtatsSurPoste(ids, seuil));

        return page.map(b -> BatimentCarteDto.builder()
                .id(b.getId())
                .numBatiment(b.getNumBatiment())
                .nbSalles(nbSalles.getOrDefault(b.getId(), 0L))
                .stockages(stockages.getOrDefault(b.getId(), List.of()))
                .anneau(anneaux.getOrDefault(b.getId(), AnneauDto.VIDE))
                .build());
    }
}