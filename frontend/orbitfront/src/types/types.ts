// Role.java (TECHNIQUE n'est jamais attribué à un utilisateur, il n'est pas repris ici)
export const LIBELLE_ROLE = {
    STAGIAIRE: "Stagiaire",
    FORMATEUR: "Formateur",
    GESTIONNAIRE: "Gestionnaire",
    ADMINISTRATEUR: "Administrateur",
} as const;
export type Role = keyof typeof LIBELLE_ROLE;

// TypeSalle.java
export const LIBELLE_TYPE_SALLE = {
    SalleDeCours: "Salle de cours",
    SalleStockage: "Salle de stockage",
} as const;
export type TypeSalle = keyof typeof LIBELLE_TYPE_SALLE;

// Sous-classes de Materiel.java (UC, Ecran...) : valeur du champ "type" du JSON
export const LIBELLE_TYPE_MATERIEL = {
    TableauInteractif: "Tableau interactif",
    Ecran: "Écran",
    VideoProjecteur: "Vidéoprojecteur",
    UC: "Unité centrale",
} as const;
export type TypeMateriel = keyof typeof LIBELLE_TYPE_MATERIEL;

// EtatMateriel.java
export const LIBELLE_ETAT_MATERIEL = {
    NORMAL: "Normal",
    A_REMPLACER: "À remplacer",
    DEFECTUEUX: "Défectueux",
    DISPONIBLE: "Disponible",
    ARCHIVE: "Archivé",
} as const;
export type EtatMateriel = keyof typeof LIBELLE_ETAT_MATERIEL;

// MotifGel.java : pourquoi une salle est gelée
export const LIBELLE_MOTIF_GEL = {
    TRAVAUX: "Travaux",
    ATTENTE_MATERIEL: "En attente de matériel",
    MAINTENANCE: "Maintenance",
    RESERVEE: "Réservée",
    AUTRE: "Autre",
} as const;
export type MotifGel = keyof typeof LIBELLE_MOTIF_GEL;

// StatutDefaillance.java
export const LIBELLE_STATUT_DEFAILLANCE = {
    SIGNALEE: "Signalée",
    RESOLUE: "Résolue",
} as const;
export type StatutDefaillance = keyof typeof LIBELLE_STATUT_DEFAILLANCE;

export interface Page<T> {
    content: T[];
    page: number;          // numéro de la page courante (commence à 0)
    size: number;          // nombre d'éléments par page
    totalElements: number; // nombre total d'éléments (toutes pages confondues)
    totalPages: number;
    first: boolean;
    last: boolean;
}


// LoginRequest
export interface LoginRequest {
    username: string;
    password: string;
}

// TokenResponse
export interface TokenResponse {
    accessToken: string;
    tokenType: string;
    utilisateur: Utilisateur;
    expiresIn: number;
}

// UserResponseDto
export interface Utilisateur {
    id: number;
    login: string;
    role: Role;
    prenom: string;
    nom: string;
    active: boolean;
    verrouille: boolean;
    stageId?: number | null; // stage du stagiaire, absent s'il n'est dans aucun stage
}

// UserRequestDto
export interface UtilisateurRequest {
    nid?: string; //Seulement a la création : en modif le back garde le NID existant
    nom: string;
    prenom: string;
    login?: string; // seulement en modification : en création le back génère le login
    motDePasse?: string;
    role: Role;
    active?: boolean;
}
//==========================================
//      BÂTIMENTS
//==========================================

// BatimentResponseDto.StockageDto
export interface Stockage {
    id: number;
    numSalle: string;
    nbMateriels: number;
}

// BatimentCarteDto (GET /batiments/)
export interface Batiment {
    id: number;
    numBatiment: string;
    nbSalles: number;
    stockages: Stockage[];
    anneau: ComptesAnneau;
}

// BatimentRequestDto
export interface BatimentRequest {
    numBatiment: string;
    numSalleStockage?: string; // "199" côté back si vide
    etageStockage?: string;    // "0" côté back si vide
}
//==========================================
//      SALLES
//==========================================
// AnneauDto : matériels comptés par état
export interface ComptesAnneau {
    libre: number;       // en stock
    bonEtat: number;     // affecté, rien à signaler
    defectueux: number;  // défaillance signalée non résolue
    aChanger: number;    // acquis il y a plus de 2 ans
}

