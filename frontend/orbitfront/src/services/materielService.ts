import type {AffectationMateriel, CalendrierGaranties, EtatMateriel, Materiel, MaterielRequest, MaterielStock, Page, TypeMateriel} from "../types/types.ts";
import type {OperationPatch} from "../utils/patch.ts";
import {apiFetch, authHeaders} from "./api.ts";
import type {Localisation} from "../utils/materiels.ts";

const URL = `/api/v1/materiels`;

export class MaterielError extends Error {
    status: number;
    detail: string; // message du back (400 : règle métier violée)

    constructor(status: number, detail = "") {
        super("Erreur materiel " + status);
        this.status = status;
        this.detail = detail;
    }
}

// Filtres acceptés par GET /materiels/ (tous facultatifs)
export interface FiltresMateriel {
    filter?: string;               // filtre RSQL, ex. modele=ilike="dell"
    type?: TypeMateriel | null;    // UC, Ecran... (filtré par le back sur la classe)
    etat?: EtatMateriel | null;    // état calculé par le back
    localisation?: Localisation;
}

// Où se trouve le matériel affecté : posé dans une salle ou sur un poste
export type Emplacement = { salleId: number } | { posteId: number };

const param = (e: Emplacement) => "salleId" in e ? `salleId=${e.salleId}` : `posteId=${e.posteId}`;

async function requete<T>(url: string, init: RequestInit = {}): Promise<T> {
    let response: Response;
    try {
        response = await apiFetch(url, {...init, headers: authHeaders()});
    } catch {
        throw new MaterielError(0); // serveur injoignable
    }

    if (!response.ok) throw new MaterielError(response.status, await response.text().catch(() => ""));
    return response.status === 204 ? (undefined as T) : await response.json();
}

// Méthode pour récupérer une page de matériels filtrés
export async function getMateriels(filtres: FiltresMateriel, page = 0, size = 20): Promise<Page<Materiel>> {
    // URLSearchParams encode les caractères spéciaux du filtre RSQL ( & ; = espace ...)
    const params = new URLSearchParams({ page: String(page), size: String(size), sort: "id" });
    if (filtres.filter) params.set("filter", filtres.filter);
    if (filtres.type) params.set("type", filtres.type);
    if (filtres.etat) params.set("etat", filtres.etat);
    if (filtres.localisation) params.set("localisation", filtres.localisation);

    return requete(`${URL}/?${params}`);
}

// Méthode pour compter les matériels d'un filtre, on lit totalElements
export async function compterMateriels(filtres: FiltresMateriel): Promise<number> {
    return (await getMateriels(filtres, 0, 1)).totalElements;
}

// Tout le stock (tous bâtiments) pour « Affecter un matériel » : une seule requête côté back
export function getStock(): Promise<MaterielStock[]> {
    return requete(`${URL}/stock/`);
}

// Méthode pour récupérer un matériel par son id
export function getMaterielById(id: number): Promise<Materiel> {
    return requete(`${URL}/${id}/`);
}

// Méthode pour ajouter un materiel
export function ajouterMateriel(materiel: MaterielRequest): Promise<Materiel> {
    return requete(`${URL}/`, {method: "POST", body: JSON.stringify(materiel)});
}

// Méthode pour modifier un matériel : JSON Patch des seuls champs changés
// (modèle, dates, nom machine, adresse MAC ; ni le type ni le n° de série ne changent)
export function modifierMateriel(id: number, operations: OperationPatch[]): Promise<Materiel> {
    return requete(`${URL}/${id}/`, {method: "PATCH", body: JSON.stringify(operations)});
}

// Méthode pour supprimer un materiel (s'il est affecté, le back le sort d'abord de sa salle ou de son poste)
export function supprimerMateriel(id: number): Promise<void> {
    return requete(`${URL}/${id}/`, {method: "DELETE"});
}

// Affecte un matériel du stock à une salle de cours ou à un poste
export function affecterMateriel(id: number, vers: Emplacement): Promise<Materiel> {
    return requete(`${URL}/${id}/?${param(vers)}`, {method: "POST"});
}

// Archive un matériel en stock (il n'est plus utilisable ni modifiable, mais reste consultable)
export function archiverMateriel(id: number): Promise<Materiel> {
    return requete(`${URL}/${id}/archiver/`, {method: "PATCH"});
}

// Archive un matériel depuis sa fiche garantie : s'il est encore affecté, le back le remet d'abord en stock
export function retirerEtArchiverMateriel(id: number): Promise<Materiel> {
    return requete(`${URL}/${id}/archiver/?retirer=true`, {method: "PATCH"});
}

// Remet en stock un matériel posé dans une salle ou sur un poste
export function remettreEnStock(id: number, depuis: Emplacement): Promise<Materiel> {
    return requete(`${URL}/${id}/?${param(depuis)}`, {method: "DELETE"});
}

// Changer d'emplacement : déplace un matériel affecté vers une autre salle ou un autre poste
export function deplacerMateriel(id: number, vers: Emplacement): Promise<Materiel> {
    return requete(`${URL}/${id}/?${param(vers)}`, {method: "PATCH"});
}

export async function getCalendrierGaranties(annee: number): Promise<CalendrierGaranties> {
    let response: Response;
    try {
        //on aura toujorus une année
        response = await apiFetch(`${URL}/garanties/?annee=${annee}`, { headers: authHeaders() });
    } catch {
        throw new MaterielError(0); // serveur injoignable
    }

    if (!response.ok) throw new MaterielError(response.status);

    return await response.json();
}

// Matériel en service dont la garantie se termine entre du (inclus) et au (exclu), la fin la plus proche d'abord.
// Sans "du" : tout ce qui est déjà hors garantie avant "au".
export async function getMaterielsFinGarantie(du: string | null, au: string): Promise<Materiel[]> {
    const filtre = [du && `dateFinGarantie=ge=${du}`, `dateFinGarantie=lt=${au}`, "archive==false"].filter(Boolean).join(";");
    const params = new URLSearchParams({filter: filtre, page: "0", size: "500", sort: "dateFinGarantie,asc"});
    return (await requete<Page<Materiel>>(`${URL}/?${params}`)).content;
}

// Où se trouve le matériel : salle, bâtiment, poste et son occupant, stage, responsable
export function getAffectationMateriel(id: number): Promise<AffectationMateriel> {
    return requete(`${URL}/${id}/affectation/`);
}