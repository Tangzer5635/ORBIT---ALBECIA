package net.ent.etnc.orbit.services.impl;

import lombok.extern.slf4j.Slf4j;
import net.ent.etnc.orbit.dtos.requestDTO.SalleEquipeeRequestDto;
import net.ent.etnc.orbit.dtos.requestDTO.SalleModificationRequestDto;
import net.ent.etnc.orbit.dtos.responseDTO.AnneauDto;
import net.ent.etnc.orbit.dtos.responseDTO.SalleCarteDto;
import net.ent.etnc.orbit.dtos.responseDTO.SalleEquipeeResponseDto;
import net.ent.etnc.orbit.models.entities.Batiment;
import net.ent.etnc.orbit.models.entities.Ecran;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.entities.Poste;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.entities.Stage;
import net.ent.etnc.orbit.models.entities.TableauInteractif;
import net.ent.etnc.orbit.models.entities.UC;
import net.ent.etnc.orbit.models.entities.VideoProjecteur;
import net.ent.etnc.orbit.models.enums.EtatPoste;
import net.ent.etnc.orbit.models.enums.MotifGel;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.models.enums.TypeAction;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.repositories.BatimentRepository;
import net.ent.etnc.orbit.repositories.SalleRepository;
import net.ent.etnc.orbit.repositories.projections.IdCompte;
import net.ent.etnc.orbit.repositories.projections.SalleBatiment;
import net.ent.etnc.orbit.services.JournalisationService;
import net.ent.etnc.orbit.services.PosteService;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.commons.AbstractService;
import net.ent.etnc.orbit.services.commons.Libelles;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private final JournalisationService journalisationService;

    @Autowired
    public SalleServiceImpl(SalleRepository salleRepository, PosteService posteService, BatimentRepository batimentRepository,
                            JournalisationService journalisationService) {
        super(salleRepository);
        this.posteService = posteService;
        this.batimentRepository = batimentRepository;
        this.journalisationService = journalisationService;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        Salle salle = repository.findById(id).orElseThrow(() -> new ServiceException("Salle " + id + " introuvable"));
        if (salle.getType() == TypeSalle.SalleStockage)
            throw new ServiceException("Une salle de stockage ne peut disparaître qu'avec la suppression de son bâtiment");
        String libelle = libelle(salle); // lu avant la suppression
        supprimer(salle);
        journalisationService.journaliser(TypeAction.SUPPRESSION, "Salle " + libelle + " supprimée (matériel remis en stock)");
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
        Salle creee = creerDans(idBatiment, salle);
        journalisationService.journaliser(TypeAction.CREATION, "Salle " + libelle(creee) + " créée");
        return creee;
    }

    /** Crée la salle dans le bâtiment (sans journaliser). */
    private Salle creerDans(Long idBatiment, Salle salle) {
        Batiment batiment = trouverBatiment(idBatiment);
        if (batiment.getSalles().stream().anyMatch(s -> s.getNumSalle().equals(salle.getNumSalle())))
            throw new ServiceException("Une salle " + salle.getNumSalle() + " existe déjà dans ce bâtiment");
        salle.setCreePar(nomUtilisateurCourant());
        Salle creee = create(salle);
        batiment.addSalle(creee);
        return creee;
    }

    /** « Prénom Nom » de l'utilisateur connecté (null au démarrage, dans l'Init). */
    private static String nomUtilisateurCourant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof Personnel p ? p.getPrenom() + " " + p.getNom() : null;
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
        Salle creee = creerDans(dto.getIdBatiment(), salle); // vérifie le bâtiment et le numéro en double
        Batiment batiment = trouverBatiment(dto.getIdBatiment());
        Map<String, Deque<EnStock>> stock = stockDu(batiment);
        Map<String, Integer> manquants = new LinkedHashMap<>();

        String prefixe = prefixePostes(batiment, creee);
        List<String> numeros = new ArrayList<>();
        numeros.add(prefixe + "F"); // formateur en premier = servi en priorité
        for (int i = 1; i <= dto.getNbPostes(); i++) numeros.add(prefixe + String.format("P%02d", i));

        for (String numero : numeros) {
            Poste poste = new Poste();
            poste.setNumPoste(numero);
            Poste cree = ajouterPoste(creee.getId(), poste);
            prendre(stock, UC.TYPE, 1, manquants).forEach(cree::addMateriel);
            prendre(stock, Ecran.TYPE, dto.getEcransParPoste(), manquants).forEach(cree::addMateriel);
        }
        prendre(stock, VideoProjecteur.TYPE, dto.getNbVideoProjecteurs(), manquants).forEach(creee::addMateriel);
        prendre(stock, TableauInteractif.TYPE, dto.getNbTableaux(), manquants).forEach(creee::addMateriel);

        if (!manquants.isEmpty()) {
            boolean plusRien = stock.values().stream().allMatch(Deque::isEmpty);
            log.warn("Salle {} du bâtiment {} créée incomplète{} : il manque {}",
                    creee.getNumSalle(), batiment.getNumBatiment(),
                    plusRien ? " (plus rien en stock)" : "", manquants);
        }
        journalisationService.journaliser(TypeAction.CREATION, "Salle " + libelle(creee) + " créée équipée : "
                + dto.getNbPostes() + " poste(s) stagiaire(s), " + dto.getNbVideoProjecteurs() + " vidéoprojecteur(s), "
                + dto.getNbTableaux() + " tableau(x)" + manque(manquants));
        return new SalleEquipeeResponseDto(creee.getId(), manquants);
    }

    @Override
    @Transactional
    public SalleEquipeeResponseDto modifierComposition(Long idSalle, SalleModificationRequestDto dto) {
        Salle salle = trouver(idSalle);
        if (salle.getType() != TypeSalle.SalleDeCours)
            throw new ServiceException("Seule une salle de cours se modifie");
        Batiment batiment = batimentRepository.findBySalles_Id(idSalle)
                .orElseThrow(() -> new ServiceException("La salle " + salle.getNumSalle() + " n'est dans aucun bâtiment"));
        Map<String, Deque<EnStock>> stock = stockDu(batiment);
        Map<String, Integer> manquants = new LinkedHashMap<>();

        // postes stagiaires, du plus petit numéro au plus grand
        List<Poste> postes = salle.getPostes().stream()
                .filter(p -> !p.getNumPoste().endsWith("-F"))
                .sorted(Comparator.comparingInt(SalleServiceImpl::numeroPoste))
                .toList();

        if (dto.getNbPostes() < postes.size()) {
            // les derniers postes disparaissent : matériel en stock, stagiaire sans poste
            for (Poste p : postes.subList(dto.getNbPostes(), postes.size())) {
                retirerPoste(p.getId());
            }
        } else if (dto.getNbPostes() > postes.size()) {
            // même nombre d'écrans que les postes existants (1 si la salle n'en a pas)
            int ecransParPoste = postes.stream()
                    .mapToInt(p -> (int) p.getMateriels().stream().filter(m -> Ecran.TYPE.equals(m.getType())).count())
                    .max().orElse(1);
            int numero = postes.isEmpty() ? 0 : numeroPoste(postes.get(postes.size() - 1));
            String prefixe = prefixePostes(batiment, salle);
            for (int i = postes.size(); i < dto.getNbPostes(); i++) {
                Poste poste = new Poste();
                poste.setNumPoste(prefixe + String.format("P%02d", ++numero));
                Poste cree = ajouterPoste(idSalle, poste);
                prendre(stock, UC.TYPE, 1, manquants).forEach(cree::addMateriel);
                prendre(stock, Ecran.TYPE, Math.max(ecransParPoste, 1), manquants).forEach(cree::addMateriel);
            }
        }

        ajuster(salle, VideoProjecteur.TYPE, dto.getNbVideoProjecteurs(), stock, manquants);
        ajuster(salle, TableauInteractif.TYPE, dto.getNbTableaux(), stock, manquants);
        journalisationService.journaliser(TypeAction.MODIFICATION, "Salle " + Libelles.salle(salle) + " du bâtiment "
                + batiment.getNumBatiment() + " modifiée : " + dto.getNbPostes() + " poste(s) stagiaire(s), "
                + dto.getNbVideoProjecteurs() + " vidéoprojecteur(s), " + dto.getNbTableaux() + " tableau(x)" + manque(manquants));
        return new SalleEquipeeResponseDto(idSalle, manquants);
    }

    /** Ramène le matériel de salle d'un type à `voulu` : on prend dans le stock ou on y remet le surplus. */
    private void ajuster(Salle salle, String type, int voulu, Map<String, Deque<EnStock>> stock,
                         Map<String, Integer> manquants) {
        List<Materiel> presents = salle.getMateriels().stream()
                .filter(m -> type.equals(m.getType()))
                .toList();
        if (voulu > presents.size()) {
            prendre(stock, type, voulu - presents.size(), manquants).forEach(salle::addMateriel);
        } else if (voulu < presents.size()) {
            Salle reserve = trouverStockage(salle.getId());
            for (Materiel m : presents.subList(voulu, presents.size())) {
                salle.removeMateriel(m);
                reserve.addMateriel(m);
            }
        }
    }

    @Override
    @Transactional
    public Salle geler(Long idSalle, MotifGel motif, String note, LocalDate jusqu) {
        Salle salle = trouver(idSalle);
        if (salle.getType() != TypeSalle.SalleDeCours)
            throw new ServiceException("Seule une salle de cours se gèle");
        if (salle.isSuspendu())
            throw new ServiceException("La salle " + salle.getNumSalle() + " est déjà gelée");
        if (motif == null)
            throw new ServiceException("Choisissez un motif");
        if (jusqu != null && jusqu.isBefore(LocalDate.now()))
            throw new ServiceException("La date de fin prévue est déjà passée");
        salle.setSuspendu(true);
        salle.setGelMotif(motif);
        salle.setGelNote(note == null || note.isBlank() ? null : note.trim());
        salle.setGelDepuis(LocalDate.now());
        salle.setGelJusqu(jusqu);
        journalisationService.journaliser(TypeAction.MODIFICATION, "Salle " + libelle(salle) + " gelée (motif : "
                + motif.name().toLowerCase().replace('_', ' ') + (jusqu != null ? ", jusqu'au " + jusqu : "") + ")");
        return salle;
    }

    @Override
    @Transactional
    public Salle degeler(Long idSalle) {
        Salle salle = trouver(idSalle);
        if (!salle.isSuspendu())
            throw new ServiceException("La salle " + salle.getNumSalle() + " n'est pas gelée");
        salle.setSuspendu(false);
        salle.setGelMotif(null);
        salle.setGelNote(null);
        salle.setGelDepuis(null);
        salle.setGelJusqu(null);
        journalisationService.journaliser(TypeAction.MODIFICATION, "Salle " + libelle(salle) + " dégelée");
        return salle;
    }

    @Override
    @Transactional
    public Salle convertirEnStockage(Long idSalle) {
        Salle salle = trouver(idSalle);
        if (salle.getType() != TypeSalle.SalleDeCours)
            throw new ServiceException("La salle " + salle.getNumSalle() + " est déjà une salle de stockage");
        if (salle.isSuspendu())
            throw new ServiceException("La salle " + salle.getNumSalle() + " est gelée : dégelez-la avant de la convertir");
        if (salle.getStage() != null)
            throw new ServiceException("La salle " + salle.getNumSalle() + " accueille le "
                    + Libelles.stage(salle.getStage()) + " : retirez-le avant de la convertir");
        long occupes = salle.getPostes().stream().filter(p -> p.getOccupant() != null).count();
        if (occupes > 0)
            throw new ServiceException(occupes + " stagiaire(s) encore installé(s) dans la salle "
                    + salle.getNumSalle() + " : retirez-les avant de la convertir");
        if (!salle.getPersonnels().isEmpty())
            throw new ServiceException("Retirez les formateurs attribués à la salle " + salle.getNumSalle()
                    + " avant de la convertir");

        salle.setMaster(null);
        // le matériel des postes reste dans la salle : il devient son stock
        int nbPostes = salle.getPostes().size();
        for (Poste p : List.copyOf(salle.getPostes())) {
            for (Materiel m : List.copyOf(p.getMateriels())) {
                p.removeMateriel(m);
                salle.addMateriel(m);
            }
            salle.removePoste(p);
            posteService.delete(p);
        }
        salle.setType(TypeSalle.SalleStockage);
        journalisationService.journaliser(TypeAction.MODIFICATION, "Salle " + libelle(salle)
                + " convertie en salle de stockage (" + nbPostes + " poste(s) supprimé(s), "
                + salle.getMateriels().size() + " matériel(s) en stock)");
        return salle;
    }

    @Override
    @Transactional
    public Salle convertirEnSalleDeCours(Long idSalle, Long idStockageCible) {
        Salle salle = trouver(idSalle);
        if (salle.getType() != TypeSalle.SalleStockage)
            throw new ServiceException("La salle " + salle.getNumSalle() + " est déjà une salle de cours");
        List<Salle> autres = repository.findSallesDuBatiment(idSalle, TypeSalle.SalleStockage).stream()
                .filter(r -> !r.getId().equals(idSalle))
                .toList();
        if (autres.isEmpty())
            throw new ServiceException("La salle " + salle.getNumSalle()
                    + " est la seule salle de stockage du bâtiment : créez-en une autre avant de la convertir");

        // vidéoprojecteurs et tableaux restent dans la salle, le reste part dans l'autre stockage
        List<Materiel> aDeplacer = salle.getMateriels().stream()
                .filter(m -> m.isArchive()
                        || !(VideoProjecteur.TYPE.equals(m.getType()) || TableauInteractif.TYPE.equals(m.getType())))
                .toList();
        String vers = "";
        if (!aDeplacer.isEmpty()) {
            if (idStockageCible == null)
                throw new ServiceException("Choisissez la salle de stockage qui reçoit le matériel");
            Salle cible = autres.stream()
                    .filter(r -> r.getId().equals(idStockageCible))
                    .findFirst()
                    .orElseThrow(() -> new ServiceException("Le matériel doit aller dans une autre salle de stockage du même bâtiment"));
            for (Materiel m : aDeplacer) {
                salle.removeMateriel(m);
                cible.addMateriel(m);
            }
            vers = ", " + aDeplacer.size() + " matériel(s) déplacé(s) vers la " + Libelles.salle(cible);
        }
        salle.setType(TypeSalle.SalleDeCours);
        journalisationService.journaliser(TypeAction.MODIFICATION, "Salle " + libelle(salle)
                + " convertie en salle de cours" + vers);
        return salle;
    }

    @Override
    @Transactional
    public Salle modifierStockage(Long idSalle, String numSalle, String etage) {
        Salle salle = trouver(idSalle);
        if (salle.getType() != TypeSalle.SalleStockage)
            throw new ServiceException("Seule une salle de stockage se modifie ainsi");
        if (numSalle == null || numSalle.isBlank() || etage == null || etage.isBlank())
            throw new ServiceException("Le numéro et l'étage sont obligatoires");
        String num = numSalle.trim();
        Batiment batiment = batimentRepository.findBySalles_Id(idSalle)
                .orElseThrow(() -> new ServiceException("La salle " + salle.getNumSalle() + " n'est dans aucun bâtiment"));
        if (batiment.getSalles().stream().anyMatch(s -> !s.getId().equals(idSalle) && s.getNumSalle().equals(num)))
            throw new ServiceException("Une salle " + num + " existe déjà dans ce bâtiment");
        String ancien = salle.getNumSalle();
        salle.setNumSalle(num);
        salle.setEtage(etage.trim());
        journalisationService.journaliser(TypeAction.MODIFICATION, "Salle de stockage " + ancien + " du bâtiment "
                + batiment.getNumBatiment() + " modifiée : salle " + num + ", étage " + salle.getEtage());
        return salle;
    }

    @Override
    @Transactional
    public Salle deplacer(Long idSalle, Long idSalleCible) {
        if (idSalle.equals(idSalleCible))
            throw new ServiceException("Choisissez une autre salle que celle à déplacer");
        Salle source = trouver(idSalle);
        Salle cible = trouver(idSalleCible);
        for (Salle s : List.of(source, cible)) {
            if (s.getType() != TypeSalle.SalleDeCours)
                throw new ServiceException("Seule une salle de cours se déplace");
            if (s.isSuspendu())
                throw new ServiceException("La salle " + s.getNumSalle() + " est gelée : dégelez-la avant le déplacement");
        }
        if (cible.getStage() != null)
            throw new ServiceException("La salle " + cible.getNumSalle() + " accueille déjà le "
                    + Libelles.stage(cible.getStage()) + " : retirez-le avant le déplacement");

        // taille de la cible = ses postes stagiaires et son matériel de salle
        verifierPlace(cible, "poste(s) stagiaire(s)", postesStagiaires(source), postesStagiaires(cible));
        verifierPlace(cible, "vidéoprojecteur(s)", nbDeType(source, VideoProjecteur.TYPE), nbDeType(cible, VideoProjecteur.TYPE));
        verifierPlace(cible, "tableau(x) interactif(s)", nbDeType(source, TableauInteractif.TYPE), nbDeType(cible, TableauInteractif.TYPE));

        String libelleSource = libelle(source);
        String libelleCible = libelle(cible);
        Batiment batimentCible = batimentRepository.findBySalles_Id(idSalleCible)
                .orElseThrow(() -> new ServiceException("La salle " + cible.getNumSalle() + " n'est dans aucun bâtiment"));

        // 1. l'ancien contenu de la cible retourne au stock de son bâtiment (poste formateur compris)
        if (!cible.getMateriels().isEmpty()) {
            Salle reserve = trouverStockage(idSalleCible);
            for (Materiel m : List.copyOf(cible.getMateriels())) {
                cible.removeMateriel(m);
                reserve.addMateriel(m);
            }
        }
        for (Poste p : List.copyOf(cible.getPostes())) {
            retirerPoste(p.getId());
        }
        // les anciens postes doivent être supprimés en base avant de reprendre leurs numéros
        repository.flush();

        // 2. postes de la source, renumérotés pour la cible ("B1-S101-P07" -> "B2-S204-P07")
        int nbPostes = source.getPostes().size();
        String prefixe = prefixePostes(batimentCible, cible);
        for (Poste p : List.copyOf(source.getPostes())) {
            source.removePoste(p);
            p.setNumPoste(prefixe + p.getNumPoste().substring(p.getNumPoste().lastIndexOf('-') + 1));
            cible.addPoste(p);
        }

        // 3. matériel de salle (vidéoprojecteurs, tableaux)
        int nbMateriels = source.getMateriels().size();
        for (Materiel m : List.copyOf(source.getMateriels())) {
            source.removeMateriel(m);
            cible.addMateriel(m);
        }

        // 4. formateurs attribués (avec leur date limite) et master
        new HashMap<>(source.getAcces()).forEach((formateur, jusqu) -> {
            source.removePersonnel(formateur);
            if (cible.personnel(formateur.getId()).isEmpty()) cible.addPersonnel(formateur, jusqu);
        });
        cible.setMaster(source.getMaster());
        source.setMaster(null);

        // 5. stage (un stage n'est que dans une salle : on le libère en base avant de le poser sur la cible)
        Stage stage = source.getStage();
        if (stage != null) {
            source.setStage(null);
            repository.flush();
            cible.setStage(stage);
        }

        journalisationService.journaliser(TypeAction.MODIFICATION, "Salle " + libelleSource + " déplacée vers la salle "
                + libelleCible + " : " + nbPostes + " poste(s), " + nbMateriels + " matériel(s) de salle"
                + (stage != null ? ", " + Libelles.stage(stage) : "") + " (ancien contenu de la salle "
                + cible.getNumSalle() + " remis en stock)");
        return cible;
    }

    private static void verifierPlace(Salle cible, String quoi, long besoin, long place) {
        if (besoin > place)
            throw new ServiceException("La salle " + cible.getNumSalle() + " n'a que " + place + " " + quoi
                    + ", il en faut " + besoin);
    }

    private static long postesStagiaires(Salle salle) {
        return salle.getPostes().stream().filter(p -> !p.getNumPoste().endsWith("-F")).count();
    }

    private static long nbDeType(Salle salle, String type) {
        return salle.getMateriels().stream().filter(m -> type.equals(m.getType())).count();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Salle> sallesDuMaster(Long idMaster) {
        return repository.findByMaster_Id(idMaster);
    }

    private Salle trouver(Long idSalle) {
        return repository.findById(idSalle).orElseThrow(() -> new ServiceException("Salle " + idSalle + " introuvable"));
    }

    /** Pour le journal : "105 du bâtiment 2" (le numéro de salle n'est unique que dans son bâtiment). */
    private String libelle(Salle salle) {
        return salle.getNumSalle() + batimentRepository.findBySalles_Id(salle.getId())
                .map(b -> " du bâtiment " + b.getNumBatiment()).orElse("");
    }

    /** Pour le journal : " (incomplète, il manque {UC=2})" s'il manquait du matériel en stock. */
    private static String manque(Map<String, Integer> manquants) {
        return manquants.isEmpty() ? "" : " (incomplète, il manque " + manquants + ")";
    }

    /** Stock du bâtiment, rangé par type (matériel archivé exclu). */
    private Map<String, Deque<EnStock>> stockDu(Batiment batiment) {
        Map<String, Deque<EnStock>> stock = new HashMap<>();
        for (Salle reserve : batiment.getSalles()) {
            if (reserve.getType() != TypeSalle.SalleStockage) continue;
            for (Materiel m : reserve.getMateriels()) {
                if (!m.isArchive())
                    stock.computeIfAbsent(m.getType(), t -> new ArrayDeque<>()).add(new EnStock(m, reserve));
            }
        }
        return stock;
    }

    /** numPoste est unique sur tout le parc : on préfixe par bâtiment + salle (ex. "B2-S105-"). */
    private static String prefixePostes(Batiment batiment, Salle salle) {
        return "B" + batiment.getNumBatiment() + "-S" + salle.getNumSalle() + "-";
    }

    /** "B2-S105-P07" -> 7 (0 si le numéro ne suit pas ce format). */
    private static int numeroPoste(Poste poste) {
        String num = poste.getNumPoste();
        int i = num.lastIndexOf("-P");
        try {
            return i < 0 ? 0 : Integer.parseInt(num.substring(i + 2));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Retire jusqu'à `quantite` matériels du stock ; note ce qui manque. */
    private List<Materiel> prendre(Map<String, Deque<EnStock>> stock, String type,
                                   int quantite, Map<String, Integer> manquants) {
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
        Poste cree = ajouterPoste(idSalle, poste);
        journalisationService.journaliser(TypeAction.CREATION, "Poste " + poste.getNumPoste() + " créé");
        return cree;
    }

    /** Crée le poste dans la salle (sans journaliser : sert aussi à la création et à la modification de salle). */
    private Poste ajouterPoste(Long idSalle, Poste poste) {
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
        Poste poste = retirerPoste(idPoste);
        journalisationService.journaliser(TypeAction.SUPPRESSION,
                "Poste " + poste.getNumPoste() + " supprimé (matériel remis en stock)");
    }

    /** Supprime le poste et remet son matériel en stock (sans journaliser : sert aussi à la modification de salle). */
    private Poste retirerPoste(Long idPoste) {
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
        return poste;
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
    public Optional<Salle> localiser(Long idMateriel) {
        return repository.findByMateriels_Id(idMateriel)
                .or(() -> posteService.posteDuMateriel(idMateriel)
                        .flatMap(poste -> repository.findByPostes_Id(poste.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalleCarteDto> findCartesDuBatiment(Long idBatiment) {
        return cartes(repository.findByBatiment(idBatiment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalleCarteDto> findCartesDuFormateur(Long idFormateur) {
        List<Salle> salles = repository.sallesActivesDuFormateur(idFormateur);
        List<SalleCarteDto> cartes = cartes(salles);
        if (cartes.isEmpty()) return cartes;
        Map<Long, String> batiments = repository.batimentsDesSalles(salles.stream().map(Salle::getId).toList()).stream()
                .collect(Collectors.toMap(SalleBatiment::idSalle, SalleBatiment::numBatiment));
        cartes.forEach(c -> c.setNumBatiment(batiments.get(c.getId())));
        return cartes;
    }

    @Override
    @Transactional(readOnly = true)
    public void verifierAcces(Long idSalle, Personnel utilisateur) {
        if (utilisateur.getRole() == Role.FORMATEUR && !repository.existsByIdAndPersonnels_Id(idSalle, utilisateur.getId()))
            throw new AccessDeniedException("Salle " + idSalle + " non affectée à ce formateur");
    }

    /** Cartes de ces salles : tout est compté en base, nombre de requêtes fixe. */
    private List<SalleCarteDto> cartes(List<Salle> salles) {
        if (salles.isEmpty()) return List.of();
        List<Long> ids = salles.stream().map(Salle::getId).toList();

        Map<Long, Long> nbPostes = repository.nbPostes(ids).stream()
                .collect(Collectors.toMap(IdCompte::id, IdCompte::nb));
        Map<Long, AnneauDto> anneaux = anneaux(ids);
        // réserves : pas de poste, on compte leur matériel
        List<Long> idsStockage = salles.stream().filter(s -> s.getType() == TypeSalle.SalleStockage).map(Salle::getId).toList();
        if (!idsStockage.isEmpty())
            repository.comptesEtats(idsStockage, Materiel.seuilARemplacer())
                    .forEach(c -> anneaux.put(c.id(), AnneauDto.de(c)));

        return salles.stream().map(s -> SalleCarteDto.builder()
                .id(s.getId())
                .numSalle(s.getNumSalle())
                .etage(s.getEtage())
                .type(s.getType())
                .nbPostes(nbPostes.getOrDefault(s.getId(), 0L))
                .anneau(anneaux.getOrDefault(s.getId(), AnneauDto.VIDE))
                .suspendu(s.isSuspendu())
                .gelMotif(s.getGelMotif())
                .gelJusqu(s.getGelJusqu())
                .build()).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AnneauDto anneauDe(Long idSalle) {
        return anneaux(List.of(idSalle)).getOrDefault(idSalle, AnneauDto.VIDE);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, AnneauDto> anneaux(List<Long> idsSalles) {
        if (idsSalles.isEmpty()) return Map.of();
        List<Salle> salles = repository.chargerPostes(idsSalles);
        Map<Long, EtatPoste> etats = posteService.etats(salles.stream().flatMap(s -> s.getPostes().stream()).toList());
        Map<Long, AnneauDto> anneaux = new HashMap<>();
        salles.forEach(s -> anneaux.put(s.getId(),
                AnneauDto.dePostes(s.getPostes().stream().map(p -> etats.get(p.getId())).toList())));
        return anneaux;
    }
}