// SalleResponseDto
export interface Salle {
    id: number;
    numSalle: string;
    numBatiment?: string | null; // GET /salles/{id}/ seulement
    etage: string;
    type: TypeSalle;
    materielsId: number[];
    nbPostes: number;
    materielsParType: Partial<Record<TypeMateriel, number>>;
    anneau?: ComptesAnneau;
    stageId?: number | null; // stage accueilli (salle de cours)
    stageNom?: string | null;
    masterId?: number | null; // master plaqué sur la salle
    masterNom?: string | null; // "Dev (1.2)"
    creePar?: string | null; // "Prénom Nom" du gestionnaire qui a créé la salle
    // détail du stage et formateurs attribués (GET /salles/{id}/ seulement)
    stageType?: TypeFormation | null;
    stageDateDebut?: string | null; // "2026-08-07"
    stageDateFin?: string | null;
    stageFormateur?: string | null; // "Prénom Nom" du formateur qui gère le stage
    formateurs?: Formateur[];
    // salle gelée : plus d'affectation de stage, de stagiaire ni de master
    suspendu: boolean;
    gelMotif?: MotifGel | null;
    gelNote?: string | null;
    gelDepuis?: string | null; // "2026-10-15"
    gelJusqu?: string | null;  // fin prévue, facultative
}

// SalleModificationRequestDto (« Modifier la salle »)
export interface SalleModification {
    nbPostes: number; // postes stagiaires
    nbVideoProjecteurs: number;
    nbTableaux: number;
}

// GelRequestDto (« Geler la salle »)
export interface GelRequest {
    motif: MotifGel;
    note?: string;
    jusqu?: string; // "2026-12-01"
}

// FormateurDto
export interface Formateur {
    id: number;
    prenom: string;
    nom: string;
}

// SalleCarteDto (GET /salles/?batiment=XX)
export interface SalleCarte {
    id: number;
    numSalle: string;
    numBatiment?: string | null; // seulement dans « Mes salles » (formateur)
    etage: string;
    type: TypeSalle;
    nbPostes: number;
    anneau: ComptesAnneau;
    suspendu: boolean; // salle gelée
    gelMotif?: MotifGel | null;
    gelJusqu?: string | null;
    stageNom?: string | null;            // "VU82", absent si aucun stage
    stageType?: TypeFormation | null;
}

// SalleRequestDto
export interface SalleRequest {
    numSalle: string;
    etage: string;
    type: TypeSalle;
    idBatiment: number;
}

//==========================================
//      POSTES
//==========================================

// PosteResponseDto
export type EtatPoste = "DEFECTUEUX" | "A_COMPLETER" | "A_CHANGER" | "UTILISE" | "DISPONIBLE";

export interface Poste {
    id: number;
    numPoste: string;
    occupantId: number | null;
    occupant: string | null; // "Prénom Nom" du stagiaire, null si le poste est libre
    materielsId: number[];
    materielsParType: Partial<Record<TypeMateriel, number>>;
    etat?: EtatPoste;
}



// PlanPosteDto : un poste stagiaire sur le mini-plan de « Mon poste »
export interface PlanPoste {
    numPoste: string;
    etat?: EtatPoste | null;
}

// MonPosteResponseDto (GET /postes/me/)
export interface MonPoste {
    id: number;
    numPoste: string;
    etat?: EtatPoste | null;
    stageNom?: string | null;
    plan?: PlanPoste[]; // postes stagiaires de la salle, dans l'ordre
    idSalle?: number;
    numSalle?: string;
    etage?: string;
    idBatiment?: number;
    numBatiment?: string;
    materiels: Materiel[];
}

// PosteRequestDto
export interface PosteRequest {
    numPoste: string;
    idSalle?: number;
}
//==========================================
//      MATÉRIELS
//==========================================
// MaterielResponseDto
export interface Materiel {
    id: number;
    numSerie: string;
    modele: string;
    dateFinGarantie: string;
    dateAcquisition: string;
    type: TypeMateriel;
    etat: EtatMateriel;
    dateArchivage: string | null;
    nomMachine?: string; // UC uniquement
    processus?: Processus[];
    numSalle?: string; // Salle où se trouve le matériel (absents s'il est archivé)
    typeSalle?: TypeSalle;
}

// MaterielStockDto (GET /materiels/stock/) : une ligne de la liste « Affecter un matériel »
export interface MaterielStock {
    id: number;
    type: TypeMateriel;
    modele: string;
    numSerie: string;
    nomMachine?: string | null; // UC uniquement
    numSalle: string;           // réserve où il est rangé
    numBatiment: string;
}

