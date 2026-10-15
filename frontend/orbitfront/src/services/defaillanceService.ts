import type {Defaillance} from "../types/types.ts";
import {apiFetch} from "./api.ts";

const URL = `/api/v1/defaillances`;

export class DefaillanceError extends Error {
    status: number;

    constructor(status: number) {
        super("Erreur défaillance " + status);
        this.status = status;
    }
}

async function requete<T>(url: string): Promise<T> {
    let response: Response;
    try {
        response = await apiFetch(url);
    } catch {
        throw new DefaillanceError(0); // serveur injoignable
    }

    if (!response.ok) throw new DefaillanceError(response.status);
    return await response.json();
}

// Défaillances d'un matériel (signalées et résolues)
export function getDefaillancesDuMateriel(idMateriel: number): Promise<Defaillance[]> {
    return requete(`${URL}/materiel/${idMateriel}/`);
}

// Défaillances remontées par l'utilisateur connecté, les plus récentes d'abord (« Mon poste »)
export function getMesDefaillances(): Promise<Defaillance[]> {
    return requete(`${URL}/mes/`);
}