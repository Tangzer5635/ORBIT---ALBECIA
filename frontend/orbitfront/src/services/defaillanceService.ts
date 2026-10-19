import type {Defaillance, Page, StatutDefaillance} from "../types/types.ts";
import {apiFetch, authHeaders} from "./api.ts";

const URL = `/api/v1/defaillances`;

// envoyé après un signalement ou une résolution : la bulle de la navbar se recompte
export const EVT_DEFAILLANCES = "defaillances-changees";
const prevenir = (resultat: Defaillance) => {
    window.dispatchEvent(new Event(EVT_DEFAILLANCES));
    return resultat;
};

export class DefaillanceError extends Error {
    status: number;
    detail: string;

    constructor(status: number, detail = "") {
        super(detail || "Erreur lors de la gestion des défaillances.");
        this.status = status;
        this.detail = detail;
    }
}

async function requete<T>(url: string, init: RequestInit = {}): Promise<T> {
    let response: Response;
    try {
        response = await apiFetch(url, {
            ...init,
            headers: {...authHeaders(), ...(init.headers ?? {})},
        });
    } catch {
        throw new DefaillanceError(0, "Serveur injoignable. Vérifiez votre connexion.");
    }

    if (!response.ok) {
        const detail = await response.text().catch(() => "");
        throw new DefaillanceError(response.status, detail || `Erreur HTTP ${response.status}`);
    }
    return response.status === 204 ? (undefined as T) : await response.json();
}

// Liste paginée des défaillances, filtrable par statut.
export function getDefaillances(statut: StatutDefaillance, page = 0, size = 20): Promise<Page<Defaillance>> {
    const params = new URLSearchParams({statut, page: String(page), size: String(size), sort: "dateSignalement,desc"});
    return requete(`${URL}/?${params}`);
}

// Fiche d'une défaillance précise.
export function getDefaillance(id: number): Promise<Defaillance> {
    return requete(`${URL}/${id}/`);
}

// Défaillances d'un matériel (signalées et résolues).
export function getDefaillancesDuMateriel(idMateriel: number): Promise<Defaillance[]> {
    return requete(`${URL}/materiel/${idMateriel}/`);
}

// Défaillances remontées par l'utilisateur connecté (« Mon poste »).
export function getMesDefaillances(): Promise<Defaillance[]> {
    return requete(`${URL}/mes/`);
}

export function resoudreDefaillance(id: number, noteSolution: string): Promise<Defaillance> {
    return requete<Defaillance>(`${URL}/${id}/resoudre/`, {
        method: "PATCH",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({noteSolution: noteSolution.trim() || null}),
    }).then(prevenir);
}

export function signalerDefaillance(idMateriel: number, description: string): Promise<Defaillance> {
    return requete<Defaillance>(`${URL}/`, {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({idMateriel, description: description.trim()}),
    }).then(prevenir);
}