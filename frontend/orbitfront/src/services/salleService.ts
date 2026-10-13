import type { Page, Salle, SalleRequest } from "../types/types.ts";
import { authHeaders } from "./api.ts";

const URL = `/api/v1/salles`;

export class SalleError extends Error {
    status: number;
    constructor(status: number) {
        super("Erreur salles " + status);
        this.status = status;
    }
}

// Méthode pour récupérer la liste de toutes les salles
export async function getSalle(): Promise<Salle[]> {
    let response: Response;
    try {
        response = await fetch(`${URL}/`, { headers: authHeaders() });
    } catch {
        throw new SalleError(0); // serveur injoignable
    }

    if (!response.ok) throw new SalleError(response.status);

    const data: Page<Salle> = await response.json();
    return data.content; //les salles sont dans "content" car on a des pages
}

// Méthode pour récupérer les salles d'un bâtiment (cours et stockage, pas de pagination)
export async function getSallesDuBatiment(idBatiment: number): Promise<Salle[]> {
    let response: Response;
    try {
        response = await fetch(`/api/v1/batiments/${idBatiment}/salles/`, { headers: authHeaders() });
    } catch {
        throw new SalleError(0);
    }

    if (!response.ok) throw new SalleError(response.status);

    return await response.json();
}

// Méthode pour ajouter une salle (idBatiment est dans la requête)
export async function ajouterSalle(salle: SalleRequest): Promise<Salle> {
    let response: Response;
    try {
        response = await fetch(URL, {
            method: "POST",
            headers: authHeaders(),
            body: JSON.stringify(salle),
        });
    } catch {
        throw new SalleError(0);
    }

    if (!response.ok) throw new SalleError(response.status);

    return await response.json();
}

// Méthode pour supprimer une salle
export async function supprimerSalle(id: number): Promise<void> {
    let response: Response;
    try {
        response = await fetch(`${URL}/${id}/`, {
            method: "DELETE",
            headers: authHeaders(),
        });
    } catch {
        throw new SalleError(0);
    }

    if (!response.ok) throw new SalleError(response.status);
}