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
    login: string;
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
    etage: string;
    type: TypeSalle;
    materielsId: number[];
    nbPostes: number;
    materielsParType: Partial<Record<TypeMateriel, number>>;
    anneau?: ComptesAnneau;
    stageId?: number | null; // stage accueilli (salle de cours)
    stageNom?: string | null;
}

// SalleCarteDto (GET /salles/?batiment=XX)
export interface SalleCarte {
    id: number;
    numSalle: string;
    etage: string;
    type: TypeSalle;
    nbPostes: number;
    anneau: ComptesAnneau;
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
    materielsId: number[];
    etat?: EtatPoste;
}



// MonPosteResponseDto (GET /postes/me/)
export interface MonPoste {
    id: number;
    numPoste: string;
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
    // UC uniquement
    nomMachine?: string;
    processus?: Processus[];
    // Salle où se trouve le matériel (absents s'il est archivé)
    numSalle?: string;
    typeSalle?: TypeSalle;
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