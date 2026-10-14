package net.ent.etnc.orbit.init;

import net.ent.etnc.orbit.models.entities.*;
import net.ent.etnc.orbit.models.enums.Role;
import net.ent.etnc.orbit.models.enums.TypeMateriel;
import net.ent.etnc.orbit.models.enums.TypeSalle;
import net.ent.etnc.orbit.services.*;
import net.ent.etnc.orbit.services.commons.ServiceException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Component
public class Init implements CommandLineRunner {

    /** Graine fixe : les mêmes données à chaque réinitialisation de la base, plus simple pour déboguer. */
    private final Random random = new Random(47);
    private final LocalDate aujourdhui = LocalDate.now();
    private int numeroSerie = 1;

    private static final String[] NUMEROS_BATIMENTS = {"351", "352", "215", "216", "208",
            "209", "213", "214", "207", "206", "205", "204", "203", "202", "201"};

    /** Stagiaires générés en plus des 3 fixes. Chacun occupe au plus un poste (mot de passe chiffré : ~80 ms chacun). */
    private static final int NB_STAGIAIRES_GENERES = 300;

    private static final String[] PRENOMS = {"Lucas", "Emma", "Hugo", "Lea", "Nathan", "Chloe", "Louis",
            "Manon", "Theo", "Camille", "Enzo", "Sarah", "Mathis", "Ines", "Tom", "Jade", "Noah", "Lina"};
    private static final String[] NOMS = {"Martin", "Bernard", "Dubois", "Thomas", "Robert", "Richard",
            "Petit", "Durand", "Leroy", "Moreau", "Simon", "Laurent", "Lefebvre", "Michel", "Garcia",
            "David", "Bertrand", "Roux", "Vincent", "Fournier", "Morel", "Girard", "Andre", "Mercier"};

    /** Un modèle et sa probabilité de tomber en panne : certains modèles sont des séries à problèmes. */
    private record Modele(String nom, TypeMateriel type, double tauxPanne) {}

    private static final List<Modele> UC = List.of(
            new Modele("Dell OptiPlex 7010", TypeMateriel.UC, 0.03),
            new Modele("HP EliteDesk 800 G6", TypeMateriel.UC, 0.25),       // série à problèmes
            new Modele("Lenovo ThinkCentre M70q", TypeMateriel.UC, 0.05));
    private static final List<Modele> ECRANS = List.of(
            new Modele("Dell P2422H", TypeMateriel.Ecran, 0.02),
            new Modele("Iiyama ProLite XU2493", TypeMateriel.Ecran, 0.20),  // série à problèmes
            new Modele("HP E24 G5", TypeMateriel.Ecran, 0.04));
    private static final List<Modele> VIDEOPROJECTEURS = List.of(
            new Modele("Epson EB-W51", TypeMateriel.VideoProjecteur, 0.05),
            new Modele("BenQ MW560", TypeMateriel.VideoProjecteur, 0.30));  // série à problèmes
    private static final List<Modele> TABLEAUX = List.of(
            new Modele("Samsung Flip Pro", TypeMateriel.TableauInteractif, 0.10),
            new Modele("Promethean ActivPanel", TypeMateriel.TableauInteractif, 0.04));

    /** Nombre de postes stagiaires par salle : 0 = salle sans poste (réunion, travaux), rare. */
    private static final int[] TAILLES_SALLES = {0, 6, 8, 10, 12, 12, 14, 16, 16, 20};

    private static final String[] PANNES = {"L'écran reste noir au démarrage",
            "Le poste redémarre tout seul", "Ventilateur très bruyant", "Pixels morts sur l'écran",
            "Le poste ne s'allume plus", "Image qui scintille", "Lenteurs importantes au démarrage"};

    /** Ce qu'on veut voir sur un poste. Chaque salle en a un peu de chaque. */
    private enum Profil { NORMAL, VIDE, DEFECTUEUX, A_CHANGER, INCOMPLET }

    private final PersonnelService personnelService;
    private final MaterielService materielService;
    private final BatimentService batimentService;
    private final SalleService salleService;
    private final PosteService posteService;
    private final AffecterMaterielService affecterMaterielService;
    private final DefaillanceService defaillanceService;

