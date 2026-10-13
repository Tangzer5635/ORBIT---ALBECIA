import type { Comptes } from "../types/types.ts";

// ordre des segments de l'anneau et de la légende
export const ORDRE_ETATS: (keyof Comptes)[] = ["use", "dispo", "chg", "inc", "def"];

export const LIBELLE_ETAT_POSTE: Record<keyof Comptes, string> = {
    use: "Utilisés",
    dispo: "Disponibles",
    chg: "À changer bientôt",
    inc: "À compléter",
    def: "Défectueux",
};

// comptes absent (ancienne réponse de l'API) = aucun poste, au lieu de planter
export function totalPostes(comptes: Comptes | undefined) {
    if (!comptes) return 0;
    return ORDRE_ETATS.reduce((t, k) => t + comptes[k], 0);
}

// % de postes utilisables (utilisés + disponibles + à changer bientôt), null si aucun poste
export function pourcentageUtilisable(comptes: Comptes | undefined): number | null {
    const total = totalPostes(comptes);
    if (!comptes || !total) return null;
    return Math.round(((comptes.use + comptes.dispo + comptes.chg) / total) * 100);
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