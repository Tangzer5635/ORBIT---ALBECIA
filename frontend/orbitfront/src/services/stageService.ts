import type {Page, Salle, Stage, Utilisateur} from "../types/types.ts";
import {authHeaders} from "./api.ts";

export class StageError extends Error {
    status: number;
    detail: string; // message du back (400 : règle métier violée)

    constructor(status: number, detail = "") {
        super("Erreur stage " + status);
        this.status = status;
        this.detail = detail;
    }
}

async function requete<T>(url: string, method = "GET"): Promise<T> {
    let response: Response;
    try {
        response = await fetch(url, {method, headers: authHeaders()});
    } catch {
        throw new StageError(0); // serveur injoignable
    }

    if (!response.ok) throw new StageError(response.status, await response.text().catch(() => ""));
    return await response.json();
}

export async function getStages(): Promise<Stage[]> {
    return (await requete<Page<Stage>>("/api/v1/stages/?size=200")).content;
}

// Tous les stagiaires, avec leur stageId (pour savoir qui est dans quel stage)
export async function getStagiaires(): Promise<Utilisateur[]> {
    return (await requete<Page<Utilisateur>>("/api/v1/personnels/?role=STAGIAIRE&size=1000")).content;
}

export function ajouterAStage(idStagiaire: number, idStage: number): Promise<Utilisateur> {
    return requete(`/api/v1/personnels/${idStagiaire}/?stageId=${idStage}`, "POST");
}

export function supprimerAStage(idStagiaire: number, idStage: number): Promise<Utilisateur> {
    return requete(`/api/v1/personnels/${idStagiaire}/?stageId=${idStage}`, "DELETE");
}

export function affecterStageASalle(idStage: number, idSalle: number): Promise<Salle> {
    return requete(`/api/v1/salles/${idSalle}/?stageId=${idStage}`, "POST");
}

export function retirerStageDeSalle(idStage: number, idSalle: number): Promise<Salle> {
    return requete(`/api/v1/salles/${idSalle}/?stageId=${idStage}`, "DELETE");
}