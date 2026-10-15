import type { Utilisateur, UtilisateurRequest, Page } from "../types/types.ts";
import {apiFetch, authHeaders} from "./api.ts";

const URL = `/api/v1/personnels`;

export class UtilisateurError extends Error {
    status: number;
    detail: string | undefined;
    constructor(status: number) {
        super("Erreur utilisateur " + status);
        this.status = status;
    }
}

// Méthode pour récupérer la liste des utilisateurs
export async function getUtilisateurs(): Promise<Utilisateur[]> {
    let response: Response;
    try {
        response = await apiFetch(`${URL}/`, { headers: authHeaders() });    } catch {
        throw new UtilisateurError(0); // serveur injoignable
    }

    if (!response.ok) throw new UtilisateurError(response.status);

    const data: Page<Utilisateur> = await response.json();
    return data.content; //les utilisateurs sont dans "content" car on a des pages
}

// Méthode pour ajouter un utilisateur
export async function ajouterUtilisateur(utilisateur: UtilisateurRequest): Promise<Utilisateur> {
    let response: Response;
    try {
        response = await apiFetch(URL, {
            method: "POST",
            headers: authHeaders(),
            body: JSON.stringify(utilisateur),
        });
    } catch {
        throw new UtilisateurError(0);
    }

    if (!response.ok) throw new UtilisateurError(response.status);

    return await response.json();
}
// Méthode pour modifier un utilisateur (sans motDePasse, le mot de passe ne change pas)
export async function modifierUtilisateur(id: number, utilisateur: UtilisateurRequest): Promise<Utilisateur> {
    let response: Response;
    try {
        response = await apiFetch(`${URL}/${id}/`, {
            method: "PUT",
            headers: authHeaders(),
            body: JSON.stringify(utilisateur),
        });
    } catch {
        throw new UtilisateurError(0);
    }

    if (!response.ok) throw new UtilisateurError(response.status);

    return await response.json();
}

// Méthode pour verrouiller ou déverrouiller un compte
export async function changerVerrou(id: number, verrouiller: boolean): Promise<Utilisateur> {
    let response: Response;
    try {
        response = await apiFetch(`${URL}/${id}/${verrouiller ? "verrouiller" : "deverrouiller"}/`, {
            method: "PATCH",
            headers: authHeaders(),
        });
    } catch {
        throw new UtilisateurError(0);
    }

    if (!response.ok) throw new UtilisateurError(response.status);

    return await response.json();
}

// Méthode pour supprimer un utilisateur
export async function supprimerUtilisateur(id: number): Promise<void> {
    let response: Response;
    try {
        response = await apiFetch(`${URL}/${id}/`, {
            method: "DELETE",
            headers: authHeaders(),
        });
    } catch {
        throw new UtilisateurError(0);
    }

    if (!response.ok) throw new UtilisateurError(response.status);
}