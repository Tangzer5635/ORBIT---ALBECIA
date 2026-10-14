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
    private static final List<Modele> EQUIPEMENTS_SALLE = List.of(
            new Modele("Epson EB-W51", TypeMateriel.VideoProjecteur, 0.05),
            new Modele("BenQ MW560", TypeMateriel.VideoProjecteur, 0.30),   // série à problèmes
            new Modele("Samsung Flip Pro", TypeMateriel.TableauInteractif, 0.10),
            new Modele("Promethean ActivPanel", TypeMateriel.TableauInteractif, 0.04));

    /** Nombre de postes possible par salle : 0 = salle sans poste (réunion, travaux). */
    private static final int[] TAILLES_SALLES = {0, 4, 6, 8, 10, 12, 12, 14, 16, 16, 20, 24};

    /** Âge (en mois) du matériel installé dans une salle : au-delà de 24 mois, il est à remplacer. */
    private static final int[] ANCIENNETE_MOIS = {1, 3, 6, 9, 12, 15, 18, 21, 27, 33, 40, 50};

    private static final String[] PANNES = {"L'écran reste noir au démarrage",
            "Le poste redémarre tout seul", "Ventilateur très bruyant", "Pixels morts sur l'écran",
            "Le poste ne s'allume plus", "Image qui scintille", "Lenteurs importantes au démarrage"};

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
        for (int i = 0; i < 4; i++) {
            formateurs.add(personnelService.create(personneGeneree("2", 10 + i, Role.FORMATEUR)));
        }

        List<Personnel> stagiaires = new ArrayList<>();
        Personnel p1 =(personnelService.create(buildUser("4000000001", "Le Buhé",
                "Tanguy", "t.lebuhe", "1234", Role.STAGIAIRE)));
        stagiaires.add(p1);
        stagiaires.add(personnelService.create(buildUser("4000000002", "Bernard",
                "Marc", "m.bernard", "1234", Role.STAGIAIRE)));
        stagiaires.add(personnelService.create(buildUser("4000000003", "Petit",
                "Julie", "j.petit", "1234", Role.STAGIAIRE)));
        for (int i = 0; i < 40; i++) {
            stagiaires.add(personnelService.create(personneGeneree("4", 10 + i, Role.STAGIAIRE)));
        }
        Personnel inactif = personnelService.create(buildUser("4000000004", "Inactif",
                "Paul", "p.inactif", "1234", Role.STAGIAIRE));
        Personnel verrouille = personnelService.create(buildUser("4000000005", "Verrouille",
                "Paul", "p.verrouille", "1234", Role.STAGIAIRE));
        personnelService.deactivate(inactif.getId());
        verrouille.setVerrouille(true);

        // ---------- Bâtiment de test 666 ----------
        // Une salle, un poste occupé par Tanguy Le Buhé avec 1 UC et 2 écrans
        Batiment batiment666 = batimentService.create(buildBatiment("666"));
        Salle stockage666 = stockageDe(batiment666);


        // Matériel créé dans le stockage du bâtiment, puis affecté au poste
        Materiel uc666 = creerMateriel(UC.get(0), aujourdhui.minusMonths(2), stockage666);
        Materiel ecran666a = creerMateriel(ECRANS.get(0), aujourdhui.minusMonths(2), stockage666);
        Materiel ecran666b = creerMateriel(ECRANS.get(0), aujourdhui.minusMonths(2), stockage666);

        // Réserve hors bâtiment, conservée comme avant.
        Salle stockagePrincipal = salleService.create(
                buildSalle("996", "0", TypeSalle.SalleStockage));
        creerMateriel(UC.get(0), aujourdhui.minusMonths(2), stockagePrincipal);
        creerMateriel(ECRANS.get(0), aujourdhui.minusMonths(2), stockagePrincipal);
        creerMateriel(EQUIPEMENTS_SALLE.get(0), aujourdhui.minusMonths(2), stockagePrincipal);
    }

    /**
     * Crée un matériel, le pose sur le poste et, selon le taux de panne de son modèle,
     * lui signale une défaillance (1 sur 3 est déjà résolue, pour avoir de l'historique).
     */
    private void installerSurPoste(Poste poste, Modele modele, LocalDate equipement, Salle stockage,
                                   Personnel signaleur, Personnel gestionnaire) throws ServiceException {
        Materiel materiel = creerMateriel(modele, acquisitionAutour(equipement), stockage);
        affecterMaterielService.affecterAPoste(poste.getId(), materiel.getId());

        if (random.nextDouble() < modele.tauxPanne()) {
            Defaillance defaillance = defaillanceService.signaler(materiel.getId(), choisir(PANNES), signaleur);
            if (random.nextInt(3) == 0) {
                defaillanceService.resoudre(defaillance.getId(), "Pièce remplacée", gestionnaire);
            }
        }
    }

    /** Date d'achat proche de l'équipement de la salle ; 1 matériel sur 6 a été changé depuis (plus récent). */
    private LocalDate acquisitionAutour(LocalDate equipement) {
        if (random.nextInt(6) == 0) {
            return aujourdhui.minusDays(random.nextInt(365));
        }
        return equipement.minusDays(random.nextInt(20));
    }

    private Poste creerPoste(String numPoste, Salle salle, Personnel occupant)
            throws ServiceException {
        Poste poste = new Poste();
        poste.setNumPoste(numPoste);
        poste.setOccupant(occupant);
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

    private void remplirStock(Salle stockage, List<Modele> famille, int nombre) throws ServiceException {
        for (int i = 0; i < nombre; i++) {
            creerMateriel(choisir(famille), aujourdhui.minusMonths(random.nextInt(18)), stockage);
        }
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