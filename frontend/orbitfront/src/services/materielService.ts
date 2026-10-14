import type {EtatMateriel, Materiel, MaterielRequest, Page, TypeMateriel} from "../types/types.ts";
import { authHeaders } from "./api.ts";
import type {Localisation} from "../utils/materiels.ts";

const URL = `/api/v1/materiels`;

export class MaterielError extends Error {
    status: number;
    constructor(status: number) {
        super("Erreur materiel " + status);
        this.status = status;
    }
}

// Filtres acceptés par GET /materiels/ (tous facultatifs)
export interface FiltresMateriel {
    filter?: string;               // filtre RSQL, ex. modele=ilike="dell"
    type?: TypeMateriel | null;    // UC, Ecran... (filtré par le back sur la classe)
    etat?: EtatMateriel | null;    // état calculé par le back
    localisation?: Localisation;
}


// Méthode pour récupérer une page de matériels filtrés
export async function getMateriels(filtres: FiltresMateriel, page = 0, size = 20): Promise<Page<Materiel>> {
    // URLSearchParams encode les caractères spéciaux du filtre RSQL (; = " espaces)
    const params = new URLSearchParams({ page: String(page), size: String(size), sort: "id" });
    if (filtres.filter) params.set("filter", filtres.filter);
    if (filtres.type) params.set("type", filtres.type);
    if (filtres.etat) params.set("etat", filtres.etat);
    if (filtres.localisation) params.set("localisation", filtres.localisation);

    let response: Response;
    try {
        response = await fetch(`${URL}/?${params}`, { headers: authHeaders() });
    } catch {
        throw new MaterielError(0); // serveur injoignable
    }

    if (!response.ok) throw new MaterielError(response.status);

    return await response.json();
}

// Méthode pour compter les matériels d'un filtre, on lit totalElements
export async function compterMateriels(filtres: FiltresMateriel): Promise<number> {
    return (await getMateriels(filtres, 0, 1)).totalElements;
}

// Méthode pour récupérer un matériel par son id
export async function getMaterielById(id: number): Promise<Materiel> {
    let response: Response;
    try {
        response = await fetch(`${URL}/${id}/`, { headers: authHeaders() });
    } catch {
        throw new MaterielError(0);
    }

    if (!response.ok) throw new MaterielError(response.status);

    return await response.json();
}

// Méthode pour ajouter un materiel
export async function ajouterMateriel(materiel: MaterielRequest): Promise<Materiel> {
    let response: Response;
    try {
        response = await fetch(URL, {
            method: "POST",
            headers: authHeaders(),
            body: JSON.stringify(materiel),
        });
    } catch {
        throw new MaterielError(0);
    }

    if (!response.ok) throw new MaterielError(response.status);

    return await response.json();
}

// Méthode pour supprimer un materiel
export async function supprimerMateriel(id: number, stockageCibleId?: number): Promise<void> {
    const params = stockageCibleId !== undefined ? `?stockageCible=${stockageCibleId}` : "";

    let response: Response;
    try {
        response = await fetch(`${URL}/${id}/${params}`, {
            method: "DELETE",
            headers: authHeaders(),
        });
    } catch {
        throw new MaterielError(0);
    }

    if (!response.ok) throw new MaterielError(response.status);
}