import type {Page, Processus, ProcessusRequest} from "../types/types.ts";
import { authHeaders } from "./api.ts";

const URL = `/api/v1/processus`;

export class ProcessusError extends Error {
    status: number;
    constructor(status: number) {
        super("Erreur processus " + status);
        this.status = status;
    }
}

// Méthode pour récupérer la liste des processus
export async function getProcessus(): Promise<Processus[]> {
    let response: Response;
    try {
        response = await fetch(`${URL}/`, { headers: authHeaders() });
    } catch {
        throw new ProcessusError(0); // serveur injoignable
    }

    if (!response.ok) throw new ProcessusError(response.status);

    const data: Page<Processus> = await response.json();
    return data.content;
}

// Méthode pour ajouter un processus
export async function ajouterProcessus(processus: ProcessusRequest): Promise<Processus> {
    let response: Response;
    try {
        response = await fetch(URL, {
            method: "POST",
            headers: authHeaders(),
            body: JSON.stringify(processus),
        });
    } catch {
        throw new ProcessusError(0);
    }

    if (!response.ok) throw new ProcessusError(response.status);

    return await response.json();
}

// Méthode pour supprimer un processus (il est désinstallé de toutes les UC)
export async function supprimerProcessus(id: number): Promise<void> {
    let response: Response;
    try {
        response = await fetch(`${URL}/${id}/`, {
            method: "DELETE",
            headers: authHeaders(),
        });
    } catch {
        throw new ProcessusError(0);
    }

    if (!response.ok) throw new ProcessusError(response.status);
}