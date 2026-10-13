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

// TypeMateriel.java
export const LIBELLE_TYPE_MATERIEL = {
    TableauInteractif: "Tableau interactif",
    Imprimante: "Imprimante",
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
    totalElements: number;
    totalPages: number;
    number: number;   // numéro de la page (commence à 0)
    size: number;
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
}

// UserRequestDto
export interface UtilisateurRequest {
    nid: string;
    nom: string;
    prenom: string;
    login: string;
    motDePasse?: string;
    role: Role;
    active?: boolean;
}

//BÂTIMENTS


// BatimentResponseDto.StockageDto
export interface Stockage {
    id: number;
    numSalle: string;
    nbMateriels: number;
}

// BatimentResponseDto
export interface Batiment {
    id: number;
    numBatiment: string;
    sallesId: number[];
    nbSalles: number;
    stockages: Stockage[];
    comptes: Comptes;
}

// BatimentRequestDto
export interface BatimentRequest {
    numBatiment: string;
}

//SALLES


export interface Comptes {
    use: number;    // poste occupé, matériel en ordre
    dispo: number;  // poste libre, matériel en ordre
    chg: number;    // un matériel à remplacer bientôt
    inc: number;    // poste sans matériel
    def: number;    // un matériel défectueux
}

// SalleResponseDto
export interface Salle {
    id: number;
    numSalle: string;
    etage: string;
    type: TypeSalle;
    materielsId: number[];
    nbPostes: number;
    comptes: Comptes;
    materielsParType: Partial<Record<TypeMateriel, number>>;
}

// SalleRequestDto
export interface SalleRequest {
    numSalle: string;
    etage: string;
    type: TypeSalle;
    idBatiment: number;
}

//POSTES


// PosteResponseDto
export interface Poste {
    id: number;
    numPoste: string;
    occupantId: number | null;
    materielsId: number[];
}

// PosteRequestDto
export interface PosteRequest {
    numPoste: string;
    idSalle?: number;
}

//MATÉRIELS

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
}

// MaterielRequestDto
export interface MaterielRequest {
    numSerie: string;
    modele: string;
    dateFinGarantie: string;
    dateAcquisition: string;
    type: TypeMateriel;
    idStockage?: number;
}