    public Init(PersonnelService personnelService, MaterielService materielService,
                BatimentService batimentService, SalleService salleService,
                PosteService posteService, AffecterMaterielService affecterMaterielService,
                DefaillanceService defaillanceService) {
        this.personnelService = personnelService;
        this.materielService = materielService;
        this.batimentService = batimentService;
        this.salleService = salleService;
        this.posteService = posteService;
        this.affecterMaterielService = affecterMaterielService;
        this.defaillanceService = defaillanceService;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Évite de recréer toutes les données à chaque démarrage.
        if (personnelService.count() > 0) return;

        // ---------- Personnel ----------
        personnelService.create(buildUser("1000000001", "Admin", "Orbit",
                "admin", "1234", Role.ADMINISTRATEUR));
        Personnel gestionnaire = personnelService.create(buildUser("3000000001", "Besnard", "Romain",
                "r.besnard", "1234", Role.GESTIONNAIRE));
        personnelService.create(buildUser("3000000002", "Martin", "Alice",
                "a.martin", "1234", Role.GESTIONNAIRE));

        List<Personnel> formateurs = new ArrayList<>();
        formateurs.add(personnelService.create(buildUser("2000000001", "Ledeme",
                "Kevin", "k.ledeme", "1234", Role.FORMATEUR)));
        formateurs.add(personnelService.create(buildUser("2000000002", "Durand",
                "Sophie", "s.durand", "1234", Role.FORMATEUR)));
        for (int i = 0; i < 6; i++) {
            formateurs.add(personnelService.create(personneGeneree("2", 10 + i, Role.FORMATEUR)));
        }

        // Les 3 stagiaires fixes passent en premier : ils ont toujours un poste (page « Mon poste »).
        List<Personnel> stagiairesFixes = List.of(
                personnelService.create(buildUser("4000000001", "Le Buhé", "Tanguy", "t.lebuhe", "1234", Role.STAGIAIRE)),
                personnelService.create(buildUser("4000000002", "Bernard", "Marc", "m.bernard", "1234", Role.STAGIAIRE)),
                personnelService.create(buildUser("4000000003", "Petit", "Julie", "j.petit", "1234", Role.STAGIAIRE)));
        List<Personnel> stagiairesGeneres = new ArrayList<>();
        for (int i = 0; i < NB_STAGIAIRES_GENERES; i++) {
            stagiairesGeneres.add(personnelService.create(personneGeneree("4", 10 + i, Role.STAGIAIRE)));
        }
        Personnel inactif = personnelService.create(buildUser("4000000004", "Inactif",
                "Paul", "p.inactif", "1234", Role.STAGIAIRE));
        Personnel verrouille = personnelService.create(buildUser("4000000005", "Verrouille",
                "Paul", "p.verrouille", "1234", Role.STAGIAIRE));
        personnelService.desactiver(inactif.getId());
        verrouille.setVerrouille(true);

        // Postes à occuper, remplis à la fin pour répartir les stagiaires sur tout le parc
        List<Poste> aOccuper = new ArrayList<>();

        // ---------- Bâtiments, salles, postes, matériel ----------
        for (String numBatiment : NUMEROS_BATIMENTS) {
            Batiment batiment = batimentService.create(buildBatiment(numBatiment));
            Salle stockage = stockageDe(batiment);

            // de 2 à 4 niveaux (rez-de-chaussée au 3e), 1 à 3 salles par niveau
            int nbNiveaux = 2 + random.nextInt(3);
            for (int etage = 0; etage < nbNiveaux; etage++) {
                int nbSallesEtage = 1 + random.nextInt(3);
                for (int n = 1; n <= nbSallesEtage; n++) {
                    String numSalle = String.format("%d%02d", etage, n); // 001, 002, 101, 102...
                    creerSalleEquipee(batiment, numSalle, etage, stockage, formateurs, gestionnaire, aOccuper);
                }
            }

            // stock de rechange : de quoi équiper 3 ou 4 salles, plutôt récent
            remplirStock(stockage, UC, 40 + random.nextInt(21));               // 40 à 60 UC
            remplirStock(stockage, ECRANS, 60 + random.nextInt(41));           // 60 à 100 écrans
            remplirStock(stockage, VIDEOPROJECTEURS, 3 + random.nextInt(4));   // 3 à 6 vidéoprojecteurs
            remplirStock(stockage, TABLEAUX, 2 + random.nextInt(3));           // 2 à 4 tableaux

            // un vieux matériel archivé dans 1 bâtiment sur 3 (ne doit pas compter dans l'anneau)
            if (random.nextInt(3) == 0) {
                Materiel vieux = creerMateriel(choisir(UC), aujourdhui.minusYears(6), stockage);
                materielService.archiver(vieux.getId());
            }
        }

        // ---------- Occupants : un stagiaire n'a qu'un seul poste ----------
        Collections.shuffle(aOccuper, random);
        Collections.shuffle(stagiairesGeneres, random);
        List<Personnel> stagiaires = new ArrayList<>(stagiairesFixes);
        stagiaires.addAll(stagiairesGeneres);
        // les postes en trop (plus de postes que de stagiaires) restent vides
        for (int i = 0; i < Math.min(aOccuper.size(), stagiaires.size()); i++) {
            aOccuper.get(i).setOccupant(stagiaires.get(i));
        }

        // Réserve hors bâtiment, conservée comme avant.
        Salle stockagePrincipal = salleService.create(
                buildSalle("999", "0", TypeSalle.SalleStockage));
        creerMateriel(UC.get(0), aujourdhui.minusMonths(2), stockagePrincipal);
        creerMateriel(ECRANS.get(0), aujourdhui.minusMonths(2), stockagePrincipal);
        creerMateriel(VIDEOPROJECTEURS.get(0), aujourdhui.minusMonths(2), stockagePrincipal);
    }

