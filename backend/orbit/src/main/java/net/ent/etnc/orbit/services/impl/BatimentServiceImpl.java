package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.dtos.responseDTO.AnneauDto;
import net.ent.etnc.orbit.dtos.responseDTO.BatimentCarteDto;
import net.ent.etnc.orbit.dtos.responseDTO.BatimentResponseDto;
import net.ent.etnc.orbit.models.entities.Batiment;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.TypeAction;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.repositories.BatimentRepository;
import net.ent.etnc.orbit.repositories.projections.IdCompte;
import net.ent.etnc.orbit.repositories.projections.StockageCompte;
import net.ent.etnc.orbit.services.BatimentService;
import net.ent.etnc.orbit.services.JournalisationService;
import net.ent.etnc.orbit.services.PosteService;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.commons.AbstractService;
import net.ent.etnc.orbit.services.commons.Libelles;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BatimentServiceImpl extends AbstractService<Batiment, BatimentRepository> implements BatimentService {

    private static final String NUM_SALLE_STOCKAGE = "199";
    private static final String ETAGE_SALLE_STOCKAGE = "0";

    private final SalleService salleService;
    private final PosteService posteService;
    private final JournalisationService journalisationService;

    @Autowired
    public BatimentServiceImpl(BatimentRepository batimentRepository, SalleService salleService, PosteService posteService,
                               JournalisationService journalisationService) {
        super(batimentRepository);
        this.salleService = salleService;
        this.posteService = posteService;
        this.journalisationService = journalisationService;
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
        journalisationService.journaliser(TypeAction.CREATION,
                "Bâtiment " + batiment.getNumBatiment() + " créé (avec sa salle de stockage " + NUM_SALLE_STOCKAGE + ")");
        return cree;
    }

    @Override
    @Transactional
    public void supprimer(Long idBatiment, Long idStockageCible) {
        Batiment batiment = repository.findById(idBatiment)
                .orElseThrow(() -> new ServiceException("Bâtiment " + idBatiment + " introuvable"));
        List<Salle> salles = List.copyOf(batiment.getSalles());

        // 1. Détacher tout le matériel des salles et des postes
        List<Materiel> materiels = new ArrayList<>();
        String vers = ""; // pour le journal : où est parti le matériel
        for (Salle salle : salles) {
            for (Poste poste : salle.getPostes()) {
                List<Materiel> surPoste = List.copyOf(poste.getMateriels());
                surPoste.forEach(poste::removeMateriel);
                materiels.addAll(surPoste);
            }
            List<Materiel> enSalle = List.copyOf(salle.getMateriels());
            enSalle.forEach(salle::removeMateriel);
            materiels.addAll(enSalle);
        }

        if (!materiels.isEmpty()) {
            if (idStockageCible == null)
                throw new ServiceException("Choisissez une salle de stockage pour le matériel");
            Salle cible = salleService.findById(idStockageCible)
                    .orElseThrow(() -> new ServiceException("Salle " + idStockageCible + " introuvable"));
            if (!cible.getType().equals(TypeSalle.SalleStockage))
                throw new ServiceException("La salle cible n'est pas une salle de stockage");
            if (salles.contains(cible))
                throw new ServiceException("La salle cible appartient au bâtiment supprimé");
            materiels.forEach(cible::addMateriel);
            vers = ", " + materiels.size() + " matériel(s) déplacé(s) vers la " + Libelles.salle(cible);
        }
        for (Salle salle : salles) {
            for (Poste poste : List.copyOf(salle.getPostes())) {
                salle.removePoste(poste);
                posteService.deleteById(poste.getId());
            }
            batiment.removeSalle(salle);
            salleService.supprimerAvecBatiment(salle.getId());
        }
        repository.delete(batiment);
        journalisationService.journaliser(TypeAction.SUPPRESSION,
                "Bâtiment " + batiment.getNumBatiment() + " supprimé avec ses " + salles.size() + " salle(s)" + vers);
    }

    @Override
    @Transactional
    public Batiment affecterSalle(Long idBatiment, Long idSalle) {
        Batiment batiment = repository.findById(idBatiment).orElseThrow(() -> new ServiceException("Bâtiment " + idBatiment + " introuvable"));
        Salle salle = salleService.findById(idSalle).orElseThrow(() -> new ServiceException("Salle " + idSalle + " introuvable"));
        if (repository.existsBySalles_Id(idSalle))
            throw new ServiceException("La salle est déjà affectée à un bâtiment");
        if (batiment.getSalles().stream().anyMatch(s -> s.getNumSalle().equals(salle.getNumSalle())))
            throw new ServiceException("Une salle " + salle.getNumSalle() + " existe déjà dans ce bâtiment");
        batiment.addSalle(salle);
        journalisationService.journaliser(TypeAction.AFFECTATION,
                Libelles.salle(salle) + " rattachée au bâtiment " + batiment.getNumBatiment());
        return batiment;
    }

    @Override
    @Transactional
    public void retirerSalle(Long idBatiment, Long idSalle) {
        Batiment batiment = repository.findById(idBatiment).orElseThrow(() -> new ServiceException("Bâtiment " + idBatiment + " introuvable"));
        Salle salle = batiment.getSalles().stream()
                .filter(s -> s.getId().equals(idSalle))
                .findFirst()
                .orElseThrow(() -> new ServiceException("Cette salle n'appartient pas à ce bâtiment"));
        if (salle.getType() == TypeSalle.SalleStockage)
            throw new ServiceException("Une salle de stockage ne peut disparaître qu'avec la suppression de son bâtiment");
        batiment.removeSalle(salle);
        journalisationService.journaliser(TypeAction.RETRAIT,
                Libelles.salle(salle) + " détachée du bâtiment " + batiment.getNumBatiment());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BatimentCarteDto> findCartes(Pageable pageable) {
        Page<Batiment> page = repository.findAll(pageable);
        List<Long> ids = page.map(Batiment::getId).getContent();
        if (ids.isEmpty()) return Page.empty(pageable);

        Map<Long, Long> nbSalles = repository.nbSallesDeCours(ids).stream()
                .collect(Collectors.toMap(IdCompte::id, IdCompte::nb));
        Map<Long, List<BatimentResponseDto.StockageDto>> stockages = repository.stockages(ids).stream()
                .collect(Collectors.groupingBy(StockageCompte::idBatiment, Collectors.mapping(
                        s -> new BatimentResponseDto.StockageDto(s.id(), s.numSalle(), s.nbMateriels()),
                        Collectors.toList())));
        // anneau du bâtiment = anneaux de ses salles de cours additionnés (une salle gelée ne compte plus)
        List<Long> idsSalles = page.getContent().stream()
                .flatMap(b -> b.getSalles().stream())
                .filter(s -> s.getType() == TypeSalle.SalleDeCours && !s.isSuspendu())
                .map(Salle::getId).toList();
        Map<Long, AnneauDto> anneauxSalles = salleService.anneaux(idsSalles);

        return page.map(b -> BatimentCarteDto.builder()
                .id(b.getId())
                .numBatiment(b.getNumBatiment())
                .nbSalles(nbSalles.getOrDefault(b.getId(), 0L))
                .stockages(stockages.getOrDefault(b.getId(), List.of()))
                .anneau(b.getSalles().stream()
                        .map(s -> anneauxSalles.getOrDefault(s.getId(), AnneauDto.VIDE))
                        .reduce(AnneauDto.VIDE, AnneauDto::plus))
                .build());
    }

    @Transactional
    @Override
    public Optional<Batiment> batimentDeLaSalle(Long idSalle) {
        return repository.findBySalles_Id(idSalle);
    }
}