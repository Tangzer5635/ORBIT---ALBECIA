import type {Defaillance} from "../types/types.ts";
import {authHeaders} from "./api.ts";

const URL = `/api/v1/defaillances`;

export class DefaillanceError extends Error {
    status: number;

    constructor(status: number) {
        super("Erreur défaillance " + status);
        this.status = status;
    }
}

// Défaillances d'un matériel (signalées et résolues)
export async function getDefaillancesDuMateriel(idMateriel: number): Promise<Defaillance[]> {
    let response: Response;
    try {
        response = await fetch(`${URL}/materiel/${idMateriel}/`, {headers: authHeaders()});
    } catch {
        throw new DefaillanceError(0); // serveur injoignable
    }

    if (!response.ok) throw new DefaillanceError(response.status);
    return await response.json();
}