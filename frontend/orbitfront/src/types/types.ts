export type Role = "ADMINISTRATEUR" | "GESTIONNAIRE" | "FORMATEUR" | "STAGIAIRE";

export interface Utilisateur {
    id: number;
    prenom: string;
    nom: string;
    role: Role;
}

// (BatimentResponseDto)
export interface Batiment {
    id: number;
    numBatiment: string;
    sallesId: number[];
}

// (BatimentRequestDto)
export interface BatimentRequest {
    numBatiment: string;
}

