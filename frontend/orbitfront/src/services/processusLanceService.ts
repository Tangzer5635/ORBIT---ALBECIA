import type {ProcessusPoste} from "../types/types.ts";
import {apiFetch} from "./api.ts";

// Processus lancés sur la machine d'un poste : le back interroge l'Agent ORBIT de l'UC du poste
const URL = `/api/v1/processus-lances`;

export class ProcessusLanceError extends Error {
    status: number;
    detail: string; // message du back (400 : agent injoignable, UC sans IP, machine non conforme...)

    constructor(status: number, detail = "") {
        super("Erreur processus lancés " + status);
        this.status = status;
        this.detail = detail;
    }
}

// Requête avec le message du back en cas d'erreur
async function requete<T>(url: string, init: RequestInit = {}): Promise<T> {
    let response: Response;
    try {
        response = await apiFetch(url, init);
    } catch {
        throw new ProcessusLanceError(0, "Serveur ORBIT injoignable.");
    }

    if (!response.ok) throw new ProcessusLanceError(response.status, await response.text().catch(() => ""));
    return await response.json();
}

// Processus lancés sur le poste, regroupés par nom
export function getProcessusLances(posteId: number): Promise<ProcessusPoste> {
    return requete<ProcessusPoste>(`${URL}/?posteId=${posteId}`);
}

// Tue toutes les instances de ce processus sur le poste ; renvoie le nombre d'instances arrêtées
export function tuerProcessus(posteId: number, nom: string): Promise<number> {
    return requete<number>(`${URL}/?posteId=${posteId}&nom=${encodeURIComponent(nom)}`, {method: "DELETE"});
}