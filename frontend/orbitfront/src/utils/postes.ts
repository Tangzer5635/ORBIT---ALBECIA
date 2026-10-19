import type { Batiment, ComptesAnneau, EtatPoste, Poste } from "../types/types.ts";

export const ANNEAU_VIDE: ComptesAnneau = { libre: 0, bonEtat: 0, defectueux: 0, aChanger: 0 };

// ordre des segments de l'anneau et de la légende
export const ORDRE_ETATS: (keyof ComptesAnneau)[] = ["bonEtat", "libre", "aChanger", "defectueux"];

// l'anneau d'une salle compte ses postes (celui d'un bâtiment, les postes de toutes ses salles)
export const LIBELLE_ETAT: Record<keyof ComptesAnneau, string> = {
    bonEtat: "Utilisés",
    libre: "Disponibles",
    aChanger: "À changer",
    defectueux: "Défectueux ou à compléter",
};

// couleur de chaque état : on réutilise les classes déjà dans index.css
export const CLASSE_ETAT: Record<keyof ComptesAnneau, "use" | "dispo" | "chg" | "def"> = {
    bonEtat: "use",
    libre: "dispo",
    aChanger: "chg",
    defectueux: "def",
};

export function totalAnneau(anneau: ComptesAnneau | undefined) {
    if (!anneau) return 0;
    return ORDRE_ETATS.reduce((t, k) => t + anneau[k], 0);
}

// % de matériel utilisable (tout sauf le défectueux), null si aucun matériel
export function pourcentageUtilisable(anneau: ComptesAnneau | undefined): number | null {
    const total = totalAnneau(anneau);
    if (!anneau || !total) return null;
    return Math.round(((total - anneau.defectueux) / total) * 100);
}

// choix d'étage des formulaires : l'index est la valeur envoyée ("0" = rez-de-chaussée)
export const ETAGES = ["Rez-de-chaussée", "1er étage", "2e étage", "3e étage"];

// "0" -> "Rez-de-chaussée", "1" -> "1er étage", "2" -> "2e étage"
export function libelleEtage(etage: string) {
    if (etage === "0") return "Rez-de-chaussée";
    if (etage === "1") return "1er étage";
    return `${etage}e étage`;
}

// comme les salles : les bâtiments les moins disponibles d'abord, ceux sans poste à la fin, puis par numéro
export function batimentsParDisponibilite(a: Batiment, b: Batiment) {
    const pa = pourcentageUtilisable(a.anneau) ?? 101;
    const pb = pourcentageUtilisable(b.anneau) ?? 101;
    return pa - pb || a.numBatiment.localeCompare(b.numBatiment, "fr", { numeric: true });
}

export function niveau(pct: number): "def" | "chg" | "use" {
    if (pct < 70) return "def";
    if (pct < 90) return "chg";
    return "use";
}

// pluriel(3, "salle") -> "3 salles" ; pluriel(2, "unité centrale", "unités centrales") pour les pluriels irréguliers
export const pluriel = (n: number, mot: string, motPluriel = `${mot}s`) => `${n} ${n > 1 ? motPluriel : mot}`;

// "2026-08-07" ou "2026-08-07T10:12:00" -> "07/08/2026" (sans passer par Date : pas de décalage de fuseau)
export const dateFr = (iso: string) => iso.slice(0, 10).split("-").reverse().join("/");

// "B201-S302-F" = poste formateur, "B201-S302-P07" -> 7
export const estFormateur = (p: Poste) => p.numPoste.endsWith("-F");
export const numeroPoste = (p: Poste) => Number(p.numPoste.match(/P(\d+)$/)?.[1] ?? 0);

// "Poste formateur" ou "Poste 07"
export const nomPoste = (p: Poste) =>
    estFormateur(p) ? "Poste formateur" : `Poste ${String(numeroPoste(p)).padStart(2, "0")}`;

// états d'un poste : ordre des tuiles et de la légende, libellé et classe de couleur
export const ETATS_POSTE: { etat: EtatPoste; libelle: string; classe: string }[] = [
    {etat: "DEFECTUEUX", libelle: "Défectueux", classe: "def"},
    {etat: "A_COMPLETER", libelle: "À compléter", classe: "inc"},
    {etat: "A_CHANGER", libelle: "Prévoir changement", classe: "chg"},
    {etat: "UTILISE", libelle: "Utilisé", classe: "use"},
    {etat: "DISPONIBLE", libelle: "Disponible", classe: "dispo"},
];

export const etatPoste = (e?: EtatPoste) => ETATS_POSTE.find(x => x.etat === e);

// "Emma Bernard" -> "Emma B." : tient dans une case du plan (nom complet au survol)
export function nomCourt(nomComplet: string) {
    const [prenom, nom] = nomComplet.split(" ", 2);
    return nom ? `${prenom} ${nom[0]}.` : prenom;
}