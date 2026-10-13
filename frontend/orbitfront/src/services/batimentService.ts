import type { Batiment, BatimentRequest, Page } from "../types/types.ts";
import { authHeaders } from "./api.ts";

const URL = `/api/v1/batiments`;

export class BatimentError extends Error {
    status: number;
    constructor(status: number) {
        super("Erreur bâtiment " + status);
        this.status = status;
    }
}

// Méthode pour récupérer la liste des bâtiments
export async function getBatiments(): Promise<Batiment[]> {
    let response: Response;
    try {
        response = await fetch(`${URL}/`, { headers: authHeaders() });
    } catch {
        throw new BatimentError(0); // serveur injoignable
    }

    if (!response.ok) throw new BatimentError(response.status);

    const data: Page<Batiment> = await response.json();
    return data.content; //les bâtiments sont dans "content" car on a des pages
}

// Méthode pour ajouter un bâtiment
export async function ajouterBatiment(batiment: BatimentRequest): Promise<Batiment> {
    let response: Response;
    try {
        response = await fetch(URL, {
            method: "POST",
            headers: authHeaders(),
            body: JSON.stringify(batiment),
        });
    } catch {
        throw new BatimentError(0);
    }

    if (!response.ok) throw new BatimentError(response.status);

    return await response.json();
}

// Méthode pour supprimer un bâtiment
export async function supprimerBatiment(id: number, stockageCibleId?: number): Promise<void> {
    const params = stockageCibleId !== undefined ? `?stockageCible=${stockageCibleId}` : "";

    let response: Response;
    try {
        response = await fetch(`${URL}/${id}/${params}`, {
            method: "DELETE",
            headers: authHeaders(),
        });
    } catch {
        throw new BatimentError(0);
    }

    if (!response.ok) throw new BatimentError(response.status);
}