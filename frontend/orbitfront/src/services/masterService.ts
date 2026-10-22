import type {Master, MasterRequest, Page, Salle} from "../types/types.ts";
import type {OperationPatch} from "../utils/patch.ts";
import {apiFetch} from "./api.ts";

const URL = `/api/v1/masters`;

export class MasterError extends Error {
    status: number;
    detail: string; // message du back (400 : règle métier violée)

    constructor(status: number, detail = "") {
        super("Erreur master " + status);
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
        throw new MasterError(0); // serveur injoignable
    }

    if (!response.ok) throw new MasterError(response.status, await response.text().catch(() => ""));
    return response.status === 204 ? (undefined as T) : await response.json();
}

export async function getMasters(): Promise<Master[]> {
    const page = await requete<Page<Master>>(`${URL}/?size=200&sort=nom`);
    return page.content;
}

export function creerMaster(dto: MasterRequest): Promise<Master> {
    return requete(`${URL}/`, {method: "POST", body: JSON.stringify(dto)});
}

// JSON Patch des seuls champs changés (nom, version, processus) ; les salles où il est plaqué suivent
export function modifierMaster(id: number, operations: OperationPatch[]): Promise<Master> {
    return requete(`${URL}/${id}/`, {method: "PATCH", body: JSON.stringify(operations)});
}

// Les salles où il est plaqué n'ont plus de master ni de processus
export function supprimerMaster(id: number): Promise<void> {
    return requete(`${URL}/${id}/`, {method: "DELETE"});
}

// Les UC de la salle reçoivent exactement les processus du master
export function plaquerMaster(idSalle: number, idMaster: number): Promise<Salle> {
    return requete(`/api/v1/salles/${idSalle}/?masterId=${idMaster}`, {method: "POST"});
}

// Un seul poste (ex. poste en erreur) : son UC reçoit exactement les processus du master
export function plaquerMasterSurPoste(idPoste: number, idMaster: number): Promise<void> {
    return requete(`/api/v1/postes/${idPoste}/?masterId=${idMaster}`, {method: "POST"});
}