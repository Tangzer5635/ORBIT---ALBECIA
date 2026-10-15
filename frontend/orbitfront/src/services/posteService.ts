import type {MonPoste, Page, Poste, PosteRequest} from "../types/types.ts";
import { authHeaders } from "./api.ts";

const URL = `/api/v1/postes`;

export class PosteError extends Error {
    status: number;
    detail: string; // message du back (400 : règle métier violée)
    constructor(status: number, detail = "") {
        super("Erreur poste " + status);
        this.status = status;
        this.detail = detail;
    }
}

// Méthode pour récupérer la liste des postes
export async function getPostes(): Promise<Poste[]> {
    let response: Response;
    try {
        response = await fetch(`${URL}/`, { headers: authHeaders() });
    } catch {
        throw new PosteError(0); // serveur injoignable
    }

    if (!response.ok) throw new PosteError(response.status);

    const data: Page<Poste> = await response.json();
    return data.content; //les postes sont dans "content" car on a des pages
}

// Méthode pour récupérer le poste de l'utilisateur connecté (stagiaire)
// null si aucun poste ne lui est affecté (404)
export async function getMyPoste(): Promise<MonPoste | null> {
    let response: Response;
    try {
        response = await fetch(`${URL}/me/`, { headers: authHeaders() });
    } catch {
        throw new PosteError(0); // serveur injoignable
    }

    if (response.status === 404) return null;
    if (!response.ok) throw new PosteError(response.status);

    return await response.json();
}

// Méthode pour ajouter un poste
export async function ajouterPoste(poste: PosteRequest): Promise<Poste> {
    let response: Response;
    try {
        response = await fetch(URL, {
            method: "POST",
            headers: authHeaders(),
            body: JSON.stringify(poste),
        });
    } catch {
        throw new PosteError(0);
    }

    if (!response.ok) throw new PosteError(response.status);

    return await response.json();
}

// Méthode pour supprimer un poste
export async function supprimerPoste(id: number): Promise<void> {
    let response: Response;
    try {
        response = await fetch(`${URL}/${id}/`, {
            method: "DELETE",
            headers: authHeaders(),
        });
    } catch {
        throw new PosteError(0);
    }

    if (!response.ok) throw new PosteError(response.status);
}

// Assied un stagiaire du stage de la salle sur ce poste
export async function affecterStagiaireAPoste(idPoste: number, idStagiaire: number): Promise<Poste> {
    let response: Response;
    try {
        response = await fetch(`${URL}/${idPoste}/?stagiaireId=${idStagiaire}`, {
            method: "POST",
            headers: authHeaders(),
        });
    } catch {
        throw new PosteError(0);
    }

    if (!response.ok) throw new PosteError(response.status, await response.text().catch(() => ""));

    return await response.json();
}