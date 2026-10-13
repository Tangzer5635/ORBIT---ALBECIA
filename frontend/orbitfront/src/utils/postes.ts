import type { ComptesAnneau } from "../types/types.ts";

export const ANNEAU_VIDE: ComptesAnneau = { libre: 0, bonEtat: 0, defectueux: 0, aChanger: 0 };

// ordre des segments de l'anneau et de la légende
export const ORDRE_ETATS: (keyof ComptesAnneau)[] = ["bonEtat", "libre", "aChanger", "defectueux"];

export const LIBELLE_ETAT: Record<keyof ComptesAnneau, string> = {
    bonEtat: "Bon état",
    libre: "Libres (en stock)",
    aChanger: "À changer",
    defectueux: "Défectueux",
};

// couleur de chaque état : on réutilise les classes déjà dans index.css
export const CLASSE_ETAT: Record<keyof ComptesAnneau, "use" | "dispo" | "chg" | "def"> = {
    bonEtat: "use",
    libre: "dispo",
    aChanger: "chg",
    defectueux: "def",
};

export function totalMateriels(anneau: ComptesAnneau | undefined) {
    if (!anneau) return 0;
    return ORDRE_ETATS.reduce((t, k) => t + anneau[k], 0);
}

// % de matériel utilisable (tout sauf le défectueux), null si aucun matériel
export function pourcentageUtilisable(anneau: ComptesAnneau | undefined): number | null {
    const total = totalMateriels(anneau);
    if (!anneau || !total) return null;
    return Math.round(((total - anneau.defectueux) / total) * 100);
}

// "0" -> "Rez-de-chaussée", "1" -> "1er étage", "2" -> "2e étage"
export function libelleEtage(etage: string) {
    if (etage === "0") return "Rez-de-chaussée";
    if (etage === "1") return "1er étage";
    return `${etage}e étage`;
}

export function niveau(pct: number): "def" | "chg" | "use" {
    if (pct < 70) return "def";
    if (pct < 90) return "chg";
    return "use";
}