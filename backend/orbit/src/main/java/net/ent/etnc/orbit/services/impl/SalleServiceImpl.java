package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.dtos.responseDTO.AnneauDto;
import net.ent.etnc.orbit.dtos.responseDTO.SalleCarteDto;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.repositories.SalleRepository;
import net.ent.etnc.orbit.repositories.projections.IdCompte;
import net.ent.etnc.orbit.services.PosteService;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.commons.AbstractService;
import net.ent.etnc.orbit.services.commons.NotFoundException;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SalleServiceImpl extends AbstractService<Salle, SalleRepository> implements SalleService {

    private final PosteService posteService;

    @Autowired
    public SalleServiceImpl(SalleRepository salleRepository, PosteService posteService) {
        super(salleRepository);
        this.posteService = posteService;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        Salle salle = repository.findById(id).orElseThrow(() -> new NotFoundException("Salle " + id + " introuvable"));
        if (salle.getType() == TypeSalle.SalleStockage && !salle.getMateriels().isEmpty())
            throw new ServiceException("Vide la salle de stockage avant de la supprimer");
        List<Materiel> aRanger = new ArrayList<>(salle.getMateriels());
        for (Poste p : salle.getPostes()) {
            aRanger.addAll(p.getMateriels());
        }
        if (!aRanger.isEmpty()) {
            Salle stockage = trouverStockage(id);
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
    public Poste creerPoste(Long idSalle, Poste poste) {
        Salle salle = repository.findById(idSalle).orElseThrow(() -> new NotFoundException("Salle " + idSalle + " introuvable"));
        if (salle.getType() == TypeSalle.SalleStockage)
            throw new ServiceException("On ne crée pas de poste dans une salle de stockage");
        Poste cree = posteService.create(poste);
        salle.addPoste(cree);
        return cree;
    }

    @Override
    @Transactional
    public void supprimerPoste(Long idPoste) {
        Poste poste = posteService.findById(idPoste).orElseThrow(() -> new NotFoundException("Poste " + idPoste + " introuvable"));
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
        if (!reservesDuBatiment.isEmpty()) {
            return reservesDuBatiment.get(0);
        }
        return repository.findFirstByTypeOrderByIdAsc(TypeSalle.SalleStockage).orElseThrow(() -> new ServiceException("Aucune salle de stockage n'existe"));
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