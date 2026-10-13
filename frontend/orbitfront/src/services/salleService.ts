import type { Page, Salle, SalleCarte, SalleRequest } from "../types/types.ts";
import { authHeaders } from "./api.ts";

const URL = `/api/v1/salles`;

export class SalleError extends Error {
    status: number;
    constructor(status: number) {
        super(`HTTP ${status}`);
        this.status = status;
    }
}

export interface SalleEquipeeRequest {
    idBatiment: number;
    numSalle: string;
    etage: string;
    nbPostes: number;
    ecransParPoste: 1 | 2;
    nbVideoProjecteurs: number;
    nbTableaux: number;
}

export interface SalleEquipeeResponse {
    idSalle: number;
    manquants: Partial<Record<"UC" | "Ecran" | "VideoProjecteur" | "TableauInteractif", number>>;
}

export async function creerSalleEquipee(dto: SalleEquipeeRequest): Promise<SalleEquipeeResponse> {
    let response: Response;
    try {
        response = await fetch(`${URL}/equipee`, {
            method: "POST",
            headers: {...authHeaders(), "Content-Type": "application/json"},
            body: JSON.stringify(dto),
        });
    } catch {
        throw new SalleError(0);
    }

    if (!response.ok) throw new SalleError(response.status);
    return response.json();
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
export async function getSallesDuBatiment(idBatiment: number): Promise<SalleCarte[]> {
    let response: Response;
    try {
        response = await fetch(`${URL}/?batiment=${idBatiment}`, { headers: authHeaders() });
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