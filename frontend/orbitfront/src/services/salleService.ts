import type {GelRequest, Page, Poste, Salle, SalleCarte, SalleModification, SalleRequest} from "../types/types.ts";
import {apiFetch, authHeaders} from "./api.ts";

const URL = `/api/v1/salles`;

export class SalleError extends Error {
    status: number;
    detail: string; // message du back (400 : règle métier violée)

    constructor(status: number, detail = "") {
        super(`HTTP ${status}`);
        this.status = status;
        this.detail = detail;
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
        response = await apiFetch(`${URL}/equipee/`, {
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

// liste de toutes les salles
export async function getSalle(): Promise<Salle[]> {
    let response: Response;
    try {
        response = await apiFetch(`${URL}/`, {headers: authHeaders()});
    } catch {
        throw new SalleError(0); // serveur injoignable
    }

    if (!response.ok) throw new SalleError(response.status);

    const data: Page<Salle> = await response.json();
    return data.content; //les salles sont dans "content" car on a des pages
}

// Salles affectées au formateur connecté (tous bâtiments)
export async function getMesSalles(): Promise<SalleCarte[]> {
    let response: Response;
    try {
        response = await apiFetch(`${URL}/mes-salles/`, {headers: authHeaders()});
    } catch {
        throw new SalleError(0);
    }

    if (!response.ok) throw new SalleError(response.status);

    return await response.json();
}

export async function getSalleById(id: string): Promise<Salle> {
    let response: Response;
    try {
        response = await apiFetch(`${URL}/${id}/`, {headers: authHeaders()});
    } catch {
        throw new SalleError(0);
    }

    if (!response.ok) throw new SalleError(response.status);

    return await response.json();
}

export async function getMaterielByNumSalle(numSalle: string): Promise<Salle> {
    let response: Response;
    try {
        response = await apiFetch(`${URL}/?numSalle=${numSalle}`, {headers: authHeaders()});
    } catch {
        throw new SalleError(0);
    }

    if (!response.ok) throw new SalleError(response.status);

    return await response.json();
}

// Méthode pour récupérer les salles d'un bâtiment (cours et stockage, pas de pagination)
export async function getSallesDuBatiment(idBatiment: number): Promise<SalleCarte[]> {
    let response: Response;
    try {
        response = await apiFetch(`${URL}/?batiment=${idBatiment}`, {headers: authHeaders()});
    } catch {
        throw new SalleError(0);
    }

    if (!response.ok) throw new SalleError(response.status);

    return await response.json();
}

export async function getPostesBySalle(idSalle: string): Promise<Poste[]> {
    let response: Response;
    try {
        response = await apiFetch(`/api/v1/postes/?salle=${idSalle}`, {headers: authHeaders()});
    } catch {
        throw new SalleError(0);
    }

    if (!response.ok) throw new SalleError(response.status);
    return await response.json();
}

//(idBatiment est dans la requête)
export async function ajouterSalle(salle: SalleRequest): Promise<Salle> {
    let response: Response;
    try {
        response = await apiFetch(URL, {
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


export async function supprimerSalle(id: number): Promise<void> {
    let response: Response;
    try {
        response = await apiFetch(`${URL}/${id}/`, {
            method: "DELETE",
            headers: authHeaders(),
        });
    } catch {
        throw new SalleError(0);
    }

    if (!response.ok) throw new SalleError(response.status, await response.text().catch(() => ""));
}

// Requête qui renvoie du JSON, avec le message du back en cas d'erreur
async function requete<T>(url: string, init: RequestInit): Promise<T> {
    let response: Response;
    try {
        response = await apiFetch(url, init);
    } catch {
        throw new SalleError(0);
    }

    if (!response.ok) throw new SalleError(response.status, await response.text().catch(() => ""));
    return await response.json();
}

// « Modifier la salle » : nombre de postes, de vidéoprojecteurs et de tableaux (manquants = pas assez en stock)
export function modifierSalle(id: number, dto: SalleModification): Promise<SalleEquipeeResponse> {
    return requete(`${URL}/${id}/`, {method: "PUT", body: JSON.stringify(dto)});
}

export function gelerSalle(id: number, gel: GelRequest): Promise<Salle> {
    return requete(`${URL}/${id}/geler/`, {method: "PATCH", body: JSON.stringify(gel)});
}

export function degelerSalle(id: number): Promise<Salle> {
    return requete(`${URL}/${id}/degeler/`, {method: "PATCH"});
}