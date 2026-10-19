import type {Page, Salle, Utilisateur} from "../types/types.ts";
import {apiFetch, authHeaders} from "./api.ts";

export class FormateurError extends Error {
    status: number;
    detail: string; // message du back (400 : règle métier violée)

    constructor(status: number, detail = "") {
        super("Erreur formateur " + status);
        this.status = status;
        this.detail = detail;
    }
}

async function requete<T>(url: string, method = "GET"): Promise<T> {
    let response: Response;
    try {
        response = await apiFetch(url, {method, headers: authHeaders()});
    } catch {
        throw new FormateurError(0); // serveur injoignable
    }

    if (!response.ok) throw new FormateurError(response.status, await response.text().catch(() => ""));
    return await response.json();
}

export async function getFormateurs(): Promise<Utilisateur[]> {
    return (await requete<Page<Utilisateur>>("/api/v1/personnels/?role=FORMATEUR&size=200")).content;
}

// jusqu = dernier jour d'accès, au format "2026-12-15"
export function affecterFormateurASalle(idFormateur: number, idSalle: number, jusqu: string): Promise<Salle> {
    return requete(`/api/v1/salles/${idSalle}/?formateurId=${idFormateur}&jusqu=${jusqu}`, "POST");
}

// nouvelle date limite (prolonger, ou réactiver un accès expiré)
export function prolongerFormateur(idFormateur: number, idSalle: number, jusqu: string): Promise<Salle> {
    return requete(`/api/v1/salles/${idSalle}/?formateurId=${idFormateur}&jusqu=${jusqu}`, "PATCH");
}

// date du jour en heure locale, au format des champs date ("2026-10-19")
export function aujourdhuiIso(): string {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

export function retirerFormateurDeSalle(idFormateur: number, idSalle: number): Promise<Salle> {
    return requete(`/api/v1/salles/${idSalle}/?formateurId=${idFormateur}`, "DELETE");
}