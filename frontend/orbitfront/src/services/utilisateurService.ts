import type {Page, Role, Utilisateur, UtilisateurRequest} from "../types/types.ts";
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

// Filtres acceptés par GET /personnels/ (tous facultatifs)
export interface FiltresUtilisateur {
    filter?: string;            // filtre RSQL sur nom, prénom, login (voir rechercheVersRsql)
    role?: Role | null;
    active?: boolean | null;     // true = actifs, false = inactifs, absent = tous
    verrouille?: boolean | null; // true = verrouillés, absent = tous
}

// Méthode pour récupérer une page d'utilisateurs filtrés (tri par nom, prénom : fait par le back)
export async function getUtilisateurs(filtres: FiltresUtilisateur, page = 0, size = 20): Promise<Page<Utilisateur>> {
    // URLSearchParams encode les caractères spéciaux du filtre RSQL (; = " espaces)
    const params = new URLSearchParams({page: String(page), size: String(size)});
    if (filtres.filter) params.set("filter", filtres.filter);
    if (filtres.role) params.set("filtreRole", filtres.role);
    // != null : false est une vraie valeur à envoyer (inactifs)
    if (filtres.active != null) params.set("active", String(filtres.active));
    if (filtres.verrouille != null) params.set("verrouille", String(filtres.verrouille));

    let response: Response;
    try {
        response = await apiFetch(`${URL}/?${params}`, {headers: authHeaders()});
    } catch {
        throw new UtilisateurError(0); // serveur injoignable
    }

    if (!response.ok) throw new UtilisateurError(response.status);

    return await response.json();
}

// Méthode pour compter les utilisateurs d'un filtre : on demande une page de 1 et on lit totalElements
export async function compterUtilisateurs(filtres: FiltresUtilisateur): Promise<number> {
    return (await getUtilisateurs(filtres, 0, 1)).totalElements;
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