    /**
     * Crée une salle de cours avec son poste formateur et ses postes stagiaires.
     * Chaque salle d'au moins 5 postes a au moins un poste vide, défectueux, à changer et incomplet.
     */
    private void creerSalleEquipee(Batiment batiment, String numSalle, int etage, Salle stockage,
                                   List<Personnel> formateurs, Personnel gestionnaire,
                                   List<Poste> aOccuper) throws ServiceException {
        Salle salle = salleService.create(batiment.getId(),
                buildSalle(numSalle, Integer.toString(etage), TypeSalle.SalleDeCours));
        Personnel formateur = choisir(formateurs);
        salle.addPersonnel(formateur);

        int nbPostes = choisir(TAILLES_SALLES);
        if (nbPostes == 0) return; // salle de réunion : ni poste ni matériel

        // tout le matériel d'une salle est acheté à peu près en même temps, il y a moins de 18 mois
        LocalDate equipement = aujourdhui.minusMonths(1 + random.nextInt(18));
        int ecransParPoste = random.nextInt(4) == 0 ? 2 : 1;
        String prefixe = "B" + batiment.getNumBatiment() + "-S" + numSalle + "-";

        // poste formateur : toujours complet, sans occupant stagiaire
        Poste posteFormateur = creerPoste(prefixe + "F", salle);
        installer(posteFormateur, choisir(UC), equipement, stockage);
        for (int e = 0; e < ecransParPoste; e++) installer(posteFormateur, choisir(ECRANS), equipement, stockage);

        int numero = 0;
        for (Profil profil : profilsDeSalle(nbPostes)) {
            Poste poste = creerPoste(prefixe + String.format("P%02d", ++numero), salle);
            LocalDate achat = profil == Profil.A_CHANGER
                    ? aujourdhui.minusMonths(26 + random.nextInt(30)) // plus de 2 ans : à remplacer
                    : equipement.minusDays(random.nextInt(20));

            Materiel uc = installer(poste, choisir(UC), achat, stockage);
            List<Materiel> ecrans = new ArrayList<>();
            int nbEcrans = profil == Profil.INCOMPLET ? ecransParPoste - 1 : ecransParPoste;
            for (int e = 0; e < nbEcrans; e++) ecrans.add(installer(poste, choisir(ECRANS), achat, stockage));

            switch (profil) {
                case DEFECTUEUX -> {
                    Materiel enPanne = ecrans.isEmpty() || random.nextBoolean() ? uc : choisir(ecrans);
                    signaler(enPanne, formateur, gestionnaire, false);
                }
                // un peu d'historique : de temps en temps une panne déjà réparée
                case NORMAL -> {
                    if (random.nextInt(10) == 0) signaler(uc, formateur, gestionnaire, true);
                }
                default -> { }
            }
            if (profil != Profil.VIDE) aOccuper.add(poste);
        }

        // 2 salles sur 3 ont un vidéoprojecteur, 1 sur 3 un tableau interactif (1 sur 4 en panne)
        if (random.nextInt(3) != 0) equiperSalle(salle, choisir(VIDEOPROJECTEURS), equipement, stockage, formateur, gestionnaire);
        if (random.nextInt(3) == 0) equiperSalle(salle, choisir(TABLEAUX), equipement, stockage, formateur, gestionnaire);
    }

    /**
     * Profils des postes d'une salle, mélangés : au moins un de chaque cas particulier
     * (si la salle est assez grande), le reste majoritairement normal.
     */
    private List<Profil> profilsDeSalle(int nbPostes) {
        List<Profil> profils = new ArrayList<>();
        if (nbPostes >= 5) {
            profils.addAll(List.of(Profil.VIDE, Profil.DEFECTUEUX, Profil.A_CHANGER, Profil.INCOMPLET));
        }
        while (profils.size() < nbPostes) {
            int tirage = random.nextInt(20);
            if (tirage < 13) profils.add(Profil.NORMAL);           // 65 %
            else if (tirage < 16) profils.add(Profil.VIDE);        // 15 %
            else if (tirage < 18) profils.add(Profil.DEFECTUEUX);  // 10 %
            else if (tirage < 19) profils.add(Profil.A_CHANGER);   //  5 %
            else profils.add(Profil.INCOMPLET);                    //  5 %
        }
        Collections.shuffle(profils, random);
        return profils;
    }

