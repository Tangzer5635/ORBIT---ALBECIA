package net.ent.etnc.orbit.services.impl;

import lombok.extern.slf4j.Slf4j;
import net.ent.etnc.orbit.dtos.requestDTO.SalleEquipeeRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.AnneauDto;
import net.ent.etnc.orbit.dtos.responseDTO.BatimentCarteDto;
import net.ent.etnc.orbit.dtos.responseDTO.BatimentResponseDto;
import net.ent.etnc.orbit.dtos.responseDTO.SalleEquipeeResponseDto;
import net.ent.etnc.orbit.models.entities.Batiment;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.TypeMateriel;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.repositories.BatimentRepository;
import net.ent.etnc.orbit.repositories.projections.IdCompte;
import net.ent.etnc.orbit.repositories.projections.StockageCompte;
import net.ent.etnc.orbit.services.BatimentService;
import net.ent.etnc.orbit.services.PosteService;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.commons.AbstractService;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BatimentServiceImpl extends AbstractService<Batiment, BatimentRepository> implements BatimentService {

    private static final String NUM_SALLE_STOCKAGE = "199";
    private static final String ETAGE_SALLE_STOCKAGE = "0";

    private record EnStock(Materiel materiel, Salle reserve) {}

    private final SalleService salleService;
    private final PosteService posteService;

    @Autowired
    public BatimentServiceImpl(BatimentRepository batimentRepository, SalleService salleService, PosteService posteService) {
        super(batimentRepository);
        this.salleService = salleService;
        this.posteService = posteService;
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
        Batiment batiment = repository.findById(idBatiment).orElseThrow(() -> new ServiceException("Bâtiment " + idBatiment + " introuvable"));
        if (batiment.getSalles().stream().anyMatch(s -> s.getNumSalle().equals(salle.getNumSalle())))
            throw new ServiceException("Une salle " + salle.getNumSalle() + " existe déjà dans ce bâtiment");
        Salle creee = salleService.create(salle);
        batiment.addSalle(creee);
        return creee;
    }

    @Override
    @Transactional
    public void supprimer(Long idBatiment, Long idStockageCible) {
        Batiment batiment = repository.findById(idBatiment)
                .orElseThrow(() -> new ServiceException("Bâtiment " + idBatiment + " introuvable"));
        List<Salle> salles = List.copyOf(batiment.getSalles());

        // 1. Détacher tout le matériel des salles et des postes
        List<Materiel> materiels = new ArrayList<>();
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
        }
        for (Salle salle : salles) {
            for (Poste poste : List.copyOf(salle.getPostes())) {
                salle.removePoste(poste);
                posteService.deleteById(poste.getId());
            }
            batiment.removeSalle(salle);
            salleService.deleteById(salle.getId());
        }
        repository.delete(batiment);
    }

    @Override
    @Transactional
    public SalleEquipeeResponseDto creerSalleEquipee(SalleEquipeeRequestDto dto) {
        Salle salle = new Salle();
        salle.setNumSalle(dto.getNumSalle().trim());
        salle.setEtage(dto.getEtage());
        salle.setType(TypeSalle.SalleDeCours);
        Salle creee = create(dto.getIdBatiment(), salle); // vérifie le bâtiment et le numéro en double
        Batiment batiment = repository.findById(dto.getIdBatiment()).orElseThrow();

        // Stock du bâtiment, rangé par type (matériel archivé exclu)
        Map<TypeMateriel, Deque<EnStock>> stock = new EnumMap<>(TypeMateriel.class);
        for (Salle reserve : batiment.getSalles()) {
            if (reserve.getType() != TypeSalle.SalleStockage) continue;
            for (Materiel m : reserve.getMateriels()) {
                if (!m.isArchive())
                    stock.computeIfAbsent(m.getType(), t -> new ArrayDeque<>()).add(new EnStock(m, reserve));
            }
        }
        Map<TypeMateriel, Integer> manquants = new EnumMap<>(TypeMateriel.class);

        // numPoste est unique sur tout le parc : on préfixe par bâtiment + salle
        String prefixe = "B" + batiment.getNumBatiment() + "-S" + creee.getNumSalle() + "-";
        List<String> numeros = new ArrayList<>();
        numeros.add(prefixe + "F"); // formateur en premier = servi en priorité
        for (int i = 1; i <= dto.getNbPostes(); i++) numeros.add(prefixe + String.format("P%02d", i));

        for (String numero : numeros) {
            Poste poste = new Poste();
            poste.setNumPoste(numero);
            Poste cree = salleService.creerPoste(creee.getId(), poste);
            prendre(stock, TypeMateriel.UC, 1, manquants).forEach(cree::addMateriel);
            prendre(stock, TypeMateriel.Ecran, dto.getEcransParPoste(), manquants).forEach(cree::addMateriel);
        }
        prendre(stock, TypeMateriel.VideoProjecteur, dto.getNbVideoProjecteurs(), manquants).forEach(creee::addMateriel);
        prendre(stock, TypeMateriel.TableauInteractif, dto.getNbTableaux(), manquants).forEach(creee::addMateriel);

        if (!manquants.isEmpty()) {
            boolean plusRien = stock.values().stream().allMatch(Deque::isEmpty);
            log.warn("Salle {} du bâtiment {} créée incomplète{} : il manque {}",
                    creee.getNumSalle(), batiment.getNumBatiment(),
                    plusRien ? " (plus rien en stock)" : "", manquants);
        }
        return new SalleEquipeeResponseDto(creee.getId(), manquants);
    }

    /** Retire jusqu'à `quantite` matériels du stock ; note ce qui manque. */
    private List<Materiel> prendre(Map<TypeMateriel, Deque<EnStock>> stock, TypeMateriel type,
                                   int quantite, Map<TypeMateriel, Integer> manquants) {
        Deque<EnStock> dispo = stock.getOrDefault(type, new ArrayDeque<>());
        List<Materiel> pris = new ArrayList<>();
        for (int i = 0; i < quantite; i++) {
            EnStock e = dispo.poll();
            if (e == null) {
                manquants.merge(type, quantite - i, Integer::sum);
                break;
            }
            e.reserve().removeMateriel(e.materiel());
            pris.add(e.materiel());
        }
        return pris;
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

    @Transactional
    @Override
    public Optional<Batiment> batimentDeLaSalle(Long idSalle) {
        return repository.findBySalles_Id(idSalle);
    }
}
