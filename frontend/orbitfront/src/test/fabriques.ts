// Données de test : un objet valide par défaut, on ne surcharge que ce qui compte pour le test
import {vi} from "vitest";
import type {Batiment, ComptesAnneau, Journalisation, Materiel, Page, Poste, Stage, Utilisateur} from "../types/types.ts";

export const unUtilisateur = (s: Partial<Utilisateur> = {}): Utilisateur => ({
    id: 1, login: "jdupont", role: "GESTIONNAIRE", prenom: "Jean", nom: "Dupont",
    active: true, verrouille: false, ...s,
});

export const unMateriel = (s: Partial<Materiel> = {}): Materiel => ({
    id: 1, numSerie: "SN-001", modele: "Dell OptiPlex 7010", type: "UC", etat: "NORMAL",
    dateAcquisition: "2024-01-01", dateFinGarantie: "2027-01-01", dateArchivage: null,
    numSalle: "S101", typeSalle: "SalleDeCours", ...s,
});

export const unPoste = (s: Partial<Poste> = {}): Poste => ({
    id: 1, numPoste: "B201-S302-P07", occupantId: null, occupant: null,
    materielsId: [], materielsParType: {}, ...s,
});

export const unStage = (s: Partial<Stage> = {}): Stage => ({
    id: 1, nom: "CDA 2026", type: "PDI", dateDebut: "2026-09-01", dateFin: "2026-12-15",
    formateurId: 2, formateur: "Marie Curie", ...s,
});

export const uneLigneJournal = (s: Partial<Journalisation> = {}): Journalisation => ({
    id: 1, dateHeure: "2026-10-17T20:15:02.123456", auteurId: 3, action: "CONNEXION", detail: "OK", ...s,
});

export const unAnneau = (s: Partial<ComptesAnneau> = {}): ComptesAnneau => ({
    libre: 0, bonEtat: 0, defectueux: 0, aChanger: 0, ...s,
});

export const unBatiment = (s: Partial<Batiment> = {}): Batiment => ({
    id: 1, numBatiment: "B201", nbSalles: 0, stockages: [], anneau: unAnneau(), ...s,
});

export function unePage<T>(content: T[], s: Partial<Page<T>> = {}): Page<T> {
    return {content, page: 0, size: 20, totalElements: content.length, totalPages: 1, first: true, last: true, ...s};
}

// Fausse réponse du back : un objet est envoyé en JSON, une chaîne en texte brut
export function reponse(status: number, corps?: unknown): Response {
    if (corps === undefined) return new Response(null, {status});
    if (typeof corps === "string") return new Response(corps, {status});
    return new Response(JSON.stringify(corps), {status, headers: {"Content-Type": "application/json"}});
}

// Remplace fetch : chaque appel renvoie la réponse suivante (une Error simule un serveur injoignable)
export function simulerFetch(...reponses: (Response | Error)[]) {
    const mock = vi.fn<typeof fetch>();
    for (const r of reponses) {
        if (r instanceof Error) mock.mockRejectedValueOnce(r);
        else mock.mockResolvedValueOnce(r);
    }
    vi.stubGlobal("fetch", mock);
    return mock;
}

// URL et options du n-ième appel à fetch
export function appel(mock: ReturnType<typeof simulerFetch>, n = 0) {
    const [url, init = {}] = mock.mock.calls[n];
    return {url: String(url), init, headers: (init.headers ?? {}) as Record<string, string>};
}

// Connecte un utilisateur comme le fait login() (même clés de sessionStorage)
export function connecter(utilisateur: Utilisateur = unUtilisateur(), token = "jeton-test") {
    sessionStorage.setItem("orbit-token", token);
    sessionStorage.setItem("orbit-user", JSON.stringify(utilisateur));
}
