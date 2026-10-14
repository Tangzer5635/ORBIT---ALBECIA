import type { Utilisateur, UtilisateurRequest, Page } from "../types/types.ts";
import { authHeaders } from "./api.ts";

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
export async function getUtilisateurs(filter: string, page = 0, size = 30): Promise<Page<Utilisateur>> {
    // URLSearchParams encode les caractères spéciaux du filtre RSQL ( & ; = espace ...)
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (filter) params.set("filter", filter);

    let response: Response;
    try {
        response = await fetch(`${URL}/?${params}`, { headers: authHeaders() });
    } catch {
        throw new UtilisateurError(0); // serveur injoignable
    }

    if (!response.ok) throw new UtilisateurError(response.status);

    return await response.json();
}

// juste une requete pour avoir le total d'utilisateur de la pagination
export async function compterUtilisateurs(filter = ""): Promise<number> {
    return (await getUtilisateurs(filter, 0, 1)).totalElements;
}

export async function ajouterUtilisateur(utilisateur: UtilisateurRequest): Promise<Utilisateur> {
    let response: Response;
    try {
        response = await fetch(`${URL}/`, {
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
// (le mot de passe ne change pas)
// TODO possibilité de modifier le mdp pour l'admin ?
export async function modifierUtilisateur(id: number, utilisateur: UtilisateurRequest): Promise<Utilisateur> {
    let response: Response;
    try {
        response = await fetch(`${URL}/${id}/`, {
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


export async function changerVerrou(id: number, verrouiller: boolean): Promise<Utilisateur> {
    let response: Response;
    try {
        response = await fetch(`${URL}/${id}/${verrouiller ? "verrouiller" : "deverrouiller"}/`, {
            method: "PATCH",
            headers: authHeaders(),
        });
    } catch {
        throw new UtilisateurError(0);
    }

    if (!response.ok) throw new UtilisateurError(response.status);

    return await response.json();
}


export async function supprimerUtilisateur(id: number): Promise<void> {
    let response: Response;
    try {
        response = await fetch(`${URL}/${id}/`, {
            method: "DELETE",
            headers: authHeaders(),
        });
    } catch {
        throw new UtilisateurError(0);
    }

    if (!response.ok) throw new UtilisateurError(response.status);
}