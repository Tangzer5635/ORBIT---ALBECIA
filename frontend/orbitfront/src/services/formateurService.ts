import type {Page, Salle, Utilisateur} from "../types/types.ts";
import {authHeaders} from "./api.ts";

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
        response = await fetch(url, {method, headers: authHeaders()});
    } catch {
        throw new FormateurError(0); // serveur injoignable
    }

    if (!response.ok) throw new FormateurError(response.status, await response.text().catch(() => ""));
    return await response.json();
}

export async function getFormateurs(): Promise<Utilisateur[]> {
    return (await requete<Page<Utilisateur>>("/api/v1/personnels/?role=FORMATEUR&size=200")).content;
}

export function affecterFormateurASalle(idFormateur: number, idSalle: number): Promise<Salle> {
    return requete(`/api/v1/salles/${idSalle}/?formateurId=${idFormateur}`, "POST");
}

export function retirerFormateurDeSalle(idFormateur: number, idSalle: number): Promise<Salle> {
    return requete(`/api/v1/salles/${idSalle}/?formateurId=${idFormateur}`, "DELETE");
}
