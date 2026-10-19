package net.ent.etnc.orbit.services.impl;

import net.ent.etnc.orbit.dtos.responseDTO.BilanImportStagesDto;
import net.ent.etnc.orbit.models.entities.Personnel;
import net.ent.etnc.orbit.models.entities.Stage;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.models.enums.TypeAction;
import net.ent.etnc.orbit.models.enums.TypeFormation;
import net.ent.etnc.orbit.repositories.StageRepository;
import net.ent.etnc.orbit.repositories.projections.SalleDuStage;
import net.ent.etnc.orbit.services.JournalisationService;
import net.ent.etnc.orbit.services.PersonnelService;
import net.ent.etnc.orbit.services.StageService;
import net.ent.etnc.orbit.services.commons.AbstractService;
import net.ent.etnc.orbit.services.commons.Libelles;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StageServiceImpl extends AbstractService<Stage, StageRepository> implements StageService {

    /** Colonnes obligatoires de l'import (les autres, comme nbStagiaires, sont ignorées). */
    private static final List<String> COLONNES = List.of("nom", "type", "dateDebut", "dateFin", "nidCDSF");
    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PersonnelService personnelService;
    private final JournalisationService journalisationService;

    @Autowired
    public StageServiceImpl(StageRepository stageRepository, PersonnelService personnelService,
                            JournalisationService journalisationService) {
        super(stageRepository);
        this.personnelService = personnelService;
        this.journalisationService = journalisationService;
    }

    @Override
    @Transactional
    public Stage creer(Stage stage, Long idFormateur) throws ServiceException {
        Personnel formateur = formateur(idFormateur);
        stage.setFormateur(formateur);
        Stage cree = create(stage);
        journalisationService.journaliser(TypeAction.CREATION,
                "Stage " + stage.getNom() + " (" + stage.getType() + ") créé, formateur " + Libelles.personne(formateur));
        return cree;
    }

    @Override
    @Transactional
    public Stage modifier(Long id, Stage stage, Long idFormateur) throws ServiceException {
        stage.setId(id);
        stage.setFormateur(formateur(idFormateur));
        Stage modifie = update(stage);
        journalisationService.journaliser(TypeAction.MODIFICATION, Libelles.stage(stage) + " modifié");
        return modifie;
    }

    @Override
    @Transactional
    public Stage create(Stage stage) throws ServiceException {
        verifierDates(stage);
        if (repository.existsByNom(stage.getNom()))
            throw new ServiceException("Le stage " + stage.getNom() + " existe déjà");
        return super.create(stage);
    }

    @Override
    @Transactional
    public Stage update(Stage stage) throws ServiceException {
        verifierDates(stage);
        if (repository.existsByNomAndIdNot(stage.getNom(), stage.getId()))
            throw new ServiceException("Le stage " + stage.getNom() + " existe déjà");
        return super.update(stage);
    }

    @Override
    @Transactional
    public BilanImportStagesDto importer(String contenu) throws ServiceException {
        List<String> lignes = contenu == null ? List.of()
                : contenu.replace("\uFEFF", "").lines().map(String::strip).toList(); // retire le BOM ajouté par Excel
        if (lignes.stream().allMatch(String::isEmpty))
            throw new ServiceException("Le fichier est vide");

        // position de chaque colonne d'après l'en-tête (l'ordre peut varier, la casse est ignorée)
        List<String> entete = Arrays.stream(lignes.get(0).split(";")).map(c -> c.strip().toLowerCase(Locale.ROOT)).toList();
        Map<String, Integer> col = new HashMap<>();
        for (String c : COLONNES) {
            int i = entete.indexOf(c.toLowerCase(Locale.ROOT));
            if (i < 0) throw new ServiceException("En-tête attendu : " + String.join(";", COLONNES));
            col.put(c, i);
        }

        List<String> crees = new ArrayList<>();
        List<String> misAJour = new ArrayList<>();
        List<String> erreurs = new ArrayList<>();
        for (int n = 1; n < lignes.size(); n++) {
            if (lignes.get(n).isEmpty()) continue;
            String[] v = lignes.get(n).split(";", -1);
            try {
                importerLigne(v, col, crees, misAJour);
            } catch (ServiceException e) {
                erreurs.add("Ligne " + (n + 1) + " : " + e.getMessage());
            }
        }
        journalisationService.journaliser(TypeAction.IMPORT, "Import de stages : " + crees.size() + " créé(s), "
                + misAJour.size() + " mis à jour, " + erreurs.size() + " ligne(s) rejetée(s)");
        return new BilanImportStagesDto(crees, misAJour, erreurs);
    }

    /**
     * Tout est vérifié avant d'écrire en base : une exception levée par le repository
     * annulerait l'import entier, pas seulement la ligne.
     */
    private void importerLigne(String[] v, Map<String, Integer> col, List<String> crees, List<String> misAJour) throws ServiceException {
        String nom = valeur(v, col, "nom").toUpperCase(Locale.ROOT);
        if (!nom.matches("^[A-Z]{2}[0-9]{2}$"))
            throw new ServiceException("nom « " + nom + " » invalide (2 lettres puis 2 chiffres, ex. WB47)");

        TypeFormation type;
        try {
            type = TypeFormation.valueOf(valeur(v, col, "type").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ServiceException("type « " + valeur(v, col, "type") + " » inconnu (attendu : "
                    + Arrays.stream(TypeFormation.values()).map(Enum::name).collect(Collectors.joining(", ")) + ")");
        }

        LocalDate debut = date(valeur(v, col, "dateDebut"));
        LocalDate fin = date(valeur(v, col, "dateFin"));
        if (fin.isBefore(debut))
            throw new ServiceException("la date de fin est avant la date de début");

        // NID du CDSF (Chef de Section Formation) : le formateur qui gère le stage
        String nid = valeur(v, col, "nidCDSF");
        Personnel formateur = personnelService.findByNid(nid)
                .filter(p -> p.getRole() == Role.FORMATEUR)
                .orElseThrow(() -> new ServiceException("aucun formateur (CDSF) avec le NID « " + nid + " »"));

        Optional<Stage> existant = repository.findByNom(nom);
        if (existant.isPresent()) {
            Stage stage = existant.get(); // entité suivie : enregistrée à la fin de la transaction
            stage.setType(type);
            stage.setDateDebut(debut);
            stage.setDateFin(fin);
            stage.setFormateur(formateur);
            misAJour.add(nom);
        } else {
            Stage stage = new Stage();
            stage.setNom(nom);
            stage.setType(type);
            stage.setDateDebut(debut);
            stage.setDateFin(fin);
            stage.setFormateur(formateur);
            super.create(stage);
            crees.add(nom);
        }
    }

    private static String valeur(String[] v, Map<String, Integer> col, String colonne) throws ServiceException {
        int i = col.get(colonne);
        String valeur = i < v.length ? v[i].strip() : "";
        if (valeur.isEmpty()) throw new ServiceException(colonne + " manquant");
        return valeur;
    }

    /** "2026-08-07" ou "07/08/2026". */
    private static LocalDate date(String texte) throws ServiceException {
        try {
            return texte.contains("/") ? LocalDate.parse(texte, DATE_FR) : LocalDate.parse(texte);
        } catch (DateTimeParseException e) {
            throw new ServiceException("date « " + texte + " » invalide (attendu : AAAA-MM-JJ ou JJ/MM/AAAA)");
        }
    }

    /**
     * On ne supprime qu'un stage terminé (date de fin passée), et qui n'a plus ni stagiaires ni salle :
     * il faut d'abord les retirer.
     */
    @Override
    @Transactional
    public void deleteById(Long id) throws ServiceException {
        Stage stage = repository.findById(id)
                .orElseThrow(() -> new ServiceException("Stage " + id + " introuvable"));
        if (!stage.getDateFin().isBefore(LocalDate.now()))
            throw new ServiceException("Le stage " + stage.getNom() + " n'est pas terminé : on ne supprime qu'un stage terminé");
        if (personnelService.existeDansStage(id))
            throw new ServiceException("Le stage " + stage.getNom() + " contient encore des stagiaires");
        if (repository.nbSalles(id) > 0)
            throw new ServiceException("Le stage " + stage.getNom() + " est affecté à une salle : retirez-le d'abord");
        repository.delete(stage);
        journalisationService.journaliser(TypeAction.SUPPRESSION, Libelles.stage(stage) + " supprimé");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SalleDuStage> salleDuStage(Long idStage) {
        return Optional.ofNullable(sallesDesStages(List.of(idStage)).get(idStage));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, SalleDuStage> sallesDesStages(Collection<Long> idsStages) throws ServiceException {
        if (idsStages.isEmpty()) return Map.of();
        return repository.sallesDesStages(idsStages).stream()
                .collect(Collectors.toMap(SalleDuStage::idStage, Function.identity(), (a, b) -> a));
    }

    private Personnel formateur(Long idFormateur) throws ServiceException {
        Personnel formateur = personnelService.findById(idFormateur)
                .orElseThrow(() -> new ServiceException("Formateur " + idFormateur + " introuvable"));
        if (formateur.getRole() != Role.FORMATEUR)
            throw new ServiceException("Un stage est forcément géré par un formateur");
        return formateur;
    }

    private void verifierDates(Stage stage) throws ServiceException {
        if (stage.getDateDebut() != null && stage.getDateFin() != null && stage.getDateFin().isBefore(stage.getDateDebut()))
            throw new ServiceException("La date de fin doit être après la date de début");
    }
}