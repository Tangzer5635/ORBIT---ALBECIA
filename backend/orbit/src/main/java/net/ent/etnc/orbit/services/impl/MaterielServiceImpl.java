package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.dtos.responseDTO.calendrier.CalendrierGarantiesDto;
import net.ent.etnc.orbit.dtos.responseDTO.calendrier.MoisGarantieDto;
import net.ent.etnc.orbit.models.entities.Materiel;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.entities.Processus;
import net.ent.etnc.orbit.models.entities.Salle;
import net.ent.etnc.orbit.models.entities.UC;
import net.ent.etnc.orbit.models.enums.EtatMateriel;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.models.enums.TypeAction;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.repositories.MaterielRepository;
import net.ent.etnc.orbit.services.DefaillanceService;
import net.ent.etnc.orbit.services.JournalisationService;
import net.ent.etnc.orbit.services.MaterielService;
import net.ent.etnc.orbit.services.PosteService;
import net.ent.etnc.orbit.services.SalleService;
import net.ent.etnc.orbit.services.commons.AbstractService;
import net.ent.etnc.orbit.services.commons.Libelles;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class MaterielServiceImpl extends AbstractService<Materiel, MaterielRepository> implements MaterielService {

    private final SalleService salleService;
    private final PosteService posteService;
    /** @Lazy : DefaillanceService dépend déjà de MaterielService (dépendance circulaire sinon). */
    private final DefaillanceService defaillanceService;
    private final JournalisationService journalisationService;

    @Autowired
    public MaterielServiceImpl(MaterielRepository materielRepository, SalleService salleService,
                               PosteService posteService, @Lazy DefaillanceService defaillanceService,
                               JournalisationService journalisationService) {
        super(materielRepository);
        this.salleService = salleService;
        this.posteService = posteService;
        this.defaillanceService = defaillanceService;
        this.journalisationService = journalisationService;
    }

    @Override
    @Transactional
    public Materiel creerEnStock(Materiel materiel, Long idStockage) {
        if (idStockage == null)
            throw new ServiceException("La salle de stockage est obligatoire");
        if (repository.existsByNumSerie(materiel.getNumSerie()))
            throw new ServiceException("Le numéro de série " + materiel.getNumSerie() + " existe déjà");
        Salle stockage = salleService.findById(idStockage).orElseThrow(() -> new ServiceException("Salle " + idStockage + " introuvable"));
        if (stockage.getType() != TypeSalle.SalleStockage)
            throw new ServiceException("Un matériel se crée dans une salle de stockage");
        materiel.setArchive(false);
        Materiel cree = repository.save(materiel);
        stockage.addMateriel(cree);
        journalisationService.journaliser(TypeAction.CREATION,
                Libelles.materiel(cree) + " ajouté en stock (" + Libelles.salle(stockage) + ")");
        return cree;
    }

    @Override
    @Transactional
    public Materiel update(Materiel materiel) {
        Materiel existant = trouver(materiel.getId());
        //TODO a voir avec l'équipe
        if (existant.isArchive())
            throw new ServiceException("Un matériel archivé ne peut pas être modifié");
        if (!existant.getType().equals(materiel.getType()))
            throw new ServiceException("Le type d'un matériel ne peut pas être modifié");

        if (repository.existsByNumSerieAndIdNot(materiel.getNumSerie(), materiel.getId()))
            throw new ServiceException("Le numéro de série " + materiel.getNumSerie() + " existe déjà");
        existant.setNumSerie(materiel.getNumSerie());
        existant.setModele(materiel.getModele());
        existant.setDateFinGarantie(materiel.getDateFinGarantie());
        existant.setDateAcquisition(materiel.getDateAcquisition());
        if (existant instanceof UC uc && materiel instanceof UC modifie)
            uc.setNomMachine(modifie.getNomMachine());
        Materiel enregistre = repository.save(existant);
        journalisationService.journaliser(TypeAction.MODIFICATION, Libelles.materiel(existant) + " modifié");
        return enregistre;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Salle> salleDe(Materiel materiel) {
        if (materiel.isArchive()) return Optional.empty();
        return salleService.localiser(materiel.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Materiel> findPourUtilisateur(Long id, Personnel utilisateur) {
        Optional<Materiel> materiel = repository.findById(id);
        if (materiel.isPresent() && utilisateur.getRole() == Role.FORMATEUR) {
            boolean dansSesSalles = salleDe(materiel.get())
                    .map(s -> s.getPersonnels().stream().anyMatch(p -> p.getId().equals(utilisateur.getId())))
                    .orElse(false);
            if (!dansSesSalles) {
                throw new ServiceException("Ce matériel n'est pas dans vos salles");
            }
        }
        return materiel;
    }

    @Override
    @Transactional
    public void desinstallerPartout(Processus processus) {
        repository.findUcAvecProcessus(processus.getId()).forEach(uc -> uc.removeProcessus(processus));
    }

    @Override
    @Transactional
    public int installerImage(Long idSalle, List<Processus> processus) {
        return installer(repository.findUcDeLaSalle(idSalle), processus);
    }

    @Override
    @Transactional
    public int installerImageSurPoste(Long idPoste, List<Processus> processus) {
        return installer(repository.findUcDuPoste(idPoste), processus);
    }

    /** Chaque UC a exactement ces processus (les anciens sont retirés). */
    private int installer(List<UC> ucs, List<Processus> processus) {
        for (UC uc : ucs) {
            List.copyOf(uc.getProcessus()).forEach(uc::removeProcessus);
            processus.forEach(uc::addProcessus);
        }
        return ucs.size();
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        Materiel materiel = trouver(id);
        if (defaillanceService.aDesDefaillances(id))
            throw new ServiceException("Ce matériel a un historique de défaillances : archive-le au lieu de le supprimer");
        // affecté ou en stock : on le sort d'abord de son poste ou de sa salle
        posteService.posteDuMateriel(id).ifPresent(poste -> poste.removeMateriel(materiel));
        salleService.salleDuMateriel(id).ifPresent(salle -> salle.removeMateriel(materiel));
        repository.delete(materiel);
        journalisationService.journaliser(TypeAction.SUPPRESSION, Libelles.materiel(materiel) + " supprimé");
    }

    @Override
    @Transactional
    public Materiel archiver(Long id) {
        Materiel materiel = trouver(id);
        if (materiel.isArchive())
            throw new ServiceException("Ce matériel est déjà archivé");
        Salle salle = salleService.salleDuMateriel(id).orElse(null);
        if (salle == null || salle.getType() != TypeSalle.SalleStockage)
            throw new ServiceException("Un matériel affecté ne peut pas être archivé, remets-le en stock d'abord");
        salle.removeMateriel(materiel);
        materiel.setArchive(true);
        materiel.setDateArchivage(LocalDate.now());
        journalisationService.journaliser(TypeAction.MODIFICATION, Libelles.materiel(materiel) + " archivé");
        return materiel;
    }

    @Override
    @Transactional(readOnly = true)
    public EtatMateriel etatDe(Materiel materiel) {
        if (materiel.isArchive()) return materiel.calculerEtat(false, false);
        boolean enStock = salleService.estEnStock(materiel.getId());
        boolean defaillanceOuverte = defaillanceService.aUneDefaillanceOuverte(materiel.getId());
        return materiel.calculerEtat(enStock, defaillanceOuverte);
    }

    @Override
    @Transactional
    public CalendrierGarantiesDto calendrierGaranties(int annee) {
        LocalDate debut = LocalDate.of(annee, 1, 1);   // 1er janvier
        LocalDate fin = debut.plusYears(1);            // 1er janvier suivant exclu

        // nombre de fins de garantie par mois
        long[] parMois = new long[12];
        for (LocalDate date : repository.datesFinGarantie(debut, fin)) {
            parMois[date.getMonthValue() - 1]++;
        }

        List<MoisGarantieDto> mois = new ArrayList<>();
        for (int numero = 1; numero <= 12; numero++) {
            mois.add(new MoisGarantieDto(YearMonth.of(annee, numero).toString(), parMois[numero - 1]));
        }

        // ce qui est hors garantie à ce jour
        long expirees = repository.countByArchiveFalseAndDateFinGarantieBefore(LocalDate.now());
        return new CalendrierGarantiesDto(annee, expirees, mois);
    }

    private Materiel trouver(Long id) {
        return repository.findById(id).orElseThrow(() -> new ServiceException("Matériel " + id + " introuvable"));
    }
}