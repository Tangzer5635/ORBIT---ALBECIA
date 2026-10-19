import type {Journalisation, Page, TypeAction} from "../types/types.ts";
import {apiFetch, authHeaders} from "./api.ts";

const URL = `/api/v1/logs`;

export class JournalError extends Error {
    status: number;
    constructor(status: number) {
        super("Erreur journal " + status);
        this.status = status;
    }
}

// Filtres acceptés par GET /logs/ (tous facultatifs)
export interface FiltresJournal {
    filter?: string; // filtre RSQL sur le détail et l'id de l'auteur (voir rechercheJournalVersRsql)
    action?: TypeAction | null; // absent = tous les types
    debut?: string;  // "2026-10-01T00:00:00"
    fin?: string;
    sort?: string;   // "dateHeure,asc" ; par défaut le back renvoie le plus récent d'abord
}

// Méthode pour récupérer une page du journal
export async function getJournal(filtres: FiltresJournal, page = 0, size = 50): Promise<Page<Journalisation>> {
    const params = new URLSearchParams({page: String(page), size: String(size)});
    if (filtres.filter) params.set("filter", filtres.filter);
    if (filtres.action) params.set("action", filtres.action);
    if (filtres.debut) params.set("debut", filtres.debut);
    if (filtres.fin) params.set("fin", filtres.fin);
    if (filtres.sort) params.set("sort", filtres.sort);

    let response: Response;
    try {
        response = await apiFetch(`${URL}/?${params}`, {headers: authHeaders()});
    } catch {
        throw new JournalError(0); // serveur injoignable
    }

    if (!response.ok) throw new JournalError(response.status);

    return await response.json();
}

// Méthode pour récupérer tout le journal d'une période, du plus ancien au plus récent (pour l'export CSV).
// Dates au format "2026-10-17" ; on demande les pages une par une jusqu'à la dernière.
export async function getJournalPeriode(du: string, au: string): Promise<Journalisation[]> {
    const filtres: FiltresJournal = {debut: `${du}T00:00:00`, fin: `${au}T23:59:59.999`, sort: "dateHeure,asc"};
    const lignes: Journalisation[] = [];
    for (let page = 0; ; page++) {
        const p = await getJournal(filtres, page, 500);
        lignes.push(...p.content);
        if (p.last) return lignes;
    }
}