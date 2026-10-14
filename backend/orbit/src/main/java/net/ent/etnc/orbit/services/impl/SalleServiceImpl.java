package net.ent.etnc.orbit.services.impl;

import lombok.extern.slf4j.Slf4j;
import net.ent.etnc.orbit.dtos.requestDTO.SalleEquipeeRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.AnneauDto;
import net.ent.etnc.orbit.dtos.responseDTO.SalleCarteDto;
import net.ent.etnc.orbit.dtos.responseDTO.SalleEquipeeResponseDto;
import net.ent.etnc.orbit.models.entities.Batiment;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.TypeMateriel;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.repositories.BatimentRepository;
import net.ent.etnc.orbit.repositories.SalleRepository;
import net.ent.etnc.orbit.repositories.projections.IdCompte;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SalleServiceImpl extends AbstractService<Salle, SalleRepository> implements SalleService {

    private record EnStock(Materiel materiel, Salle reserve) {}

    private final PosteService posteService;
    private final BatimentRepository batimentRepository;

    @Autowired
    public SalleServiceImpl(SalleRepository salleRepository, PosteService posteService, BatimentRepository batimentRepository) {
        super(salleRepository);
        this.posteService = posteService;
        this.batimentRepository = batimentRepository;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        Salle salle = repository.findById(id).orElseThrow(() -> new ServiceException("Salle " + id + " introuvable"));
        if (salle.getType() == TypeSalle.SalleStockage)
            throw new ServiceException("Une salle de stockage ne peut disparaître qu'avec la suppression de son bâtiment");
        supprimer(salle);
    }

    /** Seul chemin qui supprime une salle de stockage : appelé par la suppression du bâtiment. */
    @Override
    @Transactional
    public void supprimerAvecBatiment(Long id) {
        Salle salle = repository.findById(id).orElseThrow(() -> new ServiceException("Salle " + id + " introuvable"));
        supprimer(salle);
    }

    private void supprimer(Salle salle) {
        if (salle.getType() == TypeSalle.SalleStockage && !salle.getMateriels().isEmpty())
            throw new ServiceException("Vide la salle de stockage avant de la supprimer");
        List<Materiel> aRanger = new ArrayList<>(salle.getMateriels());
        for (Poste p : salle.getPostes()) {
            aRanger.addAll(p.getMateriels());
        }
        if (!aRanger.isEmpty()) {
            Salle stockage = trouverStockage(salle.getId());
            for (Materiel m : List.copyOf(salle.getMateriels())) {
                salle.removeMateriel(m);
            }
            for (Poste p : salle.getPostes()) {
                for (Materiel m : List.copyOf(p.getMateriels())) {
                    p.removeMateriel(m);
                }
            }
            for (Materiel m : aRanger) {
                stockage.addMateriel(m);
            }
        }
        for (Poste p : List.copyOf(salle.getPostes())) {
            salle.removePoste(p);
            posteService.delete(p);
        }
        repository.delete(salle);
    }

    @Override
    @Transactional
    public Salle create(Long idBatiment, Salle salle) {
        Batiment batiment = trouverBatiment(idBatiment);
        if (batiment.getSalles().stream().anyMatch(s -> s.getNumSalle().equals(salle.getNumSalle())))
            throw new ServiceException("Une salle " + salle.getNumSalle() + " existe déjà dans ce bâtiment");
        Salle creee = create(salle);
        batiment.addSalle(creee);
        return creee;
    }

    private Batiment trouverBatiment(Long idBatiment) {
        return batimentRepository.findById(idBatiment)
                .orElseThrow(() -> new ServiceException("Bâtiment " + idBatiment + " introuvable"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Salle> findAllAvecDetails(Pageable pageable) {
        Page<Salle> page = repository.findAll(pageable);
        charger(page.getContent().stream().map(Salle::getId).toList());
        return page;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Salle> findByIdAvecDetails(Long id) {
        Optional<Salle> salle = repository.findById(id);
        salle.ifPresent(s -> charger(List.of(s.getId())));
        return salle;
    }

    /** Initialise matériels et postes de ces salles : 2 requêtes, quel que soit le nombre de salles. */
    private void charger(List<Long> ids) {
        if (ids.isEmpty()) return;
        repository.chargerMateriels(ids);
        repository.chargerPostes(ids);
    }

    @Override
    @Transactional
    public SalleEquipeeResponseDto creerSalleEquipee(SalleEquipeeRequestDto dto) {
        Salle salle = new Salle();
        salle.setNumSalle(dto.getNumSalle());
        salle.setEtage(dto.getEtage());
        salle.setType(TypeSalle.SalleDeCours);
        Salle creee = create(dto.getIdBatiment(), salle); // vérifie le bâtiment et le numéro en double
        Batiment batiment = trouverBatiment(dto.getIdBatiment());

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
            Poste cree = creerPoste(creee.getId(), poste);
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
    public Poste creerPoste(Long idSalle, Poste poste) {
        Salle salle = repository.findById(idSalle).orElseThrow(() -> new ServiceException("Salle " + idSalle + " introuvable"));
        if (salle.getType() == TypeSalle.SalleStockage)
            throw new ServiceException("On ne crée pas de poste dans une salle de stockage");
        Poste cree = posteService.create(poste);
        salle.addPoste(cree);
        return cree;
    }

    @Override
    @Transactional
    public void supprimerPoste(Long idPoste) {
        Poste poste = posteService.findById(idPoste).orElseThrow(() -> new ServiceException("Poste " + idPoste + " introuvable"));
        List<Materiel> aRanger = List.copyOf(poste.getMateriels());
        if (!aRanger.isEmpty()) {
            // la réserve se cherche avant de détacher le poste de sa salle
            Salle stockage = trouverStockageDuPoste(idPoste);
            for (Materiel m : aRanger) {
                poste.removeMateriel(m);
                stockage.addMateriel(m);
            }
        }
        repository.findByPostes_Id(idPoste).ifPresent(salle -> salle.removePoste(poste));
        posteService.delete(poste);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean estEnStock(Long idMateriel) {
        return repository.existsByTypeAndMateriels_Id(TypeSalle.SalleStockage, idMateriel);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Salle> salleDuMateriel(Long idMateriel) {
        return repository.findByMateriels_Id(idMateriel);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Salle> salleDuPoste(Long idPoste) {
        return repository.findByPostes_Id(idPoste);
    }

    @Override
    @Transactional(readOnly = true)
    public Salle trouverStockage(Long idSalle) {
        return premiereReserve(repository.findSallesDuBatiment(idSalle, TypeSalle.SalleStockage));
    }

    @Override
    @Transactional(readOnly = true)
    public Salle trouverStockageDuPoste(Long idPoste) {
        return premiereReserve(repository.findSallesDuBatimentDuPoste(idPoste, TypeSalle.SalleStockage));
    }

    private Salle premiereReserve(List<Salle> reservesDuBatiment) {
        if (reservesDuBatiment.isEmpty())
            throw new ServiceException("Aucune salle de stockage dans le bâtiment");
        return reservesDuBatiment.get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalleCarteDto> findCartesDuBatiment(Long idBatiment) {
        List<Salle> salles = repository.findByBatiment(idBatiment);
        if (salles.isEmpty()) return List.of();
        List<Long> ids = salles.stream().map(Salle::getId).toList();

        LocalDate seuil = Materiel.seuilARemplacer();
        Map<Long, Long> nbPostes = repository.nbPostes(ids).stream()
                .collect(Collectors.toMap(IdCompte::id, IdCompte::nb));
        Map<Long, AnneauDto> anneaux = AnneauDto.parId(
                repository.comptesEtatsEnSalle(ids, seuil), repository.comptesEtatsSurPoste(ids, seuil));

        return salles.stream().map(s -> SalleCarteDto.builder()
                .id(s.getId())
                .numSalle(s.getNumSalle())
                .etage(s.getEtage())
                .type(s.getType())
                .nbPostes(nbPostes.getOrDefault(s.getId(), 0L))
                .anneau(anneaux.getOrDefault(s.getId(), AnneauDto.VIDE))
                .build()).toList();
    }
}