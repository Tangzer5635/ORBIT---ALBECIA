import type {Page, Poste, PosteRequest} from "../types/types.ts";
import { authHeaders } from "./api.ts";

const URL = `/api/v1/postes`;

export class PosteError extends Error {
    status: number;
    constructor(status: number) {
        super("Erreur poste " + status);
        this.status = status;
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

export async function getMyPoste(): Promise<Poste> {
    let response: Response;
    try {
        response = await fetch(`${URL}/me/`, { headers: authHeaders() });
    } catch {
        throw new PosteError(0); // serveur injoignable
    }

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