    /** Crée un matériel dans la réserve du bâtiment puis le pose sur le poste. */
    private Materiel installer(Poste poste, Modele modele, LocalDate achat, Salle stockage) throws ServiceException {
        Materiel materiel = creerMateriel(modele, achat, stockage);
        affecterMaterielService.affecterAPoste(poste.getId(), materiel.getId());
        return materiel;
    }

    /** Vidéoprojecteur ou tableau posé dans la salle ; tombe en panne selon le taux du modèle. */
    private void equiperSalle(Salle salle, Modele modele, LocalDate equipement, Salle stockage,
                              Personnel signaleur, Personnel gestionnaire) throws ServiceException {
        Materiel materiel = creerMateriel(modele, equipement.minusDays(random.nextInt(20)), stockage);
        affecterMaterielService.affecterASalle(salle.getId(), materiel.getId());
        if (random.nextDouble() < modele.tauxPanne()) signaler(materiel, signaleur, gestionnaire, false);
    }

    private void signaler(Materiel materiel, Personnel signaleur, Personnel gestionnaire, boolean resolue)
            throws ServiceException {
        Defaillance defaillance = defaillanceService.signaler(materiel.getId(), choisir(PANNES), signaleur);
        if (resolue) defaillanceService.resoudre(defaillance.getId(), "Pièce remplacée", gestionnaire);
    }

    /** Ajoute `nombre` matériels récents dans la réserve (1 sur 10 a plus de 2 ans). */
    private void remplirStock(Salle stockage, List<Modele> famille, int nombre) throws ServiceException {
        for (int i = 0; i < nombre; i++) {
            LocalDate achat = random.nextInt(10) == 0
                    ? aujourdhui.minusMonths(26 + random.nextInt(20))
                    : aujourdhui.minusMonths(random.nextInt(18));
            creerMateriel(choisir(famille), achat, stockage);
        }
    }

    private Poste creerPoste(String numPoste, Salle salle) throws ServiceException {
        Poste poste = new Poste();
        poste.setNumPoste(numPoste);
        Poste cree = posteService.create(poste);
        salle.addPoste(cree);
        return cree;
    }

    private Materiel creerMateriel(Modele modele, LocalDate acquisition, Salle stockage)
            throws ServiceException {
        Materiel materiel = new Materiel();
        materiel.setNumSerie(String.format("%s-%05d", prefixe(modele.type()), numeroSerie++));
        materiel.setModele(modele.nom());
        materiel.setDateAcquisition(acquisition);
        materiel.setDateFinGarantie(acquisition.plusYears(3));
        materiel.setType(modele.type());
        return materielService.creerEnStock(materiel, stockage.getId());
    }

    private static String prefixe(TypeMateriel type) {
        return switch (type) {
            case UC -> "UC";
            case Ecran -> "EC";
            case VideoProjecteur -> "VP";
            case TableauInteractif -> "TI";
        };
    }

    /** Personne générée : nid = préfixe du rôle + numéro, login unique grâce au numéro. */
    private Personnel personneGeneree(String prefixeNid, int numero, Role role) {
        String prenom = choisir(PRENOMS);
        String nom = choisir(NOMS);
        String login = prenom.toLowerCase().charAt(0) + "." + nom.toLowerCase() + numero;
        return buildUser(String.format("%s%09d", prefixeNid, numero), nom, prenom, login, "1595", role);
    }

    private <T> T choisir(List<T> liste) {
        return liste.get(random.nextInt(liste.size()));
    }

    private <T> T choisir(T[] tableau) {
        return tableau[random.nextInt(tableau.length)];
    }

    private int choisir(int[] tableau) {
        return tableau[random.nextInt(tableau.length)];
    }

    private Personnel buildUser(String nid, String nom, String prenom, String login,
                                String password, Role role) {
        Personnel utilisateur = new Personnel();
        utilisateur.setNid(nid);
        utilisateur.setNom(nom);
        utilisateur.setPrenom(prenom);
        utilisateur.setLogin(login);
        utilisateur.setMotDePasse(password);
        utilisateur.setRole(role);
        utilisateur.setActive(true);
        return utilisateur;
    }

    private Salle buildSalle(String numSalle, String etage, TypeSalle type) {
        Salle salle = new Salle();
        salle.setNumSalle(numSalle);
        salle.setEtage(etage);
        salle.setType(type);
        return salle;
    }

    private Salle stockageDe(Batiment batiment) {
        return batiment.getSalles().stream()
                .filter(salle -> salle.getType() == TypeSalle.SalleStockage)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Aucune salle de stockage pour le bâtiment "
                                + batiment.getNumBatiment()));
    }

    private Batiment buildBatiment(String numBatiment) {
        Batiment batiment = new Batiment();
        batiment.setNumBatiment(numBatiment);
        return batiment;
    }
}