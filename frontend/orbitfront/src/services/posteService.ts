import type {MonPoste, Poste} from "../types/types.ts";
import {apiFetch, authHeaders} from "./api.ts";

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

// Méthode pour récupérer le poste de l'utilisateur connecté (stagiaire)
// null si aucun poste ne lui est affecté (404)
export async function getMyPoste(): Promise<MonPoste | null> {
    let response: Response;
    try {
        response = await apiFetch(`${URL}/me/`, { headers: authHeaders() });
    } catch {
        throw new PosteError(0); // serveur injoignable
    }

    if (response.status === 404) return null;
    if (!response.ok) throw new PosteError(response.status);

    return await response.json();
}

// Assied un stagiaire du stage de la salle sur ce poste
export async function affecterStagiaireAPoste(idPoste: number, idStagiaire: number): Promise<Poste> {
    let response: Response;
    try {
        response = await apiFetch(`${URL}/${idPoste}/?stagiaireId=${idStagiaire}`, {
            method: "POST",
            headers: authHeaders(),
        });
    } catch {
        throw new PosteError(0);
    }

    if (!response.ok) throw new PosteError(response.status, await response.text().catch(() => ""));

    return await response.json();
}

// Libère le poste : le stagiaire n'y est plus assis (il reste dans son stage)
export async function retirerStagiaireDuPoste(idPoste: number, idStagiaire: number): Promise<Poste> {
    let response: Response;
    try {
        response = await apiFetch(`${URL}/${idPoste}/?stagiaireId=${idStagiaire}`, {
            method: "DELETE",
            headers: authHeaders(),
        });
    } catch {
        throw new PosteError(0);
    }

    if (!response.ok) throw new PosteError(response.status, await response.text().catch(() => ""));

    return await response.json();
}