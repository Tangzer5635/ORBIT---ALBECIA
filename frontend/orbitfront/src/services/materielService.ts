import type {Materiel, MaterielRequest, Page} from "../types/types.ts";
import { authHeaders } from "./api.ts";

const URL = `/api/v1/materiels`;

export class MaterielError extends Error {
    status: number;
    constructor(status: number) {
        super("Erreur materiel " + status);
        this.status = status;
    }
}

// Méthode pour récupérer la liste des materiels
export async function getMateriel(): Promise<Materiel[]> {
    let response: Response;
    try {
        response = await fetch(`${URL}/`, { headers: authHeaders() });
    } catch {
        throw new MaterielError(0); // serveur injoignable
    }

    if (!response.ok) throw new MaterielError(response.status);

    const data: Page<Materiel> = await response.json();
    return data.content; //les materiels sont dans "content" car on a des pages
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