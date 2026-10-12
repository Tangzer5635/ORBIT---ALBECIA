export type Role = "ADMINISTRATEUR" | "GESTIONNAIRE" | "FORMATEUR" | "STAGIAIRE";

export interface Utilisateur {
    id: number;
    prenom: string;
    nom: string;
    role: Role;
}

export interface Batiment {
    id: number;
    numBatiment: string;
}