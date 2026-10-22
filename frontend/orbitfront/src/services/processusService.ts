import type {ListeProcessus, Page, Processus, ProcessusRequest, ProcessusSalle} from "../types/types.ts";
import {apiFetch} from "./api.ts";

const URL = `/api/v1/processus`;

export class ProcessusError extends Error {
    status: number;
    detail: string; // message du back (400 : règle métier violée)

    constructor(status: number, detail = "") {
        super("Erreur processus " + status);
        this.status = status;
        this.detail = detail;
    }
}

// Requête avec le message du back en cas d'erreur (pas de corps pour un 204)
async function requete<T>(url: string, init: RequestInit = {}): Promise<T> {
    let response: Response;
    try {
        response = await apiFetch(url, init);
    } catch {
        throw new ProcessusError(0); // serveur injoignable
    }

    if (!response.ok) throw new ProcessusError(response.status, await response.text().catch(() => ""));
    return response.status === 204 ? (undefined as T) : await response.json();
}

// Tout le référentiel des processus
export async function getProcessus(): Promise<Processus[]> {
    const page = await requete<Page<Processus>>(`${URL}/?size=500&sort=nom`);
    return page.content;
}

export function ajouterProcessus(processus: ProcessusRequest): Promise<Processus> {
    return requete(`${URL}/`, {method: "POST", body: JSON.stringify(processus)});
}

// Liste blanche ou noire : refusé en noire si le processus fait partie d'un master (la blanche a le dessus)
export function changerListe(id: number, liste: ListeProcessus): Promise<Processus> {
    return requete(`${URL}/${id}/?liste=${liste}`, {method: "PATCH"});
}

// Processus avec la liste qui s'applique dans la salle (liste globale ou surcharge d'un de ses formateurs)
export function getProcessusDeLaSalle(salleId: number): Promise<ProcessusSalle[]> {
    return requete(`/api/v1/surcharges-processus/?salleId=${salleId}`);
}

// Le formateur connecté passe ce processus en blanche ou noire pour toutes ses salles
export function surchargerProcessus(processusId: number, liste: ListeProcessus): Promise<void> {
    return requete(`/api/v1/surcharges-processus/?processusId=${processusId}&liste=${liste}`, {method: "PUT"});
}

// Le formateur connecté revient à la liste globale pour ce processus
export function reinitialiserProcessus(processusId: number): Promise<void> {
    return requete(`/api/v1/surcharges-processus/?processusId=${processusId}`, {method: "DELETE"});
}

// Méthode pour supprimer un processus (il est désinstallé de toutes les UC)
export function supprimerProcessus(id: number): Promise<void> {
    return requete(`${URL}/${id}/`, {method: "DELETE"});
}