// AffectationMaterielDto (GET /materiels/{id}/affectation/) : tout est null pour un matériel archivé
export interface AffectationMateriel {
    idSalle: number | null;
    numSalle: string | null;
    typeSalle: TypeSalle | null;
    etage: string | null;
    numBatiment: string | null;
    idPoste: number | null;     // null : matériel posé dans la salle ou rangé en stock
    numPoste: string | null;    // "B201-S302-P07", "B201-S302-F" = poste formateur
    occupant: string | null;    // "Prénom Nom" du stagiaire du poste
    stageNom: string | null;
    stageDateDebut: string | null;
    stageDateFin: string | null;
    responsable: string | null; // gestionnaire qui a créé la salle
}

// MaterielRequestDto
export interface MaterielRequest {
    numSerie: string;
    modele: string;
    dateFinGarantie: string;
    dateAcquisition: string;
    type: TypeMateriel;
    nomMachine?: string;   // UC uniquement
    idStockage?: number;
}

//==========================================
//      PROCESSUS
//==========================================
// ListeProcessus.java
export const LIBELLE_LISTE_PROCESSUS = {
    BLANCHE: "Liste blanche",
    NOIRE: "Liste noire",
} as const;
export type ListeProcessus = keyof typeof LIBELLE_LISTE_PROCESSUS;

// ProcessusResponseDto
export interface Processus {
    id: number;
    nom: string;
    liste: ListeProcessus;
}

// ProcessusRequestDto
export interface ProcessusRequest {
    nom: string;
    liste: ListeProcessus;
}

//==========================================
//      MASTERS
//==========================================

// MasterDto (lecture)
export interface Master {
    id: number;
    nom: string;
    versionMaster: string;
    dateCreation: string; // "2026-10-18T15:20:00"
    processusIds: number[];
    processus: Processus[];
}

// MasterDto (création, modification) : la liste complète des processus est renvoyée à chaque fois
export interface MasterRequest {
    nom: string;
    versionMaster: string;
    processusIds: number[];
}

//==========================================
//      STAGES
//==========================================

export const LIBELLE_TYPE_FORMATION = {
    ESR: "Réseaux",
    ESI: "Admin Système",
    PDI: "Développement",
    CYB: "Cybersécurité",
    EST: "Télécommunication",
} as const;
export type TypeFormation = keyof typeof LIBELLE_TYPE_FORMATION;

// BilanImportStagesDto (POST /stages/import/)
export interface BilanImportStages {
    crees: string[];    // noms des stages créés
    misAJour: string[]; // noms des stages mis à jour
    erreurs: string[];  // "Ligne 4 : type « XXX » inconnu"
}

// StageResponseDto
export interface Stage {
    id: number;
    nom: string;
    type: TypeFormation;
    dateDebut: string; // "2026-08-07"
    dateFin: string;
    formateurId: number;
    formateur: string; // "Prénom Nom"
    salleId?: number | null; // salle qui accueille le stage
    numSalle?: string | null;
    numBatiment?: string | null;
}

//==========================================
//      DÉFAILLANCES
//==========================================

// DefaillanceResponseDto
export interface Defaillance {
    id: number;
    description: string;
    dateSignalement: string; // "2026-10-09T14:32:00"
    statut: StatutDefaillance;
    idMateriel: number;
    numSerieMateriel: string;
    typeMateriel: TypeMateriel;
    idSignalePar: number;
    signalePar: string; // "Prénom Nom"
    idTraitePar: number | null;
    traitePar: string | null;
    noteSolution: string | null;
    dateResolution: string | null;
}

//==========================================
//      JOURNAL
//==========================================

// TypeAction.java : type d'une ligne du journal
export const LIBELLE_ACTION = {
    CONNEXION: "Connexion",
    ECHEC_CONNEXION: "Échec de connexion",
    DECONNEXION: "Déconnexion",
    CREATION: "Création",
    MODIFICATION: "Modification",
    SUPPRESSION: "Suppression",
    AFFECTATION: "Affectation",
    RETRAIT: "Retrait",
    SIGNALEMENT: "Signalement",
    RESOLUTION: "Résolution",
    IMPORT: "Import",
} as const;
export type TypeAction = keyof typeof LIBELLE_ACTION;

// JournalisationResponseDto
export interface Journalisation {
    id: number;
    dateHeure: string; // "2026-10-17T20:15:02.123456"
    auteurId?: number; // absent si l'auteur est inconnu (échec de connexion)
    action: TypeAction;
    detail: string;
}
//===================================
//      CALENDRIER
//===================================
export interface MoisGarantie {
    mois: string;   // "2026-10"
    nb: number;     // materiels dont la garantie se termine ce mois la
}

export interface CalendrierGaranties {
    annee: number;
    expirees: number;        // hors garantie à ce jour
    mois: MoisGarantie[];    // janvier à décembre de l'année affichée
}