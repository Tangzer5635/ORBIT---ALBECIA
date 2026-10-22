import type {Page, Role, Utilisateur, UtilisateurRequest} from "../types/types.ts";
import {versPatch} from "../utils/patch.ts";
import {apiFetch, authHeaders} from "./api.ts";

const URL = `/api/v1/personnels`;

export class UtilisateurError extends Error {
    status: number;
    detail: string | undefined;

    constructor(status: number, detail?: string) {
        super("Erreur utilisateur " + status);
        this.status = status;
        this.detail = detail;
    }
}

// Message d'erreur du back : texte simple (règle métier) ou objet {champ: message} (validation du formulaire)
async function erreurDepuis(response: Response): Promise<UtilisateurError> {
    const texte = await response.text().catch(() => "");
    let detail: string | undefined = texte || undefined;
    try {
        const json = JSON.parse(texte);
        if (json && typeof json === "object") detail = Object.values(json).join(" ");
    } catch {
        // pas du JSON : on garde le texte tel quel
    }
    return new UtilisateurError(response.status, detail);
}

// Envoie une requête JSON au back et renvoie l'utilisateur de la réponse
async function envoyer(url: string, method: string, body?: object): Promise<Utilisateur> {
    let response: Response;
    try {
        response = await apiFetch(url, {
            method,
            headers: authHeaders(),
            body: body ? JSON.stringify(body) : undefined,
        });
    } catch {
        throw new UtilisateurError(0); // serveur injoignable
    }

    if (!response.ok) throw await erreurDepuis(response);

    return await response.json();
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

    if (!response.ok) throw await erreurDepuis(response);

    return await response.json();
}

// Méthode pour compter les utilisateurs d'un filtre : on demande une page de 1 et on lit totalElements
export async function compterUtilisateurs(filtres: FiltresUtilisateur): Promise<number> {
    return (await getUtilisateurs(filtres, 0, 1)).totalElements;
}

// Méthode pour ajouter un utilisateur (le "/" final est obligatoire : le controller est mappé sur "/")
export async function ajouterUtilisateur(utilisateur: UtilisateurRequest): Promise<Utilisateur> {
    return envoyer(`${URL}/`, "POST", utilisateur);
}

// Méthode pour modifier un utilisateur. Le PATCH ne porte que les champs d'identité changés et le mot de passe
// s'il est saisi ; le rôle et l'état actif ont leurs propres endpoints, appelés seulement s'ils ont changé.
export async function modifierUtilisateur(id: number, utilisateur: UtilisateurRequest, ancien: Utilisateur): Promise<Utilisateur> {
    const {role, active, ...identite} = utilisateur;

    const operations = versPatch(
        {nom: ancien.nom, prenom: ancien.prenom, login: ancien.login, motDePasse: undefined as string | undefined},
        {nom: identite.nom, prenom: identite.prenom, login: identite.login, motDePasse: identite.motDePasse || undefined});
    let modifie = operations.length ? await envoyer(`${URL}/${id}/`, "PATCH", operations) : ancien;
    if (role !== ancien.role) modifie = await envoyer(`${URL}/${id}/role/`, "PATCH", {role});
    if (active !== undefined && active !== ancien.active) modifie = await changerActif(id, active);
    return modifie;
}

// Méthode pour activer ou désactiver un compte (route sans accent : /desactiver/)
export async function changerActif(id: number, actif: boolean): Promise<Utilisateur> {
    return envoyer(`${URL}/${id}/${actif ? "activer" : "desactiver"}/`, "PATCH");
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

    if (!response.ok) throw await erreurDepuis(response);

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

    if (!response.ok) throw await erreurDepuis(response);
}